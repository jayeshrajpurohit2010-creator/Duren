package com.duren.app.data.tribe

import com.duren.app.core.DomainError
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
                        createdBy = doc.getString("createdBy") ?: "",
                        createdAt = doc.getTimestamp("createdAt"),
                        memberCount = (doc.getLong("memberCount") ?: 0L).toInt()
                    )
                } ?: emptyList()
                trySend(list)
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

    companion object {
        const val TRIBES = "tribes"
        const val MEMBERSHIPS = "memberships"
    }
}
