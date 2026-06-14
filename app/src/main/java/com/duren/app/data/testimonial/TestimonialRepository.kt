package com.duren.app.data.testimonial

import com.duren.app.core.DomainError
import com.duren.app.data.signal.SignalRepository
import com.duren.app.data.signal.model.SignalType
import com.duren.app.data.testimonial.model.Testimonial
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
 * Testimonials (Feature 27) — 150-char notes Nest members leave on each other's
 * presence, 30 days each. The Nest-membership gate lives in the Firestore rules
 * (an `exists()` check on userNests/{owner}/members/{writer}); the client just
 * doesn't offer the button to strangers.
 */
@Singleton
class TestimonialRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val signalRepository: SignalRepository
) {

    /** Live testimonials on [userId]'s presence — unexpired only, newest first. */
    fun observeFor(userId: String): Flow<List<Testimonial>> = callbackFlow {
        val reg = firestore.collection(TESTIMONIALS).document(userId).collection(ITEMS)
            .addSnapshotListener { snap, _ ->
                val now = Timestamp.now()
                val list = snap?.documents?.map { doc ->
                    Testimonial(
                        id = doc.id,
                        text = doc.getString("text") ?: "",
                        authorId = doc.getString("authorId") ?: "",
                        authorName = doc.getString("authorName") ?: "",
                        createdAt = doc.getTimestamp("createdAt"),
                        expiresAt = doc.getTimestamp("expiresAt")
                    )
                }?.filter { t -> t.expiresAt?.let { it > now } ?: true }
                    ?.sortedByDescending { it.createdAt?.seconds ?: 0L }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    /** Leave a testimonial on a Nest member's presence. One Signal rides along. */
    suspend fun write(toUserId: String, text: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(DomainError.NotAuthenticated)
        val trimmed = text.trim().take(MAX_CHARS)
        if (trimmed.isEmpty()) return Result.failure(DomainError.EmptyEmber)
        if (uid == toUserId) return Result.failure(DomainError.Unknown)
        return try {
            val myName = firestore.collection("profiles").document(uid).get().await()
                .getString("displayName") ?: ""
            val now = Timestamp.now()
            firestore.collection(TESTIMONIALS).document(toUserId).collection(ITEMS)
                .document().set(
                    mapOf(
                        "text" to trimmed,
                        "authorId" to uid,
                        "authorName" to myName,
                        "createdAt" to now,
                        "expiresAt" to Timestamp(now.seconds + LIFESPAN_SECONDS, 0)
                    )
                ).await()
            signalRepository.notify(toUserId, SignalType.Testimonial)
            Result.success(Unit)
        } catch (_: Exception) {
            Result.failure(DomainError.Unknown)
        }
    }

    /** Take a testimonial down — the rules allow the profile owner or the author. */
    suspend fun delete(ownerUserId: String, testimonialId: String): Result<Unit> = try {
        firestore.collection(TESTIMONIALS).document(ownerUserId).collection(ITEMS)
            .document(testimonialId).delete().await()
        Result.success(Unit)
    } catch (_: Exception) {
        Result.failure(DomainError.Unknown)
    }

    companion object {
        const val TESTIMONIALS = "testimonials"
        const val ITEMS = "items"
        const val MAX_CHARS = 150
        const val LIFESPAN_SECONDS = 30L * 24 * 3600    // 30 days
    }
}
