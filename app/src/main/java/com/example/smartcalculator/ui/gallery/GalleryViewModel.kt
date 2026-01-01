package com.example.smartcalculator.ui.gallery

import android.app.Application
import android.content.ContentResolver
import android.content.Context
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
                    val fileName = getFileName(contentResolver, uri) ?: "unknown"
                    val fileType = when {
                        contentResolver.getType(uri)?.startsWith("image") == true -> "image"
                        contentResolver.getType(uri)?.startsWith("video") == true -> "video"
                        else -> "unknown"
                    }

                    // Encrypt the file
                    val encryptedFile = File(
                        encryptionUtils.getEncryptedDir(),
                        "enc_${System.currentTimeMillis()}"
                    )

                    if (encryptionUtils.encryptFile(tempFile, encryptedFile)) {
                        // Save to database
                        val encryptedFileEntity = EncryptedFile(
                            originalPath = uri.toString(),
                            encryptedPath = encryptedFile.absolutePath,
                            fileName = fileName,
                            fileType = fileType,
                            vaultType = vaultType
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
}
