package com.duren.app.data.tribe.model

import com.google.firebase.Timestamp

/**
 * A Tribe Bulletin (tribes/{tribeId}/bulletins/{id}) — a Keeper-curated notice pinned
 * to the top of the tribe. Up to five live at once, each with its own expiry (Feature 21).
 */
data class Bulletin(
    val id: String = "",
    val title: String = "",
    val text: String = "",
    val emoji: String = "",
    val createdAt: Timestamp? = null,
    val expiresAt: Timestamp? = null
)
