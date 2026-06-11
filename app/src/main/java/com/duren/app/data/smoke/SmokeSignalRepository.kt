package com.duren.app.data.smoke

import com.duren.app.data.signal.SignalRepository
import com.duren.app.data.signal.model.SignalType
import com.duren.app.data.smoke.model.SmokeOutcome
import com.duren.app.data.smoke.model.SmokeSignal
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Smoke Signals (Feature 30) — one post that reaches everyone in your Nest at once.
 * Rationed to one a week (checked against your own last signal, on-device — single
 * equality filter, no composite index), and each one burns out after seven days.
 *
 * The recipient list is frozen into `sentTo` at send time, so the read rule and the
 * `array-contains` query agree forever, even if the Nest changes afterwards.
 */
@Singleton
class SmokeSignalRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val signalRepository: SignalRepository
) {

    /** Signals sent up for me — unexpired, newest first. */
    fun observeIncoming(): Flow<List<SmokeSignal>> {
        val uid = auth.currentUser?.uid ?: return flowOf(emptyList())
        return callbackFlow {
            val reg = firestore.collection(SMOKE_SIGNALS)
                .whereArrayContains("sentTo", uid)
                .addSnapshotListener { snap, _ ->
                    val now = Timestamp.now()
                    val list = snap?.documents?.map { doc ->
                        SmokeSignal(
                            id = doc.id,
                            fromUserId = doc.getString("fromUserId") ?: "",
                            fromName = doc.getString("fromName") ?: "",
                            text = doc.getString("text") ?: "",
                            createdAt = doc.getTimestamp("createdAt"),
                            expiresAt = doc.getTimestamp("expiresAt")
                        )
                    }?.filter { s -> s.expiresAt?.let { it > now } ?: true }
                        ?.sortedByDescending { it.createdAt?.seconds ?: 0L }
                        ?: emptyList()
                    trySend(list)
                }
            awaitClose { reg.remove() }
        }
    }

    /** Send one up. Enforces the 1/week ration before writing anything. */
    suspend fun send(text: String): SmokeOutcome {
        val uid = auth.currentUser?.uid ?: return SmokeOutcome.Failed
        val trimmed = text.trim().take(MAX_CHARS)
        if (trimmed.isEmpty()) return SmokeOutcome.Failed
        return try {
            // The week gate: my most recent signal, found on-device.
            val mine = firestore.collection(SMOKE_SIGNALS)
                .whereEqualTo("fromUserId", uid)
                .get().await()
            val lastAt = mine.documents
                .mapNotNull { it.getTimestamp("createdAt")?.seconds }
                .maxOrNull()
            val now = Timestamp.now()
            if (lastAt != null && now.seconds - lastAt < WEEK_SECONDS) {
                return SmokeOutcome.OncePerWeek
            }

            val memberIds = firestore.collection("userNests").document(uid)
                .collection("members").get().await().documents.map { it.id }
            if (memberIds.isEmpty()) return SmokeOutcome.EmptyNest

            val myName = firestore.collection("profiles").document(uid).get().await()
                .getString("displayName") ?: ""
            firestore.collection(SMOKE_SIGNALS).document().set(
                mapOf(
                    "fromUserId" to uid,
                    "fromName" to myName,
                    "text" to trimmed,
                    "sentTo" to memberIds,
                    "createdAt" to now,
                    "expiresAt" to Timestamp(now.seconds + LIFESPAN_SECONDS, 0)
                )
            ).await()
            // A Signal per nest member, so the smoke is noticed even off the feed.
            memberIds.forEach { member ->
                runCatching { signalRepository.notify(member, SignalType.SmokeSignal) }
            }
            SmokeOutcome.Sent
        } catch (_: Exception) {
            SmokeOutcome.Failed
        }
    }

    companion object {
        const val SMOKE_SIGNALS = "smokeSignals"
        const val MAX_CHARS = 280
        const val WEEK_SECONDS = 7L * 24 * 3600
        const val LIFESPAN_SECONDS = 7L * 24 * 3600
    }
}
