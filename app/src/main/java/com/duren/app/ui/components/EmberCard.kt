package com.duren.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import coil3.compose.AsyncImage
import com.duren.app.data.ember.model.Ember
import com.duren.app.data.ember.model.PostMode
import com.duren.app.feature.whisper.WhisperThread
import com.duren.app.ui.theme.DurenColors
import com.duren.app.ui.theme.DurenShapes
import com.duren.app.ui.theme.DurenSpacing
import kotlinx.coroutines.launch

/**
 * An Ember on the darkness.
 *
 * Deliberately NOT a card: there is no surface, no border box, no row of counts.
 * Text floats directly on #0A0A0A; photos bleed edge-to-edge with the caption
 * laid over them. The whole ember fades and blurs as it burns down (see
 * [EmberDecay]). Tapping reveals the quiet actions (echo, whisper, more) — they
 * are not on the surface, so the feed reads as a place, not a feed.
 *
 * Identity is masked for Anonymous and Confess modes — a 🎭 stands in for the
 * avatar and name, so the author stays hidden even in the UI.
 */
@Composable
fun EmberCard(
    ember: Ember,
    onEcho: () -> Unit,
    onColdMark: (reason: String) -> Unit,
    modifier: Modifier = Modifier,
    // Horizontal breathing room for text content. Photos always bleed full-width.
    contentPadding: androidx.compose.ui.unit.Dp = DurenSpacing.space4,
    interactive: Boolean = true,
    canDelete: Boolean = false,
    onDelete: () -> Unit = {},
    // Quick Poll: cast a yes/no vote (F18). No-op for non-poll embers.
    onVotePoll: (yes: Boolean) -> Unit = {},
    // Keeper-only tribe moderation (F19 pin, F23 wisdom). Only the tribe feed sets these.
    canModerate: Boolean = false,
    onTogglePin: () -> Unit = {},
    onToggleWisdom: () -> Unit = {}
) {
    var showColdMarkDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showWhispers by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var zoomedMedia by remember { mutableStateOf<String?>(null) }
    // Local-only so the poll flips to results the moment you vote; the authoritative
    // tallies still arrive via the realtime listener on the ember doc.
    var myVote by remember(ember.id) { mutableStateOf<Boolean?>(null) }

    val scope = rememberCoroutineScope()
    val heartScale = remember { Animatable(1f) }
    fun popHeart() = scope.launch {
        heartScale.animateTo(1.2f, tween(75))
        heartScale.animateTo(1f, tween(75))
    }

    // The ember's burn state — opacity, blur, and the burning bar — from its own
    // timestamps. Recomputes whenever the feed re-emits this ember.
    val decay = remember(ember.id, ember.expiresAt, ember.extended) {
        EmberDecay.of(ember.createdAt, ember.expiresAt)
    }

    // Hot embers earn a faint teal halo around the photo; cold ones stay dark.
    val temperature = ember.echoCount
    val isHot = temperature >= 5

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Let this ember go?") },
            text = { Text("It'll fade now instead of burning out on its own. Can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) {
                    Text("Let go", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Keep") }
            }
        )
    }

    zoomedMedia?.let { media ->
        FullScreenImageViewer(imageUrl = media, onDismiss = { zoomedMedia = null })
    }

    if (showColdMarkDialog) {
        ColdMarkDialog(
            onDismiss = { showColdMarkDialog = false },
            onSubmit = { reason ->
                onColdMark(reason)
                showColdMarkDialog = false
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            // Embers of Wisdom wear a thin gold frame — the one deliberate border in
            // an otherwise borderless feed (F23).
            .then(
                if (ember.isWisdom)
                    Modifier
                        .border(1.dp, WisdomGold.copy(alpha = 0.55f), DurenShapes.large)
                        .padding(vertical = DurenSpacing.space2)
                else Modifier
            )
            .alpha(decay.opacity)
            .then(if (decay.blur > 0.dp) Modifier.blur(decay.blur) else Modifier)
            // Tap anywhere on the body to reveal/hide the quiet actions.
            .pointerInput(canDelete) {
                detectTapGestures(
                    onTap = { expanded = !expanded },
                    onLongPress = { if (canDelete) showDeleteDialog = true }
                )
            }
    ) {
        // Keeper marks ride at the very top, above the author line.
        if (ember.pinnedNow() || ember.isWisdom || ember.isFinal || ember.subEmberName.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = contentPadding, vertical = DurenSpacing.space1),
                horizontalArrangement = Arrangement.spacedBy(DurenSpacing.space3)
            ) {
                if (ember.pinnedNow()) {
                    Text(
                        text = "📌 Floating Lantern",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DurenColors.AccentTeal
                    )
                }
                if (ember.isWisdom) {
                    Text(
                        text = "✨ Ember of Wisdom",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WisdomGold
                    )
                }
                if (ember.isFinal) {
                    // A goodbye left behind on the way out of a tribe (F35).
                    Text(
                        text = "🕯️ Final Ember",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DurenColors.TextSecondary
                    )
                }
                if (ember.subEmberName.isNotBlank()) {
                    // The topic thread it lives in (F36).
                    Text(
                        text = "#${ember.subEmberName}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = DurenColors.TextMuted
                    )
                }
            }
        }

        // Identity line — small, muted, never shouting.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = contentPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (ember.mode.isMasked) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(DurenColors.SurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🎭", fontSize = 14.sp)
                }
            } else {
                DurenAvatar(
                    avatarUrl = ember.authorAvatarUrl,
                    fallbackColorHex = ember.authorAvatarColor,
                    size = 32.dp,
                    contentDescription = "Avatar for ${ember.authorName}"
                )
            }

            Spacer(modifier = Modifier.width(DurenSpacing.space3))

            Column(modifier = Modifier.weight(1f)) {
                val name = when {
                    ember.mode == PostMode.Confess -> ember.poeticAlias.ifBlank { "A confession" }
                    ember.mode.isMasked -> "A soul"
                    else -> ember.authorName
                }
                Text(
                    text = name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DurenColors.TextPrimary
                )
                // Ember signature — the author's tagline, italic and quiet.
                if (!ember.mode.isMasked && ember.emberSignature.isNotBlank()) {
                    Text(
                        text = ember.emberSignature,
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                        color = DurenColors.TextSecondary
                    )
                }
                val tribeLine = ember.tribeName.trim().ifBlank { null }
                Text(
                    text = if (tribeLine != null) "in $tribeLine" else "in the clearing",
                    fontSize = 11.sp,
                    color = DurenColors.TextMuted
                )
            }

            TemperatureBadge(echoCount = ember.echoCount)
        }

        // Body — text floats on darkness; photos bleed to the edges.
        if (ember.text.isNotBlank()) {
            // Fragment mode: hold back everything past the threshold until echoed.
            val fragmentHeld = ember.isFragment &&
                !ember.echoedByMe &&
                ember.text.length > ember.fragmentThreshold
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = contentPadding, vertical = DurenSpacing.space3)
            ) {
                Text(
                    text = if (fragmentHeld) ember.text.take(ember.fragmentThreshold).trimEnd() + "…"
                    else ember.text,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = DurenColors.TextPrimary
                )
                if (fragmentHeld) {
                    Spacer(modifier = Modifier.height(DurenSpacing.space2))
                    Text(
                        text = "🔒 Echo to read the rest",
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic,
                        color = DurenColors.AccentTeal
                    )
                }
            }
        }

        // Quick Poll — the body above is the question; vote once, then the bar moves.
        if (ember.isPoll) {
            PollSection(
                ember = ember,
                myVote = myVote,
                interactive = interactive,
                contentPadding = contentPadding,
                onVote = { yes ->
                    if (myVote == null) {
                        myVote = yes
                        onVotePoll(yes)
                    }
                }
            )
        }

        ember.mediaUrl?.let { media ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (ember.text.isBlank()) DurenSpacing.space3 else 0.dp)
            ) {
                val imageModifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(media) {
                        detectTapGestures(onTap = { zoomedMedia = media })
                    }
                if (media.startsWith("data:")) {
                    val bitmap = remember(media) { decodeDataUri(media) }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Ember media",
                            contentScale = ContentScale.FillWidth,
                            modifier = imageModifier
                        )
                    }
                } else {
                    AsyncImage(
                        model = media,
                        contentDescription = "Ember media",
                        contentScale = ContentScale.FillWidth,
                        modifier = imageModifier
                    )
                }
                // Hot embers glow faintly at the photo's foot.
                if (isHot) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .align(Alignment.BottomCenter)
                            .background(DurenColors.AccentTeal.copy(alpha = 0.6f))
                    )
                }
            }
        }

        // The burning bar — how much life is left, teal → amber → red.
        BurningBar(
            remaining = decay.remaining,
            color = decay.barColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = contentPadding, vertical = DurenSpacing.space2)
        )

        // Quiet actions — only when the ember is tapped open. Counts live here,
        // never on the resting surface.
        AnimatedVisibility(visible = expanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = contentPadding, vertical = DurenSpacing.space2),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (ember.isFinal) {
                        // Final Embers cannot be echoed (F35) — the goodbye just rests.
                        Text(
                            text = "🕯️ let it rest",
                            fontSize = 12.sp,
                            color = DurenColors.TextMuted
                        )
                    } else Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = if (interactive) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { popHeart(); onEcho() }
                        } else Modifier
                    ) {
                        Icon(
                            imageVector = if (ember.echoedByMe) Icons.Filled.Favorite else Icons.Outlined.Favorite,
                            contentDescription = if (ember.echoedByMe) "Un-echo" else "Echo",
                            tint = if (ember.echoedByMe) DurenColors.AccentTeal else DurenColors.TextMuted,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer { scaleX = heartScale.value; scaleY = heartScale.value }
                        )
                        Spacer(modifier = Modifier.width(DurenSpacing.space1))
                        Text(
                            text = "${ember.echoCount} echoes",
                            fontSize = 12.sp,
                            color = if (ember.echoedByMe) DurenColors.AccentTeal else DurenColors.TextMuted
                        )
                    }

                    Spacer(modifier = Modifier.width(DurenSpacing.space4))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showWhispers = !showWhispers }
                    ) {
                        Text(text = "💬", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(DurenSpacing.space1))
                        Text(
                            text = "${ember.whisperCount} whispers",
                            fontSize = 12.sp,
                            color = DurenColors.TextMuted
                        )
                    }
                }

                if (canDelete || interactive || canModerate) {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = "More",
                                tint = DurenColors.TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (canDelete) {
                                DropdownMenuItem(
                                    text = { Text("Let go", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMenu = false
                                        showDeleteDialog = true
                                    }
                                )
                            }
                            // Keeper tools — only in a tribe the viewer keeps.
                            if (canModerate) {
                                DropdownMenuItem(
                                    text = { Text(if (ember.pinnedNow()) "Unpin from tribe" else "📌 Pin to tribe") },
                                    onClick = {
                                        showMenu = false
                                        onTogglePin()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (ember.isWisdom) "Remove wisdom" else "✨ Mark as wisdom") },
                                    onClick = {
                                        showMenu = false
                                        onToggleWisdom()
                                    }
                                )
                            }
                            if (interactive && !canDelete) {
                                DropdownMenuItem(
                                    text = { Text("Cold mark") },
                                    onClick = {
                                        showMenu = false
                                        showColdMarkDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showWhispers) {
            WhisperThread(
                emberId = ember.id,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = contentPadding, vertical = DurenSpacing.space2)
            )
        }
    }
}

