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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duren.app.core.time.NightEconomy
import com.duren.app.core.time.NightPhase
import com.duren.app.ui.animation.EmptyState
import com.duren.app.ui.animation.ShimmerBox
import com.duren.app.ui.components.DurenMasthead
import com.duren.app.ui.components.EmberCard
import com.duren.app.ui.components.NightBanner
import com.duren.app.ui.theme.DurenSpacing
import kotlinx.coroutines.delay

/**
 * The Clearing — the global ephemeral feed.
 * Newest embers first, auto-expiry filtered server-side.
 * Infinite scroll via increasing Firestore limit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onOpenSearch: () -> Unit = {},
    onOpenSignal: () -> Unit = {},
    onOpenMessages: () -> Unit = {},
    viewModel: FeedViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tab by viewModel.tab.collectAsStateWithLifecycle()
    val unreadSignals by viewModel.unreadSignals.collectAsStateWithLifecycle()

    // Night Economy phase for the global Clearing follows the viewer's own device
    // timezone (free, no backend). Re-checked each minute so the banner appears and
    // clears on its own as 2 AM / 6 AM roll past.
    var nightPhase by remember { mutableStateOf(NightEconomy.phaseFor(null)) }
    LaunchedEffect(Unit) {
        while (true) {
            nightPhase = NightEconomy.phaseFor(null)
            delay(60_000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { DurenMasthead(subtitle = "The Clearing") },
                actions = {
                    IconButton(onClick = onOpenSignal) {
                        BadgedBox(
                            badge = {
                                if (unreadSignals > 0) {
                                    Badge { Text(if (unreadSignals > 9) "9+" else "$unreadSignals") }
                                }
                            }
                        ) {
                            Icon(Icons.Outlined.Notifications, contentDescription = "Signals")
                        }
                    }
                    IconButton(onClick = onOpenMessages) {
                        Icon(Icons.Outlined.MailOutline, contentDescription = "Messages")
                    }
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Filled.Search, contentDescription = "Find people")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NightBanner(phase = nightPhase)

            FeedTabRow(selected = tab, onSelect = viewModel::selectTab)

            when (val state = uiState) {
                is FeedUiState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(DurenSpacing.space4),
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
                    // Empty copy follows the active tab so each surface reads true.
                    val (title, body, emoji) = when (tab) {
                        FeedTab.Campfire ->
                            Triple("The clearing is still.", "Be the first ember.", "🏕️")
                        FeedTab.BurningNow ->
                            Triple("Nothing's caught fire yet.", "Echo a post to fan the flames.", "🔥")
                        FeedTab.AboutToFade ->
                            Triple("Nothing's fading right now.", "Posts appear here as they near their last hour.", "⏳")
                        FeedTab.ColdEmbers ->
                            Triple("No cold embers.", "Quiet, un-echoed posts gather here.", "❄️")
                    }
                    EmptyState(
                        title = title,
                        body = body,
                        emoji = emoji,
                        modifier = Modifier.fillMaxSize()
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
                        modifier = Modifier.fillMaxSize(),
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
}

/**
 * The four discovery tabs (design Screen 4). Text labels with a sliding teal
 * underline mark the active one — no emoji, per the brand's anti-clutter aesthetic.
 */
@Composable
private fun FeedTabRow(selected: FeedTab, onSelect: (FeedTab) -> Unit) {
    ScrollableTabRow(
        selectedTabIndex = selected.ordinal,
        edgePadding = DurenSpacing.space4,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.primary,
        divider = {}
    ) {
        FeedTab.entries.forEach { feedTab ->
            val active = feedTab == selected
            Tab(
                selected = active,
                onClick = { onSelect(feedTab) },
                text = {
                    Text(
                        text = feedTab.label,
                        color = if (active) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            )
        }
    }
}
