package com.duren.app.feature.tribes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duren.app.data.tribe.model.Tribe
import com.duren.app.ui.animation.EmptyState
import com.duren.app.ui.animation.ShimmerBox
import com.duren.app.ui.animation.pressableCard
import com.duren.app.ui.components.DurenIcon
import com.duren.app.ui.theme.DurenColors
import com.duren.app.ui.theme.DurenShapes
import com.duren.app.ui.theme.DurenSpacing

/**
 * Discover — the tribe catalog as a 2-column grid of gradient tiles.
 *
 * Each tile carries its own dark gradient keyed off the tribe's vibe, so opening
 * Discover reads as a wall of distinct campfires rather than a list of identical
 * Reddit cards. Darkness is the canvas; the gradients only ever lift a few shades
 * off #0A0A0A, never into bright Material surfaces.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TribesScreen(
    onCreateTribe: () -> Unit,
    onOpenTribe: (String) -> Unit,
    viewModel: TribesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = DurenColors.BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = { Text("Discover") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = DurenColors.TextPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateTribe,
                containerColor = DurenColors.AccentTeal,
                contentColor = DurenColors.OnAccent
            ) {
                DurenIcon(DurenIcon.Plus, size = 24.dp)
            }
        }
    ) { padding ->
        when (val state = uiState) {
            is TribesUiState.Loading -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(DurenSpacing.space4),
                    horizontalArrangement = Arrangement.spacedBy(DurenSpacing.space3),
                    verticalArrangement = Arrangement.spacedBy(DurenSpacing.space3)
                ) {
                    items((0 until 6).toList()) {
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }
                }
            }

            is TribesUiState.Empty -> {
                EmptyState(
                    title = "Find your clearing.",
                    body = "Join a tribe — or start the first fire.",
                    emoji = "🌲",
                    modifier = Modifier.padding(padding)
                )
            }

            is TribesUiState.Content -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(DurenSpacing.space4),
                    horizontalArrangement = Arrangement.spacedBy(DurenSpacing.space3),
                    verticalArrangement = Arrangement.spacedBy(DurenSpacing.space3)
                ) {
                    items(state.tribes, key = { it.id }) { tribe ->
                        TribeTile(
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
private fun TribeTile(
    tribe: Tribe,
    onOpen: () -> Unit,
    onToggleMembership: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(shape)
            .background(Brush.verticalGradient(vibeGradient(tribe.vibe)))
            // A barely-there edge so the darkest gradients (e.g. "raw") still read
            // as a tile against the #0A0A0A canvas, without becoming a hard divider.
            .border(1.dp, Color.White.copy(alpha = 0.06f), shape)
            .pressableCard(onClick = onOpen)
            .padding(DurenSpacing.space3)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(DurenSpacing.space2))
            Text(text = tribe.emoji.ifBlank { "🔥" }, fontSize = 34.sp)
            Spacer(Modifier.height(DurenSpacing.space2))
            Text(
                text = tribe.name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (tribe.vibe.isNotBlank()) {
                Text(
                    text = tribe.vibe,
                    color = DurenColors.TextMuted,
                    fontStyle = FontStyle.Italic,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${tribe.memberCount} souls",
                color = DurenColors.TextSecondary,
                fontSize = 11.sp,
                maxLines = 1
            )
            JoinPill(isMember = tribe.isMember, onClick = onToggleMembership)
        }
    }
}

@Composable
private fun JoinPill(isMember: Boolean, onClick: () -> Unit) {
    if (isMember) {
        Box(
            modifier = Modifier
                .clip(DurenShapes.pill)
                .border(1.dp, DurenColors.TextMuted.copy(alpha = 0.5f), DurenShapes.pill)
                .clickable(onClick = onClick)
                .padding(horizontal = DurenSpacing.space3, vertical = DurenSpacing.space1)
        ) {
            Text(
                text = "Joined",
                color = DurenColors.TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    } else {
        Box(
            modifier = Modifier
                .clip(DurenShapes.pill)
                .background(DurenColors.AccentTeal)
                .clickable(onClick = onClick)
                .padding(horizontal = DurenSpacing.space3, vertical = DurenSpacing.space1)
        ) {
            Text(
                text = "Join",
                color = DurenColors.OnAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Dark gradient per vibe (two stops, vertical). Values stay within a few shades of
 * #0A0A0A so the grid never lights up — each vibe just leans toward its own hue.
 * Vibes the design didn't name fall back to sensible neighbours, then a neutral.
 */
private fun vibeGradient(vibe: String): List<Color> {
    fun c(hex: Long) = Color(hex)
    return when (vibe.trim().lowercase()) {
        "energetic" -> listOf(c(0xFF1A0A00), c(0xFF2D1200))
        "hype" -> listOf(c(0xFF1A001A), c(0xFF2D002D))
        "competitive" -> listOf(c(0xFF001A2D), c(0xFF00263D))
        "cozy" -> listOf(c(0xFF001A1A), c(0xFF002D2D))
        "chill" -> listOf(c(0xFF0A0A1A), c(0xFF12122D))
        "spooky" -> listOf(c(0xFF1A0A1A), c(0xFF000000))
        "peaceful" -> listOf(c(0xFF001A0A), c(0xFF002D12))
        "raw" -> listOf(c(0xFF1A1A1A), c(0xFF0A0A0A))
        "safe" -> listOf(c(0xFF001A2D), c(0xFF001A3D))
        "creative" -> listOf(c(0xFF1A1A00), c(0xFF2D2D00))
        "niche" -> listOf(c(0xFF1A001A), c(0xFF2D002D))
        "focused" -> listOf(c(0xFF001A1A), c(0xFF001A2D))
        "stressed" -> listOf(c(0xFF2D0000), c(0xFF1A0000))
        "geeky" -> listOf(c(0xFF001A00), c(0xFF002D00))
        "chaotic" -> listOf(c(0xFF2D1A00), c(0xFF1A0D00))
        // Fallbacks for seed vibes the design map didn't cover.
        "intense" -> listOf(c(0xFF2D0A00), c(0xFF1A0500))
        "calm" -> listOf(c(0xFF0A0A1A), c(0xFF10182D))
        "confessional" -> listOf(c(0xFF14141A), c(0xFF0A0A0A))
        "open" -> listOf(c(0xFF001A14), c(0xFF002D20))
        else -> listOf(c(0xFF161616), c(0xFF0C0C0C))
    }
}
