package com.duren.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.duren.app.core.time.NightPhase
import com.duren.app.ui.theme.DurenShapes
import com.duren.app.ui.theme.DurenSpacing

/**
 * A slim banner that announces the [NightPhase] at the top of the feed.
 *
 * Renders nothing during [NightPhase.Day]. Dead Hours gets a calm, dim treatment;
 * Morning Fade gets the spec's warm gold tint. Costs nothing to show — the phase is
 * a local clock read (see [com.duren.app.core.time.NightEconomy]).
 */
@Composable
fun NightBanner(
    phase: NightPhase,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = phase != NightPhase.Day,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        when (phase) {
            NightPhase.DeadHours -> Banner(
                modifier = modifier,
                accent = DeadHoursAccent,
                title = "The campfire's resting",
                body = "It's Dead Hours (2–3 AM). New embers wait until the night lifts — sleep, the fire will keep."
            )
            NightPhase.MorningFade -> Banner(
                modifier = modifier,
                accent = MorningGold,
                title = "Morning Fade",
                body = "Dawn's here. Last night's embers are cooling toward their end — catch them before they go."
            )
            NightPhase.Day -> Unit
        }
    }
}

@Composable
private fun Banner(
    modifier: Modifier,
    accent: Color,
    title: String,
    body: String
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DurenSpacing.space4, vertical = DurenSpacing.space2)
            .background(color = accent.copy(alpha = 0.10f), shape = DurenShapes.medium)
            .border(width = 1.dp, color = accent.copy(alpha = 0.40f), shape = DurenShapes.medium)
            .padding(DurenSpacing.space3)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = accent
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = DurenSpacing.space1)
        )
    }
}

// Warm gold for the dawn window; a muted slate-violet for the resting hours.
private val MorningGold = Color(0xFFFFC24D)
private val DeadHoursAccent = Color(0xFF8B95C9)
