package com.duren.app.data.profile

import com.duren.app.data.profile.model.Profile
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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

    companion object {
        const val PROFILES = "profiles"
    }
}
