package com.duren.app.feature.tribes

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duren.app.data.tribe.NightQuestions
import com.duren.app.data.tribe.model.Bulletin
import com.duren.app.data.tribe.model.Tribe
import com.duren.app.ui.animation.EmptyState
import com.duren.app.ui.animation.ShimmerBox
import com.duren.app.ui.components.EmberCard
import com.duren.app.ui.theme.DurenColors
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
    // The Keeper (tribe creator) gets the pin + wisdom tools on each ember.
    val isKeeper = tribe != null && tribe.createdBy == viewModel.currentUserId

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

                // See The Fire (F34): how many embers were lit in the last half hour —
                // drives the flame's size in the header.
                val recentActivity = remember(embers) {
                    val cutoff = System.currentTimeMillis() - 30 * 60 * 1000
                    embers.count { (it.createdAt?.toDate()?.time ?: 0L) >= cutoff }
                }

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
                                isKeeper = isKeeper,
                                activity = recentActivity,
                                onToggleMembership = { viewModel.toggleMembership() }
                            )
                        }
                        if (uiState.presentCount > 0) {
                            item(key = "presence") {
                                PresenceBeacon(count = uiState.presentCount)
                            }
                        }
                        if (uiState.bulletins.isNotEmpty() || isKeeper) {
                            item(key = "bulletins") {
                                BulletinBoard(
                                    bulletins = uiState.bulletins,
                                    isKeeper = isKeeper,
                                    onAdd = { title, text, emoji ->
                                        viewModel.addBulletin(title, text, emoji)
                                    },
                                    onDelete = { viewModel.deleteBulletin(it) }
                                )
                            }
                        }
                        item(key = "qotn") { QuestionOfNightCard() }
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
                                onColdMark = { reason -> viewModel.coldMark(ember.id, reason) },
                                onVotePoll = { yes -> viewModel.votePoll(ember.id, yes) },
                                canModerate = isKeeper,
                                onTogglePin = { viewModel.togglePin(ember) },
                                onToggleWisdom = { viewModel.toggleWisdom(ember) }
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
    isKeeper: Boolean,
    activity: Int,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tribe.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                // See The Fire — the flame grows with the last half hour of embers.
                SeeTheFire(activity = activity)
            }

            if (tribe.vibe.isNotBlank()) {
                Spacer(Modifier.height(DurenSpacing.space1))
                Text(
                    text = tribe.vibe,
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = DurenColors.AccentTeal
                )
            }

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

            // Tribe Roles (F20) — the Keeper sees their key, a reminder they hold
            // the pin + wisdom tools on every ember here.
            if (isKeeper) {
                Spacer(Modifier.height(DurenSpacing.space2))
                Text(
                    text = "🔑 You keep this fire",
                    style = MaterialTheme.typography.labelSmall,
                    color = DurenColors.AccentTeal
                )
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
                    text = "${tribe.memberCount} around the fire" +
                        if (activity > 0) " · $activity just now" else "",
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

/**
 * See The Fire (F34) — a flame whose size and pulse follow the tribe's recent
 * activity. Quiet tribe: small, slow. Busy tribe: big, fast. Derived purely from
 * the embers already on screen, so it costs nothing.
 */
@Composable
private fun SeeTheFire(activity: Int) {
    val base = when {
        activity >= 8 -> 1.2f
        activity >= 3 -> 1.0f
        else -> 0.85f
    }
    val period = if (activity >= 8) 1200 else 2600
    val infinite = rememberInfiniteTransition(label = "see-the-fire")
    val scale by infinite.animateFloat(
        initialValue = base * 0.92f,
        targetValue = base * 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(period, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame"
    )
    Text(text = "🔥", fontSize = 26.sp, modifier = Modifier.scale(scale))
}

/**
 * Question of the Night (F22) — tonight's shared prompt, the same for everyone, sitting
 * at the top of the tribe so there's always something to gather around.
 */
@Composable
private fun QuestionOfNightCard() {
    val prompt = remember { NightQuestions.forToday() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DurenShapes.large)
            .background(DurenColors.SurfacePrimary)
            .padding(DurenSpacing.space4)
    ) {
        Text(
            text = "QUESTION OF THE NIGHT",
            fontSize = 11.sp,
            letterSpacing = 1.5.sp,
            fontWeight = FontWeight.Medium,
            color = DurenColors.AccentTeal
        )
        Spacer(Modifier.height(DurenSpacing.space2))
        Text(
            text = prompt,
            fontSize = 16.sp,
            color = DurenColors.TextPrimary
        )
    }
}

/**
 * Presence Beacon (F33) — a soft breathing dot and a count of who's gathered here in
 * the last couple of minutes. The dot's glow rises and falls (Animation Bible A35,
 * "Beacon Glow") so the fire feels lived-in even when no one's posting.
 */
@Composable
private fun PresenceBeacon(count: Int) {
    val infinite = rememberInfiniteTransition(label = "beacon")
    val glow by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .alpha(glow)
                .clip(CircleShape)
                .background(DurenColors.AccentTeal)
        )
        Spacer(Modifier.width(DurenSpacing.space2))
        Text(
            text = if (count == 1) "Someone's around the fire right now"
            else "$count around the fire right now",
            fontSize = 13.sp,
            color = DurenColors.AccentTeal
        )
    }
}

