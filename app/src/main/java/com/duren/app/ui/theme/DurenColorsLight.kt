package com.duren.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Duren color tokens — LIGHT mode (optional, opt-in via Settings).
 *
 * Source: PRD v2.1 §3.3 Settings amendment (June 4, 2026). Accents stay identical
 * to dark mode (#2dd4bf teal, #10b981 green); only surfaces and text invert.
 */
object DurenColorsLight {
    // Backgrounds
    val BackgroundPrimary = Color(0xFFFAFAFA)
    val BackgroundSecondary = Color(0xFFFFFFFF)
    val BackgroundTertiary = Color(0xFFF0F0F0)

    // Surfaces (cards)
    val SurfacePrimary = Color(0xFFFFFFFF)
    val SurfaceElevated = Color(0xFFF5F5F5)
    val SurfacePressed = Color(0xFFEDEDED)

    // Accents — SAME as dark mode
    val AccentTeal = Color(0xFF2DD4BF)
    val AccentGreen = Color(0xFF10B981)

    // Text
    val TextPrimary = Color(0xFF0A0A0A)
    val TextSecondary = Color(0xFF555560)
    val TextMuted = Color(0xFF6B7280)
    val TextDisabled = Color(0xFFB0B0B8)

    // Borders
    val BorderDefault = Color(0xFFE2E0DA)
    val BorderFocused = Color(0xFF2DD4BF)

    // Semantic (same hues read fine on light)
    val SemanticError = Color(0xFFE5484D)

    // Button text on teal — near-black on light too
    val OnAccent = Color(0xFF1A1A1A)
}
