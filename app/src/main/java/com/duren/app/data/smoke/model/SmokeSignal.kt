package com.duren.app.data.smoke.model

import com.google.firebase.Timestamp

/**
 * A Smoke Signal (smokeSignals/{id}) — one broadcast to your whole Nest, at most
 * one a week, gone in seven days (Feature 30).
 */
data class SmokeSignal(
    val id: String = "",
    val fromUserId: String = "",
    val fromName: String = "",
    val text: String = "",
    val createdAt: Timestamp? = null,
    val expiresAt: Timestamp? = null
)

/** Why a send attempt didn't go out (or did). */
enum class SmokeOutcome { Sent, OncePerWeek, EmptyNest, Failed }
