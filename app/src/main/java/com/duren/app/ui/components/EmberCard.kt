package com.duren.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
    onDelete: () -> Unit = {}
) {
    var showColdMarkDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showWhispers by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var zoomedMedia by remember { mutableStateOf<String?>(null) }

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
                    ember.mode == PostMode.Confess -> "A confession"
                    ember.mode.isMasked -> "A soul"
                    else -> ember.authorName
                }
                Text(
                    text = name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DurenColors.TextPrimary
                )
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
            Text(
                text = ember.text,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = DurenColors.TextPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = contentPadding, vertical = DurenSpacing.space3)
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
                    Row(
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

                if (canDelete || interactive) {
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
