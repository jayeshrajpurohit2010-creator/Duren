package com.duren.app.feature.signal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duren.app.data.signal.model.Signal
import com.duren.app.data.signal.model.SignalType
import com.duren.app.ui.animation.EmptyState
import com.duren.app.ui.animation.pressableCard
import com.duren.app.ui.components.DurenAvatar
import com.duren.app.ui.theme.DurenSpacing

/** The Signal inbox — echoes, whispers, Nest requests and DMs, newest first. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignalScreen(
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onOpenChat: (String) -> Unit,
    viewModel: SignalViewModel = hiltViewModel()
) {
    val signals by viewModel.signals.collectAsStateWithLifecycle()

    // Opening the screen is the "read" action.
    LaunchedEffect(Unit) { viewModel.markAllRead() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Signals") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (signals.isEmpty()) {
            EmptyState(
                title = "All quiet.",
                body = "Embers are warm.",
                emoji = "✨",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = DurenSpacing.space4),
                verticalArrangement = Arrangement.spacedBy(DurenSpacing.space1)
            ) {
                items(signals, key = { it.id }) { signal ->
                    SignalRow(
                        signal = signal,
                        onClick = {
                            when (signal.type) {
                                SignalType.Dm -> onOpenChat(signal.fromUserId)
                                else -> onOpenProfile(signal.fromUserId)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SignalRow(signal: Signal, onClick: () -> Unit) {
    val handle = signal.fromProfile?.username?.takeIf { it.isNotBlank() }?.let { "@$it" } ?: "Someone"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressableCard(onClick = onClick)
            .padding(vertical = DurenSpacing.space2),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(44.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
            DurenAvatar(
                avatarUrl = signal.fromProfile?.avatarUrl,
                fallbackColorHex = signal.fromProfile?.avatarColor ?: "#FF6B35",
                size = 44.dp,
                contentDescription = null
            )
        }
        Spacer(Modifier.width(DurenSpacing.space3))
        Column(Modifier.weight(1f)) {
            Text(
                text = "$handle ${verbFor(signal.type)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (signal.isRead) FontWeight.Normal else FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (signal.preview.isNotBlank()) {
                Text(
                    text = "“${signal.preview}”",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (!signal.isRead) {
            Spacer(Modifier.width(DurenSpacing.space2))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

private fun verbFor(type: SignalType): String = when (type) {
    SignalType.NestRequest -> "wants to join your Nest"
    SignalType.NestAccepted -> "joined your Nest"
    SignalType.Echo -> "echoed your ember"
    SignalType.Whisper -> "whispered on your ember"
    SignalType.Dm -> "sent you an ember"
    SignalType.Nudge -> "nudged you tonight 👀"
    SignalType.Unknown -> "did something"
}
