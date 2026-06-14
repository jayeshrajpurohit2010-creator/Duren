package com.duren.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The active set of Duren color tokens for the current theme.
 *
 * [DurenColors] (dark) and [DurenColorsLight] are fixed objects — reading them
 * directly bakes the dark palette into a screen, so in light mode that screen's
 * white text lands on a white background and vanishes. Components should read the
 * *active* palette through [LocalDurenColors] instead, so they recolor correctly
 * when the user flips light mode.
 *
 * Property names mirror [DurenColors] one-for-one, so a usage is a clean swap:
 * `DurenColors.TextPrimary` → `LocalDurenColors.current.TextPrimary`.
 *
 * Brand, semantic, temperature and timer hues are shared across both themes (they
 * read fine on either background); only backgrounds, surfaces, text and borders
 * actually invert.
 */
class DurenPalette(
    // True for the dark (default) theme. Lets dark-only flourishes — e.g. the feed's
    // campfire ambience — switch themselves off in light mode instead of looking muddy.
    val isDark: Boolean,
    val BackgroundPrimary: Color,
    val BackgroundSecondary: Color,
    val BackgroundTertiary: Color,
    val Glass: Color,
    val SurfacePrimary: Color,
    val SurfaceElevated: Color,
    val SurfacePressed: Color,
    val AccentTeal: Color,
    val AccentGreen: Color,
    val AccentTealGlow: Color,
    val TextPrimary: Color,
    val TextSecondary: Color,
    val TextMuted: Color,
    val TextDisabled: Color,
    val BorderDefault: Color,
    val BorderFocused: Color,
    val SemanticError: Color,
    val SemanticWarning: Color,
    val SemanticSuccess: Color,
    val SemanticInfo: Color,
    val OnAccent: Color,
    val TempCold: Color,
    val TempWarm: Color,
    val TempHot: Color,
    val TempBlazing: Color,
    val TempDrumCircle: Color,
    val TimerNormal: Color,
    val TimerWarning: Color,
    val TimerCritical: Color
)

/** Dark palette — the brand default, straight from [DurenColors]. */
val DarkDurenPalette = DurenPalette(
    isDark = true,
    BackgroundPrimary = DurenColors.BackgroundPrimary,
    BackgroundSecondary = DurenColors.BackgroundSecondary,
    BackgroundTertiary = DurenColors.BackgroundTertiary,
    Glass = DurenColors.Glass,
    SurfacePrimary = DurenColors.SurfacePrimary,
    SurfaceElevated = DurenColors.SurfaceElevated,
    SurfacePressed = DurenColors.SurfacePressed,
    AccentTeal = DurenColors.AccentTeal,
    AccentGreen = DurenColors.AccentGreen,
    AccentTealGlow = DurenColors.AccentTealGlow,
    TextPrimary = DurenColors.TextPrimary,
    TextSecondary = DurenColors.TextSecondary,
    TextMuted = DurenColors.TextMuted,
    TextDisabled = DurenColors.TextDisabled,
    BorderDefault = DurenColors.BorderDefault,
    BorderFocused = DurenColors.BorderFocused,
    SemanticError = DurenColors.SemanticError,
    SemanticWarning = DurenColors.SemanticWarning,
    SemanticSuccess = DurenColors.SemanticSuccess,
    SemanticInfo = DurenColors.SemanticInfo,
    OnAccent = DurenColors.OnAccent,
    TempCold = DurenColors.TempCold,
    TempWarm = DurenColors.TempWarm,
    TempHot = DurenColors.TempHot,
    TempBlazing = DurenColors.TempBlazing,
    TempDrumCircle = DurenColors.TempDrumCircle,
    TimerNormal = DurenColors.TimerNormal,
    TimerWarning = DurenColors.TimerWarning,
    TimerCritical = DurenColors.TimerCritical
)

/** Light palette — surfaces/text/borders invert; brand + status hues stay put. */
val LightDurenPalette = DurenPalette(
    isDark = false,
    BackgroundPrimary = DurenColorsLight.BackgroundPrimary,
    BackgroundSecondary = DurenColorsLight.BackgroundSecondary,
    BackgroundTertiary = DurenColorsLight.BackgroundTertiary,
    Glass = Color(0xFFFFFFFF).copy(alpha = 0.6f),
    SurfacePrimary = DurenColorsLight.SurfacePrimary,
    SurfaceElevated = DurenColorsLight.SurfaceElevated,
    SurfacePressed = DurenColorsLight.SurfacePressed,
    AccentTeal = DurenColorsLight.AccentTeal,
    AccentGreen = DurenColorsLight.AccentGreen,
    AccentTealGlow = DurenColors.AccentTealGlow,
    TextPrimary = DurenColorsLight.TextPrimary,
    TextSecondary = DurenColorsLight.TextSecondary,
    TextMuted = DurenColorsLight.TextMuted,
    TextDisabled = DurenColorsLight.TextDisabled,
    BorderDefault = DurenColorsLight.BorderDefault,
    BorderFocused = DurenColorsLight.BorderFocused,
    SemanticError = DurenColorsLight.SemanticError,
    SemanticWarning = DurenColors.SemanticWarning,
    SemanticSuccess = DurenColors.SemanticSuccess,
    SemanticInfo = DurenColors.SemanticInfo,
    OnAccent = DurenColorsLight.OnAccent,
    TempCold = DurenColors.TempCold,
    TempWarm = DurenColors.TempWarm,
    TempHot = DurenColors.TempHot,
    TempBlazing = DurenColors.TempBlazing,
    TempDrumCircle = DurenColors.TempDrumCircle,
    TimerNormal = DurenColors.TimerNormal,
    TimerWarning = DurenColors.TimerWarning,
    TimerCritical = DurenColors.TimerCritical
)

/**
 * The palette in force for the current theme. Defaults to dark; [DurenTheme]
 * provides the light set when the user has light mode on.
 */
val LocalDurenColors = staticCompositionLocalOf { DarkDurenPalette }
