package com.duren.app.data.tribe.model

import com.google.firebase.Timestamp

/**
 * A Sub-Ember (tribes/{tribeId}/subEmbers/{id}) — a named topic thread inside a
 * tribe, like "#episode-drops" or "#midnight-music" (Feature 36). Embers carry the
 * topic id; the tribe screen filters by it.
 */
data class SubEmber(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val createdBy: String = "",
    val postCount: Int = 0,
    val lastActiveAt: Timestamp? = null
)
