package com.duren.app.data.signal

import com.duren.app.data.signal.model.Signal
import com.duren.app.data.signal.model.SignalType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Signals — Duren's notifications, fully client-driven so they cost nothing on the
 * free Firebase plan. The actor writes a doc into the recipient's
 * `notifications/{uid}/items` collection; the recipient reads + marks it read.
 *
 * What this CAN'T do without Blaze: deliver a push when the app is closed (that needs
 * FCM + a Cloud Function). The in-app bell + list below work fully for free.
 */
@Singleton
class SignalRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    val currentUserId: String? get() = auth.currentUser?.uid

    private fun itemsRef(uid: String) =
        firestore.collection(NOTIFS).document(uid).collection(ITEMS)

    /**
     * Drop a Signal into [toUserId]'s inbox. Best-effort and self-suppressing: it
     * never throws (a failed notification must never break the action that triggered
     * it) and never notifies yourself.
     */
    suspend fun notify(
        toUserId: String,
        type: SignalType,
        emberId: String? = null,
        preview: String = ""
    ) {
        val me = auth.currentUser?.uid ?: return
        if (me == toUserId) return
        runCatching {
            val data = mutableMapOf<String, Any>(
                "type" to type.wire,
                "fromUserId" to me,
                "isRead" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
            if (!emberId.isNullOrBlank()) data["emberId"] = emberId
            if (preview.isNotBlank()) data["preview"] = preview.take(120)
            itemsRef(toUserId).document().set(data).await()
        }
    }

    /** The current user's Signals, newest first (sorted on-device — no index needed). */
    fun observeSignals(): Flow<List<Signal>> {
        val me = auth.currentUser?.uid ?: return flowOf(emptyList())
        return callbackFlow {
            val reg = itemsRef(me).addSnapshotListener { snap, _ ->
                val list = snap?.documents?.map { doc ->
                    Signal(
                        id = doc.id,
                        type = SignalType.fromWire(doc.getString("type")),
                        fromUserId = doc.getString("fromUserId") ?: "",
                        emberId = doc.getString("emberId"),
                        preview = doc.getString("preview") ?: "",
                        isRead = doc.getBoolean("isRead") == true,
                        createdAt = doc.getTimestamp("createdAt")
                    )
                }?.sortedByDescending { it.createdAt?.seconds ?: 0L } ?: emptyList()
                trySend(list)
            }
            awaitClose { reg.remove() }
        }
    }

    /** Count of unread Signals, for the bell badge. */
    fun observeUnreadCount(): Flow<Int> =
        observeSignals().map { signals -> signals.count { !it.isRead } }

    /** Mark every unread Signal read (called when the user opens the Signal screen). */
    suspend fun markAllRead() {
        val me = auth.currentUser?.uid ?: return
        runCatching {
            val snap = itemsRef(me).whereEqualTo("isRead", false).get().await()
            if (snap.isEmpty) return@runCatching
            val batch = firestore.batch()
            snap.documents.forEach { batch.update(it.reference, "isRead", true) }
            batch.commit().await()
        }
    }

    companion object {
        const val NOTIFS = "notifications"
        const val ITEMS = "items"
    }
}
