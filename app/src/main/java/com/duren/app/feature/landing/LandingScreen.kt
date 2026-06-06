package com.duren.app.feature.landing

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duren.app.ui.components.DurenLogo
import com.duren.app.ui.components.FloatingEmbers
import com.duren.app.ui.theme.DurenSpacing

/**
 * The brand landing — the first thing a signed-out visitor sees, ported from the
 * design export's Hero: teal ceiling glow + warm floor glow, drifting embers, the
 * Ember Mark, the lowercase wordmark with a breathing pulse, the three-colour
 * tagline, and one warm CTA into auth.
 */
@Composable
fun LandingScreen(onGetStarted: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 900),
        label = "landingAlpha"
    )
    val rise by animateDpAsState(
        targetValue = if (visible) 0.dp else 20.dp,
        animationSpec = tween(durationMillis = 900),
        label = "landingRise"
    )

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A))) {
        // Ambient glows.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Teal.copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.40f),
                    radius = maxOf(w, h) * 0.62f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Warm.copy(alpha = 0.14f), Color.Transparent),
                    center = Offset(w * 0.5f, h * 1.02f),
                    radius = w * 0.9f
                )
            )
        }

        FloatingEmbers(modifier = Modifier.fillMaxSize(), count = 20)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = DurenSpacing.space6, vertical = DurenSpacing.space6),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))

            DurenLogo(size = 124.dp, animated = true)

            Spacer(Modifier.height(DurenSpacing.space5))

            Column(
                modifier = Modifier
                    .offset(y = rise)
                    .alpha(contentAlpha),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "duren",
                    fontSize = 68.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-2.5).sp,
                    color = Color.White
                )
                Spacer(Modifier.height(DurenSpacing.space3))
                DurenPulse()
                Spacer(Modifier.height(DurenSpacing.space5))
                Row(horizontalArrangement = Arrangement.spacedBy(DurenSpacing.space2)) {
                    Text("Ephemeral.", style = MaterialTheme.typography.labelLarge, color = Teal, fontWeight = FontWeight.Medium)
                    Text("Present.", style = MaterialTheme.typography.labelLarge, color = Green, fontWeight = FontWeight.Medium)
                    Text("Belong.", style = MaterialTheme.typography.labelLarge, color = Warm, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(DurenSpacing.space3))
                Text(
                    text = "A social network designed to let you go.",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFB4B4BB),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .alpha(contentAlpha),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Teal,
                    // Locked rule: text on teal is always near-black, never white.
                    contentColor = Color(0xFF1A1A1A)
                )
            ) {
                Text("Get started", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(DurenSpacing.space1))

            TextButton(onClick = onGetStarted, modifier = Modifier.alpha(contentAlpha)) {
                Text("I already have an account", color = Color(0xFF8A8A92))
            }

            Spacer(Modifier.height(DurenSpacing.space4))
        }
    }
}

/** The breathing teal pulse line under the wordmark (design `pulse-line`). */
@Composable
private fun DurenPulse(width: androidx.compose.ui.unit.Dp = 150.dp, height: androidx.compose.ui.unit.Dp = 5.dp) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scaleX by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    Canvas(
        modifier = Modifier
            .width(width)
            .height(height)
            .graphicsLayer { this.scaleX = scaleX }
    ) {
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, Teal, Green, Teal, Color.Transparent)
            ),
            cornerRadius = CornerRadius(size.height / 2f)
        )
    }
}

private val Teal = Color(0xFF2DD4BF)
private val Green = Color(0xFF2BB673)
private val Warm = Color(0xFFFFA040)
