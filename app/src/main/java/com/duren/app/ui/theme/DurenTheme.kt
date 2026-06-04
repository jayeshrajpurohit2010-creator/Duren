package com.duren.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Duren theme — applies brand tokens to Material 3.
 *
 * [darkTheme] and [accent] are driven by the signed-in user's settings
 * (lightModeEnabled + accentColor). Before a profile loads, callers pass the
 * defaults (dark mode, teal). The accent becomes MaterialTheme primary, so every
 * button/link/active control recolors globally when the user picks a new accent.
 */
@Composable
fun DurenTheme(
    darkTheme: Boolean = true,
    accent: Color = DurenAccent.default.color,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = accent,
            onPrimary = DurenColors.OnAccent,
            secondary = DurenColors.AccentGreen,
            onSecondary = DurenColors.OnAccent,
            background = DurenColors.BackgroundPrimary,
            onBackground = DurenColors.TextPrimary,
            surface = DurenColors.SurfacePrimary,
            onSurface = DurenColors.TextPrimary,
            surfaceVariant = DurenColors.SurfaceElevated,
            onSurfaceVariant = DurenColors.TextSecondary,
            outline = DurenColors.BorderDefault,
            error = DurenColors.SemanticError,
            onError = DurenColors.TextPrimary
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = DurenColorsLight.OnAccent,
            secondary = DurenColorsLight.AccentGreen,
            onSecondary = DurenColorsLight.OnAccent,
            background = DurenColorsLight.BackgroundPrimary,
            onBackground = DurenColorsLight.TextPrimary,
            surface = DurenColorsLight.SurfacePrimary,
            onSurface = DurenColorsLight.TextPrimary,
            surfaceVariant = DurenColorsLight.SurfaceElevated,
            onSurfaceVariant = DurenColorsLight.TextSecondary,
            outline = DurenColorsLight.BorderDefault,
            error = DurenColorsLight.SemanticError,
            onError = Color.White
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = durenTypography(),
        shapes = durenShapes(),
        content = content
    )
}

private fun durenTypography(): Typography = Typography(
    displayLarge = DurenType.DisplayLarge,
    displayMedium = DurenType.DisplayMedium,
    displaySmall = DurenType.DisplaySmall,
    headlineSmall = DurenType.DisplaySmall,
    titleLarge = DurenType.TitleLarge,
    titleMedium = DurenType.TitleMedium,
    bodyLarge = DurenType.BodyLarge,
    bodyMedium = DurenType.BodyMedium,
    labelLarge = DurenType.LabelLarge,
    labelSmall = DurenType.LabelSmall
)

private fun durenShapes(): Shapes = Shapes(
    small = DurenShapes.small,
    medium = DurenShapes.medium,
    large = DurenShapes.large,
    extraLarge = DurenShapes.extraLarge
)
