package com.cat.data

import com.cat.ai.CloudChatClient
import com.cat.ai.CopilotEngine
import com.cat.ai.CopilotMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CopilotRepository(
    private val database: AppDatabase,
    private val prefs: CopilotPrefs,
    private val engine: CopilotEngine = CopilotEngine(),
    private val cloud: CloudChatClient = CloudChatClient()
) {
    data class Turn(val messages: List<ChatMessage>, val notice: String?)

    suspend fun history(): List<ChatMessage> = database.chatDao().getAll()

    suspend fun clear(): Turn {
        database.chatDao().clearAll()
        return Turn(emptyList(), null)
    }

    suspend fun send(raw: String): Turn {
        val prepared = engine.prepare(raw)
        if (prepared.text.isBlank()) {
            return Turn(history(), "Nothing to send.")
        }
        database.chatDao().insert(
            ChatMessage(
                role = "user",
                content = prepared.text,
                createdAt = System.currentTimeMillis(),
                filtered = prepared.filtered
            )
        )
        val all = database.chatDao().getAll()
        val prior = all.dropLast(1).map { it.role to it.content }
        val offline = engine.respond(prepared.text, prior, memoryLines())
        if (!offline.memoryToSave.isNullOrBlank()) {
            database.memoryDao().insert(
                MemoryEntity(
                    content = offline.memoryToSave,
                    category = "copilot",
                    createdAt = System.currentTimeMillis()
                )
            )
        }
        return storeReply(offline)
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
        val offline = engine.respond(lastUser.content, prior, memoryLines())
            .copy(memoryToSave = null)
        return storeReply(offline)
    }

    private suspend fun storeReply(offline: CopilotEngine.OfflineResult): Turn {
        val (reply, notice) = resolve(offline)
        val stored = engine.prepare(reply)
        database.chatDao().insert(
            ChatMessage(
                role = "assistant",
                content = stored.text.ifBlank { "C@T had an empty reply." },
                createdAt = System.currentTimeMillis(),
                filtered = stored.filtered
            )
        )
        return Turn(history(), notice)
    }

    private suspend fun resolve(offline: CopilotEngine.OfflineResult): Pair<String, String?> {
        if (prefs.mode == CopilotMode.OFFLINE) {
            return offline.reply to null
        }
        if (prefs.baseUrl.isBlank()) {
            return offline.reply to "No cloud base URL. Answered offline."
        }
        return try {
            val messages = cloudMessages()
            val raw = withContext(Dispatchers.IO) {
                cloud.complete(prefs.baseUrl, prefs.apiKey, prefs.model, messages)
            }
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

    private suspend fun cloudMessages(): List<Pair<String, String>> {
        val notes = memoryLines().take(12).joinToString("\n").ifBlank { "(none)" }
        val system = """
            You are C@T, a privacy-first cyber assistant on the user's Android phone.
            Be concise, direct, and actionable.
            You only know the chat and memory notes in this request.
            Do not claim you scanned the phone, files, messages, or sensors.
            Do not role-play as a lie detector or investigator.
            Memory notes:
            $notes
        """.trimIndent()
        val history = database.chatDao().getAll().takeLast(16).map { it.role to it.content }
        return listOf("system" to system) + history
    }

    private suspend fun memoryLines(): List<String> {
        return database.memoryDao().getAll().map { it.content }
    }
}
