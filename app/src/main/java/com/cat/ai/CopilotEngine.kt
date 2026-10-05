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
        val todoToAdd: String? = null,
        /** Short user fact for the evolving feed. Never a truth score. */
        val learnToSave: String? = null
    )

    fun prepare(raw: String): Prepared {
        val clean = filter.sanitize(raw).trim()
        return Prepared(clean, clean != raw.trim())
    }

    fun respond(
        userText: String,
        recentChat: List<Pair<String, String>>,
        memories: List<String>,
        todos: List<String> = emptyList(),
        memoryTags: List<String> = emptyList(),
        persona: AiPersona = AiPersona.OFFLINE_CAT
    ): OfflineResult {
        val clean = filter.sanitize(userText).trim()
        val result = dispatch(clean, recentChat, memories, todos, memoryTags, persona)
        val learned = result.learnToSave ?: learnCandidate(clean)
        if (learned.isNullOrBlank()) return result
        val queued = result.copy(learnToSave = learned.take(160))
        if (result.memoryToSave != null || result.reply.contains("evolving memory")) return queued
        return queued.copy(
            reply = result.reply +
                "\nC@T evolving memory saved a short note (Unsure until you tag it True, False, or Unsure)."
        )
    }

    /** Persona-flavoured offline answer without memory side effects (group solver). */
    fun respondAs(
        userText: String,
        recentChat: List<Pair<String, String>>,
        memories: List<String>,
        persona: AiPersona,
        memoryTags: List<String> = emptyList(),
        todos: List<String> = emptyList()
    ): OfflineResult {
        val clean = filter.sanitize(userText).trim()
        return dispatch(clean, recentChat, memories, todos, memoryTags, persona)
            .copy(memoryToSave = null, todoToAdd = null, learnToSave = null)
    }

    private fun dispatch(
        clean: String,
        recentChat: List<Pair<String, String>>,
        memories: List<String>,
        todos: List<String>,
        memoryTags: List<String>,
        persona: AiPersona = AiPersona.OFFLINE_CAT
    ): OfflineResult {
        if (clean.isEmpty()) {
            return OfflineResult("C@T here. Send a message and I'll work with it locally. Wi-Fi is not required.")
        }
        if (clean.startsWith("/")) {
            return slash(clean.drop(1).trim(), recentChat, memories, todos, memoryTags)
        }
        val lower = clean.lowercase()

        rememberFact(clean)?.let { fact ->
            return OfflineResult(
                reply = "Locked into C@T hard save: $fact. On the evolving memory feed as Unsure until you tag it.",
                memoryToSave = fact,
                learnToSave = fact
            )
        }

        if (isRecall(lower)) {
            return OfflineResult(recallReply(memories, memoryTags))
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
                styleReply(
                    persona,
                    "C@T offline, Australia/Perth, en-AU. Wi-Fi is not required. " +
                        "Try /help, /remember <fact>, /recall, or /tools."
                )
            )
        }
        return OfflineResult(
            reply = styleReply(persona, contextualReply(clean, recentChat, memories, memoryTags)),
            skipCloud = false
        )
    }

    private fun slash(
        body: String,
        recentChat: List<Pair<String, String>>,
        memories: List<String>,
        todos: List<String>,
        memoryTags: List<String>
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
                    OfflineResult(
                        "Locked into C@T hard save: $fact. On the evolving memory feed as Unsure until you tag it.",
                        memoryToSave = fact,
                        learnToSave = fact
                    )
                }
            }
            "recall", "notes", "memories" -> OfflineResult(recallReply(memories, memoryTags))
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

    /**
     * Pulls a short user fact for the evolving feed.
     * Remember, or a confidence phrase the user typed.
     * Does not score the fact as true or false.
     */
    private fun learnCandidate(clean: String): String? {
        if (clean.isEmpty()) return null
        rememberFact(clean)?.let { return it.take(160).ifBlank { null } }
        if (clean.startsWith("/")) {
            val body = clean.drop(1).trim()
            if (!body.lowercase().startsWith("remember")) return null
            val arg = if (' ' in body) body.substringAfter(' ').trim() else ""
            val fact = filter.sanitize(arg.replace(Regex("(?i)^that\\s+"), ""))
                .trim()
                .trimEnd('.')
            return fact.take(160).ifBlank { null }
        }
        val lower = clean.lowercase()
        if (isHelp(lower) || isSettings(lower) || isFold(lower) || isSummarize(lower) || isRecall(lower)) {
            return null
        }
        if (lower == "tools" || lower == "tool" || lower == "time" || lower == "clock") return null
        if (CONFIDENCE.none { phrase -> PHRASE(phrase).containsMatchIn(lower) }) return null
        val short = clean.replace(Regex("\\s+"), " ").trim().trimEnd('.').take(160)
        if (short.length < 12) return null
        return short
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

    private fun recallReply(memories: List<String>, tags: List<String>): String {
        if (memories.isEmpty()) {
            return "Local memory is empty. Say /remember <fact>. It stays on this phone in C@T hard save with no expiry."
        }
        val shown = memories.take(40)
        val lines = shown.mapIndexed { index, note ->
            val tag = tags.getOrNull(index)?.takeIf { it.isNotBlank() }
            if (tag == null) "- $note" else "- [${com.cat.data.TruthTag.normalize(tag)}] $note"
        }.joinToString("\n")
        val extra = memories.size - shown.size
        val more = if (extra > 0) "\n… and $extra more on the Memory tab." else ""
        return "C@T hard save (${memories.size}, kept on this phone, no expiry):\n$lines$more"
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
        memories: List<String>,
        tags: List<String>
    ): String {
        val keywords = clean.lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 3 && it !in STOP }
            .distinct()
        val noteHits = memories.mapIndexed { index, note ->
            val hay = note.lowercase()
            Triple(note, tags.getOrNull(index).orEmpty(), keywords.count { hay.contains(it) })
        }.filter { it.third > 0 }
            .sortedByDescending { it.third }
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
            parts.add(
                "Using saved memory:\n" + noteHits.joinToString("\n") { (note, tag, _) ->
                    if (tag.isBlank()) "- $note" else "- [${com.cat.data.TruthTag.normalize(tag)}] $note"
                }
            )
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


    data class TerminalOutcome(
        val reply: String,
        val memoryToSave: String? = null,
        val todoToAdd: String? = null,
        val skipCloud: Boolean = true,
        val clearVault: Boolean = false
    )

    /**
     * English Terminal reply. Mode personas (Offline / Cloud / Auto) speak as Analyst.
     * Named wheel personas keep their own voice. Every reply carries Room memories
     * and recent vault lines. This is not a shell.
     */
    fun respondForTerminal(
        userText: String,
        vaultLines: List<Pair<String, String>>,
        memories: List<String>,
        todos: List<String> = emptyList(),
        memoryTags: List<String> = emptyList(),
        persona: AiPersona = AiPersona.OFFLINE_CAT,
        versionName: String = "1.9",
        versionCode: Int = 14,
        online: Boolean = false,
        privateMode: Boolean = false
    ): TerminalOutcome {
        val clean = filter.sanitize(userText).trim()
        val voice = terminalVoice(persona)
        if (clean.isEmpty()) {
            return pack(voice, "C@T Terminal. Type help, or tap a command chip.", vaultLines, memories, memoryTags)
        }
        val lower = clean.lowercase()
        val body = if (clean.startsWith("/")) clean.drop(1).trim() else clean
        val cmd = body.substringBefore(' ').lowercase()
        val arg = if (' ' in body) body.substringAfter(' ').trim() else ""
        val slash = clean.startsWith("/")

        if (slash && cmd == "help" || lower == "help" || isHelp(lower)) {
            return pack(voice, TERMINAL_HELP, vaultLines, memories, memoryTags)
        }
        if (cmd == "clear" && (arg.isEmpty() || slash)) {
            return pack(
                voice,
                "Transcript cleared. English Terminal stays ready. Chat is untouched. Not a shell.",
                emptyList(),
                memories,
                memoryTags,
                clearVault = true
            )
        }
        if (cmd == "status" && (arg.isEmpty() || slash)) {
            val net = when {
                privateMode -> "private · offline (cloud blocked)"
                online -> "online"
                else -> "offline"
            }
            val status = """
                status: $net
                version: $versionName (build $versionCode)
                memories: ${memories.size}
                vault lines: ${vaultLines.size}
                persona: ${voice.label}
                private: ${if (privateMode) "on" else "off"}
                Terminal default voice is Analyst unless the Wheel names Coder, Coach, or Creative.
                English Terminal. Not a system shell.
            """.trimIndent()
            return pack(voice, status, vaultLines, memories, memoryTags)
        }
        if (cmd == "version" && (arg.isEmpty() || slash)) {
            return pack(
                voice,
                "C@T $versionName (build $versionCode). English Terminal, not a system shell.",
                vaultLines,
                memories,
                memoryTags
            )
        }
        if (cmd == "unlock" && (arg.isEmpty() || slash || arg.lowercase().startsWith("help"))) {
            return pack(voice, UNLOCK_HELP, vaultLines, memories, memoryTags)
        }

        val base = dispatch(clean, vaultLines, memories, todos, memoryTags, voice)
        val reply = attachTerminalContext(ensureVoice(voice, base.reply), vaultLines, memories, memoryTags)
        val spoken = if (privateMode && !base.skipCloud) {
            reply + "\nPrivate is on. This answer stayed on the phone. No cloud."
        } else {
            reply
        }
        return TerminalOutcome(
            reply = spoken,
            memoryToSave = base.memoryToSave,
            todoToAdd = base.todoToAdd,
            skipCloud = base.skipCloud || privateMode,
            clearVault = false
        )
    }

    fun terminalVoice(persona: AiPersona): AiPersona {
        return if (persona.isModePersona) AiPersona.ANALYST else persona
    }

    fun attachTerminalContext(
        reply: String,
        vaultLines: List<Pair<String, String>>,
        memories: List<String>,
        tags: List<String>
    ): String {
        if (reply.contains("\n— memories:")) return reply
        val memBlock = if (memories.isEmpty()) {
            "memories: none yet (say remember <fact>)"
        } else {
            val bits = memories.take(3).mapIndexed { index, note ->
                val tag = tags.getOrNull(index)
                    ?.takeIf { it.isNotBlank() }
                    ?.let { "[${com.cat.data.TruthTag.normalize(it)}] " }
                    .orEmpty()
                "$tag${note.replace("\n", " ").take(90)}"
            }.joinToString(" · ")
            "memories (${memories.size}): $bits"
        }
        val vaultBlock = if (vaultLines.isEmpty()) {
            "vault: no earlier lines"
        } else {
            val bits = vaultLines.takeLast(3).joinToString(" · ") { (role, content) ->
                val who = if (role == "user") "you" else "C@T"
                "$who: ${content.replace("\n", " ").take(70)}"
            }
            "vault (${vaultLines.size}): $bits"
        }
        return reply.trimEnd() + "\n— $memBlock\n— $vaultBlock"
    }

    private fun pack(
        voice: AiPersona,
        body: String,
        vaultLines: List<Pair<String, String>>,
        memories: List<String>,
        tags: List<String>,
        clearVault: Boolean = false
    ): TerminalOutcome {
        return TerminalOutcome(
            reply = attachTerminalContext(ensureVoice(voice, body), vaultLines, memories, tags),
            skipCloud = true,
            clearVault = clearVault
        )
    }

    private fun ensureVoice(voice: AiPersona, reply: String): String {
        val lenses = listOf("Analyst lens", "Coder lens", "Coach lens", "Creative lens")
        if (lenses.any { reply.startsWith(it) }) return reply
        return styleReply(voice, reply)
    }

    private fun styleReply(persona: AiPersona, body: String): String {
        if (persona == AiPersona.OFFLINE_CAT || persona.isModePersona) {
            return body
        }
        val prefix = when (persona) {
            AiPersona.ANALYST -> "Analyst lens — facts and trade-offs:"
            AiPersona.CODER -> "Coder lens — practical steps:"
            AiPersona.COACH -> "Coach lens — next actions:"
            AiPersona.CREATIVE -> "Creative lens — fresh angles:"
            else -> "${persona.label}:"
        }
        val tip = when (persona) {
            AiPersona.ANALYST -> "\nNext: list constraints, then pick one option."
            AiPersona.CODER -> "\nNext: one concrete step you can try now."
            AiPersona.COACH -> "\nYou have this — take the smallest useful step."
            AiPersona.CREATIVE -> "\nTwist: what if you flipped the usual approach?"
            else -> ""
        }
        return "$prefix\n$body$tip"
    }

    companion object {
        private val REMEMBER = Regex("(?i)^(?:please\\s+)?remember(?:\\s+that)?\\s+(.+)$")
        private val CONFIDENCE = listOf(
            "i know", "i knew", "i'm sure", "im sure", "i am sure",
            "definitely", "for sure", "i'm certain", "im certain", "i am certain",
            "always", "never", "i think", "i believe", "probably", "maybe",
            "certainly", "no doubt"
        )
        private fun PHRASE(phrase: String) = Regex("(?i)(?<![a-z])" + Regex.escape(phrase) + "(?![a-z])")
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
            Saved memory stays in C@T hard save (Room) on this phone with no expiry and shows live on screen.
            You set each note True, False, or Unsure. C@T does not decide that and is not a lie detector.
            Confidence words and /remember can add a short note to the evolving memory feed, tagged Unsure until you change it.
            Autopilot on the chat strip speaks replies and one follow-up, then waits.
            Evolve writes a short best-so-far note from this chat. Online evolve (optional) uses public Wikipedia only — never browser history.
            Offline answers name the tag on notes your words match.
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

        val TERMINAL_HELP = """
            C@T English Terminal. Australia/Perth, en-AU. Not a system shell.
            No packages, no path building, no other-app scan, no self-update.

            Commands (a slash is optional):
            - help — this guide
            - remember <fact> — hard-save a note on this phone
            - recall — list saved memories and your True / False / Unsure tags
            - time — Australia/Perth clock
            - status — online or offline, version, memory count, vault size, persona
            - version — build name and code
            - clear — wipe this Terminal transcript only (Chat stays)
            - unlock — how the PIN lock works
            - summarize — snapshot of this Terminal thread
            - tools — on-phone utilities
            - todo <item> — checklist
            - hash, b64, json, convert, pass — local text tools

            Typo repair fixes the command word (hlp, remeber, recell, staus, verson, unlok, cler).
            Every reply includes saved Room memories and recent encrypted vault lines.
            Terminal voice defaults to Analyst. Coder, Coach, or Creative apply when the Wheel names them.
            Cloud is optional in Settings. Offline still answers.
            The PIN lock is separate from Chat. This transcript is AES-GCM and never writes chat history.
            Private ON forces offline answers, blocks cloud, and blanks this app in the recents card.
        """.trimIndent()

        val UNLOCK_HELP = """
            Terminal PIN is 4 to 8 digits. C@T stores a salted hash, not the PIN.
            Unlock lasts until you tap Lock or this app process ends.
            Chat is a different tab and does not use this PIN.
            Too many wrong tries slow the gate down.
            This screen is English AI, not a shell.
        """.trimIndent()
    }
}
