package com.duren.app.data.lantern.model

import com.google.firebase.Timestamp

/**
 * A Lantern — a short ANONYMOUS message a user "lights" and sets adrift
 * (lanterns/{lanternId}). Author identity is never surfaced to other users.
 *
 * Lifespan is fixed at 48h; [expiresAt] is filtered on-device, mirroring embers.
 * [foundCount] tracks how many wanderers have discovered it.
 */
data class Lantern(
    val id: String = "",
    val authorId: String = "",
    val text: String = "",
    val createdAt: Timestamp? = null,
    val expiresAt: Timestamp? = null,
    val foundCount: Int = 0
)
