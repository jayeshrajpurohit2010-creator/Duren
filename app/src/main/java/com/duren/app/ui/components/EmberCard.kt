package com.duren.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.duren.app.ui.theme.DurenAvatarColors
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
    modifier: Modifier = Modifier
) {
    var showColdMarkDialog by remember { mutableStateOf(false) }

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
            modifier = Modifier.padding(DurenSpacing.space4)
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
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(DurenAvatarColors.colorForHex(ember.authorAvatarColor)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ember.authorAvatarUrl,
                            contentDescription = "Avatar for ${ember.authorName}",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )
                    }
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
                // Echo button + count
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onEcho)
                ) {
                    Icon(
                        imageVector = if (ember.echoedByMe) Icons.Filled.Favorite else Icons.Outlined.Favorite,
                        contentDescription = if (ember.echoedByMe) "Un-echo" else "Echo",
                        tint = if (ember.echoedByMe) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(DurenSpacing.space1))
                    Text(
                        text = ember.echoCount.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (ember.echoedByMe) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Expiry timer
                ExpiryTimer(
                    expiresAt = ember.expiresAt,
                    extended = ember.extended
                )

                // Overflow → cold mark
                IconButton(
                    onClick = { showColdMarkDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
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
