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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import coil3.compose.AsyncImage
import com.duren.app.data.ember.model.Ember
import com.duren.app.data.ember.model.PostMode
import com.duren.app.feature.whisper.WhisperThread
import com.duren.app.ui.theme.LocalDurenColors
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
    // Kindling (F31): light an anonymous 🔥. Null where a surface doesn't wire it — the
    // count still shows (read-only), the flame just isn't tappable there.
    onKindle: (() -> Unit)? = null,
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
    // Kindling is fire-and-forget and anonymous, so we just remember that you lit one
    // this session — to bump the count immediately and stop a second tap.
    var kindledLocally by remember(ember.id) { mutableStateOf(false) }

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
                    MarkerLabel(DurenIcon.Pin, "Floating Lantern", LocalDurenColors.current.AccentTeal)
                }
                if (ember.isWisdom) {
                    MarkerLabel(DurenIcon.Spark, "Ember of Wisdom", WisdomGold)
                }
                if (ember.isFinal) {
                    // A goodbye left behind on the way out of a tribe (F35).
                    MarkerLabel(DurenIcon.Ember, "Final Ember", LocalDurenColors.current.TextSecondary)
                }
                if (ember.subEmberName.isNotBlank()) {
                    // The topic thread it lives in (F36).
                    Text(
                        text = "#${ember.subEmberName}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = LocalDurenColors.current.TextMuted
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
                        .background(LocalDurenColors.current.SurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    DurenIcon(
                        icon = DurenIcon.Mask,
                        size = 16.dp,
                        tint = LocalDurenColors.current.TextSecondary
                    )
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
                    color = LocalDurenColors.current.TextPrimary
                )
                // Ember signature — the author's tagline, italic and quiet.
                if (!ember.mode.isMasked && ember.emberSignature.isNotBlank()) {
                    Text(
                        text = ember.emberSignature,
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                        color = LocalDurenColors.current.TextSecondary
                    )
                }
                val tribeLine = ember.tribeName.trim().ifBlank { null }
                Text(
                    text = if (tribeLine != null) "in $tribeLine" else "in the clearing",
                    fontSize = 11.sp,
                    color = LocalDurenColors.current.TextMuted
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
                    color = LocalDurenColors.current.TextPrimary
                )
                if (fragmentHeld) {
                    Spacer(modifier = Modifier.height(DurenSpacing.space2))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DurenIcon(
                            icon = DurenIcon.Lock,
                            size = 13.dp,
                            tint = LocalDurenColors.current.AccentTeal
                        )
                        Spacer(modifier = Modifier.width(DurenSpacing.space1))
                        Text(
                            text = "Echo to read the rest",
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                            color = LocalDurenColors.current.AccentTeal
                        )
                    }
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

        val photos = ember.photos
        if (photos.isNotEmpty()) {
            EmberMedia(
                photos = photos,
                tightTop = ember.text.isBlank(),
                isHot = isHot,
                onZoom = { zoomedMedia = it }
            )
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
                            text = "let it rest",
                            fontSize = 12.sp,
                            color = LocalDurenColors.current.TextMuted
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
                            tint = if (ember.echoedByMe) LocalDurenColors.current.AccentTeal else LocalDurenColors.current.TextMuted,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer { scaleX = heartScale.value; scaleY = heartScale.value }
                        )
                        Spacer(modifier = Modifier.width(DurenSpacing.space1))
                        Text(
                            text = "${ember.echoCount} echoes",
                            fontSize = 12.sp,
                            color = if (ember.echoedByMe) LocalDurenColors.current.AccentTeal else LocalDurenColors.current.TextMuted
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
                        DurenIcon(
                            icon = DurenIcon.Whisper,
                            size = 14.dp,
                            tint = LocalDurenColors.current.TextMuted
                        )
                        Spacer(modifier = Modifier.width(DurenSpacing.space1))
                        Text(
                            text = "${ember.whisperCount} whispers",
                            fontSize = 12.sp,
                            color = LocalDurenColors.current.TextMuted
                        )
                    }

                    // Kindling (F31) — an anonymous 🔥. Shows wherever there are any, but
                    // is only tappable on a surface that wired onKindle (and never on a
                    // Final Ember, which just rests).
                    val kindleTotal = ember.kindlingCount + if (kindledLocally) 1 else 0
                    if (onKindle != null || kindleTotal > 0) {
                        Spacer(modifier = Modifier.width(DurenSpacing.space4))
                        val canKindle = interactive && onKindle != null && !kindledLocally && !ember.isFinal
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = if (canKindle) {
                                Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { kindledLocally = true; onKindle?.invoke() }
                            } else Modifier
                        ) {
                            DurenIcon(
                                icon = DurenIcon.Ember,
                                size = 14.dp,
                                tint = if (kindledLocally) LocalDurenColors.current.AccentTeal
                                       else LocalDurenColors.current.TextMuted
                            )
                            Spacer(modifier = Modifier.width(DurenSpacing.space1))
                            Text(
                                text = "$kindleTotal",
                                fontSize = 12.sp,
                                color = if (kindledLocally) LocalDurenColors.current.AccentTeal
                                        else LocalDurenColors.current.TextMuted
                            )
                        }
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
                                tint = LocalDurenColors.current.TextMuted,
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
                                    text = { Text(if (ember.pinnedNow()) "Unpin from tribe" else "Pin to tribe") },
                                    onClick = {
                                        showMenu = false
                                        onTogglePin()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (ember.isWisdom) "Remove wisdom" else "Mark as wisdom") },
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
                // A confession keeps its room faceless: every whisper here is anonymous.
                forceAnonymous = ember.mode == PostMode.Confess,
                // Only badge the author as OP when they posted in the open — never on a
                // masked ember, where it would reveal who's behind it.
                emberAuthorId = if (ember.mode == PostMode.Named) ember.authorId else null,
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
            .background(LocalDurenColors.current.BorderDefault.copy(alpha = 0.4f))
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

/** A small keeper/marker label — a hand-drawn glyph + a word, never an emoji. */
@Composable
private fun MarkerLabel(icon: DurenIcon, text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        DurenIcon(icon = icon, size = 12.dp, tint = color)
        Spacer(modifier = Modifier.width(DurenSpacing.space1))
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

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
                color = LocalDurenColors.current.TextMuted
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
            .background(LocalDurenColors.current.SurfaceElevated)
            .clickable(onClick = onClick)
            .padding(vertical = DurenSpacing.space2),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = LocalDurenColors.current.TextSecondary)
    }
}

