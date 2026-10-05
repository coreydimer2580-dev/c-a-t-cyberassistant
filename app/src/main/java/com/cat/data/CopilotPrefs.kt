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
        migrateEmulatorUrl()
    }

    /**
     * v1.14: an emulator / loopback base URL can never work on a real phone.
     * Clear it once and drop back to Offline so nothing tries to connect.
     */
    private fun migrateEmulatorUrl() {
        val stored = prefs.getString(KEY_URL, "").orEmpty()
        if (stored.isNotBlank() && isLocalOnlyUrl(stored)) {
            prefs.edit()
                .putString(KEY_URL, "")
                .putString(KEY_MODE, CopilotMode.OFFLINE.storage)
                .apply()
        }
    }

    /** True only for a real, non-emulator https/http URL. Offline is used otherwise. */
    val cloudReady: Boolean
        get() {
            val url = baseUrl
            return url.isNotBlank() && !isLocalOnlyUrl(url) && looksLikeUrl(url)
        }

    /** Group solver may add a cloud voice only with a real URL *and* a key. */
    val groupCloudReady: Boolean
        get() = cloudReady && apiKey.isNotBlank()

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

    /**
     * Speak replies + one auto follow-up chip (then wait for user).
     * v1.17: defaults ON when the key was never set (first launch / unset).
     * Users who already turned it off keep false.
     */
    var autopilot: Boolean
        get() = prefs.getBoolean(KEY_AUTOPILOT, true)
        set(value) {
            prefs.edit().putBoolean(KEY_AUTOPILOT, value).apply()
        }

    /**
     * When ON and network is up, do a public Wikipedia/DuckDuckGo lookup
     * for topic keywords and save an Unsure "online: …" note.
     * Never reads browser history, OneDrive, or other apps.
     */
    /** v1.16: Autopilot speech volume, 0.0–1.0. Chat only. */
    var autopilotVolume: Float
        get() = prefs.getFloat(KEY_AUTOPILOT_VOLUME, 0.8f).coerceIn(0f, 1f)
        set(value) {
            prefs.edit().putFloat(KEY_AUTOPILOT_VOLUME, value.coerceIn(0f, 1f)).apply()
        }

    /** v1.16: picked neon accent id (see NeonAccent). */
    var accentId: String
        get() = prefs.getString(KEY_ACCENT, "cyan").orEmpty().ifBlank { "cyan" }
        set(value) {
            prefs.edit().putString(KEY_ACCENT, value).apply()
        }

    /** v1.16: when the user last exported memory as PDF (for the text-only backup reminder). */
    var lastMemoryExport: Long
        get() = prefs.getLong(KEY_LAST_EXPORT, 0L)
        set(value) {
            prefs.edit().putLong(KEY_LAST_EXPORT, value).apply()
        }

    var onlineEvolve: Boolean
        get() = prefs.getBoolean(KEY_ONLINE_EVOLVE, false)
        set(value) {
            prefs.edit().putBoolean(KEY_ONLINE_EVOLVE, value).apply()
        }

    var baseUrl: String
        get() = prefs.getString(KEY_URL, "").orEmpty()
        set(value) {
            val clean = value.trim()
            prefs.edit().putString(KEY_URL, if (isLocalOnlyUrl(clean)) "" else clean).apply()
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


    fun terminalPinSalt(): String = prefs.getString(KEY_TERM_SALT, "").orEmpty()

    fun terminalPinHash(): String = prefs.getString(KEY_TERM_HASH, "").orEmpty()

    fun setTerminalPin(saltHex: String, hashHex: String) {
        prefs.edit().putString(KEY_TERM_SALT, saltHex).putString(KEY_TERM_HASH, hashHex).apply()
    }

    fun terminalFails(): Int = prefs.getInt(KEY_TERM_FAILS, 0)

    fun setTerminalFails(count: Int) {
        prefs.edit().putInt(KEY_TERM_FAILS, count).apply()
    }

    fun terminalCooldownUntil(): Long = prefs.getLong(KEY_TERM_COOLDOWN, 0L)

    fun setTerminalCooldownUntil(epochMs: Long) {
        prefs.edit().putLong(KEY_TERM_COOLDOWN, epochMs).apply()
    }

    fun terminalAesKey(): String = prefs.getString(KEY_TERM_AES, "").orEmpty()

    fun setTerminalAesKey(base64: String) {
        prefs.edit().putString(KEY_TERM_AES, base64).apply()
    }

    /** Terminal only. When true, replies stay on device and the recents card is blanked. */
    var terminalPrivate: Boolean
        get() = prefs.getBoolean(KEY_TERM_PRIVATE, false)
        set(value) {
            prefs.edit().putBoolean(KEY_TERM_PRIVATE, value).apply()
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
        /** Shown as a hint only. Never saved by default. */
        const val URL_HINT = "https://api.groq.com/openai/v1"

        /**
         * v1.18 Gemini key-only preset. Google's official OpenAI-compatible endpoint
         * (CloudChatClient appends /chat/completions and sends "Authorization: Bearer <key>").
         * No key ships with the app — the user pastes their own from Google AI Studio.
         */
        const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/openai"
        /** Auto-updating Gemini Flash alias, so the preset keeps working as models change. */
        const val GEMINI_MODEL = "gemini-flash-latest"

        fun isGeminiUrl(url: String): Boolean =
            url.trim().lowercase().contains("generativelanguage.googleapis.com")

        /** Gemini endpoint with a non-Gemini model (e.g. the llama3.2 default) -> use the Gemini alias. */
        fun modelFor(url: String, model: String): String {
            val clean = model.trim()
            return if (isGeminiUrl(url) && !clean.lowercase().removePrefix("models/").startsWith("gemini")) {
                GEMINI_MODEL
            } else {
                clean
            }
        }

        private val LOCAL_HOSTS = listOf(
            "10.0.2.2", "10.0.3.2", "localhost", "127.0.0.1", "0.0.0.0", "::1", "[::1]"
        )

        /** Emulator-only or loopback hosts. These never reach anything from a real phone. */
        fun isLocalOnlyUrl(url: String): Boolean {
            val lower = url.trim().lowercase()
            if (lower.isBlank()) return false
            val host = lower.substringAfter("://", lower)
                .substringBefore('/')
                .substringBeforeLast(':')
                .removePrefix("[").removeSuffix("]")
            return LOCAL_HOSTS.any { it.removePrefix("[").removeSuffix("]") == host } ||
                host.startsWith("127.") || lower.contains("10.0.2.2")
        }

        fun looksLikeUrl(url: String): Boolean {
            val lower = url.trim().lowercase()
            return (lower.startsWith("https://") || lower.startsWith("http://")) &&
                lower.substringAfter("://").substringBefore('/').contains('.')
        }
        private const val KEY_MODE = "mode"
        private const val KEY_PERSONA = "persona"
        private const val KEY_GROUP = "group_solver"
        private const val KEY_AUTOPILOT = "autopilot"
        private const val KEY_ONLINE_EVOLVE = "online_evolve"
        private const val KEY_AUTOPILOT_VOLUME = "autopilot_volume"
        private const val KEY_ACCENT = "neon_accent"
        private const val KEY_LAST_EXPORT = "last_memory_export"
        private const val KEY_URL = "base_url"
        private const val KEY_API = "api_key"
        private const val KEY_MODEL = "model"
        private const val KEY_TODOS = "todos"
        private const val KEY_TERM_SALT = "term_pin_salt"
        private const val KEY_TERM_HASH = "term_pin_hash"
        private const val KEY_TERM_FAILS = "term_fails"
        private const val KEY_TERM_COOLDOWN = "term_cooldown"
        private const val KEY_TERM_AES = "term_aes_key"
        private const val KEY_TERM_PRIVATE = "term_private"
    }
}
