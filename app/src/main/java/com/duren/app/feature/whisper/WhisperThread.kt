package com.duren.app.feature.whisper

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duren.app.data.ember.model.Whisper
import com.duren.app.ui.components.DurenAvatar
import com.duren.app.ui.theme.DurenSpacing

/**
 * Inline whisper (comment) thread shown under an expanded ember.
 *
 * Each card gets its own [WhisperViewModel] keyed by [emberId], so streams only
 * run while a thread is open. Anonymous whispers show as "A Soul"; long-press
 * one of your own to delete it.
 */
@Composable
fun WhisperThread(
    emberId: String,
    modifier: Modifier = Modifier,
    viewModel: WhisperViewModel = hiltViewModel(key = "whisper_$emberId")
) {
    viewModel.bind(emberId)
    val whispers by viewModel.whispers.collectAsStateWithLifecycle()
    val myUid = viewModel.currentUserId

    var input by remember { mutableStateOf("") }
    var anonymous by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        if (whispers.isEmpty()) {
            Text(
                text = "No whispers yet. Say something.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = DurenSpacing.space2)
            )
        } else {
            whispers.forEach { whisper ->
                WhisperRow(
                    whisper = whisper,
                    isMine = whisper.authorId == myUid,
                    onDelete = { viewModel.delete(whisper.id) }
                )
            }
        }

        Spacer(Modifier.height(DurenSpacing.space2))
        OutlinedTextField(
            value = input,
            onValueChange = { if (it.length <= 500) input = it },
            placeholder = { Text("Add a whisper…") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FilterChip(
                selected = anonymous,
                onClick = { anonymous = !anonymous },
                label = { Text("Anonymous") }
            )
            TextButton(
                onClick = {
                    val text = input.trim()
                    if (text.isNotEmpty()) {
                        viewModel.add(text, anonymous)
                        input = ""
                    }
                },
                enabled = input.isNotBlank()
            ) {
                Text("Send")
            }
        }
    }
}

@Composable
private fun WhisperRow(
    whisper: Whisper,
    isMine: Boolean,
    onDelete: () -> Unit
) {
    val name = when {
        whisper.isAnonymous -> "A Soul"
        whisper.authorName.isNotBlank() -> whisper.authorName
        whisper.authorUsername.isNotBlank() -> "@${whisper.authorUsername}"
        else -> "Someone"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DurenSpacing.space2)
            .pointerInput(isMine) {
                if (isMine) detectTapGestures(onLongPress = { onDelete() })
            },
        verticalAlignment = Alignment.Top
    ) {
        if (whisper.isAnonymous) {
            // No avatar for anonymous souls — keep them faceless.
            Spacer(Modifier.width(4.dp))
        } else {
            DurenAvatar(
                avatarUrl = whisper.authorAvatarUrl,
                fallbackColorHex = whisper.authorAvatarColor,
                size = 28.dp,
                contentDescription = "Avatar for $name"
            )
        }
        Spacer(Modifier.width(DurenSpacing.space2))
        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = whisper.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
