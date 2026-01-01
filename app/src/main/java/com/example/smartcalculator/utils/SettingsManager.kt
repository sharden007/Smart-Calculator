package com.example.smartcalculator.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SettingsManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "smart_calculator_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_REAL_PIN = "real_pin"
        private const val KEY_DECOY_PIN = "decoy_pin"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val DEFAULT_REAL_PIN = "123+="
        private const val DEFAULT_DECOY_PIN = "456+="
    }

    var realPin: String
        get() = sharedPreferences.getString(KEY_REAL_PIN, DEFAULT_REAL_PIN) ?: DEFAULT_REAL_PIN
        set(value) = sharedPreferences.edit().putString(KEY_REAL_PIN, value).apply()

    var decoyPin: String
        get() = sharedPreferences.getString(KEY_DECOY_PIN, DEFAULT_DECOY_PIN) ?: DEFAULT_DECOY_PIN
        set(value) = sharedPreferences.edit().putString(KEY_DECOY_PIN, value).apply()

    var isFirstLaunch: Boolean
        get() = sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_FIRST_LAUNCH, value).apply()

    fun checkPinMatch(input: String): VaultType {
        return when (input) {
            realPin -> VaultType.REAL
            decoyPin -> VaultType.DECOY
            else -> VaultType.NONE
        }
    }

    enum class VaultType {
        REAL, DECOY, NONE
    }
}
