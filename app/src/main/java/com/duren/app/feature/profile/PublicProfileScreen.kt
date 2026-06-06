package com.duren.app.feature.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duren.app.data.nest.model.NestRelation
import com.duren.app.ui.components.DurenAvatar
import com.duren.app.ui.components.EmberCard
import com.duren.app.ui.theme.DurenSpacing

@Composable
private fun NestAction(
    relation: NestRelation,
    onAdd: () -> Unit,
    onCancel: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    when (relation) {
        NestRelation.Self -> Unit
        NestRelation.None -> Button(
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) { Text("Add to Nest") }

        NestRelation.OutgoingPending -> OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) { Text("Request sent · tap to cancel") }

        NestRelation.IncomingPending -> Row(Modifier.fillMaxWidth()) {
            Button(
                onClick = onAccept,
                modifier = Modifier.weight(1f).height(48.dp)
            ) { Text("Accept") }
            Spacer(Modifier.width(DurenSpacing.space3))
            OutlinedButton(
                onClick = onDecline,
                modifier = Modifier.weight(1f).height(48.dp)
            ) { Text("Decline") }
        }

        NestRelation.Member -> Text(
            text = "✓ In your Nest",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicProfileScreen(
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit = {},
    viewModel: PublicProfileViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val embers by viewModel.embers.collectAsStateWithLifecycle()
    val relation by viewModel.relation.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(profile?.username?.let { "@$it" } ?: "Presence") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val p = profile
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = DurenSpacing.space4),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(Modifier.height(DurenSpacing.space4))
                DurenAvatar(
                    avatarUrl = p?.avatarUrl,
                    fallbackColorHex = p?.avatarColor ?: "#FF6B35",
                    size = 88.dp,
                    contentDescription = "Avatar"
                )
                Spacer(Modifier.height(DurenSpacing.space3))
                Text(
                    text = p?.displayName?.ifBlank { p.username } ?: "…",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (p != null) {
                    Text(
                        text = "@${p.username}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (p.bio.isNotBlank()) {
                        Spacer(Modifier.height(DurenSpacing.space2))
                        Text(
                            text = p.bio,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(DurenSpacing.space4))
                NestAction(
                    relation = relation,
                    onAdd = viewModel::addToNest,
                    onCancel = viewModel::cancelRequest,
                    onAccept = viewModel::acceptRequest,
                    onDecline = viewModel::declineRequest
                )
                if (relation == NestRelation.Member) {
                    Spacer(Modifier.height(DurenSpacing.space2))
                    Button(
                        onClick = { onOpenChat(viewModel.userId) },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) { Text("Message") }
                }
                Spacer(Modifier.height(DurenSpacing.space6))
                HorizontalDivider()
                Spacer(Modifier.height(DurenSpacing.space4))
                Text(
                    text = "Their embers",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(DurenSpacing.space3))
            }

            if (embers.isEmpty()) {
                item {
                    Text(
                        text = "Nothing burning right now.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                items(embers, key = { it.id }) { ember ->
                    // Read-only here: echo/report live in the main feed.
                    EmberCard(
                        ember = ember,
                        onEcho = {},
                        onColdMark = {},
                        interactive = false
                    )
                    Spacer(Modifier.height(DurenSpacing.space3))
                }
            }
        }
    }
}
