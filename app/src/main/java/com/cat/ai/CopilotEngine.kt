package com.cat.ai

import com.cat.tools.LocalTools

/**
 * On-device assistant. It only sees text the caller passes in.
 * It does not read the phone, files, or accounts.
 * Offline answers do not use the network.
 */
class CopilotEngine(
    private val filter: SensitiveFilter = SensitiveFilter(),
    private val clockMillis: () -> Long = { System.currentTimeMillis() }
) {
    data class Prepared(val text: String, val filtered: Boolean)

    data class OfflineResult(
        val reply: String,
        val memoryToSave: String? = null,
        val skipCloud: Boolean = true,
        val todoToAdd: String? = null
    )

    fun prepare(raw: String): Prepared {
        val clean = filter.sanitize(raw).trim()
        return Prepared(clean, clean != raw.trim())
    }

    fun respond(
        userText: String,
        recentChat: List<Pair<String, String>>,
        memories: List<String>,
        todos: List<String> = emptyList()
    ): OfflineResult {
        val clean = filter.sanitize(userText).trim()
        if (clean.isEmpty()) {
            return OfflineResult("C@T here. Send a message and I'll work with it locally. Wi-Fi is not required.")
        }
        if (clean.startsWith("/")) {
            return slash(clean.drop(1).trim(), recentChat, memories, todos)
        }
        val lower = clean.lowercase()

        rememberFact(clean)?.let { fact ->
            return OfflineResult(
                reply = "Locked into local memory: $fact",
                memoryToSave = fact
            )
        }

        if (isRecall(lower)) {
            return OfflineResult(recallReply(memories))
        }
        if (lower == "tools" || lower == "tool") {
            return OfflineResult(TOOLS_REPLY)
        }
        if (isSummarize(lower)) {
            return OfflineResult(summarizeReply(recentChat, clean))
        }
        if (isSettings(lower)) {
            return OfflineResult(SETTINGS_REPLY)
        }
        if (isFold(lower)) {
            return OfflineResult(FOLD_REPLY)
        }
        if (isHelp(lower)) {
            return OfflineResult(HELP_REPLY)
        }
        if (lower == "time" || lower == "clock" || lower.contains("perth time")) {
            return OfflineResult(LocalTools.clockReport(clockMillis()))
        }
        if (isGreeting(lower) && clean.length < 48) {
            return OfflineResult(
                "C@T offline, Australia/Perth, en-AU. Wi-Fi is not required. " +
                    "Try /help, /remember <fact>, /recall, or /tools."
            )
        }
        return OfflineResult(
            reply = contextualReply(clean, recentChat, memories),
            skipCloud = false
        )
    }

    private fun slash(
        body: String,
        recentChat: List<Pair<String, String>>,
        memories: List<String>,
        todos: List<String>
    ): OfflineResult {
        if (body.isEmpty()) return OfflineResult(HELP_REPLY)
        val cmd = body.substringBefore(' ').lowercase()
        val arg = if (' ' in body) body.substringAfter(' ').trim() else ""
        return when (cmd) {
            "help" -> OfflineResult(HELP_REPLY)
            "remember" -> {
                val fact = filter.sanitize(arg.replace(Regex("(?i)^that\\s+"), ""))
                    .trim()
                    .trimEnd('.')
                if (fact.isEmpty()) {
                    OfflineResult("Say /remember tea is at 4")
                } else {
                    OfflineResult("Locked into local memory: $fact", memoryToSave = fact)
                }
            }
            "recall", "notes", "memories" -> OfflineResult(recallReply(memories))
            "tools", "tool" -> OfflineResult(TOOLS_REPLY)
            "summarize", "summary" -> OfflineResult(summarizeReply(recentChat, body))
            "settings", "setting" -> OfflineResult(SETTINGS_REPLY)
            "fold" -> OfflineResult(FOLD_REPLY)
            "time", "clock" -> OfflineResult(LocalTools.clockReport(clockMillis()))
            "todo" -> todoReply(arg, todos)
            "hash", "sha256", "sha" -> {
                if (arg.isEmpty()) OfflineResult("Use /hash some text")
                else OfflineResult("SHA-256:\n${LocalTools.sha256(arg)}")
            }
            "b64", "base64" -> {
                if (arg.isEmpty()) OfflineResult("Use /b64 some text")
                else OfflineResult(LocalTools.base64Encode(arg))
            }
            "b64d" -> {
                if (arg.isEmpty()) {
                    OfflineResult("Use /b64d <encoded>")
                } else {
                    val decoded = runCatching { LocalTools.base64Decode(arg) }.getOrElse {
                        return OfflineResult("Could not decode that Base64.")
                    }
                    OfflineResult(decoded)
                }
            }
            "json" -> {
                if (arg.isEmpty()) {
                    OfflineResult("Use /json {\"a\":1}")
                } else {
                    val pretty = runCatching { LocalTools.prettyJson(arg) }.getOrElse {
                        return OfflineResult("Could not pretty-print that JSON.")
                    }
                    OfflineResult(pretty)
                }
            }
            "convert" -> convertReply(arg)
            "pass", "passphrase" -> {
                val n = arg.toIntOrNull() ?: 16
                val generated = LocalTools.generatePassphrase(n)
                OfflineResult("Passphrase ($n): $generated")
            }
            "strength" -> {
                if (arg.isEmpty()) {
                    OfflineResult("Use /strength followed by the phrase to score.")
                } else {
                    val strength = LocalTools.passphraseStrength(arg)
                    OfflineResult("Strength ${strength.score}/4 ${strength.label} (length ${arg.length}).")
                }
            }
            "call", "dial", "sms", "text", "msg" -> OfflineResult(
                "Use /call 0412345678 or /sms 0412345678 your draft. " +
                    "C@T opens your phone's dialer or SMS app only. It does not call or text by itself."
            )
            else -> OfflineResult("Unknown command /$cmd. Try /help")
        }
    }

    private fun todoReply(arg: String, todos: List<String>): OfflineResult {
        if (arg.isBlank()) {
            if (todos.isEmpty()) {
                return OfflineResult("Checklist is empty. Add one with /todo buy milk, or use the Tools tab.")
            }
            return OfflineResult("Checklist:\n" + todos.joinToString("\n") { "- $it" })
        }
        return OfflineResult("Added to checklist: $arg", todoToAdd = arg)
    }

    private fun convertReply(arg: String): OfflineResult {
        val parts = arg.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (parts.size < 3) return OfflineResult("Use /convert 10 km mi")
        val value = parts[0].toDoubleOrNull() ?: return OfflineResult("Need a number first. Example: /convert 10 km mi")
        val result = LocalTools.convert(value, parts[1], parts[2])
            ?: return OfflineResult("Unknown units. Try km, m, mi, ft, kg, lb, C, F.")
        return OfflineResult(
            "${LocalTools.formatAmount(value)} ${parts[1]} = ${LocalTools.formatAmount(result)} ${parts[2]}"
        )
    }

    private fun rememberFact(clean: String): String? {
        val match = REMEMBER.matchEntire(clean) ?: return null
        val fact = filter.sanitize(match.groupValues[1]).trim().trimEnd('.')
        return fact.ifEmpty { null }
    }

    private fun isRecall(lower: String): Boolean {
        return lower == "recall" ||
            lower.startsWith("recall ") ||
            lower.contains("what do you remember") ||
            lower.contains("what do i remember") ||
            lower.contains("my notes") ||
            lower.contains("show memories") ||
            lower.contains("show memory")
    }

    private fun isSummarize(lower: String): Boolean {
        return lower.contains("summarize") || lower.contains("summary")
    }

    private fun isSettings(lower: String): Boolean {
        return lower.contains("setting") ||
            lower.contains("api key") ||
            lower.contains("cloud mode") ||
            lower.contains("offline mode")
    }

    private fun isFold(lower: String): Boolean {
        return lower.contains("fold") ||
            lower.contains("cover screen") ||
            lower.contains("inner display")
    }

    private fun isHelp(lower: String): Boolean {
        return lower == "help" ||
            lower.contains("what can you do") ||
            lower.contains("how do you work")
    }

    private fun isGreeting(lower: String): Boolean {
        return GREETING.containsMatchIn(lower)
    }

    private fun recallReply(memories: List<String>): String {
        if (memories.isEmpty()) {
            return "Local memory is empty. Say /remember <fact>. It stays on this phone in Room with no expiry."
        }
        val shown = memories.take(40)
        val lines = shown.joinToString("\n") { "- $it" }
        val extra = memories.size - shown.size
        val more = if (extra > 0) "\n… and $extra more on the Memory tab." else ""
        return "C@T local memory (${memories.size}, kept on this phone, no expiry):\n$lines$more"
    }

    private fun summarizeReply(recentChat: List<Pair<String, String>>, current: String): String {
        val lines = recentChat.takeLast(8).map { (role, content) ->
            val who = if (role == "user") "You" else "C@T"
            "$who: ${content.replace("\n", " ").take(140)}"
        }
        if (lines.isEmpty()) {
            return "Nothing earlier in this thread. You just said: ${current.take(140)}"
        }
        return "Thread snapshot:\n" + lines.joinToString("\n")
    }

    private fun contextualReply(
        clean: String,
        recentChat: List<Pair<String, String>>,
        memories: List<String>
    ): String {
        val keywords = clean.lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 3 && it !in STOP }
            .distinct()
        val noteHits = memories.map { note ->
            val hay = note.lowercase()
            note to keywords.count { hay.contains(it) }
        }.filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
            .take(3)
        val chatHits = recentChat.filter { (_, content) ->
            val hay = content.lowercase()
            keywords.any { hay.contains(it) }
        }.take(3)

        if (noteHits.isEmpty() && chatHits.isEmpty()) {
            return "C@T offline (Australia/Perth). No matching note for \"${clean.take(80)}\". " +
                "Try /remember <fact>, /recall, /tools, or /help. Wi-Fi is not required. " +
                "Cloud is optional in Settings and falls back here if it cannot connect."
        }
        val parts = mutableListOf("C@T offline (Australia/Perth).")
        if (noteHits.isNotEmpty()) {
            parts.add("Using saved memory:\n" + noteHits.joinToString("\n") { "- $it" })
        }
        if (chatHits.isNotEmpty()) {
            parts.add(
                "From this thread:\n" + chatHits.joinToString("\n") { (role, content) ->
                    val who = if (role == "user") "You" else "C@T"
                    "- $who: ${content.replace("\n", " ").take(120)}"
                }
            )
        }
        return parts.joinToString("\n")
    }

    companion object {
        private val REMEMBER = Regex("(?i)^(?:please\\s+)?remember(?:\\s+that)?\\s+(.+)$")
        private val GREETING = Regex("(?i)^(hi|hey|hello|yo|good morning|good evening|good night|g'day)\\b")
        private val STOP = setOf(
            "where", "what", "when", "which", "your", "this", "that", "have", "with",
            "from", "about", "does", "like", "tell", "please", "want", "need", "into",
            "there", "here", "would", "could", "should", "just", "them", "they", "then",
            "the", "and", "for", "you", "are", "was", "not", "but", "can", "how", "why",
            "who", "its", "it's"
        )
        private val HELP_REPLY = """
            C@T offline mode. Australia/Perth, en-AU. Wi-Fi is not required.
            Commands:
            - /help
            - /remember <fact>
            - /recall
            - /tools
            - /summarize
            - /settings
            - /fold
            - /time
            - /todo <item>
            - /hash <text>
            - /b64 <text>
            - /json <json>
            - /convert 10 km mi
            - /pass 16
            - /call 0412345678 opens your dialer
            - /sms 0412345678 your draft opens your SMS app
            You can also type remember, recall, summarize, or help without a slash.
            Saved memory stays in Room on this phone with no expiry and shows live on screen.
            Offline answers use those notes when your words match them.
            C@T does not send texts or place calls. Your phone's own apps do that if you confirm.
            There is no separate message network and no paid API. Cloud is optional and falls back offline.
        """.trimIndent()
        private val SETTINGS_REPLY = """
            Offline is the default and stays on this device. It does not wait for Wi-Fi.
            Cloud and Auto can use a network when one is available, then fall back offline.
            Point the base URL at a free local Ollama server if you want that. No paid API is required.
            Emulator host preset is 10.0.2.2.
            The API key is stored in encrypted preferences on the phone. C@T never reads other apps.
        """.trimIndent()
        private val FOLD_REPLY = """
            Cover screen (under 600dp) is one column. The inner display uses multiple columns.
            Launch opens this chat. Tabs scroll if the cover is tight.
            Same APK, no phone scan, on both screens.
        """.trimIndent()
        private val TOOLS_REPLY = """
            Tools tab, all on this phone, no network:
            - Route log (typed lines only, Australia/Perth timestamps)
            - Notes
            - Clipboard scrubber
            - Passphrase strength
            - Unit converter
            - World clock (Australia/Perth first, en-AU)
            - Passphrase generator
            - Base64
            - JSON pretty
            - SHA-256
            - Timer
            - Checklist
            - Call or text via your own dialer and SMS app
            Chat: /time /hash /b64 /json /convert /pass /todo /call /sms
            C@T does not run a carrier-free message network.
        """.trimIndent()
    }
}