/** A hairline that shows how much of an ember's life remains. */
@Composable
private fun BurningBar(
    remaining: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(2.dp)
            .clip(CircleShape)
            .background(DurenColors.BorderDefault.copy(alpha = 0.4f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(remaining.coerceIn(0f, 1f))
                .height(2.dp)
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(color.copy(alpha = 0.5f), color)
                    )
                )
        )
    }
}

/** Gold for Keeper-blessed embers — the Design System's Drum-Circle gold (#FFD700). */
private val WisdomGold = Color(0xFFFFD700)

/**
 * Quick Poll (F18). The ember body is the question; this is the answer. Before you
 * vote you see two choices; after, the live yes/no split. One vote, no take-backs.
 */
@Composable
private fun PollSection(
    ember: Ember,
    myVote: Boolean?,
    interactive: Boolean,
    contentPadding: androidx.compose.ui.unit.Dp,
    onVote: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = contentPadding, vertical = DurenSpacing.space2),
        verticalArrangement = Arrangement.spacedBy(DurenSpacing.space2)
    ) {
        if (myVote == null && interactive) {
            Row(horizontalArrangement = Arrangement.spacedBy(DurenSpacing.space3)) {
                PollChoiceButton("Yes", { onVote(true) }, Modifier.weight(1f))
                PollChoiceButton("No", { onVote(false) }, Modifier.weight(1f))
            }
        } else {
            val total = ember.pollYes + ember.pollNo
            val yesPct = if (total == 0) (if (myVote == true) 100 else 0) else ember.pollYes * 100 / total
            val noPct = if (total == 0) (if (myVote == false) 100 else 0) else ember.pollNo * 100 / total
            PollResultRow("Yes", yesPct, mine = myVote == true)
            PollResultRow("No", noPct, mine = myVote == false)
            Text(
                text = if (total == 1) "1 voice" else "$total voices",
                fontSize = 11.sp,
                color = DurenColors.TextMuted
            )
        }
    }
}

