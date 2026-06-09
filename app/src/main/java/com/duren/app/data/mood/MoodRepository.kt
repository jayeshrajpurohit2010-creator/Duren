package com.duren.app.data.mood

import com.duren.app.data.mood.model.Mood
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mood Canvas (Feature 12) — one quiet check-in a day, stored at
 * `moodCanvas/{uid}/days/{yyyy-MM-dd}`. It paints a coloured aura around the avatar
 * (opt-in via the profile's showMoodCanvas). Fully client-side, free.
 *
 * minSdk 24 ⇒ [SimpleDateFormat], not java.time. The date key uses the device's
 * local day, which is what "tonight's mood" means to the person setting it.
 */
@Singleton
class MoodRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun daysRef(uid: String) =
        firestore.collection(MOOD).document(uid).collection(DAYS)

    /** Today's mood for [uid], or null. Live, so a fresh check-in re-tints instantly. */
    fun observeToday(uid: String): Flow<Mood?> {
        if (uid.isBlank()) return flowOf(null)
        return callbackFlow {
            val reg = daysRef(uid).document(today()).addSnapshotListener { snap, _ ->
                val mood = if (snap != null && snap.exists()) {
                    Mood(
                        mood = (snap.getLong("mood") ?: 0L).toInt(),
                        emoji = snap.getString("emoji") ?: "",
                        note = snap.getString("note") ?: "",
                        createdAt = snap.getTimestamp("createdAt")
                    )
                } else null
                trySend(mood)
            }
            awaitClose { reg.remove() }
        }
    }

    /** Set (or overwrite) today's mood for the signed-in user. */
    suspend fun setToday(mood: Int, emoji: String = "", note: String = ""): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("not signed in"))
        return runCatching {
            daysRef(uid).document(today()).set(
                mapOf(
                    "mood" to mood,
                    "emoji" to emoji,
                    "note" to note.take(140),
                    "createdAt" to Timestamp.now()
                )
            ).await()
        }
    }

    companion object {
        const val MOOD = "moodCanvas"
        const val DAYS = "days"
    }
}
