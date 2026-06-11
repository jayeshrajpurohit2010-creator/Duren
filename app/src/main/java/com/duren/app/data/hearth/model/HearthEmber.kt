package com.duren.app.data.hearth.model

import com.google.firebase.Timestamp

/**
 * A Hearth ember (hearths/{userId}/embers/{id}) — a private postcard from a Nest
 * member, visible only to the hearth's owner. Burns out after 24h (Feature 26).
 */
data class HearthEmber(
    val id: String = "",
    val text: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val createdAt: Timestamp? = null,
    val expiresAt: Timestamp? = null
)
