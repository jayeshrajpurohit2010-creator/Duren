package com.duren.app.feature.tabs

import androidx.compose.runtime.Composable
import com.duren.app.ui.animation.EmptyState

@Composable
fun StateTabScreen() {
    EmptyState(
        title = "The campfire's quiet.",
        body = "Come back at 10PM.",
        emoji = "🏕️"
    )
}
