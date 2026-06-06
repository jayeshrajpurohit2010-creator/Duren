package com.duren.app.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import com.duren.app.ui.theme.DurenAvatarColors

/**
 * One circular avatar renderer used everywhere (feed, profile, settings).
 *
 * [avatarUrl] may be a custom inline photo (`data:image/...;base64,…`, decoded
 * locally), a remote URL (DiceBear, loaded via Coil), or null/blank (just the
 * colored circle shows). The [fallbackColorHex] tints the circle behind it.
 */
@Composable
fun DurenAvatar(
    avatarUrl: String?,
    fallbackColorHex: String,
    size: Dp,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(DurenAvatarColors.colorForHex(fallbackColorHex)),
        contentAlignment = Alignment.Center
    ) {
        val url = avatarUrl
        when {
            url != null && url.startsWith("data:") -> {
                val bitmap = remember(url) { decodeDataUri(url) }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = contentDescription,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(size).clip(CircleShape)
                    )
                }
            }

            !url.isNullOrBlank() -> {
                AsyncImage(
                    model = url,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(size).clip(CircleShape)
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
