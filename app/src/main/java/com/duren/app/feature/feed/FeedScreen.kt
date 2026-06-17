package com.duren.app.feature.feed

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.duren.app.ui.components.FloatingEmbers
import com.duren.app.ui.components.NightBanner
import com.duren.app.ui.components.glassTopAppBarColors
import com.duren.app.ui.theme.LocalDurenColors
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
    doveMode: Boolean = false,
    onToggleDove: () -> Unit = {},
    viewModel: FeedViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tab by viewModel.tab.collectAsStateWithLifecycle()
    val unreadSignals by viewModel.unreadSignals.collectAsStateWithLifecycle()

    // The top bar slips away as you scroll down into the embers and returns the moment
    // you pull back up — less chrome, more campfire (PART2 "top-bar-hides-on-scroll").
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

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
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = { if (!doveMode) DurenMasthead(subtitle = "The Clearing") },
                actions = {
                    if (!doveMode) {
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
                    // Dove Mode (F32) — the one control that survives, so you can always
                    // come back. Teal once the campfire's gone private.
                    IconButton(onClick = onToggleDove) {
                        DurenIcon(
                            DurenIcon.Feather,
                            size = 18.dp,
                            tint = if (doveMode) LocalDurenColors.current.AccentTeal
                                    else LocalDurenColors.current.TextMuted
                        )
                    }
                },
                colors = glassTopAppBarColors()
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        // Campfire ambience behind the feed — a teal ceiling-glow, a warm floor-glow,
        // and a few embers drifting up, so the Clearing reads as a place to sit at
        // rather than a list to scroll. Dark-only: on the near-white light theme the
        // warm glow just looks muddy, so it switches itself off.
        if (LocalDurenColors.current.isDark) {
            FeedAmbience(Modifier.matchParentSize())
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                    // The third value carries the surface's hand-drawn DurenIcon (no emoji).
                    val (title, body, icon) = when (tab) {
                        FeedTab.Campfire ->
                            Triple("The clearing is still.", "Be the first ember.", DurenIcon.Tribe)
                        FeedTab.BurningNow ->
                            Triple("Nothing's caught fire yet.", "Echo a post to fan the flames.", DurenIcon.Ember)
                        FeedTab.AboutToFade ->
                            Triple("Nothing's fading right now.", "Posts appear here as they near their last hour.", DurenIcon.Smoke)
                        FeedTab.ColdEmbers ->
                            Triple("No cold embers.", "Quiet, un-echoed posts gather here.", DurenIcon.Frost)
                    }
                    FeedEmptyState(
                        title = title,
                        body = body,
                        icon = icon,
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
                                modifier = Modifier.animateItem(),
                                onEcho = { viewModel.echo(ember.id) },
                                onColdMark = { reason -> viewModel.coldMark(ember.id, reason) },
                                canDelete = ember.authorId == viewModel.currentUserId,
                                onDelete = { viewModel.deleteEmber(ember.id) },
                                onVotePoll = { yes -> viewModel.votePoll(ember.id, yes) },
                                onKindle = { viewModel.kindle(ember.id) }
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

/**
 * A10 — the feed's empty state. The same brand-voice copy and gentle alpha flicker
 * as [EmptyState], but headed by the surface's hand-drawn [DurenIcon] instead of an
 * emoji, so each empty surface reads as designed rather than emoji-littered.
 */
@Composable
private fun FeedEmptyState(
    title: String,
    body: String?,
    icon: DurenIcon,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "empty_flicker")
    val flicker by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "empty_alpha"
    )
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(DurenSpacing.space6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        DurenIcon(
            icon,
            size = 56.dp,
            tint = LocalDurenColors.current.TextMuted,
            modifier = Modifier.alpha(flicker)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = DurenSpacing.space4)
        )
        if (body != null) {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = LocalDurenColors.current.TextSecondary,
                modifier = Modifier.padding(top = DurenSpacing.space2)
            )
        }
    }
}

/**
 * Campfire ambience drawn behind the feed: a teal ceiling-glow up top, a warm
 * floor-glow at the foot, and a few embers drifting upward — the same Hero
 * language as the landing, so the Clearing feels lit by a fire, not a white void.
 */
@Composable
private fun FeedAmbience(modifier: Modifier = Modifier) {
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(FeedTeal.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.12f),
                    radius = maxOf(w, h) * 0.6f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(FeedWarm.copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(w * 0.5f, h * 1.04f),
                    radius = w * 0.9f
                )
            )
        }
        // A sparse drift — the feed should feel lived-in, not busy.
        FloatingEmbers(modifier = Modifier.fillMaxSize(), count = 8)
    }
}

private val FeedTeal = Color(0xFF2DD4BF)
private val FeedWarm = Color(0xFFFFA040)

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
            color = LocalDurenColors.current.AccentTeal,
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(Modifier.height(DurenSpacing.space2))
        Row(horizontalArrangement = Arrangement.spacedBy(DurenSpacing.space2)) {
            FeedTab.entries.forEach { feedTab ->
                val active = feedTab == selected
                val dotColor by animateColorAsState(
                    targetValue = if (active) LocalDurenColors.current.AccentTeal else LocalDurenColors.current.TextDisabled,
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
