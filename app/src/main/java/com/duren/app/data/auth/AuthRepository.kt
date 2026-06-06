package com.duren.app.data.auth

import com.duren.app.core.DomainError
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    val currentUser: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun isUsernameAvailable(username: String): Boolean {
        val normalized = username.trim().lowercase()
        if (!isValidUsernameFormat(normalized)) return false
        return try {
            val doc = firestore.collection(USERNAMES).document(normalized).get().await()
            !doc.exists()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun signUp(
        email: String,
        password: String,
        username: String,
        displayName: String
    ): Result<FirebaseUser> {
        val normalizedEmail = email.trim().lowercase()
        val normalizedUsername = username.trim().lowercase()
        val trimmedDisplay = displayName.trim()

        if (!isValidUsernameFormat(normalizedUsername)) {
            return Result.failure(DomainError.UsernameTaken)
        }
        if (!isStrongPassword(password)) {
            return Result.failure(DomainError.WeakPassword)
        }

        return try {
            val result = auth.createUserWithEmailAndPassword(normalizedEmail, password).await()
            val user = result.user ?: return Result.failure(DomainError.Unknown)

            val profileRef = firestore.collection(PROFILES).document(user.uid)
            val usernameRef = firestore.collection(USERNAMES).document(normalizedUsername)

            try {
                firestore.runBatch { batch ->
                    batch.set(
                        profileRef,
                        mapOf(
                            "username" to normalizedUsername,
                            "displayName" to trimmedDisplay.ifBlank { normalizedUsername },
                            "email" to normalizedEmail,
                            "bio" to "",
                            "pronouns" to "",
                            "avatarUrl" to dicebearUrl(user.uid),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "lastSeen" to FieldValue.serverTimestamp(),
                            // Settings & customization defaults (June 4 §3.3)
                            "accentColor" to "#2dd4bf",
                            "lightModeEnabled" to false,
                            "avatarColor" to "#FF6B35",
                            "showLantern" to true,
                            "showMoodCanvas" to false,
                            "allowAnonBox" to true,
                            "showTestimonials" to false
                        )
                    )
                    batch.set(usernameRef, mapOf("uid" to user.uid))
                }.await()
            } catch (e: Exception) {
                // Rollback: username already taken (rule rejected sentinel write) or any other failure.
                runCatching { user.delete().await() }
                return Result.failure(DomainError.UsernameTaken)
            }

            Result.success(user)
        } catch (e: FirebaseAuthWeakPasswordException) {
            Result.failure(DomainError.WeakPassword)
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(DomainError.InvalidCredentials)
        } catch (e: Exception) {
            Result.failure(DomainError.Unknown)
        }
    }

    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email.trim().lowercase(), password).await()
            val user = result.user ?: return Result.failure(DomainError.InvalidCredentials)
            // Touch lastSeen — fire-and-forget, ignore failures.
            runCatching {
                firestore.collection(PROFILES).document(user.uid)
                    .update("lastSeen", FieldValue.serverTimestamp()).await()
            }
            Result.success(user)
        } catch (_: FirebaseAuthInvalidCredentialsException) {
            Result.failure(DomainError.InvalidCredentials)
        } catch (_: FirebaseAuthInvalidUserException) {
            Result.failure(DomainError.InvalidCredentials)
        } catch (_: Exception) {
            Result.failure(DomainError.Unknown)
        }
    }

    /**
     * Anti-enumeration: always returns success after an 800ms delay, regardless of whether
     * the email exists. Matches the pattern from Security & Privacy v1.0 Part 3.
     */
    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email.trim().lowercase()).await()
            Result.success(Unit)
        } catch (_: FirebaseAuthInvalidUserException) {
            delay(800)
            Result.success(Unit)
        } catch (_: Exception) {
            Result.failure(DomainError.Unknown)
        }
    }

    /**
     * Permanently delete the signed-in user. Re-authenticates with the password first so the
     * delete never hits a "recent login required" error, then removes the Firestore footprint
     * (profile doc + username sentinel) while still authed, and finally deletes the auth user.
     */
    suspend fun deleteAccount(password: String): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(DomainError.NotAuthenticated)
        val email = user.email ?: return Result.failure(DomainError.Unknown)
        return try {
            user.reauthenticate(EmailAuthProvider.getCredential(email, password)).await()

            val uid = user.uid
            val profileRef = firestore.collection(PROFILES).document(uid)
            val username = runCatching { profileRef.get().await().getString("username") }.getOrNull()
            runCatching { profileRef.delete().await() }
            if (!username.isNullOrBlank()) {
                runCatching { firestore.collection(USERNAMES).document(username).delete().await() }
            }

            user.delete().await()
            Result.success(Unit)
        } catch (_: FirebaseAuthInvalidCredentialsException) {
            Result.failure(DomainError.InvalidCredentials)
        } catch (_: FirebaseAuthRecentLoginRequiredException) {
            Result.failure(DomainError.InvalidCredentials)
        } catch (_: Exception) {
            Result.failure(DomainError.Unknown)
        }
    }

    fun signOut() {
        auth.signOut()
    }

    private fun isValidUsernameFormat(value: String): Boolean =
        value.length in 3..30 && value.matches(Regex("^[a-z0-9_]+$"))

    private fun isStrongPassword(value: String): Boolean =
        value.length >= 8 && value.any { it.isDigit() } && value.any { it.isLetter() }

    private fun dicebearUrl(seed: String): String =
        "https://api.dicebear.com/7.x/identicon/png?seed=$seed"

    companion object {
        const val PROFILES = "profiles"
        const val USERNAMES = "usernames"
    }
}