/**
 * Tribe Bulletin Board (F21) — a horizontal strip of Keeper-curated notices that live
 * for a day. Anyone can read them; only the Keeper sees the "pin a notice" tile and the
 * little ✕ to take one down early.
 */
@Composable
private fun BulletinBoard(
    bulletins: List<Bulletin>,
    isKeeper: Boolean,
    onAdd: (title: String, text: String, emoji: String) -> Unit,
    onDelete: (String) -> Unit
) {
    var composing by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "ON THE BOARD",
            fontSize = 11.sp,
            letterSpacing = 1.5.sp,
            fontWeight = FontWeight.Medium,
            color = DurenColors.TextSecondary
        )
        Spacer(Modifier.height(DurenSpacing.space2))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(DurenSpacing.space2)) {
            if (isKeeper) {
                item(key = "add") {
                    AddBulletinTile(onClick = { composing = true })
                }
            }
            items(items = bulletins, key = { it.id }) { b ->
                BulletinCard(
                    bulletin = b,
                    canDelete = isKeeper,
                    onDelete = { onDelete(b.id) }
                )
            }
        }
    }

    if (composing) {
        AddBulletinDialog(
            onDismiss = { composing = false },
            onConfirm = { title, text, emoji ->
                onAdd(title, text, emoji)
                composing = false
            }
        )
    }
}

@Composable
private fun BulletinCard(
    bulletin: Bulletin,
    canDelete: Boolean,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(220.dp)
            .clip(DurenShapes.large)
            .background(DurenColors.SurfaceElevated)
            .padding(DurenSpacing.space3)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = bulletin.emoji.ifBlank { "📌" }, fontSize = 16.sp)
            Spacer(Modifier.width(DurenSpacing.space2))
            Text(
                text = bulletin.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = DurenColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (canDelete) {
                Text(
                    text = "✕",
                    fontSize = 14.sp,
                    color = DurenColors.TextSecondary,
                    modifier = Modifier.clickable { onDelete() }
                )
            }
        }
        if (bulletin.text.isNotBlank()) {
            Spacer(Modifier.height(DurenSpacing.space1))
            Text(
                text = bulletin.text,
                fontSize = 13.sp,
                color = DurenColors.TextSecondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AddBulletinTile(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .height(96.dp)
            .clip(DurenShapes.large)
            .background(DurenColors.SurfacePrimary)
            .clickable { onClick() }
            .padding(DurenSpacing.space3),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "📌", fontSize = 20.sp)
        Spacer(Modifier.height(DurenSpacing.space1))
        Text(
            text = "Pin a notice",
            fontSize = 12.sp,
            color = DurenColors.AccentTeal
        )
    }
}

@Composable
private fun AddBulletinDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, text: String, emoji: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("📌") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pin a notice") },
        text = {
            Column {
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { emoji = it.take(2) },
                    label = { Text("Emoji") },
                    singleLine = true,
                    modifier = Modifier.width(96.dp)
                )
                Spacer(Modifier.height(DurenSpacing.space2))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(60) },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(DurenSpacing.space2))
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it.take(280) },
                    label = { Text("Say more (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title, body, emoji) },
                enabled = title.isNotBlank()
            ) {
                Text("Pin it")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
