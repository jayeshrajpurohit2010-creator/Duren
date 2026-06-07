package com.duren.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * The Campfire Reveal — Duren's signature launch animation (design `flow.jsx` →
 * CampfireReveal). Scattered embers gather inward to a glowing core, then the Ember
 * Mark + wordmark resolve out of it with a soft "KINDLING…" caption.
 *
 * Self-contained: reuses [FloatingEmbers] for ambient drift and [DurenLogo] for the
 * mark. Auto-dismisses via [onEnter] after [holdMillis]; a tap skips it early. Shown
 * once per cold start (the host keeps a rememberSaveable flag), never blocking the app.
 */
@Composable
fun CampfireRevealSplash(
    onEnter: () -> Unit,
    holdMillis: Long = 1800L
) {
    val gather = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        gather.animateTo(1f, animationSpec = tween(1500, easing = FastOutSlowInEasing))
    }
    // Auto-advance after the reveal has had a beat to breathe.
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(holdMillis)
        onEnter()
    }

    val particles = remember {
        val rnd = Random(11)
        List(26) {
            Particle(
                angle = rnd.nextFloat() * 6.2831855f,
                distance = 0.18f + rnd.nextFloat() * 0.30f, // fraction of min dimension
                size = 1.6f + rnd.nextFloat() * 2.6f,
                lead = rnd.nextFloat() * 0.25f               // staggered convergence
            )
        }
    }

    val progress = gather.value
    val wordmarkAlpha = ((progress - 0.5f) / 0.5f).coerceIn(0f, 1f)
    val capAlpha = ((progress - 0.78f) / 0.22f).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onEnter
            ),
        contentAlignment = Alignment.Center
    ) {
        // Teal ceiling glow, matching the design's radial backdrop.
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Teal.copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.54f),
                    radius = maxOf(size.width, size.height) * 0.6f
                )
            )
        }

        FloatingEmbers(modifier = Modifier.fillMaxSize(), count = 10)

        // Gathering particles + central glow burst.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val minDim = minOf(size.width, size.height)

            // Core glow swells as the embers arrive.
            val burst = sin(progress * Math.PI.toFloat())
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Amber.copy(alpha = 0.5f * burst), Color.Transparent),
                    center = center,
                    radius = minDim * (0.06f + 0.22f * progress)
                ),
                radius = minDim * (0.06f + 0.22f * progress),
                center = center
            )

            particles.forEach { p ->
                // Each particle finishes converging slightly before the whole reveal,
                // so they appear to be absorbed into the forming mark.
                val local = ((progress - p.lead) / (1f - p.lead)).coerceIn(0f, 1f)
                val startR = minDim * p.distance
                val r = startR * (1f - local)
                val pos = Offset(
                    center.x + cos(p.angle) * r,
                    center.y + sin(p.angle) * r * 0.9f
                )
                val alpha = when {
                    local < 0.1f -> local / 0.1f
                    local > 0.8f -> (1f - local) / 0.2f
                    else -> 1f
                }.coerceIn(0f, 1f)
                val px = p.size.dp.toPx()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Glow.copy(alpha = alpha), Glow.copy(alpha = 0f)),
                        center = pos,
                        radius = px * 3f
                    ),
                    radius = px * 3f,
                    center = pos
                )
                drawCircle(color = Core.copy(alpha = alpha), radius = px, center = pos)
            }
        }

        // The reveal: mark + wordmark + caption.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.alpha(wordmarkAlpha)
        ) {
            DurenLogo(size = 84.dp, animated = true)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "duren",
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1.5).sp,
                color = Color.White
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "KINDLING…",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 3.sp,
                color = Color(0xFF8A8A92),
                modifier = Modifier.alpha(capAlpha)
            )
        }
    }
}

private data class Particle(
    val angle: Float,
    val distance: Float,
    val size: Float,
    val lead: Float
)

private val Teal = Color(0xFF2DD4BF)
private val Amber = Color(0xFFFFA040)
private val Glow = Color(0xFFFFA040)
private val Core = Color(0xFFFFD080)
