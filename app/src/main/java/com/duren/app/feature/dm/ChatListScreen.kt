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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duren.app.data.dm.model.ChatSummary
import com.duren.app.ui.animation.EmptyState
import com.duren.app.ui.animation.pressableCard
import com.duren.app.ui.components.DurenAvatar
import com.duren.app.ui.theme.DurenSpacing

private const val DM_LIFESPAN_MS = 48L * 60 * 60 * 1000

/** Your conversations — "Expiring Embers". Each thread fades 48h after the last message. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    viewModel: ChatListViewModel = hiltViewModel()
) {
    val chats by viewModel.chats.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expiring Embers") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (chats.isEmpty()) {
            EmptyState(
                title = "Whisper to start a fire.",
                body = "Message someone in your Nest — it fades in 48 hours.",
                emoji = "🤫",
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
                items(chats, key = { it.chatId }) { chat ->
                    ChatRow(chat = chat, onClick = { onOpenChat(chat.otherUserId) })
                }
            }
        }
    }
}

@Composable
private fun ChatRow(chat: ChatSummary, onClick: () -> Unit) {
    val profile = chat.otherProfile
    val name = profile?.let { it.displayName.ifBlank { it.username } } ?: "Someone"
    val faded = (chat.lastMessageAt?.toDate()?.time ?: 0L) + DM_LIFESPAN_MS < System.currentTimeMillis()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressableCard(onClick = onClick)
            .padding(vertical = DurenSpacing.space2),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DurenAvatar(
            avatarUrl = profile?.avatarUrl,
            fallbackColorHex = profile?.avatarColor ?: "#FF6B35",
            size = 48.dp,
            contentDescription = "Avatar for $name"
        )
        Spacer(Modifier.width(DurenSpacing.space3))
        Column(Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (faded) "This ember faded." else chat.lastMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (chat.unread) {
            Spacer(Modifier.width(DurenSpacing.space2))
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}
