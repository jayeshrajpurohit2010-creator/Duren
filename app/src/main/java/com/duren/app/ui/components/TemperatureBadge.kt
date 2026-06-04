package com.duren.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.duren.app.ui.theme.DurenShapes
import com.duren.app.ui.theme.DurenSpacing
import com.duren.app.ui.theme.Temperature

/**
 * A small pill badge showing the temperature of an ember derived from its echo count.
 * Temperature is a presence signal — not a trophy.
 */
@Composable
fun TemperatureBadge(
    echoCount: Int,
    modifier: Modifier = Modifier
) {
    val temperature = Temperature.fromEchoCount(echoCount)
    val bgColor = temperature.color.copy(alpha = 0.12f)

    Row(
        modifier = modifier
            .background(color = bgColor, shape = DurenShapes.pill)
            .padding(horizontal = DurenSpacing.space2, vertical = DurenSpacing.space1),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${temperature.emoji} ${temperature.label}",
            style = MaterialTheme.typography.labelSmall,
            color = temperature.color
        )
    }
}
