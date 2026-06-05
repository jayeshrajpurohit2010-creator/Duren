package com.duren.app.data.ember

import android.net.Uri
import com.duren.app.core.DomainError
import com.duren.app.data.ember.model.Ember
import com.duren.app.data.ember.model.PostMode
import com.duren.app.data.media.MediaUploadRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
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
 * Embers — 48h posts — plus echoes and cold marks.
 *
 * Phase 1 keeps everything client-driven (no Cloud Functions until Phase 4):
 *  - The feed is the global "Clearing": newest-first, expiry filtered on-device,
 *    realtime via an increasing limit (simple infinite scroll).
 *  - Echo is a toggle written in a transaction; [maybeExtend] runs a follow-up
 *    30-minute count and silently pushes expiry to 72h once (Echo Extension).
 *  - Counters (echoCount/coldMarkCount) use FieldValue.increment; Phase 4 moves
 *    them server-side for anti-gaming.
 */
@Singleton
class EmberRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val mediaUploader: MediaUploadRepository
) {

    /** The Clearing: newest non-expired embers across all tribes. Re-query with a larger [limit] to paginate. */
    fun observeFeed(limit: Long): Flow<List<Ember>> = feedQuery(
        firestore.collection(EMBERS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
    )

    /** A single tribe's embers, newest non-expired first. */
    fun observeTribeEmbers(tribeId: String, limit: Long): Flow<List<Ember>> = feedQuery(
        firestore.collection(EMBERS)
            .whereEqualTo("tribeId", tribeId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
    )

    suspend fun createEmber(
        text: String,
        tribeId: String?,
        tribeName: String,
        mode: PostMode,
        mediaUri: Uri?
    ): Result<String> {
        val user = auth.currentUser ?: return Result.failure(DomainError.NotAuthenticated)
        val trimmed = text.trim()
        if (trimmed.isEmpty() && mediaUri == null) return Result.failure(DomainError.EmptyEmber)

        // Upload media first (Cloudinary, unsigned) so we never write an ember
        // pointing at an image that failed to land.
        var mediaUrl: String? = null
        var mediaType: String? = null
        if (mediaUri != null) {
            mediaUrl = mediaUploader.uploadImage(mediaUri).getOrNull()
                ?: return Result.failure(DomainError.MediaUploadFailed)
            mediaType = "photo"
        }

        return try {
            val profileSnap = firestore.collection(PROFILES).document(user.uid).get().await()
            val masked = mode.isMasked

            val now = Timestamp.now()
            val expiresAt = Timestamp(now.seconds + LIFESPAN_SECONDS, 0)
            val emberRef = firestore.collection(EMBERS).document()
            emberRef.set(
                mapOf(
                    "authorId" to user.uid,
                    "authorName" to if (masked) "" else (profileSnap.getString("displayName") ?: ""),
                    "authorUsername" to if (masked) "" else (profileSnap.getString("username") ?: ""),
                    "authorAvatarUrl" to if (masked) "" else (profileSnap.getString("avatarUrl") ?: ""),
                    "authorAvatarColor" to if (masked) "#FF6B35" else (profileSnap.getString("avatarColor") ?: "#FF6B35"),
                    "tribeId" to tribeId,
                    "tribeName" to tribeName.trim(),
                    "text" to trimmed,
                    "mediaUrl" to mediaUrl,
                    "mediaType" to mediaType,
                    "mode" to mode.wire,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "expiresAt" to expiresAt,
                    "echoCount" to 0,
                    "coldMarkCount" to 0,
                    "extended" to false
                )
            ).await()
            Result.success(emberRef.id)
        } catch (_: Exception) {
            Result.failure(DomainError.Unknown)
        }
    }

    /** Toggle the current user's echo. Returns the new echoed state. */
    suspend fun toggleEcho(emberId: String): Result<Boolean> {
        val uid = auth.currentUser?.uid ?: return Result.failure(DomainError.NotAuthenticated)
        val emberRef = firestore.collection(EMBERS).document(emberId)
        val echoRef = emberRef.collection(ECHOES).document(uid)
        return try {
            val nowEchoed = firestore.runTransaction { txn ->
                val existing = txn.get(echoRef)
                if (existing.exists()) {
                    txn.delete(echoRef)
                    txn.update(emberRef, "echoCount", FieldValue.increment(-1))
                    false
                } else {
                    txn.set(echoRef, mapOf("uid" to uid, "createdAt" to Timestamp.now()))
                    txn.update(emberRef, "echoCount", FieldValue.increment(1))
                    true
                }
            }.await()
            if (nowEchoed) runCatching { maybeExtend(emberRef) }
            Result.success(nowEchoed)
        } catch (_: Exception) {
            Result.failure(DomainError.Unknown)
        }
    }

    suspend fun hasEchoed(emberId: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            firestore.collection(EMBERS).document(emberId)
                .collection(ECHOES).document(uid).get().await().exists()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun coldMark(emberId: String, reason: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(DomainError.NotAuthenticated)
        val emberRef = firestore.collection(EMBERS).document(emberId)
        val markRef = emberRef.collection(COLD_MARKS).document(uid)
        return try {
            // Transaction so the "already marked?" check and the increment are
            // atomic — two rapid taps can't both pass the guard and double-count.
            firestore.runTransaction { txn ->
                if (txn.get(markRef).exists()) return@runTransaction
                txn.set(markRef, mapOf("uid" to uid, "reason" to reason, "createdAt" to Timestamp.now()))
                txn.update(emberRef, "coldMarkCount", FieldValue.increment(1))
            }.await()
            Result.success(Unit)
        } catch (_: Exception) {
            Result.failure(DomainError.Unknown)
        }
    }

    /** Echo Extension: 5+ echoes in the last 30 min → push expiry to 72h, once, silently. */
    private suspend fun maybeExtend(emberRef: DocumentReference) {
        val cutoff = Timestamp(Timestamp.now().seconds - EXTENSION_WINDOW_SECONDS, 0)
        val recent = emberRef.collection(ECHOES)
            .whereGreaterThan("createdAt", cutoff).get().await()
        if (recent.size() < EXTENSION_THRESHOLD) return
        val snap = emberRef.get().await()
        if (snap.getBoolean("extended") == true) return
        val created = snap.getTimestamp("createdAt") ?: Timestamp.now()
        val newExpiry = Timestamp(created.seconds + CEILING_SECONDS, 0)
        emberRef.update(mapOf("expiresAt" to newExpiry, "extended" to true)).await()
    }

    private fun feedQuery(query: Query): Flow<List<Ember>> = callbackFlow {
        val reg = query.addSnapshotListener { snap, _ ->
            val now = Timestamp.now()
            val list = snap?.documents
                ?.mapNotNull { it.toEmber() }
                ?.filter { e -> e.expiresAt == null || e.expiresAt > now }
                ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    private fun DocumentSnapshot.toEmber(): Ember? {
        val authorId = getString("authorId") ?: return null
        return Ember(
            id = id,
            authorId = authorId,
            authorName = getString("authorName") ?: "",
            authorUsername = getString("authorUsername") ?: "",
            authorAvatarUrl = getString("authorAvatarUrl") ?: "",
            authorAvatarColor = getString("authorAvatarColor") ?: "#FF6B35",
            tribeId = getString("tribeId"),
            tribeName = getString("tribeName") ?: "",
            text = getString("text") ?: "",
            mediaUrl = getString("mediaUrl"),
            mediaType = getString("mediaType"),
            mode = PostMode.fromWire(getString("mode")),
            createdAt = getTimestamp("createdAt"),
            expiresAt = getTimestamp("expiresAt"),
            echoCount = (getLong("echoCount") ?: 0L).toInt(),
            coldMarkCount = (getLong("coldMarkCount") ?: 0L).toInt(),
            extended = getBoolean("extended") == true
        )
    }

    companion object {
        const val EMBERS = "embers"
        const val ECHOES = "echoes"
        const val COLD_MARKS = "coldMarks"
        const val PROFILES = "profiles"

        const val LIFESPAN_SECONDS = 48L * 3600          // 48h default
        const val CEILING_SECONDS = 72L * 3600           // 72h Echo Extension ceiling
        const val EXTENSION_WINDOW_SECONDS = 30L * 60     // 30-minute echo window
        const val EXTENSION_THRESHOLD = 5                // echoes needed to extend
    }
}
