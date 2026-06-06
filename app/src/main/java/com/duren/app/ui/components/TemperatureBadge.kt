package com.duren.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.duren.app.ui.theme.DurenShapes
import com.duren.app.ui.theme.DurenSpacing
import com.duren.app.ui.theme.Temperature

/**
 * A small pill badge showing the temperature of an ember derived from its echo count.
 * Temperature is a presence signal — not a trophy. The icon is a hand-drawn
 * [EmberGlyph] (no emoji); the hottest tier breathes via a slow bloom pulse.
 */
@Composable
fun TemperatureBadge(
    echoCount: Int,
    modifier: Modifier = Modifier
) {
    val temperature = Temperature.fromEchoCount(echoCount)
    val bgColor = temperature.color.copy(alpha = 0.12f)

    // Only the Drum Circle tier animates — a slow glow breath, matching the card border.
    val pulse: Float = if (temperature == Temperature.DrumCircle) {
        val transition = rememberInfiniteTransition(label = "emberPulse")
        val animated by transition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "emberPulseValue"
        )
        animated
    } else {
        1f
    }

    Row(
        modifier = modifier
            .background(color = bgColor, shape = DurenShapes.pill)
            .padding(horizontal = DurenSpacing.space2, vertical = DurenSpacing.space1),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EmberGlyph(temperature = temperature, size = 14.dp, pulse = pulse)
        Spacer(modifier = Modifier.width(DurenSpacing.space1))
        Text(
            text = temperature.label,
            style = MaterialTheme.typography.labelSmall,
            color = temperature.color
        )
    }
}
