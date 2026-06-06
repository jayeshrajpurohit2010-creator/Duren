package com.duren.app.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.duren.app.core.DomainError
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns a picked image into a self-contained `data:` URI.
 *
 * Phase 1 MVP stores media inline: the picked [Uri] is downscaled and JPEG-
 * compressed on-device, then Base64-encoded into a `data:image/jpeg;base64,…`
 * string. That string is written straight onto the ember (the EmberCard decodes
 * it locally) or the profile, so media needs no external host — it rides on the
 * same Firebase Auth + Firestore path that's already working.
 *
 * [uploadImage] is tuned for post media (≤1080px, ~500 KB). [uploadAvatar] is
 * tuned for profile photos (≤256px, ~60 KB) so an avatar stays small enough to
 * sit on the profile doc and denormalise onto embers without bloat.
 *
 * The compression budget keeps the encoded payload well under Firestore's 1 MB
 * document limit. Failures surface as [DomainError.MediaUploadFailed] so the
 * caller never writes a reference to media that didn't materialise.
 *
 * Trade-off: inline media bloats document reads — an MVP bridge. Swapping back
 * to a hosted uploader later changes only these two methods; callers and the
 * `data:`-aware renderers stay the same.
 */
@Singleton
class MediaUploadRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /** Post media — larger budget for full-bleed photos. */
    suspend fun uploadImage(uri: Uri): Result<String> =
        encode(uri, MAX_DIMEN, MAX_JPEG_BYTES, START_QUALITY, MIN_QUALITY)

    /** Profile avatar — small budget so it can ride on the profile + ember docs. */
    suspend fun uploadAvatar(uri: Uri): Result<String> =
        encode(uri, AVATAR_DIMEN, AVATAR_MAX_BYTES, AVATAR_START_QUALITY, AVATAR_MIN_QUALITY)

    private suspend fun encode(
        uri: Uri,
        maxDimen: Int,
        maxBytes: Int,
        startQuality: Int,
        minQuality: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val bitmap = decodeBounded(uri, maxDimen)
                ?: return@withContext Result.failure(DomainError.MediaUploadFailed)
            val scaled = downscale(bitmap, maxDimen)

            // Step quality down until the JPEG fits the inline budget.
            var quality = startQuality
            var jpeg = scaled.toJpeg(quality)
            while (jpeg.size > maxBytes && quality > minQuality) {
                quality -= QUALITY_STEP
                jpeg = scaled.toJpeg(quality)
            }
            if (jpeg.size > maxBytes) {
                return@withContext Result.failure(DomainError.MediaUploadFailed)
            }

            val base64 = Base64.encodeToString(jpeg, Base64.NO_WRAP)
            Result.success("data:image/jpeg;base64,$base64")
        } catch (_: Exception) {
            Result.failure(DomainError.MediaUploadFailed)
        }
    }

    /** Decode with an [BitmapFactory.Options.inSampleSize] so huge photos never OOM. */
    private fun decodeBounded(uri: Uri, maxDimen: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxDimen)
        }
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }
    }

    private fun downscale(src: Bitmap, maxDimen: Int): Bitmap {
        val longest = maxOf(src.width, src.height)
        if (longest <= maxDimen) return src
        val ratio = maxDimen.toFloat() / longest
        return Bitmap.createScaledBitmap(
            src,
            (src.width * ratio).toInt().coerceAtLeast(1),
            (src.height * ratio).toInt().coerceAtLeast(1),
            true
        )
    }

    private fun Bitmap.toJpeg(quality: Int): ByteArray =
        ByteArrayOutputStream().use { out ->
            compress(Bitmap.CompressFormat.JPEG, quality, out)
            out.toByteArray()
        }

    private fun sampleSizeFor(width: Int, height: Int, maxDimen: Int): Int {
        var sample = 1
        var longest = maxOf(width, height)
        while (longest / 2 >= maxDimen) {
            longest /= 2
            sample *= 2
        }
        return sample
    }

    private companion object {
        // Post media
        const val MAX_DIMEN = 1080            // longest edge after downscale
        const val START_QUALITY = 80
        const val MIN_QUALITY = 35
        const val MAX_JPEG_BYTES = 500 * 1024  // ~680 KB once Base64'd — safely < 1 MB doc cap

        // Avatar (small — denormalises onto embers)
        const val AVATAR_DIMEN = 256
        const val AVATAR_START_QUALITY = 80
        const val AVATAR_MIN_QUALITY = 40
        const val AVATAR_MAX_BYTES = 60 * 1024  // ~80 KB once Base64'd

        const val QUALITY_STEP = 10
    }
}
