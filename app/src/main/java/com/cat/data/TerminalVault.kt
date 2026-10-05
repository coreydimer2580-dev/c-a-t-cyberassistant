package com.cat.data

import android.content.Context
import android.util.Base64
import com.cat.security.VaultCrypto
import java.io.File
import java.security.SecureRandom

/**
 * AES-GCM transcript for the Terminal tab only.
 * The key is kept in encrypted preferences. ChatDao is never used.
 */
class TerminalVault(context: Context, private val prefs: CopilotPrefs) {
    private val file = File(context.filesDir, "terminal_transcript.vault")

    fun load(): List<TerminalLine> {
        if (!file.exists() || file.length() == 0L) return emptyList()
        return runCatching {
            val plain = VaultCrypto.decrypt(aesKey(), file.readBytes())
            TerminalTranscript.decode(plain.toString(Charsets.UTF_8))
        }.getOrDefault(emptyList())
    }

    fun save(lines: List<TerminalLine>) {
        val plain = TerminalTranscript.encode(lines).toByteArray(Charsets.UTF_8)
        file.writeBytes(VaultCrypto.encrypt(aesKey(), plain))
    }

    fun clear() {
        if (file.exists()) file.delete()
    }

    private fun aesKey(): ByteArray {
        val existing = prefs.terminalAesKey()
        if (existing.isNotBlank()) {
            val decoded = runCatching { Base64.decode(existing, Base64.NO_WRAP) }.getOrNull()
            if (decoded != null && decoded.size == 32) return decoded
        }
        val fresh = ByteArray(32).also { SecureRandom().nextBytes(it) }
        prefs.setTerminalAesKey(Base64.encodeToString(fresh, Base64.NO_WRAP))
        return fresh
    }
}
