package com.duren.app.ui.animation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.duren.app.ui.theme.DurenShapes
import com.duren.app.ui.theme.DurenSpacing
import kotlinx.coroutines.delay

/**
 * A5 — Toast slide-in. 300ms slide up + 1400ms hold + 300ms slide out.
 */
@Composable
fun DurenToast(
    message: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visible = message != null

    LaunchedEffect(message) {
        if (visible) {
            delay(2000)
            onDismiss()
        }
    }

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .padding(DurenSpacing.space4)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = DurenShapes.large
                    )
                    .padding(horizontal = DurenSpacing.space5, vertical = DurenSpacing.space3)
            ) {
                Text(
                    text = message.orEmpty(),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
