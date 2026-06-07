package com.duren.app.feature.mynest

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duren.app.data.profile.model.Profile
import com.duren.app.ui.animation.pressableCard
import com.duren.app.ui.components.DurenAvatar
import com.duren.app.ui.components.DurenIcon
import com.duren.app.ui.components.EmberCard
import com.duren.app.ui.theme.DurenShapes
import com.duren.app.ui.theme.DurenSpacing

/**
 * The Nest — your people and what they're sharing. (Lanterns moved to their own
 * screen; this tab is now the friends-feed the spec calls for.)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NestFeedScreen(
    onOpenProfile: (String) -> Unit,
    onOpenChat: (String) -> Unit,
    onOpenMessages: () -> Unit,
    onOpenRequests: () -> Unit,
    onOpenLanterns: () -> Unit,
    onOpenSearch: () -> Unit,
    viewModel: NestFeedViewModel = hiltViewModel()
) {
    val members by viewModel.members.collectAsStateWithLifecycle()
    val feed by viewModel.feed.collectAsStateWithLifecycle()
    val requestCount by viewModel.requestCount.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("The Nest") },
                actions = {
                    IconButton(onClick = onOpenLanterns) { Text("🏮") }
                    IconButton(onClick = onOpenMessages) {
                        DurenIcon(DurenIcon.Whisper, size = 22.dp)
                    }
                    IconButton(onClick = onOpenSearch) {
                        DurenIcon(DurenIcon.Search, size = 22.dp)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (requestCount > 0) {
                RequestsChip(count = requestCount, onClick = onOpenRequests)
            }

            if (members.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = DurenSpacing.space2),
                    contentPadding = PaddingValues(horizontal = DurenSpacing.space4),
                    horizontalArrangement = Arrangement.spacedBy(DurenSpacing.space3)
                ) {
                    items(members, key = { it.uid }) { member ->
                        NestPerson(member = member, onClick = { onOpenChat(member.uid) })
                    }
                }
            }

            when {
                members.isEmpty() -> EmptyNest(onFindPeople = onOpenSearch)
                feed.isEmpty() -> EmptyFeedNotice(
                    title = "No embers from your Nest yet.",
                    body = "When your people share, it'll glow here."
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(DurenSpacing.space4),
                    verticalArrangement = Arrangement.spacedBy(DurenSpacing.space3)
                ) {
                    items(feed, key = { it.id }) { ember ->
                        EmberCard(
                            ember = ember,
                            onEcho = { viewModel.echo(ember.id) },
                            onColdMark = { reason -> viewModel.coldMark(ember.id, reason) },
                            canDelete = ember.authorId == viewModel.currentUserId,
                            onDelete = { viewModel.deleteEmber(ember.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestsChip(count: Int, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DurenSpacing.space4, vertical = DurenSpacing.space2)
            .pressableCard(onClick = onClick),
        shape = DurenShapes.medium,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    ) {
        Text(
            text = "$count ${if (count == 1) "person wants" else "people want"} to join your Nest →",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(DurenSpacing.space3)
        )
    }
}

@Composable
private fun NestPerson(member: Profile, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .widthIn(max = 72.dp)
            .pressableCard(onClick = onClick)
            .padding(DurenSpacing.space1),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DurenAvatar(
            avatarUrl = member.avatarUrl,
            fallbackColorHex = member.avatarColor,
            size = 56.dp,
            contentDescription = "Message ${member.username}"
        )
        Spacer(Modifier.height(DurenSpacing.space1))
        Text(
            text = member.displayName.ifBlank { member.username },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmptyNest(onFindPeople: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(DurenSpacing.space6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "🪺", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(DurenSpacing.space3))
        Text(
            text = "Your nest is empty.",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(DurenSpacing.space1))
        Text(
            text = "Add people you trust. Their embers — and your chats — live here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(DurenSpacing.space4))
        Button(onClick = onFindPeople) { Text("Find people") }
    }
}

/** A simple centered title/body block for the "members but no embers" state. */
@Composable
private fun EmptyFeedNotice(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(DurenSpacing.space6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "🔥", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(DurenSpacing.space3))
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(DurenSpacing.space1))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
