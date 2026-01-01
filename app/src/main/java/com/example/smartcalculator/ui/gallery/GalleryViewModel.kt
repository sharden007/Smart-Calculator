package com.example.smartcalculator.ui.gallery

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartcalculator.data.AppDatabase
import com.example.smartcalculator.data.EncryptedFile
import com.example.smartcalculator.utils.EncryptionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val encryptionUtils = EncryptionUtils(application)

    private val _realVaultFiles = MutableStateFlow<List<EncryptedFile>>(emptyList())
    val realVaultFiles: StateFlow<List<EncryptedFile>> = _realVaultFiles.asStateFlow()

    private val _decoyVaultFiles = MutableStateFlow<List<EncryptedFile>>(emptyList())
    val decoyVaultFiles: StateFlow<List<EncryptedFile>> = _decoyVaultFiles.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadFiles()
    }

    private fun loadFiles() {
        viewModelScope.launch {
            database.encryptedFileDao().getFilesByVaultType("real").collect {
                _realVaultFiles.value = it
            }
        }

        viewModelScope.launch {
            database.encryptedFileDao().getFilesByVaultType("decoy").collect {
                _decoyVaultFiles.value = it
            }
        }
    }

    fun addFile(uri: Uri, vaultType: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                withContext(Dispatchers.IO) {
                    val contentResolver = getApplication<Application>().contentResolver

                    // Copy URI to temp file
                    val tempFile = File(encryptionUtils.getTempDir(), "temp_${System.currentTimeMillis()}")
                    contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }

                    // Get file details
                    val fileName = getFileName(contentResolver, uri) ?: "unknown_file"
                    val mimeType = contentResolver.getType(uri)
                    val fileType = when {
                        mimeType?.startsWith("image") == true -> "image"
                        mimeType?.startsWith("video") == true -> "video"
                        mimeType?.startsWith("audio") == true -> "audio"
                        mimeType?.contains("pdf") == true -> "pdf"
                        mimeType?.contains("document") == true ||
                        mimeType?.contains("text") == true -> "document"
                        else -> "file"
                    }

                    // Encrypt the file
                    val encryptedFile = File(
                        encryptionUtils.getEncryptedDir(),
                        "enc_${System.currentTimeMillis()}"
                    )

                    if (encryptionUtils.encryptFile(tempFile, encryptedFile)) {
                        // Generate and encrypt thumbnail
                        val thumbnailPath = generateThumbnail(tempFile, fileType)

                        // Save to database
                        val encryptedFileEntity = EncryptedFile(
                            originalPath = uri.toString(),
                            encryptedPath = encryptedFile.absolutePath,
                            fileName = fileName,
                            fileType = fileType,
                            vaultType = vaultType,
                            thumbnailPath = thumbnailPath
                        )

                        database.encryptedFileDao().insertFile(encryptedFileEntity)

                        // Delete original file from media store if possible
                        try {
                            contentResolver.delete(uri, null, null)
                        } catch (e: Exception) {
                            // May fail on newer Android versions - that's okay
                        }
                    }

                    // Clean up temp file
                    tempFile.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteFile(file: EncryptedFile) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // Delete encrypted file from disk
                    File(file.encryptedPath).delete()
                    file.thumbnailPath?.let { File(it).delete() }

                    // Delete from database
                    database.encryptedFileDao().deleteFile(file)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getDecryptedFile(file: EncryptedFile): File? {
        return try {
            val tempFile = File(encryptionUtils.getTempDir(), file.fileName)
            val encryptedFile = File(file.encryptedPath)

            if (encryptionUtils.decryptFile(encryptedFile, tempFile)) {
                tempFile
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getFileName(contentResolver: ContentResolver, uri: Uri): String? {
        var fileName: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex)
                }
            }
        }
        return fileName
    }

    private fun generateThumbnail(file: File, fileType: String): String? {
        return try {
            val bitmap = when (fileType) {
                "image" -> {
                    // Generate thumbnail for image
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeFile(file.absolutePath, options)

                    val targetSize = 512
                    options.inSampleSize = calculateInSampleSize(options, targetSize, targetSize)
                    options.inJustDecodeBounds = false

                    BitmapFactory.decodeFile(file.absolutePath, options)
                }
                "video" -> {
                    // Generate thumbnail for video
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(file.absolutePath)
                    val bitmap = retriever.getFrameAtTime(0)
                    retriever.release()
                    bitmap
                }
                else -> null
            }

            bitmap?.let {
                // Save thumbnail
                val thumbFile = File(encryptionUtils.getTempDir(), "thumb_${System.currentTimeMillis()}.jpg")
                FileOutputStream(thumbFile).use { out ->
                    it.compress(Bitmap.CompressFormat.JPEG, 80, out)
                }
                it.recycle()

                // Encrypt thumbnail
                val encryptedThumb = File(
                    encryptionUtils.getThumbnailDir(),
                    "thumb_${System.currentTimeMillis()}"
                )

                if (encryptionUtils.encryptFile(thumbFile, encryptedThumb)) {
                    thumbFile.delete()
                    encryptedThumb.absolutePath
                } else {
                    thumbFile.delete()
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}
