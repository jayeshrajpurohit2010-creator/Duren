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
    // A tribe's distinct identity: a one-word mood (e.g. "cozy", "chaotic") and a
    // single emoji shown on its card and banner, so each campfire feels different.
    val vibe: String = "",
    val emoji: String = "",
    val createdBy: String = "",
    val createdAt: Timestamp? = null,
    val memberCount: Int = 0,
    val isMember: Boolean = false,
    // Night Economy: a tribe can opt into Dead Hours, anchored to its own home
    // timezone (IANA id, e.g. "Asia/Kolkata"). Blank/disabled = no quiet hours.
    // Evaluated entirely on-device via [com.duren.app.core.time.NightEconomy] —
    // no server, no per-country job. Older tribe docs default to off.
    val homeTimezone: String = "",
    val deadHoursEnabled: Boolean = false
)