/** A poll result: label, percentage, and a teal bar — brighter for the side you picked. */
@Composable
private fun PollResultRow(label: String, pct: Int, mine: Boolean) {
    val fill = if (mine) LocalDurenColors.current.AccentTeal else LocalDurenColors.current.AccentTeal.copy(alpha = 0.3f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DurenShapes.small)
            .background(LocalDurenColors.current.SurfaceElevated)
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
                color = LocalDurenColors.current.TextPrimary
            )
            Text(text = "$pct%", fontSize = 13.sp, color = LocalDurenColors.current.TextSecondary)
        }
    }
}

/** One ember photo — an inline `data:` URI decoded locally, otherwise loaded via Coil. */
@Composable
private fun EmberPhoto(url: String, contentScale: ContentScale, modifier: Modifier) {
    if (url.startsWith("data:")) {
        val bitmap = remember(url) { decodeDataUri(url) }
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Ember media",
                contentScale = contentScale,
                modifier = modifier
            )
        }
    } else {
        AsyncImage(
            model = url,
            contentDescription = "Ember media",
            contentScale = contentScale,
            modifier = modifier
        )
    }
}

/**
 * An ember's photo(s). One photo bleeds edge-to-edge at its natural height, exactly as
 * before; several become an edge-to-edge horizontal carousel of fixed-height frames you
 * swipe through (the next one peeks to say "there's more"). Fixed height so a four-photo
 * ember doesn't run a screen tall. Tapping any photo opens it full-screen.
 */
@Composable
private fun EmberMedia(
    photos: List<String>,
    tightTop: Boolean,
    isHot: Boolean,
    onZoom: (String) -> Unit
) {
    val topPad = if (tightTop) DurenSpacing.space3 else 0.dp
    if (photos.size == 1) {
        val media = photos[0]
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topPad)
        ) {
            EmberPhoto(
                url = media,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(media) { detectTapGestures(onTap = { onZoom(media) }) }
            )
            // Hot embers glow faintly at the photo's foot.
            if (isHot) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.BottomCenter)
                        .background(LocalDurenColors.current.AccentTeal.copy(alpha = 0.6f))
                )
            }
        }
        return
    }
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPad),
        horizontalArrangement = Arrangement.spacedBy(DurenSpacing.space2),
        contentPadding = PaddingValues(horizontal = DurenSpacing.space4)
    ) {
        items(photos) { media ->
            EmberPhoto(
                url = media,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillParentMaxWidth(0.82f)
                    .height(300.dp)
                    .clip(DurenShapes.large)
                    .pointerInput(media) { detectTapGestures(onTap = { onZoom(media) }) }
            )
        }
    }
}
