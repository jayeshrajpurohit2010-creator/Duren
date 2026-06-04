package com.duren.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.duren.app.ui.theme.DurenColors
import com.google.firebase.Timestamp
import kotlinx.coroutines.delay

private const val SIXTY_SECONDS_MS = 60_000L
private const val TWO_HOURS_SECONDS = 2L * 3600L
private const val THIRTY_MIN_SECONDS = 30L * 60L

/**
 * Displays a live countdown until an ember expires.
 * Refreshes every 60 seconds. Null [expiresAt] renders nothing.
 * If [extended], a subtle "· 72h" marker is appended — understated, not a badge.
 */
@Composable
fun ExpiryTimer(
    expiresAt: Timestamp?,
    extended: Boolean,
    modifier: Modifier = Modifier
) {
    if (expiresAt == null) return

    var remainingSeconds by remember {
        mutableLongStateOf(expiresAt.seconds - Timestamp.now().seconds)
    }

    LaunchedEffect(expiresAt) {
        while (true) {
            delay(SIXTY_SECONDS_MS)
            remainingSeconds = expiresAt.seconds - Timestamp.now().seconds
        }
    }

    val timerColor = when {
        remainingSeconds > TWO_HOURS_SECONDS -> DurenColors.TimerNormal
        remainingSeconds >= THIRTY_MIN_SECONDS -> DurenColors.TimerWarning
        else -> DurenColors.TimerCritical
    }

    val label = when {
        remainingSeconds <= 0L -> "fading"
        remainingSeconds >= 3600L -> "${remainingSeconds / 3600L}h left"
        else -> "${remainingSeconds / 60L}m left"
    }

    val text = buildAnnotatedString {
        withStyle(SpanStyle(color = timerColor)) {
            append(label)
        }
        if (extended) {
            withStyle(SpanStyle(color = DurenColors.TextMuted)) {
                append(" · 72h")
            }
        }
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier
    )
}
