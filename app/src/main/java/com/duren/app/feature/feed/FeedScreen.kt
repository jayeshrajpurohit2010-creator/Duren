package com.duren.app.feature.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duren.app.ui.animation.EmptyState
import com.duren.app.ui.animation.ShimmerBox
import com.duren.app.ui.components.DurenMasthead
import com.duren.app.ui.components.EmberCard
import com.duren.app.ui.theme.DurenSpacing

/**
 * The Clearing — the global ephemeral feed.
 * Newest embers first, auto-expiry filtered server-side.
 * Infinite scroll via increasing Firestore limit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { DurenMasthead(subtitle = "The Clearing") }
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is FeedUiState.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = DurenSpacing.space4),
                    verticalArrangement = Arrangement.spacedBy(DurenSpacing.space3)
                ) {
                    repeat(3) {
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )
                    }
                }
            }

            is FeedUiState.Empty -> {
                EmptyState(
                    title = "The clearing is still.",
                    body = "Be the first ember.",
                    emoji = "🏕️",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            is FeedUiState.Content -> {
                val listState = rememberLazyListState()
                val totalItems = state.embers.size

                // Infinite scroll: trigger loadMore when nearing the end
                val shouldLoadMore by remember {
                    derivedStateOf {
                        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()
                        lastVisible != null && lastVisible.index >= totalItems - 3
                    }
                }

                LaunchedEffect(shouldLoadMore) {
                    if (shouldLoadMore) {
                        viewModel.loadMore()
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(
                        horizontal = DurenSpacing.space4,
                        vertical = DurenSpacing.space4
                    ),
                    verticalArrangement = Arrangement.spacedBy(DurenSpacing.space3)
                ) {
                    items(
                        items = state.embers,
                        key = { ember -> ember.id }
                    ) { ember ->
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
