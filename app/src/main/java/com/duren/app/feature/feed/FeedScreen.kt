package com.duren.app.feature.feed

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duren.app.core.time.NightEconomy
import com.duren.app.core.time.NightPhase
import com.duren.app.ui.animation.EmptyState
import com.duren.app.ui.animation.ShimmerBox
import com.duren.app.ui.components.DurenIcon
import com.duren.app.ui.components.DurenMasthead
import com.duren.app.ui.components.EmberCard
import com.duren.app.ui.components.NightBanner
import com.duren.app.ui.theme.DurenColors
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
                            DurenIcon(DurenIcon.Bell, size = 22.dp)
                        }
                    }
                    IconButton(onClick = onOpenMessages) {
                        // DMs are "Expiring Embers" — the speech-bubble whisper glyph.
                        DurenIcon(DurenIcon.Whisper, size = 22.dp)
                    }
                    IconButton(onClick = onOpenSearch) {
                        DurenIcon(DurenIcon.Search, size = 22.dp)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // Horizontal swipe moves between the four sub-tabs — no tab bar.
                .pointerInput(tab) {
                    var dx = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dx = 0f },
                        onDragEnd = {
                            val tabs = FeedTab.entries
                            val i = tab.ordinal
                            when {
                                dx <= -SWIPE_THRESHOLD_PX && i < tabs.lastIndex ->
                                    viewModel.selectTab(tabs[i + 1])
                                dx >= SWIPE_THRESHOLD_PX && i > 0 ->
                                    viewModel.selectTab(tabs[i - 1])
                            }
                        },
                        onHorizontalDrag = { _, amount -> dx += amount }
                    )
                }
        ) {
            NightBanner(phase = nightPhase)

            SubTabDots(selected = tab, onSelect = viewModel::selectTab)

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
                        // No horizontal padding: photos bleed edge-to-edge. Embers are
                        // separated by space alone — no dividers, no card boxes.
                        contentPadding = PaddingValues(vertical = DurenSpacing.space6),
                        verticalArrangement = Arrangement.spacedBy(DurenSpacing.space8)
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
                                onDelete = { viewModel.deleteEmber(ember.id) },
                                onVotePoll = { yes -> viewModel.votePoll(ember.id, yes) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Swipe distance (px) past which the feed switches sub-tab. */
private const val SWIPE_THRESHOLD_PX = 64f

/**
 * The four discovery surfaces, shown as four dots — not a tab bar. The active dot
 * glows teal; the active surface's name sits quietly above. Tapping a dot or
 * swiping horizontally moves between them (design Screen 4 / Phase 0.5).
 */
@Composable
private fun SubTabDots(selected: FeedTab, onSelect: (FeedTab) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DurenSpacing.space3),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = selected.label,
            color = DurenColors.AccentTeal,
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(Modifier.height(DurenSpacing.space2))
        Row(horizontalArrangement = Arrangement.spacedBy(DurenSpacing.space2)) {
            FeedTab.entries.forEach { feedTab ->
                val active = feedTab == selected
                val dotColor by animateColorAsState(
                    targetValue = if (active) DurenColors.AccentTeal else DurenColors.TextDisabled,
                    label = "dot"
                )
                val dotSize by animateDpAsState(
                    targetValue = if (active) 9.dp else 6.dp,
                    label = "dotSize"
                )
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(feedTab) },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(dotSize)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
            }
        }
    }
}
