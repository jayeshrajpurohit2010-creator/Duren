package com.duren.app.data.ember

import android.net.Uri
import com.duren.app.core.DomainError
import com.duren.app.data.ember.model.Ember
import com.duren.app.data.ember.model.PostMode
import com.duren.app.data.ember.model.Whisper
import com.google.firebase.firestore.Query.Direction
import com.duren.app.data.media.MediaUploadRepository
import com.duren.app.data.signal.SignalRepository
import com.duren.app.data.signal.model.SignalType
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
import kotlinx.coroutines.flow.flowOf
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
    private val mediaUploader: MediaUploadRepository,
    private val signalRepository: SignalRepository
) {

    /** The Clearing: newest non-expired embers across all tribes. Re-query with a larger [limit] to paginate. */
    fun observeFeed(limit: Long): Flow<List<Ember>> = feedQuery(
        firestore.collection(EMBERS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
    )

    /**
     * A single tribe's embers, newest non-expired first.
     *
     * Like [observeMine], deliberately NO server-side `orderBy`: pairing
     * `whereEqualTo("tribeId")` with `orderBy("createdAt")` needs a composite index
     * that isn't deployed — the query just fails, the swallowed listener error leaves
     * the tribe feed permanently empty, and posting reads as "nothing happened." The
     * post was always written fine; this is what couldn't read it back. [feedQuery]
     * sorts newest-first on-device, so a tribe post shows up the instant it's written.
     */
    fun observeTribeEmbers(tribeId: String, limit: Long): Flow<List<Ember>> = feedQuery(
        firestore.collection(EMBERS)
            .whereEqualTo("tribeId", tribeId)
            .limit(limit)
    )

    /**
     * The current user's own non-expired embers, newest first.
     *
     * Deliberately NO server-side `orderBy`: combining `whereEqualTo("authorId")`
     * with `orderBy("createdAt")` would demand a composite index that isn't
     * deployed (the query would just fail and the swallowed listener error would
     * leave a permanently empty list). [feedQuery] sorts newest-first on-device
     * instead, so this works with zero Firestore console setup. Fine at MVP scale
     * (a user's live 48h embers are few); add the composite index + restore
     * orderBy if a user can ever exceed [limit] active embers.
     */
    fun observeMine(limit: Long): Flow<List<Ember>> {
        val uid = auth.currentUser?.uid
        return feedQuery(
            firestore.collection(EMBERS)
                .whereEqualTo("authorId", uid)
                .limit(limit)
        )
    }

    /**
     * Another user's non-expired embers, newest first. Like [observeMine] it skips
     * the server `orderBy` (avoids a composite index) and sorts on-device. Read is
     * permitted by the live rules, so a public profile view needs zero deploy.
     */
    fun observeByAuthor(uid: String, limit: Long): Flow<List<Ember>> = feedQuery(
        firestore.collection(EMBERS)
            .whereEqualTo("authorId", uid)
            .limit(limit)
    )

    /**
     * The Nest feed: non-expired embers authored by the given Nest members, newest
     * first. Uses a single-field `whereIn` (no orderBy ⇒ no composite index; sorted
     * on-device by [feedQuery]). Firestore caps `whereIn` at 30 values, which is
     * plenty for a trusted Nest; if it ever grows past that we'll page it.
     */
    fun observeFromAuthors(authorIds: List<String>, limit: Long): Flow<List<Ember>> {
        if (authorIds.isEmpty()) return flowOf(emptyList())
        return feedQuery(
            firestore.collection(EMBERS)
                .whereIn("authorId", authorIds.take(30))
                .limit(limit)
        )
    }

    suspend fun createEmber(
        text: String,
        tribeId: String?,
        tribeName: String,
        mode: PostMode,
        mediaUri: Uri?,
        isFragment: Boolean = false,
        fragmentThreshold: Int = 100,
        isPoll: Boolean = false
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
            // Confess mode wears a poetic mask; only worth setting a fragment when the
            // body actually runs past the reveal threshold.
            val poeticAlias = if (mode == PostMode.Confess) PoeticAlias.random() else ""
            val fragment = isFragment && trimmed.length > fragmentThreshold
            val emberRef = firestore.collection(EMBERS).document()
            emberRef.set(
                mapOf(
                    "authorId" to user.uid,
                    "authorName" to if (masked) "" else (profileSnap.getString("displayName") ?: ""),
                    "authorUsername" to if (masked) "" else (profileSnap.getString("username") ?: ""),
                    "authorAvatarUrl" to if (masked) "" else (profileSnap.getString("avatarUrl") ?: ""),
                    "authorAvatarColor" to if (masked) "#FF6B35" else (profileSnap.getString("avatarColor") ?: "#FF6B35"),
                    "emberSignature" to if (masked) "" else (profileSnap.getString("signature") ?: ""),
                    "poeticAlias" to poeticAlias,
                    "isFragment" to fragment,
                    "fragmentThreshold" to fragmentThreshold,
                    // Quick Poll: yes/no tallies live on the doc so the bar moves live.
                    "isPoll" to isPoll,
                    "pollYes" to 0,
                    "pollNo" to 0,
                    "tribeId" to tribeId,
                    "tribeName" to tribeName.trim(),
                    "text" to trimmed,
                    "mediaUrl" to mediaUrl,
                    "mediaType" to mediaType,
                    "mode" to mode.wire,
                    // Client timestamp, not serverTimestamp: the optimistic local write
                    // then carries a real createdAt, so the author's own ember sorts to
                    // the top and appears instantly — instead of waiting a round-trip
                    // while a null pending value sorts it off the page. Consistent with
                    // expiresAt above (already client-time). Phase 4 can restore server
                    // authority once ranking moves into Cloud Functions.
                    "createdAt" to now,
                    "expiresAt" to expiresAt,
                    "echoCount" to 0,
                    "coldMarkCount" to 0,
                    "whisperCount" to 0,
                    "extended" to false
                )
            ).await()
            Result.success(emberRef.id)
        } catch (_: Exception) {
            Result.failure(DomainError.Unknown)
        }
    }

    /** The signed-in user's uid, or null. Lets the UI decide which embers are deletable. */
    val currentUserId: String?
        get() = auth.currentUser?.uid

    /**
     * Permanently delete an ember the caller authored. The Firestore rule
     * (`allow delete: if resource.data.authorId == request.auth.uid`) is the real
     * guard; this just refuses early if the uid obviously doesn't match.
     */
    suspend fun deleteEmber(emberId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(DomainError.NotAuthenticated)
        return try {
            val ref = firestore.collection(EMBERS).document(emberId)
            val snap = ref.get().await()
            if (snap.getString("authorId") != uid) return Result.failure(DomainError.Unknown)
            ref.delete().await()
            Result.success(Unit)
        } catch (_: Exception) {
            Result.failure(DomainError.Unknown)
        }
    }

    /**
     * Cast a yes/no vote on a poll ember. One vote per user, write-once: the vote
     * doc under `embers/{id}/pollVotes/{uid}` guards against double-counting in a
     * transaction, then the matching tally is incremented. Voting again is a no-op.
     */
    suspend fun votePoll(emberId: String, yes: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(DomainError.NotAuthenticated)
        val emberRef = firestore.collection(EMBERS).document(emberId)
        val voteRef = emberRef.collection(POLL_VOTES).document(uid)
        return try {
            firestore.runTransaction { txn ->
                if (txn.get(voteRef).exists()) return@runTransaction
                txn.set(voteRef, mapOf("choice" to if (yes) "yes" else "no", "createdAt" to Timestamp.now()))
                txn.update(emberRef, if (yes) "pollYes" else "pollNo", FieldValue.increment(1))
            }.await()
            Result.success(Unit)
        } catch (_: Exception) {
            Result.failure(DomainError.Unknown)
        }
    }

    /** Which way the current user voted on a poll, or null if they haven't. */
    suspend fun myPollVote(emberId: String): Boolean? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            val snap = firestore.collection(EMBERS).document(emberId)
                .collection(POLL_VOTES).document(uid).get().await()
            when (snap.getString("choice")) {
                "yes" -> true
                "no" -> false
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    /** True if the caller created [tribeId] — the Keeper, who alone can pin or bless. */
    private suspend fun isKeeperOf(tribeId: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return runCatching {
            firestore.collection(TRIBES).document(tribeId).get().await()
                .getString("createdBy") == uid
        }.getOrDefault(false)
    }

    /**
     * Floating Lantern — pin/unpin an ember to the top of its tribe (Keeper only).
     * Pinning sets a 1h window; the feed treats a lapsed pin as unpinned. The ember
     * `update` rule allows it because author/text/createdAt stay untouched.
     */
    suspend fun setPinned(emberId: String, tribeId: String, pinned: Boolean): Result<Unit> {
        if (!isKeeperOf(tribeId)) return Result.failure(DomainError.Unknown)
        return try {
            val update = if (pinned) {
                val now = Timestamp.now()
                mapOf("isPinned" to true, "pinExpiresAt" to Timestamp(now.seconds + PIN_SECONDS, 0))
            } else {
                mapOf("isPinned" to false, "pinExpiresAt" to null)
            }
            firestore.collection(EMBERS).document(emberId).update(update).await()
            Result.success(Unit)
        } catch (_: Exception) {
            Result.failure(DomainError.Unknown)
        }
    }

    /**
     * Embers of Wisdom — a Keeper blesses an ember: gold-edged, kept 30 days instead
     * of 48h. Capped at five live per tribe. Un-blessing just drops the flag (the
     * ember keeps whatever life it had left). Keeper-gated; rule-safe (only flags +
     * expiry change, never author/text/createdAt).
     */
    suspend fun setWisdom(emberId: String, tribeId: String, wisdom: Boolean): Result<Unit> {
        if (!isKeeperOf(tribeId)) return Result.failure(DomainError.Unknown)
        return try {
            if (wisdom) {
                // Enforce the 5-slot cap on-device before blessing another. Single
                // equality filter only (no composite index needed); wisdom + expiry
                // are filtered here in memory.
                val now = Timestamp.now()
                val live = firestore.collection(EMBERS)
                    .whereEqualTo("tribeId", tribeId)
                    .get().await()
                    .documents.count { doc ->
                        doc.id != emberId &&
                            doc.getBoolean("isWisdom") == true &&
                            (doc.getTimestamp("wisdomExpiresAt")?.let { it > now } ?: false)
                    }
                if (live >= WISDOM_MAX) return Result.failure(DomainError.Unknown)
                val expiry = Timestamp(now.seconds + WISDOM_SECONDS, 0)
                firestore.collection(EMBERS).document(emberId).update(
                    mapOf(
                        "isWisdom" to true,
                        "wisdomMarkedBy" to (auth.currentUser?.uid ?: ""),
                        "wisdomExpiresAt" to expiry,
                        // Lengthen the ember's own life so it doesn't burn at 48h.
                        "expiresAt" to expiry
                    )
                ).await()
            } else {
                firestore.collection(EMBERS).document(emberId)
                    .update(mapOf("isWisdom" to false)).await()
            }
            Result.success(Unit)
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
            if (nowEchoed) {
                runCatching { maybeExtend(emberRef) }
                // Tell the author someone echoed (skipped automatically if it's you).
                runCatching {
                    val authorId = emberRef.get().await().getString("authorId")
                    if (authorId != null) signalRepository.notify(authorId, SignalType.Echo, emberId)
                }
            }
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

    /** Live whispers (comments) on an ember, oldest-first so the thread reads top-to-bottom. */
    fun observeWhispers(emberId: String): Flow<List<Whisper>> = callbackFlow {
        val reg = firestore.collection(EMBERS).document(emberId)
            .collection(WHISPERS)
            .orderBy("createdAt", Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull { doc ->
                    val authorId = doc.getString("authorId") ?: return@mapNotNull null
                    Whisper(
                        id = doc.id,
                        emberId = emberId,
                        authorId = authorId,
                        authorName = doc.getString("authorName") ?: "",
                        authorUsername = doc.getString("authorUsername") ?: "",
                        authorAvatarUrl = doc.getString("authorAvatarUrl") ?: "",
                        authorAvatarColor = doc.getString("authorAvatarColor") ?: "#FF6B35",
                        text = doc.getString("text") ?: "",
                        isAnonymous = doc.getBoolean("isAnonymous") == true,
                        createdAt = doc.getTimestamp("createdAt")
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    /** Add a whisper to an ember and bump its whisperCount. */
    suspend fun addWhisper(emberId: String, text: String, isAnonymous: Boolean): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(DomainError.NotAuthenticated)
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return Result.failure(DomainError.EmptyEmber)
        return try {
            val profileSnap = firestore.collection(PROFILES).document(user.uid).get().await()
            val emberRef = firestore.collection(EMBERS).document(emberId)
            emberRef.collection(WHISPERS).document().set(
                mapOf(
                    "authorId" to user.uid,
                    "authorName" to if (isAnonymous) "" else (profileSnap.getString("displayName") ?: ""),
                    "authorUsername" to if (isAnonymous) "" else (profileSnap.getString("username") ?: ""),
                    "authorAvatarUrl" to if (isAnonymous) "" else (profileSnap.getString("avatarUrl") ?: ""),
                    "authorAvatarColor" to if (isAnonymous) "#FF6B35" else (profileSnap.getString("avatarColor") ?: "#FF6B35"),
                    "text" to trimmed.take(500),
                    "isAnonymous" to isAnonymous,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            emberRef.update("whisperCount", FieldValue.increment(1)).await()
            // Notify the author — but NOT for anonymous whispers: the Signal would
            // carry the whisperer's uid and quietly unmask them.
            if (!isAnonymous) {
                runCatching {
                    val authorId = emberRef.get().await().getString("authorId")
                    if (authorId != null) {
                        // No preview text: the whisper lives on the ember (which
                        // expires); the non-expiring Signal must not keep a copy.
                        signalRepository.notify(authorId, SignalType.Whisper, emberId)
                    }
                }
            }
            Result.success(Unit)
        } catch (_: Exception) {
            Result.failure(DomainError.Unknown)
        }
    }

    /** Delete the caller's own whisper and decrement the ember's whisperCount. */
    suspend fun deleteWhisper(emberId: String, whisperId: String): Result<Unit> {
        auth.currentUser ?: return Result.failure(DomainError.NotAuthenticated)
        return try {
            val emberRef = firestore.collection(EMBERS).document(emberId)
            emberRef.collection(WHISPERS).document(whisperId).delete().await()
            emberRef.update("whisperCount", FieldValue.increment(-1)).await()
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
                // Newest-first on-device, so queries that skip server orderBy
                // (e.g. observeMine, which avoids a composite index) are ordered
                // correctly. No-op for queries already server-sorted by createdAt.
                ?.sortedByDescending { it.createdAt }
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
            emberSignature = getString("emberSignature") ?: "",
            tribeId = getString("tribeId"),
            tribeName = getString("tribeName") ?: "",
            text = getString("text") ?: "",
            mediaUrl = getString("mediaUrl"),
            mediaType = getString("mediaType"),
            mode = PostMode.fromWire(getString("mode")),
            poeticAlias = getString("poeticAlias") ?: "",
            isFragment = getBoolean("isFragment") == true,
            fragmentThreshold = (getLong("fragmentThreshold") ?: 0L).toInt(),
            isPoll = getBoolean("isPoll") == true,
            pollYes = (getLong("pollYes") ?: 0L).toInt(),
            pollNo = (getLong("pollNo") ?: 0L).toInt(),
            isPinned = getBoolean("isPinned") == true,
            pinExpiresAt = getTimestamp("pinExpiresAt"),
            isWisdom = getBoolean("isWisdom") == true,
            wisdomExpiresAt = getTimestamp("wisdomExpiresAt"),
            createdAt = getTimestamp("createdAt"),
            expiresAt = getTimestamp("expiresAt"),
            echoCount = (getLong("echoCount") ?: 0L).toInt(),
            coldMarkCount = (getLong("coldMarkCount") ?: 0L).toInt(),
            whisperCount = (getLong("whisperCount") ?: 0L).toInt(),
            extended = getBoolean("extended") == true
        )
    }

    companion object {
        const val EMBERS = "embers"
        const val ECHOES = "echoes"
        const val COLD_MARKS = "coldMarks"
        const val WHISPERS = "whispers"
        const val POLL_VOTES = "pollVotes"
        const val PROFILES = "profiles"
        const val TRIBES = "tribes"

        const val LIFESPAN_SECONDS = 48L * 3600          // 48h default
        const val CEILING_SECONDS = 72L * 3600           // 72h Echo Extension ceiling
        const val EXTENSION_WINDOW_SECONDS = 30L * 60     // 30-minute echo window
        const val EXTENSION_THRESHOLD = 5                // echoes needed to extend
        const val PIN_SECONDS = 60L * 60                  // Floating Lantern: 1h pin
        const val WISDOM_SECONDS = 30L * 24 * 3600        // Embers of Wisdom: 30 days
        const val WISDOM_MAX = 5                          // max live Wisdom per tribe
    }
}
