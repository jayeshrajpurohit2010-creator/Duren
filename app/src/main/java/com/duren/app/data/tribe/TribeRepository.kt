package com.duren.app.data.tribe

import com.duren.app.core.DomainError
import com.duren.app.data.tribe.model.Bulletin
import com.duren.app.data.tribe.model.Tribe
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tribes + membership.
 *
 * Membership is stored as a flat `memberships/{uid}_{tribeId}` document (denormalized
 * with the tribe name) so a user's tribes can be read with a single equality query —
 * no collection-group join. The tribe's [Tribe.memberCount] is kept via
 * [FieldValue.increment]; Phase 4 (anti-gaming) moves that counter server-side.
 */
@Singleton
class TribeRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    /** All tribes (browse), most-populated first, with [Tribe.isMember] hydrated for the caller. */
    fun observeTribes(): Flow<List<Tribe>> =
        combine(observeAllTribes(), observeMyTribeIds()) { tribes, myIds ->
            tribes.map { it.copy(isMember = it.id in myIds) }
        }

    /** A single tribe's live document, with [Tribe.isMember] hydrated for the current user. */
    fun observeTribe(tribeId: String): Flow<Tribe?> =
        combine(observeTribeDoc(tribeId), observeMyTribeIds()) { tribe, myIds ->
            tribe?.copy(isMember = tribe.id in myIds)
        }

    /** Lightweight list of the tribes the current user has joined (for the compose picker). */
    fun observeMyTribes(): Flow<List<Tribe>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val reg = firestore.collection(MEMBERSHIPS)
            .whereEqualTo("uid", uid)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull { doc ->
                    val tribeId = doc.getString("tribeId") ?: return@mapNotNull null
                    Tribe(
                        id = tribeId,
                        name = doc.getString("tribeName") ?: "",
                        isMember = true
                    )
                }?.sortedBy { it.name.lowercase() } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun createTribe(name: String, description: String, genre: String): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(DomainError.NotAuthenticated)
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return Result.failure(DomainError.TribeNameRequired)

        return try {
            val tribeRef = firestore.collection(TRIBES).document()
            val membershipRef = firestore.collection(MEMBERSHIPS).document("${uid}_${tribeRef.id}")
            firestore.runBatch { batch ->
                batch.set(
                    tribeRef,
                    mapOf(
                        "name" to trimmedName,
                        "description" to description.trim(),
                        "genre" to genre.trim(),
                        "createdBy" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "memberCount" to 1
                    )
                )
                batch.set(
                    membershipRef,
                    mapOf(
                        "uid" to uid,
                        "tribeId" to tribeRef.id,
                        "tribeName" to trimmedName,
                        "joinedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(tribeRef.id)
        } catch (_: Exception) {
            Result.failure(DomainError.Unknown)
        }
    }

    suspend fun joinTribe(tribeId: String, tribeName: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(DomainError.NotAuthenticated)
        val membershipRef = firestore.collection(MEMBERSHIPS).document("${uid}_$tribeId")
        return try {
            // Idempotent: if already a member, don't double-count.
            if (membershipRef.get().await().exists()) return Result.success(Unit)
            firestore.runBatch { batch ->
                batch.set(
                    membershipRef,
                    mapOf(
                        "uid" to uid,
                        "tribeId" to tribeId,
                        "tribeName" to tribeName,
                        "joinedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    firestore.collection(TRIBES).document(tribeId),
                    "memberCount", FieldValue.increment(1)
                )
            }.await()
            Result.success(Unit)
        } catch (_: Exception) {
            Result.failure(DomainError.Unknown)
        }
    }

    suspend fun leaveTribe(tribeId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(DomainError.NotAuthenticated)
        val membershipRef = firestore.collection(MEMBERSHIPS).document("${uid}_$tribeId")
        return try {
            if (!membershipRef.get().await().exists()) return Result.success(Unit)
            firestore.runBatch { batch ->
                batch.delete(membershipRef)
                batch.update(
                    firestore.collection(TRIBES).document(tribeId),
                    "memberCount", FieldValue.increment(-1)
                )
            }.await()
            Result.success(Unit)
        } catch (_: Exception) {
            Result.failure(DomainError.Unknown)
        }
    }

    // ===== Tribe Bulletin Board (F21) — Keeper-curated notices, max 5, 24h life =====

    fun observeBulletins(tribeId: String): Flow<List<Bulletin>> = callbackFlow {
        val reg = firestore.collection(TRIBES).document(tribeId).collection(BULLETINS)
            .addSnapshotListener { snap, _ ->
                val now = Timestamp.now()
                val list = snap?.documents?.map { doc ->
                    Bulletin(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        text = doc.getString("text") ?: "",
                        emoji = doc.getString("emoji") ?: "",
                        createdAt = doc.getTimestamp("createdAt"),
                        expiresAt = doc.getTimestamp("expiresAt")
                    )
                }?.filter { b -> b.expiresAt?.let { it > now } ?: true }
                    ?.sortedByDescending { it.createdAt?.seconds ?: 0L }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    /** Keeper posts a bulletin (24h). Caps at 5 live on-device; rules enforce Keeper-only. */
    suspend fun addBulletin(tribeId: String, title: String, text: String, emoji: String): Result<Unit> {
        auth.currentUser?.uid ?: return Result.failure(DomainError.NotAuthenticated)
        return try {
            val now = Timestamp.now()
            val col = firestore.collection(TRIBES).document(tribeId).collection(BULLETINS)
            val live = col.get().await().documents.count { b ->
                b.getTimestamp("expiresAt")?.let { it > now } ?: true
            }
            if (live >= 5) return Result.failure(DomainError.Unknown)
            col.document().set(
                mapOf(
                    "title" to title.trim().take(60),
                    "text" to text.trim().take(280),
                    "emoji" to emoji.ifBlank { "📌" },
                    "createdAt" to now,
                    "expiresAt" to Timestamp(now.seconds + 24L * 3600, 0)
                )
            ).await()
            Result.success(Unit)
        } catch (_: Exception) {
            Result.failure(DomainError.Unknown)
        }
    }

    suspend fun deleteBulletin(tribeId: String, bulletinId: String): Result<Unit> = try {
        firestore.collection(TRIBES).document(tribeId).collection(BULLETINS)
            .document(bulletinId).delete().await()
        Result.success(Unit)
    } catch (_: Exception) {
        Result.failure(DomainError.Unknown)
    }

    // ===== Presence Beacon (F33) — who's around the fire right now =====

    /** Mark me present at this tribe now (called on enter + every ~minute while viewing). */
    suspend fun heartbeat(tribeId: String) {
        val uid = auth.currentUser?.uid ?: return
        runCatching {
            firestore.collection(TRIBES).document(tribeId).collection(PRESENCE).document(uid)
                .set(mapOf("lastActiveAt" to Timestamp.now())).await()
        }
    }

    /** Drop my presence when I leave the tribe screen. */
    suspend fun clearPresence(tribeId: String) {
        val uid = auth.currentUser?.uid ?: return
        runCatching {
            firestore.collection(TRIBES).document(tribeId).collection(PRESENCE).document(uid)
                .delete().await()
        }
    }

    /** Live count of souls active here in the last 2 minutes. */
    fun observePresentCount(tribeId: String): Flow<Int> = callbackFlow {
        val reg = firestore.collection(TRIBES).document(tribeId).collection(PRESENCE)
            .addSnapshotListener { snap, _ ->
                val cutoff = Timestamp.now().seconds - 120
                val count = snap?.documents?.count {
                    (it.getTimestamp("lastActiveAt")?.seconds ?: 0L) >= cutoff
                } ?: 0
                trySend(count)
            }
        awaitClose { reg.remove() }
    }

    private fun observeAllTribes(): Flow<List<Tribe>> = callbackFlow {
        val reg = firestore.collection(TRIBES)
            .orderBy("memberCount", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull { doc ->
                    Tribe(
                        id = doc.id,
                        name = doc.getString("name") ?: return@mapNotNull null,
                        description = doc.getString("description") ?: "",
                        genre = doc.getString("genre") ?: "",
                        vibe = doc.getString("vibe") ?: "",
                        emoji = doc.getString("emoji") ?: "",
                        createdBy = doc.getString("createdBy") ?: "",
                        createdAt = doc.getTimestamp("createdAt"),
                        memberCount = (doc.getLong("memberCount") ?: 0L).toInt()
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    private fun observeTribeDoc(tribeId: String): Flow<Tribe?> = callbackFlow {
        val reg = firestore.collection(TRIBES).document(tribeId)
            .addSnapshotListener { doc, _ ->
                val tribe = if (doc != null && doc.exists()) {
                    Tribe(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        genre = doc.getString("genre") ?: "",
                        vibe = doc.getString("vibe") ?: "",
                        emoji = doc.getString("emoji") ?: "",
                        createdBy = doc.getString("createdBy") ?: "",
                        createdAt = doc.getTimestamp("createdAt"),
                        memberCount = (doc.getLong("memberCount") ?: 0L).toInt()
                    )
                } else {
                    null
                }
                trySend(tribe)
            }
        awaitClose { reg.remove() }
    }

    private fun observeMyTribeIds(): Flow<Set<String>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptySet())
            awaitClose { }
            return@callbackFlow
        }
        val reg = firestore.collection(MEMBERSHIPS)
            .whereEqualTo("uid", uid)
            .addSnapshotListener { snap, _ ->
                val ids = snap?.documents?.mapNotNull { it.getString("tribeId") }?.toSet() ?: emptySet()
                trySend(ids)
            }
        awaitClose { reg.remove() }
    }

    /**
     * One-time bootstrap of the pre-seeded tribe catalog. Idempotent: it only
     * writes when the `tribes` collection is empty, and uses a deterministic slug
     * as each doc id, so two devices racing on a cold app can't create duplicates.
     *
     * Each seeded tribe satisfies the create rule (createdBy = caller, memberCount
     * = 1, name present). The seeder isn't auto-joined to anything — these are a
     * public catalog to discover, not the seeder's tribes.
     */
    suspend fun seedDefaultTribes(): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(DomainError.NotAuthenticated)
        return try {
            // Seed only the catalog tribes that are actually missing, so it works even
            // when unrelated tribes already exist. Deterministic slug ids keep it
            // idempotent (re-runs and concurrent devices converge on the same docs).
            val existingIds = firestore.collection(TRIBES).limit(200).get().await()
                .documents.map { it.id }.toSet()
            val missing = DEFAULT_TRIBES.filter { slug(it.name) !in existingIds }
            if (missing.isEmpty()) return Result.success(Unit)

            firestore.runBatch { batch ->
                missing.forEach { t ->
                    val ref = firestore.collection(TRIBES).document(slug(t.name))
                    batch.set(
                        ref,
                        mapOf(
                            "name" to t.name,
                            "description" to t.description,
                            "genre" to t.genre,
                            "vibe" to t.vibe,
                            "emoji" to t.emoji,
                            "createdBy" to uid,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "memberCount" to 1
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (_: Exception) {
            Result.failure(DomainError.Unknown)
        }
    }

    private data class SeedTribe(
        val name: String,
        val genre: String,
        val vibe: String,
        val emoji: String
    ) {
        // A short ambient line — the campfire's character in one breath.
        val description: String get() = "$emoji A $vibe $genre tribe."
    }

    companion object {
        const val TRIBES = "tribes"
        const val MEMBERSHIPS = "memberships"
        const val BULLETINS = "bulletins"
        const val PRESENCE = "presence"

        /** Lowercase, hyphenated, alnum-only doc id derived from a tribe name. */
        private fun slug(name: String): String =
            name.trim().lowercase()
                .replace(Regex("[^a-z0-9]+"), "-")
                .trim('-')
                .ifBlank { "tribe" }

        private val DEFAULT_TRIBES = listOf(
            // Anime & Manga
            SeedTribe("Anime Late Night", "anime", "energetic", "🌙"),
            SeedTribe("Manga Chapter Drops", "manga", "hype", "📖"),
            SeedTribe("Shonen Nerds", "anime", "intense", "⚔️"),
            SeedTribe("Slice of Life Club", "anime", "cozy", "☕"),
            SeedTribe("Ghibli Hours", "anime", "peaceful", "🌿"),
            // Gaming
            SeedTribe("Late Night Ranked", "gaming", "competitive", "🎮"),
            SeedTribe("Gacha Pulls", "gaming", "chaotic", "🎰"),
            SeedTribe("Indie Dev Lab", "gaming", "creative", "🛠️"),
            SeedTribe("Minecraft After Dark", "gaming", "chill", "⛏️"),
            SeedTribe("Horror Game Crew", "gaming", "spooky", "👻"),
            // K-Pop & Music
            SeedTribe("K-Pop Comeback Night", "kpop", "hype", "💜"),
            SeedTribe("Lo-Fi Study Session", "music", "calm", "🎵"),
            SeedTribe("Vocaloid Heads", "music", "niche", "🤖"),
            SeedTribe("Beat Makers Den", "music", "creative", "🎧"),
            // Study & Tech
            SeedTribe("Study Grind", "study", "focused", "📚"),
            SeedTribe("3AM Deadlines", "study", "stressed", "😭"),
            SeedTribe("Dev Lounge", "tech", "geeky", "💻"),
            // Life & Chill
            SeedTribe("Insomnia Club", "life", "raw", "🌙"),
            SeedTribe("Night Owls", "life", "chill", "🦉"),
            SeedTribe("Midnight Snack", "food", "cozy", "🍜"),
            SeedTribe("Vent Space", "mentalhealth", "safe", "💙"),
            SeedTribe("Confession Booth", "anonymous", "raw", "🕯️"),
            // Special meta-tribes
            SeedTribe("The Whisper Forest", "meta", "confessional", "🌲"),
            SeedTribe("The Clearing", "meta", "open", "🏕️")
        )
    }
}
