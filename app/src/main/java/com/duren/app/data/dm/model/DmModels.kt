package com.duren.app.data.dm.model

import com.duren.app.data.profile.model.Profile
import com.google.firebase.Timestamp

/**
 * A 1:1 conversation summary (chats/{chatId}) for the chat list.
 *
 * chatId is the two uids sorted and joined with "_", so the same pair always
 * resolves to the same doc from either side. [unread] is derived for the viewer:
 * the last message came from the other person and they haven't opened it since.
 */
data class ChatSummary(
    val chatId: String = "",
    val otherUserId: String = "",
    val lastMessage: String = "",
    val lastMessageAt: Timestamp? = null,
    val unread: Boolean = false,
    /** Hydrated for display; null until the profile resolves. */
    val otherProfile: Profile? = null
)

/**
 * One message in a conversation (chats/{chatId}/messages/{id}).
 *
 * Expiring Ember: [expiresAt] is [createdAt] + 48h. There is no Cloud Function to
 * hard-delete it on the free plan, so the client simply stops showing messages once
 * they're past [expiresAt] — the same "hidden, not yet reaped" approach as embers.
 */
data class DmMessage(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val createdAt: Timestamp? = null,
    val expiresAt: Timestamp? = null
)
