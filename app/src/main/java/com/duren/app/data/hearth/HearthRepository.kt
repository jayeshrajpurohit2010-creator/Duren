package com.duren.app.data.hearth

import com.duren.app.core.DomainError
import com.duren.app.data.hearth.model.HearthEmber
import com.duren.app.data.signal.SignalRepository
import com.duren.app.data.signal.model.SignalType
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Hearth (Feature 26) — private 24h postcards between Nest members. Only the
 * owner can read their own hearth (rules-enforced); senders fire and forget. The
 * Nest gate is the same `exists()` rules check as Testimonials.
 */
@Singleton
class HearthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val signalRepository: SignalRepository
) {

    /** My own hearth — what's still warm tonight, newest first. */
    fun observeMine(): Flow<List<HearthEmber>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val reg = firestore.collection(HEARTHS).document(uid).collection(EMBERS)
            .addSnapshotListener { snap, _ ->
                val now = Timestamp.now()
                val list = snap?.documents?.map { doc ->
                    HearthEmber(
                        id = doc.id,
                        text = doc.getString("text") ?: "",
                        senderId = doc.getString("senderId") ?: "",
                        senderName = doc.getString("senderName") ?: "",
                        createdAt = doc.getTimestamp("createdAt"),
                        expiresAt = doc.getTimestamp("expiresAt")
                    )
                }?.filter { h -> h.expiresAt?.let { it > now } ?: true }
                    ?.sortedByDescending { it.createdAt?.seconds ?: 0L }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    /** Warm a Nest member's hearth. They get a Signal; only they ever see the words. */
    suspend fun send(toUserId: String, text: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(DomainError.NotAuthenticated)
        val trimmed = text.trim().take(MAX_CHARS)
        if (trimmed.isEmpty()) return Result.failure(DomainError.EmptyEmber)
        if (uid == toUserId) return Result.failure(DomainError.Unknown)
        return try {
            val myName = firestore.collection("profiles").document(uid).get().await()
                .getString("displayName") ?: ""
            val now = Timestamp.now()
            firestore.collection(HEARTHS).document(toUserId).collection(EMBERS)
                .document().set(
                    mapOf(
                        "text" to trimmed,
                        "senderId" to uid,
                        "senderName" to myName,
                        "createdAt" to now,
                        "expiresAt" to Timestamp(now.seconds + LIFESPAN_SECONDS, 0)
                    )
                ).await()
            signalRepository.notify(toUserId, SignalType.Hearth)
            Result.success(Unit)
        } catch (_: Exception) {
            Result.failure(DomainError.Unknown)
        }
    }

    companion object {
        const val HEARTHS = "hearths"
        const val EMBERS = "embers"
        const val MAX_CHARS = 280
        const val LIFESPAN_SECONDS = 24L * 3600    // one day, like a real fire
    }
}
