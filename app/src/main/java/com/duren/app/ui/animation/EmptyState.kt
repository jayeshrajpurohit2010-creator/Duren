package com.duren.app.ui.animation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.duren.app.ui.components.DurenIcon
import com.duren.app.ui.theme.DurenSpacing

/**
 * A10 — Empty state. Campfire emoji + brand-voice copy, fade-in with subtle
 * flicker (alpha 0.85 ↔ 1.0 over 800ms).
 */
@Composable
fun EmptyState(
    title: String,
    body: String? = null,
    // The brand mark for this empty state — a hand-drawn glyph, never an emoji.
    // Defaults to the Ember. (The old `emoji` param is kept as a no-op so any
    // straggler caller still compiles; we always draw the vector glyph.)
    icon: DurenIcon? = null,
    emoji: String = "",
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "empty_flicker")
    val flicker by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "empty_alpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(DurenSpacing.space6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        DurenIcon(
            icon = icon ?: DurenIcon.Ember,
            size = 56.dp,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.alpha(flicker)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = DurenSpacing.space4)
        )
        if (body != null) {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = DurenSpacing.space2)
            )
        }
    }
}
