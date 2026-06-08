package com.duren.app.data.settings

import com.duren.app.core.DomainError
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Writes the settings/customization fields onto the signed-in user's profile doc.
 * Reads happen via [com.duren.app.data.profile.ProfileRepository.observeProfile];
 * this repo only handles owner-scoped partial updates.
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    suspend fun updateAccentColor(hex: String): Result<Unit> = update("accentColor" to hex)

    suspend fun updateLightMode(enabled: Boolean): Result<Unit> =
        update("lightModeEnabled" to enabled)

    suspend fun updateAvatarColor(hex: String): Result<Unit> = update("avatarColor" to hex)

    /** Store a custom avatar image (a `data:` URI) on the profile. */
    suspend fun updateAvatarUrl(avatarUrl: String): Result<Unit> = update("avatarUrl" to avatarUrl)

    suspend fun updatePrivacy(
        showLantern: Boolean,
        showMoodCanvas: Boolean,
        allowAnonBox: Boolean,
        showTestimonials: Boolean
    ): Result<Unit> = update(
        "showLantern" to showLantern,
        "showMoodCanvas" to showMoodCanvas,
        "allowAnonBox" to allowAnonBox,
        "showTestimonials" to showTestimonials
    )

    suspend fun updateAccount(
        displayName: String,
        bio: String,
        pronouns: String,
        signature: String
    ): Result<Unit> = update(
        "displayName" to displayName.trim(),
        "bio" to bio.trim(),
        "pronouns" to pronouns.trim(),
        "signature" to signature.trim().take(30)
    )

    /**
     * Set or clear an AIM-style banked (away) status.
     * Pass note="" and untilMillis=0 to clear.
     */
    suspend fun updateBankedStatus(note: String, untilMillis: Long): Result<Unit> = update(
        "bankedStatus" to note.trim().take(80),
        "bankedUntil" to untilMillis
    )

    private suspend fun update(vararg fields: Pair<String, Any>): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(DomainError.Unknown)
        return try {
            firestore.collection(PROFILES).document(uid).update(fields.toMap()).await()
            Result.success(Unit)
        } catch (_: Exception) {
            Result.failure(DomainError.Unknown)
        }
    }

    companion object {
        private const val PROFILES = "profiles"
    }
}
