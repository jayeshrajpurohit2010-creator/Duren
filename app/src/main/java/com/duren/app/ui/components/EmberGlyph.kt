package com.duren.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.duren.app.ui.theme.Temperature

/**
 * A hand-drawn ember glyph — Duren's replacement for the temperature emoji.
 *
 * Drawn entirely in Compose [Canvas] (no image assets, no APK bloat), it reads as
 * a small flame with a soft radial "bloom" behind it for a faux-3D glow. The flame
 * grows taller and brighter, and the bloom wider, as the ember heats up; a cooled
 * ([Temperature.Cold]) ember is a quiet hollow ring instead of a flame.
 *
 * The flame body warms from the tier's accent colour toward [Amber] at its heart,
 * which is what gives it depth without any bitmap.
 *
 * @param pulse multiplies the bloom's reach — pass an animated value (Drum Circle)
 *        to make the hottest embers breathe; leave at 1f for a steady glyph.
 */
@Composable
fun EmberGlyph(
    temperature: Temperature,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    pulse: Float = 1f
) {
    Canvas(modifier = modifier.size(size)) {
        drawEmber(temperature, pulse)
    }
}

/** A warm amber the flame's core mixes toward — the source of its glow. */
private val Amber = Color(0xFFFFB347)

private fun DrawScope.drawEmber(temperature: Temperature, pulse: Float) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val base = temperature.color

    // Cooled ember: a soft hollow ring, no flame.
    if (temperature == Temperature.Cold) {
        drawCircle(
            color = base.copy(alpha = 0.40f),
            radius = w * 0.24f,
            center = Offset(cx, h * 0.55f),
            style = Stroke(width = w * 0.11f)
        )
        return
    }

    // How "alive" the flame is — drives height, brightness and bloom.
    val intensity = when (temperature) {
        Temperature.Warm -> 0.45f
        Temperature.Hot -> 0.70f
        Temperature.Blazing -> 0.88f
        Temperature.DrumCircle -> 1f
        else -> 0f
    }

    // Faux-3D bloom behind the flame — a radial gradient fading to transparent.
    val glowCenter = Offset(cx, h * 0.58f)
    val glowRadius = (w * (0.42f + 0.28f * intensity) * pulse).coerceAtLeast(0.1f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(base.copy(alpha = 0.42f * intensity), Color.Transparent),
            center = glowCenter,
            radius = glowRadius
        ),
        radius = glowRadius,
        center = glowCenter
    )

    // Outer flame body — taller (higher tip) the hotter it is.
    val flameTopY = h * (0.32f - 0.22f * intensity)
    val outer = flamePath(halfWidth = w * 0.34f, cx = cx, topY = flameTopY, bottomY = h * 0.97f)
    drawPath(
        path = outer,
        brush = Brush.verticalGradient(
            colors = listOf(base, lerp(base, Amber, 0.55f), base.copy(alpha = 0.92f)),
            startY = flameTopY,
            endY = h
        )
    )

    // Inner core — a smaller, brighter flame nested inside for depth.
    val coreTopY = h * (0.54f - 0.12f * intensity)
    val core = flamePath(halfWidth = w * 0.17f, cx = cx, topY = coreTopY, bottomY = h * 0.90f)
    drawPath(
        path = core,
        color = lerp(base, Color.White, 0.45f).copy(alpha = 0.88f)
    )
}

/** A symmetric tear-drop flame from a pointed [topY] down to a rounded [bottomY]. */
private fun flamePath(halfWidth: Float, cx: Float, topY: Float, bottomY: Float): Path {
    val left = cx - halfWidth
    val right = cx + halfWidth
    val span = bottomY - topY
    return Path().apply {
        moveTo(cx, topY)
        cubicTo(right, topY + span * 0.35f, right, topY + span * 0.78f, cx, bottomY)
        cubicTo(left, topY + span * 0.78f, left, topY + span * 0.35f, cx, topY)
        close()
    }
}
