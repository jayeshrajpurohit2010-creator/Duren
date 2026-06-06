package com.duren.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private data class Ember(
    val x: Float,        // 0..1 horizontal start
    val phase: Float,    // 0..1 offset into the rise loop
    val radiusDp: Float,
    val speed: Float,    // multiplies the base rate (slow/med/fast)
    val drift: Float     // px of horizontal sway
)

/**
 * The drifting ember particle layer from the design (`FloatingEmbers`): warm motes
 * that rise, sway, and fade — the campfire's sparks. One animation driver powers all
 * of them (cheap), each offset by a stable phase so they look independent.
 */
@Composable
fun FloatingEmbers(
    modifier: Modifier = Modifier,
    count: Int = 18
) {
    val embers = remember(count) {
        val rnd = Random(7)
        List(count) {
            Ember(
                x = rnd.nextFloat(),
                phase = rnd.nextFloat(),
                radiusDp = 1.2f + rnd.nextFloat() * 2.2f,
                speed = listOf(0.6f, 1f, 1.5f)[rnd.nextInt(3)],
                drift = (rnd.nextFloat() - 0.5f) * 64f
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "embers")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(14000, easing = LinearEasing)),
        label = "embersT"
    )

    Canvas(modifier = modifier) {
        embers.forEach { e ->
            val progress = ((t * e.speed) + e.phase) % 1f
            val y = size.height * (1f - progress)
            val x = size.width * e.x + e.drift * sin(progress * 2f * PI.toFloat())
            val alpha = when {
                progress < 0.12f -> progress / 0.12f
                progress > 0.75f -> (1f - progress) / 0.25f
                else -> 1f
            }.coerceIn(0f, 1f) * 0.9f

            val r = e.radiusDp.dp.toPx()
            val center = Offset(x, y)
            // Soft halo + bright core.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(EmberGlow.copy(alpha = alpha), EmberGlow.copy(alpha = 0f)),
                    center = center,
                    radius = r * 3.2f
                ),
                radius = r * 3.2f,
                center = center
            )
            drawCircle(color = EmberCore.copy(alpha = alpha), radius = r, center = center)
        }
    }
}

private val EmberCore = Color(0xFFFFD080)
private val EmberGlow = Color(0xFFFFA040)
