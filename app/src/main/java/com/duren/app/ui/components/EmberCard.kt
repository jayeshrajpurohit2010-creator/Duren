package com.duren.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import coil3.compose.AsyncImage
import com.duren.app.data.ember.model.Ember
import com.duren.app.data.ember.model.PostMode
import com.duren.app.feature.whisper.WhisperThread
import com.duren.app.ui.theme.DurenColors
import com.duren.app.ui.theme.DurenShapes
import com.duren.app.ui.theme.DurenSpacing

/**
 * The primary content cell for The Clearing.
 *
 * Identity is masked for Anonymous and Confess modes — the avatar and name are
 * replaced by a 🎭 placeholder, keeping the author hidden even in UI.
 */
@Composable
fun EmberCard(
    ember: Ember,
    onEcho: () -> Unit,
    onColdMark: (reason: String) -> Unit,
    modifier: Modifier = Modifier,
    // When false (e.g. your own embers on Presence), the echo + report controls
    // render as plain, non-tappable status so there's no dead/confusing UI.
    interactive: Boolean = true,
    // When true (the user authored this ember), a long-press offers Delete.
    canDelete: Boolean = false,
    onDelete: () -> Unit = {}
) {
    var showColdMarkDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showWhispers by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var zoomedMedia by remember { mutableStateOf<String?>(null) }

    // Echo-tap bounce (Animation Bible: scale 1.0 → 1.2 → 1.0 over ~0.15s).
    val scope = rememberCoroutineScope()
    val heartScale = remember { Animatable(1f) }
    fun popHeart() = scope.launch {
        heartScale.animateTo(1.2f, tween(75))
        heartScale.animateTo(1f, tween(75))
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete ember?") },
            text = { Text("This ember will be gone for good. It can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
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

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = DurenColors.BorderDefault,
                shape = DurenShapes.large
            ),
        shape = DurenShapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .padding(DurenSpacing.space4)
                .pointerInput(canDelete) {
                    if (canDelete) {
                        detectTapGestures(onLongPress = { showDeleteDialog = true })
                    }
                }
        ) {
            // Header: avatar + identity + temperature badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                if (ember.mode.isMasked) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🎭", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    DurenAvatar(
                        avatarUrl = ember.authorAvatarUrl,
                        fallbackColorHex = ember.authorAvatarColor,
                        size = 40.dp,
                        contentDescription = "Avatar for ${ember.authorName}"
                    )
                }

                Spacer(modifier = Modifier.width(DurenSpacing.space3))

                // Identity text
                Column(modifier = Modifier.weight(1f)) {
                    if (ember.mode.isMasked) {
                        Text(
                            text = if (ember.mode == PostMode.Confess) "Confession" else "Anonymous",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Text(
                            text = ember.authorName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "@${ember.authorUsername}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Temperature badge on the right
                TemperatureBadge(echoCount = ember.echoCount)
            }

            // Tribe line
            val tribeLine = ember.tribeName.trim().ifBlank { null }
            Text(
                text = if (tribeLine != null) "in $tribeLine" else "in The Clearing",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = DurenSpacing.space1)
            )

            // Body text
            if (ember.text.isNotBlank()) {
                Text(
                    text = ember.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = DurenSpacing.space3)
                )
            }

            // Media
            ember.mediaUrl?.let { media ->
                val mediaModifier = Modifier
                    .padding(top = DurenSpacing.space3)
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .clip(DurenShapes.medium)
                    // Tap any photo to open it full-screen with pinch-to-zoom.
                    .clickable { zoomedMedia = media }
                if (media.startsWith("data:")) {
                    // Inline (Base64) media — decode locally, no network fetch.
                    val bitmap = remember(media) { decodeDataUri(media) }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Ember media",
                            contentScale = ContentScale.Crop,
                            modifier = mediaModifier
                        )
                    }
                } else {
                    AsyncImage(
                        model = media,
                        contentDescription = "Ember media",
                        contentScale = ContentScale.Crop,
                        modifier = mediaModifier
                    )
                }
            }

            // Footer row: echo button | expiry timer | overflow
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = DurenSpacing.space3),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Echo button + whisper toggle, grouped on the left
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = if (interactive) {
                            Modifier.clickable { popHeart(); onEcho() }
                        } else {
                            Modifier
                        }
                    ) {
                        Icon(
                            imageVector = if (ember.echoedByMe) Icons.Filled.Favorite else Icons.Outlined.Favorite,
                            contentDescription = if (ember.echoedByMe) "Un-echo" else "Echo",
                            tint = if (ember.echoedByMe) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer { scaleX = heartScale.value; scaleY = heartScale.value }
                        )
                        Spacer(modifier = Modifier.width(DurenSpacing.space1))
                        Text(
                            text = ember.echoCount.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (ember.echoedByMe) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(DurenSpacing.space4))

                    // Whisper (comment) toggle — opens the inline thread below.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showWhispers = !showWhispers }
                    ) {
                        Text(text = "💬", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.width(DurenSpacing.space1))
                        Text(
                            text = ember.whisperCount.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Expiry timer
                ExpiryTimer(
                    expiresAt = ember.expiresAt,
                    extended = ember.extended
                )

                // Overflow menu — Delete (own embers) and/or Report. Always present
                // when there's an action, so it's a reliable trigger even where a
                // long-press would be swallowed by the photo or echo heart.
                if (canDelete || interactive) {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = "More options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (canDelete) {
                                DropdownMenuItem(
                                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMenu = false
                                        showDeleteDialog = true
                                    }
                                )
                            }
                            if (interactive && !canDelete) {
                                DropdownMenuItem(
                                    text = { Text("Report") },
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

            // Inline whisper thread (lazily streamed only while expanded)
            if (showWhispers) {
                WhisperThread(
                    emberId = ember.id,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = DurenSpacing.space3)
                )
            }
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
