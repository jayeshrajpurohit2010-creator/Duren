package com.duren.app.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Decode an inline `data:image/...;base64,…` URI into an [ImageBitmap], or null if
 * it's malformed. Duren stores media inline (no external host on the free plan), so
 * avatars, ember photos, the full-screen viewer and profile covers all decode the
 * same way — this is the single place that does it.
 *
 * Returns null (rather than throwing) on a bad payload, so callers just skip drawing.
 * Decoding runs on the caller's thread; keep it inside `remember(url) { … }` so a
 * recomposition doesn't redo the work.
 */
internal fun decodeDataUri(dataUri: String): ImageBitmap? = try {
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
