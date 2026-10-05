package com.cat.ai

/**
 * Ranks recent chat + saved memories into a short "best so far" summary.
 * Pure offline text — no network, no other apps, no browser history.
 */
object EvolveEngine {
    private val STOP = setOf(
        "where", "what", "when", "which", "your", "this", "that", "have", "with",
        "from", "about", "does", "like", "tell", "please", "want", "need", "into",
        "there", "here", "would", "could", "should", "just", "them", "they", "then",
        "the", "and", "for", "you", "are", "was", "not", "but", "can", "how", "why",
        "who", "its", "it's", "cat", "assistant", "offline", "online", "reply", "said"
    )

    data class EvolveResult(
        val summary: String,
        val statusLine: String
    )

    /**
     * Build a short Unsure-tagged memory note from recent chat and saved notes.
     * Returns null when there is nothing useful to evolve.
     */
    fun bestSoFar(
        recentChat: List<Pair<String, String>>,
        memories: List<String>,
        maxLen: Int = 200
    ): EvolveResult? {
        val chatSnips = recentChat.takeLast(10).map { (role, content) ->
            val who = if (role == "user") "You" else "C@T"
            "$who: ${content.replace("\n", " ").trim().take(80)}"
        }
        val memSnips = memories
            .filter { !it.startsWith("best so far:", ignoreCase = true) }
            .filter { !it.startsWith("online:", ignoreCase = true) }
            .take(8)

        if (chatSnips.isEmpty() && memSnips.isEmpty()) return null

        val ranked = rankTopics(chatSnips + memSnips).take(4)
        val focus = if (ranked.isEmpty()) {
            (memSnips.firstOrNull() ?: chatSnips.lastOrNull().orEmpty()).take(60)
        } else {
            ranked.joinToString(", ")
        }

        val memPart = memSnips.take(2).joinToString("; ") { it.take(50) }
        val chatPart = chatSnips.takeLast(2).joinToString("; ") { it.take(50) }

        val body = buildString {
            append("best so far: focus [$focus]")
            if (memPart.isNotBlank()) append(" · notes: $memPart")
            if (chatPart.isNotBlank()) append(" · chat: $chatPart")
        }.replace(Regex("\\s+"), " ").trim().take(maxLen)

        if (body.length < 20) return null
        return EvolveResult(
            summary = body,
            statusLine = "Evolving · ${ranked.firstOrNull() ?: "notes"}"
        )
    }

    /** One short follow-up question the assistant might ask next (offline). */
    fun followUpPrompt(
        userText: String,
        assistantReply: String,
        memories: List<String> = emptyList()
    ): String {
        val topic = keywords(userText).take(3).joinToString(" ")
            .ifBlank { keywords(assistantReply).take(2).joinToString(" ") }
            .ifBlank { "that" }
        val hasMemory = memories.any { note ->
            keywords(userText).any { note.lowercase().contains(it) }
        }
        return when {
            userText.trim().startsWith("/") -> "Want me to save anything else from that?"
            hasMemory -> "Should I refine the note about $topic?"
            userText.contains("?") -> "Want options for $topic, or a next step?"
            else -> "Want a short plan for $topic, or shall I remember it?"
        }.take(120)
    }

    fun rankTopics(snippets: List<String>): List<String> {
        val scores = linkedMapOf<String, Int>()
        snippets.forEach { snip ->
            keywords(snip).forEach { word ->
                scores[word] = (scores[word] ?: 0) + 1
            }
        }
        return scores.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .map { it.key }
            .filter { it.length >= 3 }
            .take(6)
    }

    private fun keywords(text: String): List<String> {
        return text.lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 3 && it !in STOP }
            .distinct()
    }
}
