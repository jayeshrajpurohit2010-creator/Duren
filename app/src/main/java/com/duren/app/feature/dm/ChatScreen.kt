package com.duren.app.feature.dm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duren.app.data.dm.model.DmMessage
import com.duren.app.ui.theme.DurenShapes
import com.duren.app.ui.theme.DurenSpacing
import com.google.firebase.Timestamp

/** A single live conversation. Messages stream in realtime and fade after 48h. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val other by viewModel.otherProfile.collectAsStateWithLifecycle()
    val seenByOther by viewModel.seenByOther.collectAsStateWithLifecycle()
    val me = viewModel.currentUserId

    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Realtime: every new snapshot marks the thread seen and rides the scroll down.
    LaunchedEffectMarkSeenAndScroll(messages.size, viewModel, listState)

    val title = other?.let { it.displayName.ifBlank { it.username } } ?: "Ember"
    val lastMine = messages.lastOrNull { it.senderId == me }?.id

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DurenSpacing.space3, vertical = DurenSpacing.space2),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Say something. It fades in 48h.") },
                        maxLines = 4
                    )
                    Spacer(Modifier.width(DurenSpacing.space2))
                    IconButton(
                        onClick = {
                            viewModel.send(input)
                            input = ""
                        },
                        enabled = input.isNotBlank()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (input.isNotBlank()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = DurenSpacing.space4),
            verticalArrangement = Arrangement.spacedBy(DurenSpacing.space2)
        ) {
            items(messages, key = { it.id }) { message ->
                MessageBubble(
                    message = message,
                    mine = message.senderId == me,
                    showSeen = message.id == lastMine,
                    seenByOther = seenByOther
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: DmMessage,
    mine: Boolean,
    showSeen: Boolean,
    seenByOther: Boolean
) {
    val bubbleColor = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    // Locked design rule: text on teal is always near-black, never white.
    val textColor = if (mine) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(color = bubbleColor, shape = DurenShapes.large)
                .padding(horizontal = DurenSpacing.space3, vertical = DurenSpacing.space2)
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = remainingLabel(message.expiresAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (mine && showSeen) {
                Spacer(Modifier.width(DurenSpacing.space2))
                Text(
                    text = if (seenByOther) "Seen" else "Sent",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = if (seenByOther) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Human "fades in Xh/Xm" from an expiry timestamp. */
private fun remainingLabel(expiresAt: Timestamp?): String {
    expiresAt ?: return ""
    val ms = expiresAt.toDate().time - System.currentTimeMillis()
    if (ms <= 0) return "fading…"
    val hours = ms / 3_600_000
    val minutes = (ms % 3_600_000) / 60_000
    return if (hours > 0) "fades in ${hours}h" else "fades in ${minutes}m"
}

/**
 * Side-effect helper: when the message count changes, mark the thread seen and
 * scroll to the newest. Kept as its own composable so the import stays tidy.
 */
@Composable
private fun LaunchedEffectMarkSeenAndScroll(
    count: Int,
    viewModel: ChatViewModel,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    androidx.compose.runtime.LaunchedEffect(count) {
        viewModel.markSeen()
        if (count > 0) listState.animateScrollToItem(count - 1)
    }
}
