package com.duren.app.feature.tabs

import androidx.compose.runtime.Composable
import com.duren.app.ui.animation.EmptyState

@Composable
fun NestTabScreen() {
    EmptyState(
        title = "Light a lantern.",
        body = "Someone wandering might find it.",
        emoji = "🏮"
    )
}
