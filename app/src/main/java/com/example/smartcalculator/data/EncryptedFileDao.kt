package com.example.smartcalculator.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EncryptedFileDao {
    @Query("SELECT * FROM encrypted_files WHERE vaultType = :vaultType ORDER BY dateAdded DESC")
    fun getFilesByVaultType(vaultType: String): Flow<List<EncryptedFile>>

    @Query("SELECT * FROM encrypted_files WHERE id = :id")
    suspend fun getFileById(id: Long): EncryptedFile?

    @Insert
    suspend fun insertFile(file: EncryptedFile): Long

    @Delete
    suspend fun deleteFile(file: EncryptedFile)

    @Query("DELETE FROM encrypted_files WHERE vaultType = :vaultType")
    suspend fun deleteAllByVaultType(vaultType: String)

    @Query("SELECT COUNT(*) FROM encrypted_files WHERE vaultType = :vaultType")
    fun getFileCountByVaultType(vaultType: String): Flow<Int>
}
