package com.duren.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * User-selectable accent colors and avatar preset colors.
 * Source: PRD v2.1 §3.3 Settings amendment (June 4, 2026).
 *
 * The chosen accent applies globally to buttons, links, active icons, and the
 * echo filled state (Phase 1+). Stored on the profile as a hex string.
 */
data class AccentOption(val label: String, val color: Color, val hex: String)

object DurenAccent {
    val Teal = AccentOption("Teal", Color(0xFF2DD4BF), "#2dd4bf")
    val Pink = AccentOption("Pink", Color(0xFFEC4899), "#ec4899")
    val Blue = AccentOption("Blue", Color(0xFF3B82F6), "#3b82f6")
    val Purple = AccentOption("Purple", Color(0xFFA855F7), "#a855f7")
    val Orange = AccentOption("Orange", Color(0xFFF97316), "#f97316")
    val Green = AccentOption("Green", Color(0xFF10B981), "#10b981")
    val Red = AccentOption("Red", Color(0xFFEF4444), "#ef4444")
    val Yellow = AccentOption("Yellow", Color(0xFFF59E0B), "#f59e0b")
    val Cyan = AccentOption("Cyan", Color(0xFF06B6D4), "#06b6d4")
    val Magenta = AccentOption("Magenta", Color(0xFFD946EF), "#d946ef")

    /** Ordered list for the Settings picker grid. Teal is the default. */
    val all: List<AccentOption> = listOf(
        Teal, Pink, Blue, Purple, Orange, Green, Red, Yellow, Cyan, Magenta
    )

    val default: AccentOption = Teal
    const val DEFAULT_HEX: String = "#2dd4bf"

    /** Resolve a stored hex string to its Color, falling back to the default accent. */
    fun colorForHex(hex: String?): Color = parseHex(hex) ?: default.color
}

/**
 * The 8 warm avatar preset circles (for users without a custom upload).
 * Source: PRD v2.1 §3.3 (June 4, 2026).
 */
data class AvatarColorOption(val label: String, val color: Color, val hex: String)

object DurenAvatarColors {
    val all: List<AvatarColorOption> = listOf(
        AvatarColorOption("Coral", Color(0xFFFF6B35), "#FF6B35"),
        AvatarColorOption("Orange", Color(0xFFFF8C42), "#FF8C42"),
        AvatarColorOption("Peach", Color(0xFFFFB347), "#FFB347"),
        AvatarColorOption("Sand", Color(0xFFF4A261), "#F4A261"),
        AvatarColorOption("Gold", Color(0xFFE9C46A), "#E9C46A"),
        AvatarColorOption("Warm Brown", Color(0xFFD4A373), "#D4A373"),
        AvatarColorOption("Terracotta", Color(0xFFE76F51), "#E76F51"),
        AvatarColorOption("Amber", Color(0xFFF7B05E), "#F7B05E")
    )

    val default: AvatarColorOption = all.first()
    const val DEFAULT_HEX: String = "#FF6B35"

    fun colorForHex(hex: String?): Color = parseHex(hex) ?: default.color
}

/** Parse a "#rrggbb" hex string to a Color. Returns null on malformed input. */
internal fun parseHex(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    val cleaned = hex.removePrefix("#")
    if (cleaned.length != 6) return null
    val value = cleaned.toLongOrNull(16) ?: return null
    return Color(0xFF000000 or value)
}
