package com.duren.app.data.tribe.model

import com.google.firebase.Timestamp

/**
 * A tribe — a community around a shared vibe (tribes/{tribeId}).
 *
 * [isMember] is hydrated per-user at read time from the caller's memberships;
 * it is never persisted on the tribe document.
 */
data class Tribe(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val genre: String = "",
    val createdBy: String = "",
    val createdAt: Timestamp? = null,
    val memberCount: Int = 0,
    val isMember: Boolean = false
)
