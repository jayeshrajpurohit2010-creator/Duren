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
 * Turns a picked image into a self-contained `data:` URI for the ember document.
 *
 * Phase 1 MVP stores media inline: the picked [Uri] is downscaled and JPEG-
 * compressed on-device, then Base64-encoded into a `data:image/jpeg;base64,…`
 * string. That string is written straight onto the ember (the [EmberCard]
 * decodes it locally), so media needs no external host — it rides on the same
 * Firebase Auth + Firestore path that's already working.
 *
 * The compression budget keeps the encoded payload well under Firestore's 1 MB
 * document limit. Failures surface as [DomainError.MediaUploadFailed] so the
 * caller never writes an ember pointing at media that didn't materialise.
 *
 * Trade-off: inline media bloats document reads, so this is an MVP bridge —
 * swap [uploadImage] back to a hosted uploader (Cloudinary/Storage) once a
 * verified cloud or a Blaze bucket is available; nothing else has to change.
 */
@Singleton
class MediaUploadRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun uploadImage(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val bitmap = decodeBounded(uri)
                ?: return@withContext Result.failure(DomainError.MediaUploadFailed)
            val scaled = downscale(bitmap, MAX_DIMEN)

            // Step quality down until the JPEG fits the inline budget.
            var quality = START_QUALITY
            var jpeg = scaled.toJpeg(quality)
            while (jpeg.size > MAX_JPEG_BYTES && quality > MIN_QUALITY) {
                quality -= QUALITY_STEP
                jpeg = scaled.toJpeg(quality)
            }
            if (jpeg.size > MAX_JPEG_BYTES) {
                return@withContext Result.failure(DomainError.MediaUploadFailed)
            }

            val base64 = Base64.encodeToString(jpeg, Base64.NO_WRAP)
            Result.success("data:image/jpeg;base64,$base64")
        } catch (_: Exception) {
            Result.failure(DomainError.MediaUploadFailed)
        }
    }

    /** Decode with an [BitmapFactory.Options.inSampleSize] so huge photos never OOM. */
    private fun decodeBounded(uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, MAX_DIMEN)
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
        const val MAX_DIMEN = 1080            // longest edge after downscale
        const val START_QUALITY = 80
        const val MIN_QUALITY = 35
        const val QUALITY_STEP = 10
        const val MAX_JPEG_BYTES = 500 * 1024  // ~680 KB once Base64'd — safely < 1 MB doc cap
    }
}
