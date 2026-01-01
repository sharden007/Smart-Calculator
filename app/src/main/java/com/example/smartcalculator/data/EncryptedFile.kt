package com.example.smartcalculator.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "encrypted_files")
data class EncryptedFile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalPath: String,
    val encryptedPath: String,
    val fileName: String,
    val fileType: String, // "image" or "video"
    val thumbnailPath: String? = null,
    val dateAdded: Long = System.currentTimeMillis(),
    val vaultType: String = "real" // "real" or "decoy"
)
