package com.duren.app.data.mood.model

import com.google.firebase.Timestamp

/**
 * A day's Mood Canvas entry (moodCanvas/{uid}/days/{yyyy-MM-dd}).
 *
 * [mood] is 1–5. The colour it paints the avatar aura comes from [hexFor], matching
 * the spec: 1 blue · 2 purple · 3 grey · 4 yellow · 5 teal (Feature 12).
 */
data class Mood(
    val mood: Int = 0,
    val emoji: String = "",
    val note: String = "",
    val createdAt: Timestamp? = null
) {
    val isSet: Boolean get() = mood in 1..5

    companion object {
        /** The aura colour (hex) for a mood value, or teal for "no mood set". */
        fun hexFor(mood: Int): String = when (mood) {
            1 -> "#3B82F6" // blue — low
            2 -> "#A855F7" // purple
            3 -> "#6B7280" // grey — neutral
            4 -> "#EAB308" // yellow
            5 -> "#2DD4BF" // teal — bright
            else -> "#2dd4bf"
        }

        /** A one-word label, for the picker. */
        fun labelFor(mood: Int): String = when (mood) {
            1 -> "Heavy"
            2 -> "Tender"
            3 -> "Even"
            4 -> "Bright"
            5 -> "Alight"
            else -> ""
        }
    }
}
