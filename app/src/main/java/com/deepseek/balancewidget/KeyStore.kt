package com.deepseek.balancewidget

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Securely store and retrieve the DeepSeek API key.
 *
 * Uses Android EncryptedSharedPreferences backed by the device keystore.
 * On devices (like HarmonyOS) where the keystore is unavailable, gracefully
 * falls back to regular SharedPreferences with basic obfuscation.
 */
object KeyStore {

    private const val PREFS_NAME = "deepseek_secure_prefs"
    private const val KEY_API_KEY = "api_key"

    /** true = using EncryptedSharedPreferences, false = plain fallback */
    private var _useEncrypted: Boolean? = null

    private var _prefs: SharedPreferences? = null

    private fun getPrefs(context: Context): SharedPreferences {
        if (_prefs == null) {
            // 1) Try encrypted storage first
            if (_useEncrypted != false) {
                try {
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
                    _useEncrypted = true
                    return _prefs!!
                } catch (e: Exception) {
                    Log.w("KeyStore", "EncryptedSharedPreferences 不可用，降级到普通存储", e)
                    _useEncrypted = false
                }
            }

            // 2) Fallback — plain SharedPreferences with XOR obfuscation
            _prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
        return _prefs!!
    }

    /** Simple XOR + Base64 obfuscation so the key isn't plaintext on disk. */
    private fun obfuscate(text: String): String {
        val bytes = text.toByteArray()
        val key = 0x7A
        for (i in bytes.indices) {
            bytes[i] = (bytes[i].toInt() xor key).toByte()
        }
        return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    }

    private fun deobfuscate(encoded: String): String {
        val bytes = android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP)
        val key = 0x7A
        for (i in bytes.indices) {
            bytes[i] = (bytes[i].toInt() xor key).toByte()
        }
        return String(bytes)
    }

    private fun tryDeobfuscate(value: String): String {
        return try {
            deobfuscate(value)
        } catch (_: Exception) {
            ""
        }
    }

    fun saveApiKey(context: Context, apiKey: String) {
        val prefs = getPrefs(context)
        if (_useEncrypted == true) {
            prefs.edit().putString(KEY_API_KEY, apiKey).apply()
        } else {
            prefs.edit().putString(KEY_API_KEY, obfuscate(apiKey)).apply()
        }
    }

    fun getApiKey(context: Context): String {
        val prefs = getPrefs(context)
        val value = prefs.getString(KEY_API_KEY, "") ?: ""
        if (value.isEmpty()) return ""

        return if (_useEncrypted == true) {
            value
        } else {
            tryDeobfuscate(value)
        }
    }

    fun hasApiKey(context: Context): Boolean {
        return getApiKey(context).isNotBlank()
    }

    fun clearApiKey(context: Context) {
        getPrefs(context).edit().remove(KEY_API_KEY).apply()
    }
}
