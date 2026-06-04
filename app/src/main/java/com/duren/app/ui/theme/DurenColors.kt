package com.duren.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Duren color tokens — DARK mode (default).
 *
 * Source of truth: PRD v2.1 §3.3 Settings amendment (June 4, 2026). The default
 * accent is Crystal Blue teal #2dd4bf (supersedes the older #41CBBF from Design
 * System v2.0). Light-mode equivalents live in [DurenColorsLight]; the user's
 * chosen accent ([DurenAccent]) is injected at theme level.
 */
object DurenColors {
    // Backgrounds
    val BackgroundPrimary = Color(0xFF0A0A0A)
    val BackgroundSecondary = Color(0xFF121212)
    val BackgroundTertiary = Color(0xFF1A1A1C)
    val Glass = Color(0xFF141416).copy(alpha = 0.55f)

    // Surfaces (cards)
    val SurfacePrimary = Color(0xFF1E1E1E)
    val SurfaceElevated = Color(0xFF252528)
    val SurfacePressed = Color(0xFF2A2A2D)

    // Accents (June 4 spec)
    val AccentTeal = Color(0xFF2DD4BF)        // Crystal Blue — primary default
    val AccentGreen = Color(0xFF10B981)       // secondary
    val AccentTealGlow = Color(0xFF2DD4BF).copy(alpha = 0.3f)

    // Text
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFB4B4BB)
    val TextMuted = Color(0xFF94A3B8)
    val TextDisabled = Color(0xFF555560)

    // Borders
    val BorderDefault = Color(0xFF2A2D37)
    val BorderFocused = Color(0xFF2DD4BF)

    // Semantic
    val SemanticError = Color(0xFFFF4D4F)
    val SemanticWarning = Color(0xFFFFB020)
    val SemanticSuccess = Color(0xFF10B981)
    val SemanticInfo = Color(0xFF2DD4BF)

    // Button text on teal — near-black, never white (brief requirement)
    val OnAccent = Color(0xFF1A1A1A)

    // Temperature spectrum (Phase 1+)
    val TempCold = Color(0xFF94A3B8)
    val TempWarm = Color(0xFFB4B4BB)
    val TempHot = Color(0xFF2DD4BF)
    val TempBlazing = Color(0xFFFFA040)
    val TempDrumCircle = Color(0xFFFFD700)

    // Timer states (Phase 1+)
    val TimerNormal = Color(0xFF94A3B8)
    val TimerWarning = Color(0xFF2DD4BF)
    val TimerCritical = Color(0xFFFF4D4F)
}
