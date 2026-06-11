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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.duren.app.ui.theme.VibePalette

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
    val joinState by viewModel.joinByCode.collectAsStateWithLifecycle()
    var showCodeDialog by remember { mutableStateOf(false) }

    // Joined via code → walk straight into the tribe.
    LaunchedEffect(joinState) {
        val joined = joinState as? JoinByCodeState.Joined ?: return@LaunchedEffect
        showCodeDialog = false
        viewModel.clearJoinByCode()
        onOpenTribe(joined.tribeId)
    }

    if (showCodeDialog) {
        JoinByCodeDialog(
            state = joinState,
            onSubmit = { viewModel.joinByCode(it) },
            onDismiss = {
                showCodeDialog = false
                viewModel.clearJoinByCode()
            }
        )
    }

    Scaffold(
        containerColor = DurenColors.BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = { Text("Discover") },
                actions = {
                    TextButton(onClick = { showCodeDialog = true }) {
                        Text("Have a code?", color = DurenColors.AccentTeal, fontSize = 13.sp)
                    }
                },
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
    val accent = VibePalette.accent(tribe.vibe)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(shape)
            .background(Brush.verticalGradient(VibePalette.gradient(tribe.vibe)))
            // A barely-there edge so the darkest gradients (e.g. "raw") still read
            // as a tile against the #0A0A0A canvas, without becoming a hard divider.
            .border(1.dp, accent.copy(alpha = 0.14f), shape)
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
            // The tribe's emoji sits in its own pool of light — a soft radial wash
            // of the vibe's accent, like a face lit by the fire it sits beside.
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(accent.copy(alpha = 0.30f), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = tribe.emoji.ifBlank { "🔥" }, fontSize = 34.sp)
            }
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
 * Join a tribe by its 6-digit invite code (F37). The field only accepts digits;
 * the button stays dark until all six are in.
 */
@Composable
private fun JoinByCodeDialog(
    state: JoinByCodeState,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Join with a code") },
        text = {
            Column {
                Text(
                    text = "Six digits open the door to a campfire.",
                    fontSize = 13.sp,
                    color = DurenColors.TextSecondary
                )
                Spacer(Modifier.height(DurenSpacing.space3))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.filter(Char::isDigit).take(6) },
                    label = { Text("Invite code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (state is JoinByCodeState.Error) {
                    Spacer(Modifier.height(DurenSpacing.space2))
                    Text(
                        text = state.message,
                        fontSize = 12.sp,
                        color = DurenColors.SemanticError
                    )
                }
            }
        },
        confirmButton = {
            if (state is JoinByCodeState.Checking) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp).aspectRatio(1f),
                    strokeWidth = 2.dp,
                    color = DurenColors.AccentTeal
                )
            } else {
                TextButton(
                    onClick = { onSubmit(code) },
                    enabled = code.length == 6
                ) {
                    Text("Step in")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Not now") }
        }
    )
}