/** A tappable poll answer before voting. */
@Composable
private fun PollChoiceButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(DurenColors.SurfaceElevated)
            .clickable(onClick = onClick)
            .padding(vertical = DurenSpacing.space2),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = DurenColors.TextSecondary)
    }
}

/** A poll result: label, percentage, and a teal bar — brighter for the side you picked. */
@Composable
private fun PollResultRow(label: String, pct: Int, mine: Boolean) {
    val fill = if (mine) DurenColors.AccentTeal else DurenColors.AccentTeal.copy(alpha = 0.3f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DurenShapes.small)
            .background(DurenColors.SurfaceElevated)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(pct / 100f)
                .height(28.dp)
                .clip(DurenShapes.small)
                .background(fill)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .padding(horizontal = DurenSpacing.space3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (mine) "$label ●" else label,
                fontSize = 13.sp,
                fontWeight = if (mine) FontWeight.SemiBold else FontWeight.Normal,
                color = DurenColors.TextPrimary
            )
            Text(text = "$pct%", fontSize = 13.sp, color = DurenColors.TextSecondary)
        }
    }
}

/** Decode a `data:image/...;base64,…` URI into an [ImageBitmap], or null if malformed. */
private fun decodeDataUri(dataUri: String): ImageBitmap? = try {
    val base64 = dataUri.substringAfter("base64,", "")
    if (base64.isBlank()) {
        null
    } else {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    }
} catch (_: Exception) {
    null
}
