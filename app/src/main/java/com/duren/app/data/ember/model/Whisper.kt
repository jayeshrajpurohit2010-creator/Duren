package com.duren.app.data.ember.model

import com.google.firebase.Timestamp

/**
 * A Whisper — a comment on an Ember (embers/{emberId}/whispers/{whisperId}).
 *
 * When [isAnonymous] the author is shown as "A Soul" and the name/avatar fields
 * are stored blank, mirroring how masked Embers hide identity on the document.
 * [authorId] is always the real uid so the author (and only the author) can
 * delete their own whisper.
 */
data class Whisper(
    val id: String = "",
    val emberId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorUsername: String = "",
    val authorAvatarUrl: String = "",
    val authorAvatarColor: String = "#FF6B35",
    val text: String = "",
    val isAnonymous: Boolean = false,
    // Threading: the whisper this one replies to, or null for a top-level whisper.
    // The tree is rebuilt on-device from these parent links (no extra index needed).
    val parentWhisperId: String? = null,
    val createdAt: Timestamp? = null
)
