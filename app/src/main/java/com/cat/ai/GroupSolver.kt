package com.cat.ai

/**
 * Merges several persona replies into one structured group verdict.
 * Pure helpers — safe to unit-test without Android.
 */
object GroupSolver {
    data class PersonaReply(val persona: AiPersona, val text: String)

    fun merge(question: String, replies: List<PersonaReply>): String {
        if (replies.isEmpty()) {
            return "Group solver had no replies for \"${question.take(80)}\"."
        }
        val body = replies.joinToString("\n\n") { item ->
            val clean = item.text.trim().ifBlank { "(empty)" }
            "**${item.persona.label}**\n$clean"
        }
        val verdict = buildVerdict(question, replies)
        return buildString {
            append("Group solver · ")
            append(replies.size)
            append(" voices\n\n")
            append(body)
            append("\n\n**Group verdict**\n")
            append(verdict)
        }
    }

    fun buildVerdict(question: String, replies: List<PersonaReply>): String {
        if (replies.isEmpty()) return "No consensus yet."
        if (replies.size == 1) {
            return "Single voice (${replies[0].persona.shortLabel}): ${trimLine(replies[0].text)}"
        }
        val snippets = replies.map { trimLine(it.text) }
        val shared = commonKeywords(snippets)
        val lead = replies.first()
        val others = replies.drop(1).joinToString("; ") { "${it.persona.shortLabel} adds a different angle" }
        val theme = if (shared.isEmpty()) {
            "each persona emphasised a different angle on \"${question.take(60)}\""
        } else {
            "shared focus: ${shared.take(4).joinToString(", ")}"
        }
        return "Blend ${replies.joinToString(" + ") { it.persona.shortLabel }}. " +
            "Lead with ${lead.persona.shortLabel}: ${trimLine(lead.text)} " +
            "($theme). $others. Pick the step that fits your constraints."
    }

    fun pickGroupPersonas(selected: AiPersona): List<AiPersona> {
        val roster = AiPersona.groupRoster.toMutableList()
        if (selected in roster) {
            // Keep selected first, fill to 3
            roster.remove(selected)
            return listOf(selected) + roster.take(2)
        }
        // Mode personas: use full Analyst/Coder/Coach roster
        return roster.take(3)
    }

    private fun trimLine(text: String): String {
        return text.replace("\n", " ").trim().take(160)
    }

    private fun commonKeywords(snippets: List<String>): List<String> {
        val stop = setOf(
            "the", "and", "for", "you", "are", "with", "this", "that", "from", "your",
            "have", "will", "can", "cat", "offline", "perth", "try", "use", "not"
        )
        val bags = snippets.map { text ->
            text.lowercase()
                .split(Regex("[^a-z0-9]+"))
                .filter { it.length >= 4 && it !in stop }
                .toSet()
        }
        if (bags.isEmpty()) return emptyList()
        var shared = bags.first()
        bags.drop(1).forEach { shared = shared.intersect(it) }
        return shared.toList().sorted()
    }
}
