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
    // For Confess mode: the poetic mask shown instead of a name (Feature 15).
    val poeticAlias: String = "",
    // Fragment mode (Feature 17): the body past [fragmentThreshold] chars stays
    // hidden+blurred until the reader echoes. The full text lives in [text]; the
    // reveal is gated client-side by [Ember.echoedByMe].
    val isFragment: Boolean = false,
    val fragmentThreshold: Int = 0,
    // Quick Poll (Feature 18): a yes/no question. The body is the question; the
    // running tallies live on the doc so percentages update live for everyone.
    val isPoll: Boolean = false,
    val pollYes: Int = 0,
    val pollNo: Int = 0,
    // Floating Lantern (Feature 19): a tribe Keeper pins one ember for ~1h. Sorted
    // to the top of its tribe while [pinExpiresAt] is in the future.
    val isPinned: Boolean = false,
    val pinExpiresAt: Timestamp? = null,
    // Embers of Wisdom (Feature 23): a Keeper-blessed ember — gold-edged, kept for
    // 30 days instead of burning at 48h. Max five live per tribe (enforced client-side).
    val isWisdom: Boolean = false,
    val wisdomExpiresAt: Timestamp? = null,
    // Final Ember (Feature 35): the goodbye automatically left behind when someone
    // walks away from a tribe. Candle-marked, and it cannot be echoed.
    val isFinal: Boolean = false,
    val createdAt: Timestamp? = null,
    val expiresAt: Timestamp? = null,
    val echoCount: Int = 0,
    val coldMarkCount: Int = 0,
    val whisperCount: Int = 0,
    val extended: Boolean = false,
    val echoedByMe: Boolean = false
) {
    /** A pin only counts while it hasn't lapsed. */
    fun pinnedNow(now: Timestamp = Timestamp.now()): Boolean =
        isPinned && (pinExpiresAt?.let { it > now } ?: false)
}
