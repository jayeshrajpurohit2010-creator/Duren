package com.duren.app.data.profile

import com.duren.app.data.profile.model.Profile
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    fun observeProfile(uid: String): Flow<Profile?> = callbackFlow {
        val registration = firestore.collection(PROFILES).document(uid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val data = snapshot.data ?: emptyMap()
                    trySend(
                        Profile(
                            uid = uid,
                            username = data["username"] as? String ?: "",
                            displayName = data["displayName"] as? String ?: "",
                            email = data["email"] as? String ?: "",
                            bio = data["bio"] as? String ?: "",
                            pronouns = data["pronouns"] as? String ?: "",
                            avatarUrl = data["avatarUrl"] as? String ?: "",
                            accentColor = data["accentColor"] as? String ?: "#2dd4bf",
                            lightModeEnabled = data["lightModeEnabled"] as? Boolean ?: false,
                            avatarColor = data["avatarColor"] as? String ?: "#FF6B35",
                            showLantern = data["showLantern"] as? Boolean ?: true,
                            showMoodCanvas = data["showMoodCanvas"] as? Boolean ?: false,
                            allowAnonBox = data["allowAnonBox"] as? Boolean ?: true,
                            showTestimonials = data["showTestimonials"] as? Boolean ?: false
                        )
                    )
                } else {
                    trySend(null)
                }
            }
        awaitClose { registration.remove() }
    }

    /**
     * Find people by a username or display-name prefix. Two single-field range
     * queries (username + displayName) merged and de-duped, so it needs zero
     * Firestore composite indexes and works under the already-live read rules.
     * Username is matched case-insensitively (usernames are stored lowercase).
     */
    suspend fun searchPeople(rawQuery: String, limit: Long = 20): List<Profile> {
        val q = rawQuery.trim()
        if (q.isEmpty()) return emptyList()
        val lower = q.lowercase()
        return try {
            val byUsername = firestore.collection(PROFILES)
                .orderBy("username")
                .startAt(lower)
                .endAt(lower + "")
                .limit(limit)
                .get().await()
            val byName = firestore.collection(PROFILES)
                .orderBy("displayName")
                .startAt(q)
                .endAt(q + "")
                .limit(limit)
                .get().await()
            (byUsername.documents + byName.documents)
                .mapNotNull { it.toProfile() }
                .distinctBy { it.uid }
                .take(limit.toInt())
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** One-shot profile read, for hydrating lists of uids (Nest members, requests). */
    suspend fun getProfile(uid: String): Profile? = try {
        firestore.collection(PROFILES).document(uid).get().await().toProfile()
    } catch (_: Exception) {
        null
    }

    private fun DocumentSnapshot.toProfile(): Profile? {
        val username = getString("username") ?: return null
        return Profile(
            uid = id,
            username = username,
            displayName = getString("displayName") ?: "",
            email = "",
            bio = getString("bio") ?: "",
            pronouns = getString("pronouns") ?: "",
            avatarUrl = getString("avatarUrl") ?: "",
            avatarColor = getString("avatarColor") ?: "#FF6B35"
        )
    }

    companion object {
        const val PROFILES = "profiles"
    }
}
