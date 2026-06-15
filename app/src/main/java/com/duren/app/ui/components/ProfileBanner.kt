package com.duren.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.duren.app.ui.theme.LocalDurenColors

/**
 * A profile cover image that refuses to look like an Instagram banner. It bleeds in
 * from the top and — with [scrim] on — dissolves into the page's darkness, so the
 * avatar and name below emerge from the same black the rest of Duren lives on:
 * firelight on a wall, not a framed photo.
 *
 * Renders nothing when [bannerUrl] is blank, so a profile without a cover looks
 * exactly as it did before. [bannerUrl] is usually an inline `data:` URI (decoded
 * locally, like [DurenAvatar]); a plain http(s) URL falls back to Coil.
 */
@Composable
fun ProfileBanner(
    bannerUrl: String?,
    modifier: Modifier = Modifier,
    height: Dp = 150.dp,
    scrim: Boolean = true
) {
    val url = bannerUrl
    if (url.isNullOrBlank()) return
    val background = LocalDurenColors.current.BackgroundPrimary
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        if (url.startsWith("data:")) {
            val bitmap = remember(url) { decodeDataUri(url) }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Cover photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            }
        } else {
            AsyncImage(
                model = url,
                contentDescription = "Cover photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }
        if (scrim) {
            // Let the cover fall away into the page instead of ending on a hard line.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.5f to background.copy(alpha = 0.35f),
                            1.0f to background
                        )
                    )
            )
        }
    }
}
