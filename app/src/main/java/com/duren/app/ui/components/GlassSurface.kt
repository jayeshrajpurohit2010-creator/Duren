package com.duren.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.duren.app.ui.theme.DurenShapes
import com.duren.app.ui.theme.LocalDurenColors

/**
 * Glassmorphism — Duren's surface language for chrome and overlays.
 *
 * A frosted panel is three quiet layers: a translucent fill, a faint top-down
 * sheen, and a hairline rim. True backdrop blur is API 31+ and fragile to wire
 * across scaffolds, so we render the *frosted* look with translucency + sheen
 * instead — it reads as glass on every device from minSdk 24 up, and the whole
 * treatment lives here so real blur can be layered in later without touching a
 * single call site.
 *
 * CONTRAST RULE (this is exactly why light mode broke before): a glass surface
 * must take its fill AND its text from the SAME palette. [glass] / [GlassSurface]
 * fill themselves from [LocalDurenColors] (dark-translucent in dark mode,
 * light-translucent in light mode), so content placed on them must also read from
 * the theme (`MaterialTheme.colorScheme` / `LocalDurenColors`) — never the fixed
 * `OnDark*` tokens, which belong only to the never-inverting vibe gradient. Match
 * the text's invert behaviour to the surface it sits on and the bug can't return.
 */
@Composable
fun Modifier.glass(
    shape: Shape = DurenShapes.large,
    strong: Boolean = false,
    drawBorder: Boolean = true,
): Modifier {
    val colors = LocalDurenColors.current
    val fill = if (strong) colors.GlassStrong else colors.Glass
    // The sheen sells "frosted" — a bright wash at the top fading to nothing. Brighter
    // in light mode (white reads as a sheen on pale glass), barely-there in the dark.
    val sheen = Brush.verticalGradient(
        0f to Color.White.copy(alpha = if (colors.isDark) 0.05f else 0.40f),
        0.6f to Color.Transparent,
    )
    return this
        .clip(shape)
        .background(fill, shape)
        .background(sheen, shape)
        .then(if (drawBorder) Modifier.border(1.dp, colors.GlassBorder, shape) else Modifier)
}

/**
 * A frosted panel/card. Use for overlay content (notification rows, request cards,
 * dialog bodies). Set [strong] when the surface carries body text that must stay
 * crisp; leave it false for light decorative chrome.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = DurenShapes.large,
    strong: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.glass(shape = shape, strong = strong), content = content)
}

/**
 * Translucent (frosted) colours for a Material [androidx.compose.material3.TopAppBar].
 * The container is the theme's [com.duren.app.ui.theme.DurenPalette.Glass]; title and
 * icons read from the theme, so the bar stays legible whether it floats over the dark
 * feed or a pale light-mode screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun glassTopAppBarColors(): TopAppBarColors {
    val colors = LocalDurenColors.current
    return TopAppBarDefaults.topAppBarColors(
        containerColor = colors.Glass,
        scrolledContainerColor = colors.Glass,
        titleContentColor = colors.TextPrimary,
        navigationIconContentColor = colors.TextPrimary,
        actionIconContentColor = colors.TextSecondary,
    )
}

/**
 * The ambient wash an overlay screen floats its glass on. A near-black vertical
 * gradient in the dark (darkness is the canvas), a soft off-white one in light —
 * either way it gives the frosted panels something to be translucent *against*.
 */
@Composable
fun Modifier.glassBackdrop(): Modifier {
    val colors = LocalDurenColors.current
    val gradient = if (colors.isDark) {
        Brush.verticalGradient(listOf(Color(0xFF0B0B0D), Color(0xFF121214)))
    } else {
        Brush.verticalGradient(listOf(colors.BackgroundPrimary, colors.BackgroundSecondary))
    }
    return this.background(gradient)
}

/**
 * A full-screen [glassBackdrop] with a few embers drifting behind it (dark mode only —
 * the warm motes wash out on a pale surface). Wrap an overlay screen's transparent
 * Scaffold in this so its frosted panels have something alive to float over.
 */
@Composable
fun GlassBackdrop(
    modifier: Modifier = Modifier,
    embers: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = LocalDurenColors.current
    Box(modifier = modifier.glassBackdrop()) {
        if (embers && colors.isDark) {
            FloatingEmbers(modifier = Modifier.matchParentSize(), count = 10)
        }
        content()
    }
}
