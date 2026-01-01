package com.example.smartcalculator.utils

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class EncryptionUtils(private val context: Context) {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "SmartCalculatorKey"
        private const val TRANSFORMATION = "AES/CBC/PKCS7Padding"
        private const val IV_SIZE = 16
    }

    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
        load(null)
    }

    init {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            generateKey()
        }
    }

    private fun generateKey() {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .setUserAuthenticationRequired(false)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    private fun getSecretKey(): SecretKey {
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    fun encryptFile(inputFile: File, outputFile: File): Boolean {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())

            val iv = cipher.iv

            FileOutputStream(outputFile).use { outputStream ->
                // Write IV to the beginning of the file
                outputStream.write(iv)

                FileInputStream(inputFile).use { inputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        val encryptedBytes = cipher.update(buffer, 0, bytesRead)
                        if (encryptedBytes != null) {
                            outputStream.write(encryptedBytes)
                        }
                    }

                    val finalBytes = cipher.doFinal()
                    if (finalBytes != null) {
                        outputStream.write(finalBytes)
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun decryptFile(inputFile: File, outputFile: File): Boolean {
        return try {
            FileInputStream(inputFile).use { inputStream ->
                // Read IV from the beginning of the file
                val iv = ByteArray(IV_SIZE)
                inputStream.read(iv)

                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), IvParameterSpec(iv))

                FileOutputStream(outputFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        val decryptedBytes = cipher.update(buffer, 0, bytesRead)
                        if (decryptedBytes != null) {
                            outputStream.write(decryptedBytes)
                        }
                    }

                    val finalBytes = cipher.doFinal()
                    if (finalBytes != null) {
                        outputStream.write(finalBytes)
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getEncryptedDir(): File {
        val dir = File(context.filesDir, ".secure")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getTempDir(): File {
        val dir = File(context.cacheDir, "temp")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getThumbnailDir(): File {
        val dir = File(context.filesDir, ".thumbnails")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
}
