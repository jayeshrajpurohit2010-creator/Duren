package com.duren.app.data.ember.model

import com.google.firebase.Timestamp

/**
 * How an Ember is attributed.
 *
 * - [Named]: normal post, shows author avatar + name.
 * - [Anonymous]: masked — author identity is blanked on the document so the
 *   client cannot reveal it. Shows a mask, no avatar.
 * - [Confess]: like Anonymous but framed as a confession.
 */
enum class PostMode(val wire: String) {
    Named("named"),
    Anonymous("anonymous"),
    Confess("confess");

    val isMasked: Boolean get() = this != Named

    companion object {
        fun fromWire(value: String?): PostMode =
            entries.firstOrNull { it.wire == value } ?: Named
    }
}

/**
 * An Ember — a post with a 48h lifespan (embers/{emberId}).
 *
 * Lifespan is fixed at 48h by default; Echo Extension (5+ echoes in 30 min)
 * silently pushes [expiresAt] to createdAt + 72h once and sets [extended].
 *
 * [echoedByMe] is hydrated per-user and never persisted. For masked posts
 * ([mode] != Named) the author fields are stored blank.
 */
data class Ember(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorUsername: String = "",
    val authorAvatarUrl: String = "",
    val authorAvatarColor: String = "#FF6B35",
    // The author's 30-char tagline, stamped at post time. Blank for masked modes.
    val emberSignature: String = "",
    val tribeId: String? = null,
    val tribeName: String = "",
    val text: String = "",
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val mode: PostMode = PostMode.Named,
    val createdAt: Timestamp? = null,
    val expiresAt: Timestamp? = null,
    val echoCount: Int = 0,
    val coldMarkCount: Int = 0,
    val whisperCount: Int = 0,
    val extended: Boolean = false,
    val echoedByMe: Boolean = false
)
