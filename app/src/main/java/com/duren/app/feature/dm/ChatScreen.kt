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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duren.app.data.dm.model.DmMessage
import com.duren.app.ui.components.DurenIcon
import com.duren.app.ui.theme.DurenColors
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
        containerColor = DurenColors.BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = { Text(title, color = DurenColors.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DurenColors.TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DurenColors.BackgroundSecondary)
                    .padding(horizontal = DurenSpacing.space3, vertical = DurenSpacing.space2),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Whisper into the dark…", color = DurenColors.TextDisabled) },
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DurenColors.AccentTeal,
                        unfocusedBorderColor = DurenColors.BorderDefault,
                        focusedTextColor = DurenColors.TextPrimary,
                        unfocusedTextColor = DurenColors.TextPrimary,
                        cursorColor = DurenColors.AccentTeal
                    )
                )
                Spacer(Modifier.width(DurenSpacing.space2))
                IconButton(
                    onClick = {
                        val pending = input
                        input = ""
                        // Optimistic clear, but restore the text if the send fails.
                        viewModel.send(pending) { failed ->
                            if (input.isBlank()) input = failed
                        }
                    },
                    enabled = input.isNotBlank()
                ) {
                    DurenIcon(
                        DurenIcon.Send,
                        size = 22.dp,
                        tint = if (input.isNotBlank()) DurenColors.AccentTeal else DurenColors.TextDisabled
                    )
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
            verticalArrangement = Arrangement.spacedBy(DurenSpacing.space4)
        ) {
            items(messages, key = { it.id }) { message ->
                MessageLine(
                    message = message,
                    mine = message.senderId == me,
                    showSeen = message.id == lastMine,
                    seenByOther = seenByOther
                )
            }
        }
    }
}

/**
 * A message with no bubble — text floats on the darkness. Yours sits right with a
 * small teal mark; theirs sits left in muted grey. The timestamp is a quiet mono line.
 */
@Composable
private fun MessageLine(
    message: DmMessage,
    mine: Boolean,
    showSeen: Boolean,
    seenByOther: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.widthIn(max = 300.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (mine) {
                Box(
                    modifier = Modifier
                        .padding(top = 7.dp, end = DurenSpacing.space2)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(DurenColors.AccentTeal)
                )
            }
            Text(
                text = message.text,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                color = if (mine) DurenColors.TextPrimary else DurenColors.TextSecondary
            )
        }
        Spacer(Modifier.size(DurenSpacing.space1))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = remainingLabel(message.expiresAt),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = DurenColors.TextDisabled
            )
            if (mine && showSeen) {
                Spacer(Modifier.width(DurenSpacing.space2))
                Text(
                    text = if (seenByOther) "Seen" else "Sent",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (seenByOther) DurenColors.AccentTeal else DurenColors.TextDisabled
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
