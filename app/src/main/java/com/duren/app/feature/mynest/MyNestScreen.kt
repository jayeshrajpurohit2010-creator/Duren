package com.duren.app.feature.mynest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.duren.app.ui.animation.pressableCard
import com.duren.app.ui.components.DurenAvatar
import com.duren.app.ui.theme.DurenSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyNestScreen(
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit = {},
    onOpenChat: (String) -> Unit = {},
    viewModel: MyNestViewModel = hiltViewModel()
) {
    val incoming by viewModel.incoming.collectAsStateWithLifecycle()
    val members by viewModel.members.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Nest") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = DurenSpacing.space4),
            verticalArrangement = Arrangement.spacedBy(DurenSpacing.space2)
        ) {
            if (incoming.isNotEmpty()) {
                item {
                    SectionLabel("Requests")
                }
                items(incoming, key = { it.id }) { request ->
                    val p = request.fromProfile
                    PersonRow(
                        name = p?.displayName?.ifBlank { p.username } ?: "Someone",
                        username = p?.username.orEmpty(),
                        avatarUrl = p?.avatarUrl,
                        avatarColor = p?.avatarColor ?: "#FF6B35",
                        onClick = { onOpenProfile(request.fromUserId) }
                    ) {
                        Button(
                            onClick = { viewModel.accept(request.fromUserId) },
                            modifier = Modifier.height(40.dp)
                        ) { Text("Accept") }
                        Spacer(Modifier.width(DurenSpacing.space2))
                        OutlinedButton(
                            onClick = { viewModel.decline(request.fromUserId) },
                            modifier = Modifier.height(40.dp)
                        ) { Text("Decline") }
                    }
                }
            }

            item {
                SectionLabel("In your Nest · ${members.size}")
            }
            if (members.isEmpty() && incoming.isEmpty()) {
                item {
                    Text(
                        text = "Your Nest is empty. Find people you trust and add them.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = DurenSpacing.space2)
                    )
                }
            } else {
                items(members, key = { it.uid }) { member ->
                    PersonRow(
                        name = member.displayName.ifBlank { member.username },
                        username = member.username,
                        avatarUrl = member.avatarUrl,
                        avatarColor = member.avatarColor,
                        onClick = { onOpenProfile(member.uid) }
                    ) {
                        Button(
                            onClick = { onOpenChat(member.uid) },
                            modifier = Modifier.height(40.dp)
                        ) { Text("Message") }
                        Spacer(Modifier.width(DurenSpacing.space2))
                        OutlinedButton(
                            onClick = { viewModel.remove(member.uid) },
                            modifier = Modifier.height(40.dp)
                        ) { Text("Remove") }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = DurenSpacing.space4, bottom = DurenSpacing.space1)
    )
}

@Composable
private fun PersonRow(
    name: String,
    username: String,
    avatarUrl: String?,
    avatarColor: String,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressableCard(onClick = onClick)
            .padding(vertical = DurenSpacing.space2),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DurenAvatar(
            avatarUrl = avatarUrl,
            fallbackColorHex = avatarColor,
            size = 44.dp,
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
            if (username.isNotBlank()) {
                Text(
                    text = "@$username",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailing()
    }
}
