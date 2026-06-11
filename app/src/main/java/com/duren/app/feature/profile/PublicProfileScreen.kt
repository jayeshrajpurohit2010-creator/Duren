package com.duren.app.feature.profile

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duren.app.data.mood.model.Mood
import com.duren.app.data.nest.model.NestRelation
import com.duren.app.data.testimonial.model.Testimonial
import com.duren.app.ui.components.DurenAvatar
import com.duren.app.ui.theme.DurenColors
import com.duren.app.ui.components.EmberCard
import com.duren.app.ui.theme.DurenShapes
import com.duren.app.ui.theme.DurenSpacing

@Composable
private fun NestAction(
    relation: NestRelation,
    onAdd: () -> Unit,
    onCancel: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    when (relation) {
        NestRelation.Self -> Unit
        NestRelation.None -> Button(
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) { Text("Add to Nest") }

        NestRelation.OutgoingPending -> OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) { Text("Request sent · tap to cancel") }

        NestRelation.IncomingPending -> Row(Modifier.fillMaxWidth()) {
            Button(
                onClick = onAccept,
                modifier = Modifier.weight(1f).height(48.dp)
            ) { Text("Accept") }
            Spacer(Modifier.width(DurenSpacing.space3))
            OutlinedButton(
                onClick = onDecline,
                modifier = Modifier.weight(1f).height(48.dp)
            ) { Text("Decline") }
        }

        NestRelation.Member -> Text(
            text = "✓ In your Nest",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicProfileScreen(
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit = {},
    viewModel: PublicProfileViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val embers by viewModel.embers.collectAsStateWithLifecycle()
    val relation by viewModel.relation.collectAsStateWithLifecycle()
    val nudgeMessage by viewModel.nudgeMessage.collectAsStateWithLifecycle()
    val theirMood by viewModel.theirMood.collectAsStateWithLifecycle()
    val mutualSpark by viewModel.mutualSpark.collectAsStateWithLifecycle()
    val testimonials by viewModel.testimonials.collectAsStateWithLifecycle()
    var showHearthDialog by remember { mutableStateOf(false) }
    var showTestimonialDialog by remember { mutableStateOf(false) }

    // Surface the nudge result as a brief toast, then clear it.
    val context = LocalContext.current
    LaunchedEffect(nudgeMessage) {
        nudgeMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearNudgeMessage()
        }
    }

    if (showHearthDialog) {
        NoteDialog(
            title = "Warm their hearth",
            hint = "Only they will read this. 24 hours, then it's gone.",
            maxChars = 280,
            confirmLabel = "Send warmth",
            onDismiss = { showHearthDialog = false },
            onConfirm = {
                viewModel.sendHearth(it)
                showHearthDialog = false
            }
        )
    }
    if (showTestimonialDialog) {
        NoteDialog(
            title = "Leave a testimonial",
            hint = "Public on their presence for 30 days.",
            maxChars = 150,
            confirmLabel = "Leave it",
            onDismiss = { showTestimonialDialog = false },
            onConfirm = {
                viewModel.writeTestimonial(it)
                showTestimonialDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(profile?.username?.let { "@$it" } ?: "Presence") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val p = profile
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = DurenSpacing.space4),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(Modifier.height(DurenSpacing.space4))
                // Aura ring — tinted by their mood tonight if they share it (F12).
                val auraColor = remember(theirMood, p?.showMoodCanvas) {
                    val m = theirMood
                    if (p?.showMoodCanvas == true && m != null && m.isSet)
                        Color(android.graphics.Color.parseColor(Mood.hexFor(m.mood)))
                    else DurenColors.AccentTeal
                }
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .clip(CircleShape)
                        .border(2.dp, auraColor.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    DurenAvatar(
                        avatarUrl = p?.avatarUrl,
                        fallbackColorHex = p?.avatarColor ?: "#FF6B35",
                        size = 88.dp,
                        contentDescription = "Avatar"
                    )
                }
                Spacer(Modifier.height(DurenSpacing.space3))
                Text(
                    text = p?.displayName?.ifBlank { p.username } ?: "…",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (p != null) {
                    Text(
                        text = "@${p.username}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (p.bio.isNotBlank()) {
                        Spacer(Modifier.height(DurenSpacing.space2))
                        Text(
                            text = p.bio,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Banked away-note, if they've stepped out (F11).
                    if (p.isBanked) {
                        Spacer(Modifier.height(DurenSpacing.space2))
                        Text(
                            text = "💤 ${p.bankedStatus}",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Mutual Spark (F25) — you echoed each other within 24h.
                    if (mutualSpark) {
                        Spacer(Modifier.height(DurenSpacing.space2))
                        Text(
                            text = "✨ Mutual Spark — your fires answered each other",
                            style = MaterialTheme.typography.bodySmall,
                            color = DurenColors.AccentTeal
                        )
                    }
                }
                Spacer(Modifier.height(DurenSpacing.space4))
                NestAction(
                    relation = relation,
                    onAdd = viewModel::addToNest,
                    onCancel = viewModel::cancelRequest,
                    onAccept = viewModel::acceptRequest,
                    onDecline = viewModel::declineRequest
                )
                if (relation == NestRelation.Member) {
                    Spacer(Modifier.height(DurenSpacing.space2))
                    Button(
                        onClick = { onOpenChat(viewModel.userId) },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) { Text("Message") }
                    // Nest-only gestures: a private postcard (F26) and a public note (F27).
                    Spacer(Modifier.height(DurenSpacing.space2))
                    Row(Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { showHearthDialog = true },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) { Text("Warm hearth 🔥", maxLines = 1) }
                        Spacer(Modifier.width(DurenSpacing.space3))
                        OutlinedButton(
                            onClick = { showTestimonialDialog = true },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) { Text("Testimonial ✨", maxLines = 1) }
                    }
                }
                // Nudge — a silent "I see you". Available for anyone but yourself.
                if (relation != NestRelation.Self) {
                    Spacer(Modifier.height(DurenSpacing.space2))
                    OutlinedButton(
                        onClick = viewModel::nudge,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) { Text("Nudge 👀") }
                }
                // What the Nest says — 30d testimonials (F27). Only shown when there are any.
                if (testimonials.isNotEmpty()) {
                    Spacer(Modifier.height(DurenSpacing.space6))
                    HorizontalDivider()
                    Spacer(Modifier.height(DurenSpacing.space4))
                    Text(
                        text = "What the Nest says",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(DurenSpacing.space3))
                    testimonials.forEach { t ->
                        TestimonialCard(testimonial = t)
                        Spacer(Modifier.height(DurenSpacing.space2))
                    }
                }

                Spacer(Modifier.height(DurenSpacing.space6))
                HorizontalDivider()
                Spacer(Modifier.height(DurenSpacing.space4))
                Text(
                    text = "Their embers",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(DurenSpacing.space3))
            }

            if (embers.isEmpty()) {
                item {
                    Text(
                        text = "Nothing burning right now.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                items(embers, key = { it.id }) { ember ->
                    // Read-only here: echo/report live in the main feed.
                    EmberCard(
                        ember = ember,
                        onEcho = {},
                        onColdMark = {},
                        interactive = false
                    )
                    Spacer(Modifier.height(DurenSpacing.space3))
                }
            }
        }
    }
}

/** One testimonial — the note, then who left it (F27). */
@Composable
internal fun TestimonialCard(
    testimonial: Testimonial,
    onDelete: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = DurenShapes.large,
        tonalElevation = 2.dp
    ) {
        Column(Modifier.fillMaxWidth().padding(DurenSpacing.space3)) {
            Text(
                text = "“${testimonial.text}”",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic
            )
            Spacer(Modifier.height(DurenSpacing.space1))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "— ${testimonial.authorName.ifBlank { "A soul" }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("Let it fade", maxLines = 1) }
                }
            }
        }
    }
}

/** A one-field dialog for short notes — shared by Hearth (F26) and Testimonials (F27). */
@Composable
internal fun NoteDialog(
    title: String,
    hint: String,
    maxChars: Int,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(DurenSpacing.space3))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(maxChars) },
                    label = { Text("${text.length}/$maxChars") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank()
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Not now") }
        }
    )
}
