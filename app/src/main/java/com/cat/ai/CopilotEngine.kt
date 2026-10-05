package com.cat.ai

/**
 * On-device assistant. It only sees text the caller passes in.
 * It does not read the phone, files, or accounts.
 */
class CopilotEngine(
    private val filter: SensitiveFilter = SensitiveFilter()
) {
    data class Prepared(val text: String, val filtered: Boolean)

    data class OfflineResult(
        val reply: String,
        val memoryToSave: String? = null
    )

    fun prepare(raw: String): Prepared {
        val clean = filter.sanitize(raw).trim()
        return Prepared(clean, clean != raw.trim())
    }

    fun respond(
        userText: String,
        recentChat: List<Pair<String, String>>,
        memories: List<String>
    ): OfflineResult {
        val clean = filter.sanitize(userText).trim()
        if (clean.isEmpty()) {
            return OfflineResult("C@T here. Send a message and I'll work with it locally.")
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
        if (isGreeting(lower) && clean.length < 48) {
            return OfflineResult(
                "C@T online, offline brain. Ask for help, say remember <fact>, or recall your notes."
            )
        }
        return OfflineResult(contextualReply(clean, recentChat, memories))
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
            return "Local memory is empty. Say remember <fact> and I'll keep it in this app only."
        }
        val lines = memories.take(8).joinToString("\n") { "- $it" }
        return "C@T local memory:\n$lines"
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
            .filter { it.length >= 4 && it !in STOP }
            .distinct()
        val noteHits = memories.filter { note ->
            val hay = note.lowercase()
            keywords.any { hay.contains(it) }
        }.take(3)
        val chatHits = recentChat.filter { (_, content) ->
            val hay = content.lowercase()
            keywords.any { hay.contains(it) }
        }.take(3)

        if (noteHits.isEmpty() && chatHits.isEmpty()) {
            return "C@T offline. No matching note for \"${clean.take(80)}\". " +
                "Try remember <fact>, recall, summarize, or help. Cloud is optional in Settings."
        }
        val parts = mutableListOf("C@T offline.")
        if (noteHits.isNotEmpty()) {
            parts.add("Matched notes:\n" + noteHits.joinToString("\n") { "- $it" })
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
        private val GREETING = Regex("(?i)^(hi|hey|hello|yo|good morning|good evening|good night)\\b")
        private val STOP = setOf(
            "where", "what", "when", "which", "your", "this", "that", "have", "with",
            "from", "about", "does", "like", "tell", "please", "want", "need", "into",
            "there", "here", "would", "could", "should", "just", "them", "they", "then"
        )
        private val HELP_REPLY = """
            C@T can do this on the phone, no scan:
            - remember <fact> saves a local note
            - recall or my notes reads them back
            - summarize recaps this thread
            - settings explains Offline, Cloud, and Auto
            - fold explains the cover vs inner layout
            Offline is the default. Cloud only runs if you add a URL and key.
        """.trimIndent()
        private val SETTINGS_REPLY = """
            Offline is the default and stays on this device.
            Cloud sends the filtered thread to an OpenAI-compatible URL you set.
            Auto tries Cloud, then falls back to Offline.
            The API key is stored in encrypted preferences on the phone. C@T never reads other apps.
        """.trimIndent()
        private val FOLD_REPLY = """
            Cover screen (under 600dp) is one column. The inner display uses multiple columns.
            Launch opens this chat. Tabs scroll if the cover is tight.
            Same APK, no phone scan, on both screens.
        """.trimIndent()
    }
}
