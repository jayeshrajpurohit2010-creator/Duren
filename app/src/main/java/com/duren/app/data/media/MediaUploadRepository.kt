package com.duren.app.data.media

import android.content.Context
import android.net.Uri
import com.duren.app.core.DomainError
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Uploads media to Cloudinary via an unsigned upload preset.
 *
 * Phase 1 supports images only. The picked [Uri] is streamed straight to
 * Cloudinary (no Bitmap re-encode) and the hosted `secure_url` is returned for
 * storage on the ember document. Failures are surfaced as
 * [DomainError.MediaUploadFailed] so the caller never writes an ember that
 * points at a missing image.
 */
@Singleton
class MediaUploadRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val httpClient by lazy { OkHttpClient() }

    suspend fun uploadImage(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext Result.failure(DomainError.MediaUploadFailed)

            val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"

            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    "ember",
                    bytes.toRequestBody(mime.toMediaTypeOrNull())
                )
                .addFormDataPart("upload_preset", CloudinaryConfig.UPLOAD_PRESET)
                .build()

            val request = Request.Builder()
                .url(CloudinaryConfig.UPLOAD_URL)
                .post(body)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val payload = response.body?.string()
                if (!response.isSuccessful || payload.isNullOrBlank()) {
                    return@withContext Result.failure(DomainError.MediaUploadFailed)
                }
                val secureUrl = JSONObject(payload).optString("secure_url")
                if (secureUrl.isBlank()) {
                    return@withContext Result.failure(DomainError.MediaUploadFailed)
                }
                Result.success(secureUrl)
            }
        } catch (_: Exception) {
            Result.failure(DomainError.MediaUploadFailed)
        }
    }
}
