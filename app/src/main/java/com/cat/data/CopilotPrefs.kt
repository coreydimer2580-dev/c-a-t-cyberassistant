package com.cat.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.cat.ai.CopilotMode

class CopilotPrefs(context: Context) {
    val encrypted: Boolean
    private val prefs: SharedPreferences

    init {
        val secured = runCatching { openEncrypted(context) }.getOrNull()
        if (secured != null) {
            prefs = secured
            encrypted = true
        } else {
            prefs = context.getSharedPreferences("cat_prefs_plain", Context.MODE_PRIVATE)
            encrypted = false
        }
    }

    var mode: CopilotMode
        get() = CopilotMode.fromStorage(prefs.getString(KEY_MODE, CopilotMode.OFFLINE.storage))
        set(value) {
            prefs.edit().putString(KEY_MODE, value.storage).apply()
        }

    var baseUrl: String
        get() = prefs.getString(KEY_URL, "").orEmpty()
        set(value) {
            prefs.edit().putString(KEY_URL, value.trim()).apply()
        }

    var apiKey: String
        get() = prefs.getString(KEY_API, "").orEmpty()
        set(value) {
            prefs.edit().putString(KEY_API, value.trim()).apply()
        }

    var model: String
        get() = prefs.getString(KEY_MODEL, DEFAULT_MODEL).orEmpty().ifBlank { DEFAULT_MODEL }
        set(value) {
            prefs.edit().putString(KEY_MODEL, value.trim().ifBlank { DEFAULT_MODEL }).apply()
        }

    private fun openEncrypted(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            "cat_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    companion object {
        const val DEFAULT_MODEL = "gpt-4o-mini"
        private const val KEY_MODE = "mode"
        private const val KEY_URL = "base_url"
        private const val KEY_API = "api_key"
        private const val KEY_MODEL = "model"
    }
}
