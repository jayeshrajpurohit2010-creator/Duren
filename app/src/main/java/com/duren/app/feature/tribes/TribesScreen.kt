package com.duren.app.feature.tribes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duren.app.data.tribe.model.Tribe
import com.duren.app.ui.animation.EmptyState
import com.duren.app.ui.animation.ShimmerBox
import com.duren.app.ui.animation.pressableCard
import com.duren.app.ui.components.DurenIcon
import com.duren.app.ui.theme.DurenShapes
import com.duren.app.ui.theme.DurenSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TribesScreen(
    onCreateTribe: () -> Unit,
    onOpenTribe: (String) -> Unit,
    viewModel: TribesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tribes") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateTribe) {
                DurenIcon(DurenIcon.Plus, size = 24.dp)
            }
        }
    ) { padding ->
        when (val state = uiState) {
            is TribesUiState.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = DurenSpacing.space4),
                    verticalArrangement = Arrangement.spacedBy(DurenSpacing.space3)
                ) {
                    Spacer(Modifier.height(DurenSpacing.space3))
                    repeat(5) {
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                        )
                    }
                }
            }

            is TribesUiState.Empty -> {
                EmptyState(
                    title = "No tribes yet.",
                    body = "Start one. Light the first fire.",
                    emoji = "🪵",
                    modifier = Modifier.padding(padding)
                )
            }

            is TribesUiState.Content -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(DurenSpacing.space4),
                    verticalArrangement = Arrangement.spacedBy(DurenSpacing.space3)
                ) {
                    items(state.tribes, key = { it.id }) { tribe ->
                        TribeCard(
                            tribe = tribe,
                            onOpen = { onOpenTribe(tribe.id) },
                            onToggleMembership = { viewModel.toggleMembership(tribe) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TribeCard(
    tribe: Tribe,
    onOpen: () -> Unit,
    onToggleMembership: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .pressableCard(onClick = onOpen),
        shape = DurenShapes.large,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DurenSpacing.space4)
        ) {
            Text(
                text = tribe.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (tribe.genre.isNotBlank()) {
                Spacer(Modifier.height(DurenSpacing.space1))
                Surface(
                    shape = DurenShapes.pill,
                    tonalElevation = 4.dp
                ) {
                    Text(
                        text = tribe.genre,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            horizontal = DurenSpacing.space2,
                            vertical = DurenSpacing.space1
                        )
                    )
                }
            }

            if (tribe.description.isNotBlank()) {
                Spacer(Modifier.height(DurenSpacing.space2))
                Text(
                    text = tribe.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(DurenSpacing.space3))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${tribe.memberCount} around the fire",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (tribe.isMember) {
                    OutlinedButton(onClick = onToggleMembership) {
                        Text("Joined")
                    }
                } else {
                    Button(onClick = onToggleMembership) {
                        Text("Join")
                    }
                }
            }
        }
    }
}
