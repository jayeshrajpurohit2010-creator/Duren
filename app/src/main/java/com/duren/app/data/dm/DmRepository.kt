package com.duren.app.data.dm

import com.duren.app.core.DomainError
import com.duren.app.data.dm.model.ChatSummary
import com.duren.app.data.dm.model.DmMessage
import com.duren.app.data.signal.SignalRepository
import com.duren.app.data.signal.model.SignalType
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query.Direction
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Expiring Embers — Duren's 1:1 DMs. Live (Firestore realtime listeners) and free.
 *
 * Layout: chats/{chatId} with chatId = the two uids sorted + joined by "_", so both
 * sides resolve the same doc. Messages live in a subcollection and each carries a
 * 48h [DmMessage.expiresAt]; with no Cloud Function to reap them on the free plan,
 * the client just stops showing expired ones. Queries avoid composite indexes
 * (array-contains with on-device sort; single-field message ordering).
 *
 * "Seen" is tracked cheaply at the chat level via `readBy` (who has opened the latest
 * state) rather than a write per message.
 */
@Singleton
class DmRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val signalRepository: SignalRepository
) {
    val currentUserId: String? get() = auth.currentUser?.uid

    fun chatIdFor(a: String, b: String): String = listOf(a, b).sorted().joinToString("_")

    private fun chatRef(chatId: String) = firestore.collection(CHATS).document(chatId)

    /** Conversations the current user is part of, most-recent activity first. */
    fun observeChats(): Flow<List<ChatSummary>> {
        val me = auth.currentUser?.uid ?: return flowOf(emptyList())
        return callbackFlow {
            val reg = firestore.collection(CHATS)
                .whereArrayContains("participants", me)
                .addSnapshotListener { snap, _ ->
                    val list = snap?.documents?.mapNotNull { doc ->
                        val parts = (doc.get("participants") as? List<*>)?.filterIsInstance<String>()
                            ?: return@mapNotNull null
                        val other = parts.firstOrNull { it != me } ?: return@mapNotNull null
                        val readBy = (doc.get("readBy") as? List<*>)?.filterIsInstance<String>()
                            ?: emptyList()
                        val lastSender = doc.getString("lastSenderId") ?: ""
                        ChatSummary(
                            chatId = doc.id,
                            otherUserId = other,
                            lastMessage = doc.getString("lastMessage") ?: "",
                            lastMessageAt = doc.getTimestamp("lastMessageAt"),
                            unread = lastSender != me && me !in readBy
                        )
                    }?.sortedByDescending { it.lastMessageAt?.seconds ?: 0L } ?: emptyList()
                    trySend(list)
                }
            awaitClose { reg.remove() }
        }
    }

    /** Live messages in a conversation, oldest-first; faded (expired) ones filtered out. */
    fun observeMessages(chatId: String): Flow<List<DmMessage>> = callbackFlow {
        val reg = chatRef(chatId).collection(MESSAGES)
            .orderBy("createdAt", Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                val now = System.currentTimeMillis()
                val list = snap?.documents?.mapNotNull { doc ->
                    val sender = doc.getString("senderId") ?: return@mapNotNull null
                    val expiresAt = doc.getTimestamp("expiresAt")
                    if (expiresAt != null && expiresAt.toDate().time < now) return@mapNotNull null
                    DmMessage(
                        id = doc.id,
                        senderId = sender,
                        text = doc.getString("text") ?: "",
                        createdAt = doc.getTimestamp("createdAt"),
                        expiresAt = expiresAt
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    /** Send a 48h-expiring message to [otherUserId]. Creates the chat doc if needed. */
    suspend fun sendMessage(otherUserId: String, text: String): Result<Unit> {
        val me = auth.currentUser?.uid ?: return Result.failure(DomainError.NotAuthenticated)
        val body = text.trim()
        if (body.isEmpty()) return Result.failure(DomainError.EmptyEmber)
        if (me == otherUserId) return Result.failure(DomainError.Unknown)
        val chatId = chatIdFor(me, otherUserId)
        val now = Timestamp.now()
        val expires = Timestamp(Date(now.toDate().time + LIFESPAN_MS))
        return try {
            // 1) Upsert the chat doc FIRST so the message-create rule's get() can see
            //    the participants (rules evaluate each write against committed state,
            //    so a single batch wouldn't satisfy that dependency).
            chatRef(chatId).set(
                mapOf(
                    "participants" to listOf(me, otherUserId),
                    "lastMessage" to body.take(140),
                    "lastMessageAt" to now,
                    "lastSenderId" to me,
                    "readBy" to listOf(me)
                )
            ).await()
            // 2) Append the message.
            chatRef(chatId).collection(MESSAGES).document().set(
                mapOf(
                    "senderId" to me,
                    "text" to body.take(2000),
                    "createdAt" to now,
                    "expiresAt" to expires
                )
            ).await()
            signalRepository.notify(otherUserId, SignalType.Dm, preview = body.take(120))
            Result.success(Unit)
        } catch (_: Exception) {
            Result.failure(DomainError.Unknown)
        }
    }

    /** Mark a chat's latest state as seen by the current user (clears unread + sets ticks). */
    suspend fun markRead(chatId: String) {
        val me = auth.currentUser?.uid ?: return
        runCatching { chatRef(chatId).update("readBy", FieldValue.arrayUnion(me)).await() }
    }

    /** Whether the other person has seen the current user's latest message (double-tick). */
    fun observeSeenByOther(otherUserId: String): Flow<Boolean> {
        val me = auth.currentUser?.uid ?: return flowOf(false)
        val chatId = chatIdFor(me, otherUserId)
        return callbackFlow {
            val reg = chatRef(chatId).addSnapshotListener { snap, _ ->
                val readBy = (snap?.get("readBy") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                val lastSender = snap?.getString("lastSenderId") ?: ""
                trySend(lastSender == me && otherUserId in readBy)
            }
            awaitClose { reg.remove() }
        }
    }

    companion object {
        const val CHATS = "chats"
        const val MESSAGES = "messages"
        const val LIFESPAN_MS = 48L * 60 * 60 * 1000
    }
}
