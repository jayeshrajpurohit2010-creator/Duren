package com.duren.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The Duren Ember Mark — the brand logo, ported faithfully from the design export
 * (`primitives.jsx` → EmberMark). It's an irregular 7-point crystal shard with a
 * green→teal→gold→amber internal gradient, dark/light facets for a faceted-3D read,
 * a warm radial glow, and a white glint — all drawn in Compose, no image assets.
 *
 * The one intentional deviation from the export: the teal stop uses the LOCKED accent
 * `#2dd4bf` instead of `#41CBBF`, so the logo's teal matches the rest of the app.
 *
 * @param animated when true the glow breathes like a living ember.
 */
@Composable
fun DurenLogo(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    animated: Boolean = true
) {
    val transition = rememberInfiniteTransition(label = "emberMark")
    val breathe by transition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emberMarkBreathe"
    )
    val glowAlpha = if (animated) breathe else 1f

    Canvas(modifier = modifier.size(size)) {
        drawEmberMark(glowAlpha)
    }
}

private fun DrawScope.drawEmberMark(glowAlpha: Float) {
    // Everything below is in the export's 48×48 coordinate space, scaled to fit.
    val k = this.size.minDimension / 48f
    fun p(x: Float, y: Float) = Offset(x * k, y * k)
    fun shard(points: List<Pair<Float, Float>>): Path = Path().apply {
        moveTo(points[0].first * k, points[0].second * k)
        points.drop(1).forEach { lineTo(it.first * k, it.second * k) }
        close()
    }

    // Warm radial glow behind the shard.
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(EmberAmber.copy(alpha = 0.55f * glowAlpha), Color.Transparent),
            center = p(28.4f, 31.6f),
            radius = 30f * k
        ),
        radius = 23f * k,
        center = p(24f, 25f)
    )

    // Main shard silhouette.
    val body = shard(listOf(24f to 2f, 34f to 12f, 39f to 27f, 31f to 41f, 18f to 44f, 8f to 33f, 11f to 16f))
    drawPath(
        path = body,
        brush = Brush.linearGradient(
            colorStops = arrayOf(0f to EmberGreen, 0.38f to EmberTeal, 0.72f to EmberGold, 1f to EmberOrange),
            start = p(7.2f, 45.6f),
            end = p(40.8f, 4.8f)
        )
    )
    drawPath(path = body, color = Color(0xFF0A0A0A).copy(alpha = 0.25f), style = Stroke(width = 0.5f * k))

    // Internal facets — dark recesses + a lit face — give the shard depth.
    drawPath(shard(listOf(24f to 2f, 24f to 26f, 8f to 33f, 11f to 16f)), color = Color(0xFF0A0A0A).copy(alpha = 0.10f))
    drawPath(shard(listOf(24f to 2f, 34f to 12f, 24f to 26f)), color = Color.White.copy(alpha = 0.10f))
    drawPath(shard(listOf(24f to 26f, 39f to 27f, 31f to 41f)), color = Color(0xFF0A0A0A).copy(alpha = 0.16f))
    drawPath(shard(listOf(24f to 26f, 31f to 41f, 18f to 44f)), color = Color(0xFF0A0A0A).copy(alpha = 0.08f))

    // Glint.
    drawPath(
        path = shard(listOf(22f to 9f, 28f to 16f, 24f to 24f)),
        brush = Brush.linearGradient(
            colors = listOf(Color.White.copy(alpha = 0.34f), Color.White.copy(alpha = 0f)),
            start = p(0f, 48f),
            end = p(48f, 0f)
        )
    )
}

private val EmberGreen = Color(0xFF2BB673)
private val EmberTeal = Color(0xFF2DD4BF)
private val EmberGold = Color(0xFFFFC36B)
private val EmberOrange = Color(0xFFFF9438)
private val EmberAmber = Color(0xFFFFA040)
