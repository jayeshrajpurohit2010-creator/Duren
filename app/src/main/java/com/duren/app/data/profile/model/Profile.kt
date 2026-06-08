package com.duren.app.data.profile.model

import com.google.firebase.Timestamp

/**
 * A user profile document (profiles/{uid}).
 *
 * Settings fields (accentColor … showTestimonials) were added in the June 4, 2026
 * §3.3 amendment. Defaults mirror the spec: teal accent, dark mode, lantern on.
 */
data class Profile(
    val uid: String = "",
    val username: String = "",
    val displayName: String = "",
    val email: String = "",
    val bio: String = "",
    val pronouns: String = "",
    // A 30-char italic tagline shown under your name on every ember (Feature 14).
    val signature: String = "",
    val avatarUrl: String = "",
    val createdAt: Timestamp? = null,
    val lastSeen: Timestamp? = null,
    // Settings & customization (June 4 §3.3)
    val accentColor: String = "#2dd4bf",
    val lightModeEnabled: Boolean = false,
    val avatarColor: String = "#FF6B35",
    val showLantern: Boolean = true,
    val showMoodCanvas: Boolean = false,
    val allowAnonBox: Boolean = true,
    val showTestimonials: Boolean = false
)
