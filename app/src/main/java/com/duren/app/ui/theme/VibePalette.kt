package com.duren.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Each tribe vibe owns a sliver of the spectrum — a near-black two-stop gradient for
 * surfaces and a brighter accent for glows. Shared by the Discover tiles and the
 * tribe detail header so a campfire keeps its colour wherever it appears.
 *
 * Values stay within a few shades of #0A0A0A so the app never lights up; each vibe
 * just leans toward its own hue (Design System v2.0 — darkness is the canvas).
 */
object VibePalette {

    /** Dark gradient per vibe (two stops, vertical). */
    fun gradient(vibe: String): List<Color> {
        fun c(hex: Long) = Color(hex)
        return when (vibe.trim().lowercase()) {
            "energetic" -> listOf(c(0xFF1A0A00), c(0xFF2D1200))
            "hype" -> listOf(c(0xFF1A001A), c(0xFF2D002D))
            "competitive" -> listOf(c(0xFF001A2D), c(0xFF00263D))
            "cozy" -> listOf(c(0xFF001A1A), c(0xFF002D2D))
            "chill" -> listOf(c(0xFF0A0A1A), c(0xFF12122D))
            "spooky" -> listOf(c(0xFF1A0A1A), c(0xFF000000))
            "peaceful" -> listOf(c(0xFF001A0A), c(0xFF002D12))
            "raw" -> listOf(c(0xFF1A1A1A), c(0xFF0A0A0A))
            "safe" -> listOf(c(0xFF001A2D), c(0xFF001A3D))
            "creative" -> listOf(c(0xFF1A1A00), c(0xFF2D2D00))
            "niche" -> listOf(c(0xFF1A001A), c(0xFF2D002D))
            "focused" -> listOf(c(0xFF001A1A), c(0xFF001A2D))
            "stressed" -> listOf(c(0xFF2D0000), c(0xFF1A0000))
            "geeky" -> listOf(c(0xFF001A00), c(0xFF002D00))
            "chaotic" -> listOf(c(0xFF2D1A00), c(0xFF1A0D00))
            "intense" -> listOf(c(0xFF2D0A00), c(0xFF1A0500))
            "calm" -> listOf(c(0xFF0A0A1A), c(0xFF10182D))
            "confessional" -> listOf(c(0xFF14141A), c(0xFF0A0A0A))
            "open" -> listOf(c(0xFF001A14), c(0xFF002D20))
            else -> listOf(c(0xFF161616), c(0xFF0C0C0C))
        }
    }

    /**
     * The vibe's brighter voice — used at low alpha for the glow behind a tribe's
     * emoji, never as a fill. Hues match the gradient family.
     */
    fun accent(vibe: String): Color {
        fun c(hex: Long) = Color(hex)
        return when (vibe.trim().lowercase()) {
            "energetic" -> c(0xFFFF8A3D)
            "hype" -> c(0xFFE879F9)
            "competitive" -> c(0xFF38BDF8)
            "cozy" -> c(0xFF2DD4BF)
            "chill" -> c(0xFF818CF8)
            "spooky" -> c(0xFFA855F7)
            "peaceful" -> c(0xFF34D399)
            "raw" -> c(0xFF9CA3AF)
            "safe" -> c(0xFF60A5FA)
            "creative" -> c(0xFFFDE047)
            "niche" -> c(0xFFE879F9)
            "focused" -> c(0xFF22D3EE)
            "stressed" -> c(0xFFF87171)
            "geeky" -> c(0xFF4ADE80)
            "chaotic" -> c(0xFFFB923C)
            "intense" -> c(0xFFF97316)
            "calm" -> c(0xFF93C5FD)
            "confessional" -> c(0xFFD4D4D8)
            "open" -> c(0xFF2DD4BF)
            else -> c(0xFF2DD4BF)
        }
    }
}
