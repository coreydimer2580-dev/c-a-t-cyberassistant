package com.cat.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.cat.ai.AiPersona
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

    var persona: AiPersona
        get() = AiPersona.fromId(prefs.getString(KEY_PERSONA, AiPersona.OFFLINE_CAT.id))
        set(value) {
            prefs.edit().putString(KEY_PERSONA, value.id).apply()
            value.linkedMode?.let { mode = it }
        }

    var groupSolver: Boolean
        get() = prefs.getBoolean(KEY_GROUP, false)
        set(value) {
            prefs.edit().putBoolean(KEY_GROUP, value).apply()
        }

    /** Speak replies + one auto follow-up chip (then wait for user). */
    var autopilot: Boolean
        get() = prefs.getBoolean(KEY_AUTOPILOT, false)
        set(value) {
            prefs.edit().putBoolean(KEY_AUTOPILOT, value).apply()
        }

    /**
     * When ON and network is up, do a public Wikipedia/DuckDuckGo lookup
     * for topic keywords and save an Unsure "online: …" note.
     * Never reads browser history, OneDrive, or other apps.
     */
    var onlineEvolve: Boolean
        get() = prefs.getBoolean(KEY_ONLINE_EVOLVE, false)
        set(value) {
            prefs.edit().putBoolean(KEY_ONLINE_EVOLVE, value).apply()
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

    fun loadTodos(): List<Pair<Boolean, String>> {
        val raw = prefs.getString(KEY_TODOS, "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.lineSequence().mapNotNull { line ->
            if (line.length < 2 || line[1] != '|') return@mapNotNull null
            val done = line[0] == '1'
            val text = line.substring(2).trim()
            if (text.isEmpty()) null else done to text
        }.take(100).toList()
    }

    fun saveTodos(items: List<Pair<Boolean, String>>) {
        val raw = items.take(100).joinToString("\n") { (done, text) ->
            val flag = if (done) '1' else '0'
            flag + "|" + text.replace("\n", " ").take(240)
        }
        prefs.edit().putString(KEY_TODOS, raw).apply()
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
        const val DEFAULT_MODEL = "llama3.2"
        const val EMULATOR_OLLAMA_URL = "http://10.0.2.2:11434/v1"
        private const val KEY_MODE = "mode"
        private const val KEY_PERSONA = "persona"
        private const val KEY_GROUP = "group_solver"
        private const val KEY_AUTOPILOT = "autopilot"
        private const val KEY_ONLINE_EVOLVE = "online_evolve"
        private const val KEY_URL = "base_url"
        private const val KEY_API = "api_key"
        private const val KEY_MODEL = "model"
        private const val KEY_TODOS = "todos"
    }
}
