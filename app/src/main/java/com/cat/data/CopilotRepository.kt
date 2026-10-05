package com.cat.data

import com.cat.ai.AiPersona
import com.cat.ai.CloudChatClient
import com.cat.ai.CopilotEngine
import com.cat.ai.CopilotMode
import com.cat.ai.GroupSolver
import com.cat.tools.AuPhone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class CopilotRepository(
    private val database: AppDatabase,
    private val prefs: CopilotPrefs,
    private val engine: CopilotEngine = CopilotEngine(),
    private val cloud: CloudChatClient = CloudChatClient(),
    private val networkAvailable: () -> Boolean = { true }
) {
    data class Turn(
        val messages: List<ChatMessage>,
        val notice: String?,
        val phone: AuPhone.PhoneCommand? = null
    )

    suspend fun history(): List<ChatMessage> = database.chatDao().getAll()

    suspend fun clear(): Turn {
        database.chatDao().clearAll()
        return Turn(emptyList(), null)
    }

    suspend fun send(raw: String): Turn {
        val phone = AuPhone.interpret(raw)
        val prepared = engine.prepare(raw)
        if (prepared.text.isBlank()) {
            return Turn(history(), "Nothing to send.")
        }
        if (phone != null) {
            return localPhoneTurn(prepared, phone)
        }
        database.chatDao().insert(
            ChatMessage(
                role = "user",
                content = prepared.text,
                createdAt = System.currentTimeMillis(),
                filtered = prepared.filtered,
                personaId = ""
            )
        )
        val all = database.chatDao().getAll()
        val prior = all.dropLast(1).map { it.role to it.content }
        val (lines, tags) = taggedMemory()
        val persona = prefs.persona
        val offline = engine.respond(prepared.text, prior, lines, todoLines(), tags, persona)
        // Slash/tool replies stay single-voice; group solver is for open questions.
        if (prefs.groupSolver && !offline.skipCloud) {
            return groupTurn(prepared.text, prior, lines, tags, persona, seed = offline)
        }
        persistLearned(offline)
        if (!offline.todoToAdd.isNullOrBlank()) {
            prefs.saveTodos(prefs.loadTodos() + (false to offline.todoToAdd))
        }
        return storeReply(offline, persona)
    }

    suspend fun regenerate(): Turn {
        val all = database.chatDao().getAll()
        val lastUserIndex = all.indexOfLast { it.role == "user" }
        if (lastUserIndex < 0) {
            return Turn(all, "No user message to regenerate.")
        }
        val lastUser = all[lastUserIndex]
        all.drop(lastUserIndex + 1).forEach { database.chatDao().deleteById(it.id) }
        val prior = all.take(lastUserIndex).map { it.role to it.content }
        val (lines, tags) = taggedMemory()
        val persona = prefs.persona
        val offline = engine.respond(lastUser.content, prior, lines, todoLines(), tags, persona)
            .copy(memoryToSave = null, todoToAdd = null, learnToSave = null)
        if (prefs.groupSolver && !offline.skipCloud) {
            return groupTurn(lastUser.content, prior, lines, tags, persona, persist = false, seed = offline)
        }
        return storeReply(offline, persona)
    }

    private suspend fun groupTurn(
        question: String,
        prior: List<Pair<String, String>>,
        lines: List<String>,
        tags: List<String>,
        selected: AiPersona,
        persist: Boolean = true,
        seed: CopilotEngine.OfflineResult? = null
    ): Turn = coroutineScope {
        val roster = GroupSolver.pickGroupPersonas(selected)
        val deferred = roster.map { persona ->
            async(Dispatchers.Default) {
                val result = engine.respondAs(question, prior, lines, persona, tags, todoLines())
                GroupSolver.PersonaReply(persona, result.textClean())
            }
        }
        val replies = deferred.map { it.await() }.toMutableList()

        // Optional cloud voice when mode allows and network/config work
        val wantCloud = prefs.mode == CopilotMode.CLOUD || prefs.mode == CopilotMode.AUTO
        var notice: String? = "Group solver · offline voices"
        if (wantCloud) {
            val cloudText = runCatching { pullCloud() }.getOrElse { err ->
                notice = "Group solver · cloud skipped (${err.message?.take(80) ?: "error"})"
                null
            }
            if (!cloudText.isNullOrBlank()) {
                replies.add(GroupSolver.PersonaReply(AiPersona.CLOUD_GPT, cloudText.trim()))
                notice = "Group solver · offline + cloud"
            } else if (notice == "Group solver · offline voices" && prefs.mode == CopilotMode.CLOUD) {
                notice = "Group solver · cloud unavailable, offline only"
            }
        }

        val merged = GroupSolver.merge(question, replies)
        if (persist) {
            val learn = seed ?: engine.respond(question, prior, lines, todoLines(), tags, selected)
            persistLearned(learn)
            if (!learn.todoToAdd.isNullOrBlank()) {
                prefs.saveTodos(prefs.loadTodos() + (false to learn.todoToAdd))
            }
        }
        val stored = engine.prepare(merged)
        database.chatDao().insert(
            ChatMessage(
                role = "assistant",
                content = stored.text.ifBlank { "Group solver had an empty reply." },
                createdAt = System.currentTimeMillis(),
                filtered = stored.filtered,
                personaId = "group"
            )
        )
        Turn(history(), notice)
    }

    private suspend fun localPhoneTurn(prepared: CopilotEngine.Prepared, phone: AuPhone.PhoneCommand): Turn {
        val now = System.currentTimeMillis()
        database.chatDao().insert(
            ChatMessage(
                role = "user",
                content = prepared.text,
                createdAt = now,
                filtered = prepared.filtered,
                personaId = ""
            )
        )
        val stored = engine.prepare(phone.reply)
        database.chatDao().insert(
            ChatMessage(
                role = "assistant",
                content = stored.text.ifBlank { phone.reply },
                createdAt = now + 1,
                filtered = stored.filtered,
                personaId = AiPersona.OFFLINE_CAT.id
            )
        )
        return Turn(history(), null, phone)
    }

    private suspend fun storeReply(offline: CopilotEngine.OfflineResult, persona: AiPersona): Turn {
        val (reply, notice) = resolve(offline, persona)
        val stored = engine.prepare(reply)
        database.chatDao().insert(
            ChatMessage(
                role = "assistant",
                content = stored.text.ifBlank { "C@T had an empty reply." },
                createdAt = System.currentTimeMillis(),
                filtered = stored.filtered,
                personaId = if (notice != null && prefs.mode != CopilotMode.OFFLINE) {
                    AiPersona.OFFLINE_CAT.id
                } else {
                    personaLabelForStore(persona, notice)
                }
            )
        )
        return Turn(history(), notice)
    }

    private fun personaLabelForStore(persona: AiPersona, notice: String?): String {
        if (notice != null && (notice.contains("Answered offline") || notice.contains("No network") || notice.contains("No cloud"))) {
            return if (persona.isModePersona) AiPersona.OFFLINE_CAT.id else persona.id
        }
        return when {
            persona.linkedMode == CopilotMode.CLOUD && notice == null -> AiPersona.CLOUD_GPT.id
            persona.linkedMode == CopilotMode.AUTO && notice == null && prefs.mode == CopilotMode.AUTO ->
                if (prefs.baseUrl.isNotBlank()) AiPersona.CLOUD_GPT.id else AiPersona.OFFLINE_CAT.id
            else -> persona.id
        }
    }

    private suspend fun resolve(offline: CopilotEngine.OfflineResult, persona: AiPersona): Pair<String, String?> {
        val mode = persona.linkedMode ?: prefs.mode
        if (offline.skipCloud || mode == CopilotMode.OFFLINE) {
            return offline.reply to null
        }
        val online = runCatching { networkAvailable() }.getOrDefault(false)
        if (!online) {
            return offline.reply to "No network. Answered offline."
        }
        if (prefs.baseUrl.isBlank()) {
            return offline.reply to "No cloud base URL. Answered offline."
        }
        return try {
            val raw = pullCloud(persona)
            if (raw.isBlank()) {
                offline.reply to "Cloud returned an empty reply. Answered offline."
            } else {
                raw to null
            }
        } catch (error: Exception) {
            val detail = error.message?.take(140) ?: error.javaClass.simpleName
            offline.reply to "Cloud failed ($detail). Answered offline."
        }
    }

    private suspend fun pullCloud(persona: AiPersona = prefs.persona): String {
        val messages = cloudMessages(persona)
        return withContext(Dispatchers.IO) {
            cloud.complete(prefs.baseUrl, prefs.apiKey, prefs.model, messages)
        }
    }

    private suspend fun cloudMessages(persona: AiPersona): List<Pair<String, String>> {
        val notes = database.memoryDao().getAll().take(12).joinToString("\n") {
            "[${TruthTag.normalize(it.truthTag)}] ${it.content}"
        }.ifBlank { "(none)" }
        val style = if (persona.isModePersona) {
            "Be concise, direct, and actionable."
        } else {
            "Persona: ${persona.label}. ${persona.styleHint}"
        }
        val system = """
            You are C@T, a privacy-first assistant on the user's Android phone in Australia.
            Default behaviour is offline. $style
            You only know the chat and memory notes in this request.
            Do not claim you scanned the phone, files, messages, or sensors.
            Do not claim you can send SMS or place calls yourself. The phone's own apps do that.
            Do not role-play as a lie detector or investigator.
            Truth tags are user-set True, False, or Unsure. Do not assign or change them.
            Home timezone is Australia/Perth. Locale is en-AU.
            Memory notes:
            $notes
        """.trimIndent()
        val history = database.chatDao().getAll().takeLast(16).map { it.role to it.content }
        return listOf("system" to system) + history
    }

    private suspend fun taggedMemory(): Pair<List<String>, List<String>> {
        val notes = database.memoryDao().getAll()
        return notes.map { it.content } to notes.map { TruthTag.normalize(it.truthTag) }
    }

    /** Hard-save and evolving-feed rows. Tags stay Unsure until the user changes them. */
    private suspend fun persistLearned(offline: CopilotEngine.OfflineResult) {
        val now = System.currentTimeMillis()
        if (!offline.memoryToSave.isNullOrBlank()) {
            database.memoryDao().insert(
                MemoryEntity(
                    content = offline.memoryToSave,
                    category = "copilot",
                    createdAt = now,
                    truthTag = TruthTag.UNSURE
                )
            )
        }
        if (!offline.learnToSave.isNullOrBlank()) {
            database.memoryDao().insert(
                MemoryEntity(
                    content = offline.learnToSave,
                    category = "learn",
                    createdAt = now,
                    truthTag = TruthTag.UNSURE
                )
            )
        }
    }

    private fun todoLines(): List<String> {
        return prefs.loadTodos().map { (done, text) ->
            if (done) "done: $text" else "open: $text"
        }
    }

    private fun CopilotEngine.OfflineResult.textClean(): String = reply.trim()
}
