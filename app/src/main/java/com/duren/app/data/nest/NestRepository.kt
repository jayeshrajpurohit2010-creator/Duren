package com.duren.app.data.nest

import com.duren.app.core.DomainError
import com.duren.app.data.nest.model.NestRelation
import com.duren.app.data.nest.model.NestRequest
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Nest — mutual friendships built on one-way requests.
 *
 * Phase-2, all client-driven (no Cloud Functions). A request doc lives at
 * `nestRequests/{fromUid_toUid}`; accepting it writes the mutual membership edge
 * to both users' `userNests/{uid}/members/{otherUid}` and removes the request.
 * Queries deliberately avoid composite indexes (single-field equality, filtered
 * on-device) so nothing needs Firestore console setup beyond publishing rules.
 */
@Singleton
class NestRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    private val uid: String? get() = auth.currentUser?.uid

    private fun requestId(from: String, to: String) = "${from}_${to}"

    private fun requestRef(from: String, to: String): DocumentReference =
        firestore.collection(REQUESTS).document(requestId(from, to))

    private fun memberRef(owner: String, member: String): DocumentReference =
        firestore.collection(NESTS).document(owner).collection(MEMBERS).document(member)

    /** Send a request to join [toUserId]'s Nest. */
    suspend fun sendRequest(toUserId: String): Result<Unit> {
        val me = uid ?: return Result.failure(DomainError.NotAuthenticated)
        if (me == toUserId) return Result.failure(DomainError.Unknown)
        return try {
            requestRef(me, toUserId).set(
                mapOf(
                    "fromUserId" to me,
                    "toUserId" to toUserId,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (_: Exception) {
            Result.failure(DomainError.Unknown)
        }
    }

    /** Cancel a request the current user sent to [toUserId]. */
    suspend fun cancelRequest(toUserId: String): Result<Unit> {
        val me = uid ?: return Result.failure(DomainError.NotAuthenticated)
        return runCatching { requestRef(me, toUserId).delete().await(); Unit }
            .fold({ Result.success(it) }, { Result.failure(DomainError.Unknown) })
    }

    /** Accept the pending request [fromUserId] sent to the current user. */
    suspend fun acceptRequest(fromUserId: String): Result<Unit> {
        val me = uid ?: return Result.failure(DomainError.NotAuthenticated)
        return try {
            val since = FieldValue.serverTimestamp()
            // Mutual edge: each side owns one of these two writes per the rules.
            memberRef(me, fromUserId).set(mapOf("since" to since)).await()
            memberRef(fromUserId, me).set(mapOf("since" to since)).await()
            requestRef(fromUserId, me).delete().await()
            Result.success(Unit)
        } catch (_: Exception) {
            Result.failure(DomainError.Unknown)
        }
    }

    /** Decline the pending request [fromUserId] sent to the current user. */
    suspend fun declineRequest(fromUserId: String): Result<Unit> {
        val me = uid ?: return Result.failure(DomainError.NotAuthenticated)
        return runCatching { requestRef(fromUserId, me).delete().await(); Unit }
            .fold({ Result.success(it) }, { Result.failure(DomainError.Unknown) })
    }

    /** Remove [otherUserId] from the Nest (both directions). */
    suspend fun removeMember(otherUserId: String): Result<Unit> {
        val me = uid ?: return Result.failure(DomainError.NotAuthenticated)
        return try {
            memberRef(me, otherUserId).delete().await()
            memberRef(otherUserId, me).delete().await()
            Result.success(Unit)
        } catch (_: Exception) {
            Result.failure(DomainError.Unknown)
        }
    }

    /** Live relationship between the current user and [otherUserId] for the Nest button. */
    fun observeRelation(otherUserId: String): Flow<NestRelation> {
        val me = uid ?: return flowOf(NestRelation.None)
        if (me == otherUserId) return flowOf(NestRelation.Self)
        return combine(
            docExists(memberRef(me, otherUserId)),
            docExists(requestRef(me, otherUserId)),
            docExists(requestRef(otherUserId, me))
        ) { isMember, outgoing, incoming ->
            when {
                isMember -> NestRelation.Member
                incoming -> NestRelation.IncomingPending
                outgoing -> NestRelation.OutgoingPending
                else -> NestRelation.None
            }
        }
    }

    /** Pending requests sent TO the current user. Single-field query, no index. */
    fun observeIncomingRequests(): Flow<List<NestRequest>> {
        val me = uid ?: return flowOf(emptyList())
        return callbackFlow {
            val reg = firestore.collection(REQUESTS)
                .whereEqualTo("toUserId", me)
                .addSnapshotListener { snap, _ ->
                    val list = snap?.documents?.mapNotNull { doc ->
                        val from = doc.getString("fromUserId") ?: return@mapNotNull null
                        NestRequest(
                            id = doc.id,
                            fromUserId = from,
                            toUserId = me,
                            createdAt = doc.getTimestamp("createdAt")
                        )
                    } ?: emptyList()
                    trySend(list)
                }
            awaitClose { reg.remove() }
        }
    }

    /** The current user's Nest member uids. */
    fun observeMemberIds(): Flow<List<String>> {
        val me = uid ?: return flowOf(emptyList())
        return callbackFlow {
            val reg = firestore.collection(NESTS).document(me).collection(MEMBERS)
                .addSnapshotListener { snap, _ ->
                    trySend(snap?.documents?.map { it.id } ?: emptyList())
                }
            awaitClose { reg.remove() }
        }
    }

    private fun docExists(ref: DocumentReference): Flow<Boolean> = callbackFlow {
        val reg = ref.addSnapshotListener { snap, _ -> trySend(snap?.exists() == true) }
        awaitClose { reg.remove() }
    }

    companion object {
        const val REQUESTS = "nestRequests"
        const val NESTS = "userNests"
        const val MEMBERS = "members"
    }
}
