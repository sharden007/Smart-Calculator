package com.example.smartcalculator.utils

import android.content.Context
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import okio.buffer
import okio.source
import java.io.File

class EncryptedImageFetcher(
    private val data: String,
    private val options: Options,
    private val context: Context
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val encryptionUtils = EncryptionUtils(context)
        val encryptedFile = File(data)
        val decryptedFile = File(encryptionUtils.getTempDir(), "temp_view_${System.currentTimeMillis()}.jpg")

        return if (encryptionUtils.decryptFile(encryptedFile, decryptedFile)) {
            SourceFetchResult(
                source = ImageSource(
                    source = decryptedFile.source().buffer(),
                    fileSystem = options.fileSystem,
                    metadata = null
                ),
                mimeType = null,
                dataSource = DataSource.DISK
            )
        } else {
            throw IllegalStateException("Failed to decrypt file")
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<String> {
        override fun create(data: String, options: Options, imageLoader: ImageLoader): Fetcher? {
            // Only handle encrypted file paths
            return if (data.contains(".secure") || data.contains(".thumbnails")) {
                EncryptedImageFetcher(data, options, context)
            } else {
                null
            }
        }
    }
}
