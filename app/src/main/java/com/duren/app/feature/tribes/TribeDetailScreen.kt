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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.duren.app.ui.components.EmberCard
import com.duren.app.ui.theme.DurenShapes
import com.duren.app.ui.theme.DurenSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TribeDetailScreen(
    onBack: () -> Unit,
    viewModel: TribeDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tribe = uiState.tribe

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tribe?.name ?: "Tribe") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = DurenSpacing.space4),
                    verticalArrangement = Arrangement.spacedBy(DurenSpacing.space3)
                ) {
                    Spacer(Modifier.height(DurenSpacing.space3))
                    repeat(4) {
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                        )
                    }
                }
            }

            else -> {
                val listState = rememberLazyListState()
                val embers = uiState.embers
                val totalItems = embers.size

                val shouldLoadMore by remember {
                    derivedStateOf {
                        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()
                        lastVisible != null && lastVisible.index >= totalItems - 3
                    }
                }
                LaunchedEffect(shouldLoadMore) {
                    if (shouldLoadMore) viewModel.loadMore()
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(DurenSpacing.space4),
                    verticalArrangement = Arrangement.spacedBy(DurenSpacing.space3)
                ) {
                    if (tribe != null) {
                        item(key = "header") {
                            TribeDetailHeader(
                                tribe = tribe,
                                onToggleMembership = { viewModel.toggleMembership() }
                            )
                        }
                    }

                    if (embers.isEmpty()) {
                        item(key = "empty") {
                            EmptyState(
                                title = "No embers here yet.",
                                body = "Be the first to light this fire.",
                                emoji = "🔥",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp)
                            )
                        }
                    } else {
                        items(items = embers, key = { it.id }) { ember ->
                            EmberCard(
                                ember = ember,
                                onEcho = { viewModel.echo(ember.id) },
                                onColdMark = { reason -> viewModel.coldMark(ember.id, reason) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TribeDetailHeader(
    tribe: Tribe,
    onToggleMembership: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
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
                style = MaterialTheme.typography.titleLarge,
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
