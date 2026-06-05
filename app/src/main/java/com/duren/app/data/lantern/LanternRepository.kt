package com.duren.app.data.lantern

import com.duren.app.core.DomainError
import com.duren.app.data.lantern.model.Lantern
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lanterns — short anonymous 48h messages set adrift in the Nest.
 *
 * Phase 1 stays client-driven (no Cloud Functions): the discover feed is
 * newest-first, expiry filtered on-device exactly like embers, and the
 * author's own lanterns are filtered out so they only "wander" past others'.
 * Author identity is never exposed in the UI — only authorId (for ownership
 * checks) lives on the document.
 */
@Singleton
class LanternRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    /** Light a new lantern and set it adrift. Returns the new lantern id. */
    suspend fun lightLantern(text: String): Result<String> {
        val user = auth.currentUser ?: return Result.failure(DomainError.NotAuthenticated)
        val trimmed = text.trim()
        if (trimmed.isEmpty() || trimmed.length > MAX_TEXT) {
            return Result.failure(DomainError.EmptyEmber)
        }
        return try {
            val now = Timestamp.now()
            val expiresAt = Timestamp(now.seconds + LIFESPAN_SECONDS, 0)
            val ref = firestore.collection(LANTERNS).document()
            ref.set(
                mapOf(
                    "authorId" to user.uid,
                    "text" to trimmed,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "expiresAt" to expiresAt,
                    "foundCount" to 0
                )
            ).await()
            Result.success(ref.id)
        } catch (_: Exception) {
            Result.failure(DomainError.Unknown)
        }
    }

    /** Wandering: newest non-expired lanterns lit by *other* people. */
    fun observeDiscoverable(limit: Long): Flow<List<Lantern>> {
        val uid = auth.currentUser?.uid
        return lanternQuery(
            firestore.collection(LANTERNS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
        ) { lantern -> lantern.authorId != uid }
    }

    /** Yours: the current user's own lanterns, newest first. */
    fun observeMine(limit: Long): Flow<List<Lantern>> {
        val uid = auth.currentUser?.uid
        return lanternQuery(
            firestore.collection(LANTERNS)
                .whereEqualTo("authorId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
        ) { true }
    }

    private fun lanternQuery(
        query: Query,
        extraFilter: (Lantern) -> Boolean
    ): Flow<List<Lantern>> = callbackFlow {
        val reg = query.addSnapshotListener { snap, _ ->
            val now = Timestamp.now()
            val list = snap?.documents
                ?.mapNotNull { it.toLantern() }
                ?.filter { l -> l.expiresAt == null || l.expiresAt > now }
                ?.filter(extraFilter)
                ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    private fun DocumentSnapshot.toLantern(): Lantern? {
        val authorId = getString("authorId") ?: return null
        return Lantern(
            id = id,
            authorId = authorId,
            text = getString("text") ?: "",
            createdAt = getTimestamp("createdAt"),
            expiresAt = getTimestamp("expiresAt"),
            foundCount = (getLong("foundCount") ?: 0L).toInt()
        )
    }

    companion object {
        const val LANTERNS = "lanterns"
        const val MAX_TEXT = 280
        const val LIFESPAN_SECONDS = 48L * 3600 // 48h
    }
}
