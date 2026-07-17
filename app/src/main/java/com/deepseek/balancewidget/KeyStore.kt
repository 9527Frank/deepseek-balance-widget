package com.deepseek.balancewidget

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Securely store and retrieve the DeepSeek API key.
 * Uses Android EncryptedSharedPreferences backed by the device keystore.
 */
object KeyStore {

    private const val PREFS_NAME = "deepseek_secure_prefs"
    private const val KEY_API_KEY = "api_key"

    private var _prefs: SharedPreferences? = null

    private fun getPrefs(context: Context): SharedPreferences {
        if (_prefs == null) {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            _prefs = EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
        return _prefs!!
    }

    fun saveApiKey(context: Context, apiKey: String) {
        getPrefs(context).edit().putString(KEY_API_KEY, apiKey).apply()
    }

    fun getApiKey(context: Context): String {
        return getPrefs(context).getString(KEY_API_KEY, "") ?: ""
    }

    fun hasApiKey(context: Context): Boolean {
        return getApiKey(context).isNotBlank()
    }

    fun clearApiKey(context: Context) {
        getPrefs(context).edit().remove(KEY_API_KEY).apply()
    }
}
