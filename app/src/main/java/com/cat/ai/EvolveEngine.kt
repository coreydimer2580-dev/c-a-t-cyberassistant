package com.cat.ai

/**
 * Ranks recent chat + saved memories into a short "best so far" summary.
 * Also keeps a few phrases the user actually typed (slang and style).
 * Pure offline text — no network, no other apps, no browser history.
 * Does not update the app.
 */
object EvolveEngine {
    private val STOP = setOf(
        "where", "what", "when", "which", "your", "this", "that", "have", "with",
        "from", "about", "does", "like", "tell", "please", "want", "need", "into",
        "there", "here", "would", "could", "should", "just", "them", "they", "then",
        "the", "and", "for", "you", "are", "was", "not", "but", "can", "how", "why",
        "who", "its", "it's", "cat", "assistant", "offline", "online", "reply", "said"
    )

    /** Informal words we only keep when the user typed them. */
    private val STYLE_WORDS = listOf(
        "yeah", "nah", "yep", "gonna", "wanna", "gotta", "kinda", "sorta",
        "dunno", "prolly", "tbh", "ngl", "idk", "imo", "lol", "lmao", "bruh",
        "mate", "heaps", "arvo", "reckon", "keen", "stoked", "ripper", "grouse",
        "defs", "legit", "lowkey", "highkey", "vibe", "vibes", "sus",
        "dope", "innit", "yall", "omg", "wtf", "asap", "fyi", "btw", "smh",
        "ong", "rizz", "slay", "bussin", "sheesh", "deadass", "whatevs",
        "bestie", "cheers", "periodt", "yeet", "brekkie", "servo", "footy",
        "barbie", "sunnies", "mozzie", "esky", "crikey", "bloke"
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
        val userLines = recentChat.filter { (role, _) ->
            role.equals("user", ignoreCase = true) || role.equals("you", ignoreCase = true)
        }.map { it.second }
        val style = captureStyle(userLines)
        val stylePart = if (style.isEmpty()) "" else " · style: ${style.joinToString(", ")}"
        val budget = (maxLen - stylePart.length).coerceAtLeast(48)

        val body = buildString {
            append("best so far: focus [$focus]")
            if (memPart.isNotBlank()) append(" · notes: $memPart")
            if (chatPart.isNotBlank()) append(" · chat: $chatPart")
        }.replace(Regex("\\s+"), " ").trim().take(budget) + stylePart

        if (body.length < 20) return null
        val lead = if (style.isNotEmpty()) "style" else ranked.firstOrNull() ?: "notes"
        return EvolveResult(
            summary = body.take(maxLen),
            statusLine = "Evolving · $lead"
        )
    }

    /** Informal words actually present in [text]. Does not invent slang. */
    fun styleTokens(text: String): Set<String> {
        val lower = text.lowercase()
        return STYLE_WORDS.filter { word ->
            Regex("\\b${Regex.escape(word)}\\b").containsMatchIn(lower)
        }.toSet()
    }

    /**
     * Phrases the user actually typed. Informal words plus one short quote.
     * Does not invent slang and does not read anything outside these lines.
     */
    fun captureStyle(userLines: List<String>): List<String> {
        val lines = userLines
            .map { it.replace("\n", " ").trim() }
            .filter { it.length >= 2 && !it.startsWith("/") }
        if (lines.isEmpty()) return emptyList()
        val hits = linkedSetOf<String>()
        lines.forEach { line ->
            val lower = line.lowercase()
            STYLE_WORDS.forEach { word ->
                if (Regex("\\b${Regex.escape(word)}\\b").containsMatchIn(lower)) {
                    hits += word
                }
            }
        }
        val quote = lines.firstOrNull { line ->
            val lower = line.lowercase()
            STYLE_WORDS.any { Regex("\\b${Regex.escape(it)}\\b").containsMatchIn(lower) }
        }?.replace(Regex("\\s+"), " ")?.trim()?.take(36)
        if (!quote.isNullOrBlank() && quote.length >= 4) {
            hits += "\"$quote\""
        }
        return hits.take(4).toList()
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
