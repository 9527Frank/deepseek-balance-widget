package com.deepseek.balancewidget

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64

/**
 * Persistently store and retrieve the DeepSeek API key.
 *
 * Uses plain SharedPreferences with XOR + Base64 obfuscation so the key
 * isn't stored as plaintext on disk. This is NOT true encryption, but
 * avoids any dependency on Android Keystore / security-crypto, making
 * the app work on devices like HarmonyOS where Keystore is unavailable.
 */
object KeyStore {

    private const val PREFS_NAME = "deepseek_prefs"
    private const val KEY_API_KEY = "api_key"

    private var _prefs: SharedPreferences? = null

    private fun getPrefs(context: Context): SharedPreferences {
        if (_prefs == null) {
            _prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
        return _prefs!!
    }

    /** XOR + Base64 obfuscation. */
    private fun obfuscate(text: String): String {
        val bytes = text.toByteArray()
        val xorKey = 0x7A.toByte()
        for (i in bytes.indices) bytes[i] = (bytes[i].toInt() xor xorKey.toInt()).toByte()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun deobfuscate(encoded: String): String {
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        val xorKey = 0x7A.toByte()
        for (i in bytes.indices) bytes[i] = (bytes[i].toInt() xor xorKey.toInt()).toByte()
        return String(bytes)
    }

    fun saveApiKey(context: Context, apiKey: String) {
        getPrefs(context).edit().putString(KEY_API_KEY, obfuscate(apiKey)).apply()
    }

    fun getApiKey(context: Context): String {
        val raw = getPrefs(context).getString(KEY_API_KEY, "") ?: ""
        if (raw.isEmpty()) return ""
        return try { deobfuscate(raw) } catch (_: Exception) { "" }
    }

    fun hasApiKey(context: Context): Boolean = getApiKey(context).isNotBlank()

    fun clearApiKey(context: Context) {
        getPrefs(context).edit().remove(KEY_API_KEY).apply()
    }
}
