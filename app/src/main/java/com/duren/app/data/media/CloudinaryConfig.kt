package com.duren.app.data.media

/**
 * Cloudinary unsigned-upload configuration.
 *
 * Only the cloud name and an UNSIGNED upload preset are needed on the client.
 * The Cloudinary API key/secret must never ship in the app — unsigned uploads
 * are authorized by the preset alone, so there is nothing sensitive here.
 */
object CloudinaryConfig {
    const val CLOUD_NAME = "diyqyssvh"
    const val UPLOAD_PRESET = "duren_unsigned"

    /** Cloudinary's image upload endpoint for this cloud. */
    val UPLOAD_URL: String
        get() = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload"
}
