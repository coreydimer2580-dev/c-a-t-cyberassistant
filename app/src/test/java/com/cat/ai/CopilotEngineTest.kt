package com.cat.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CopilotEngineTest {
    private val engine = CopilotEngine()

    @Test
    fun greetingNamesCat() {
        val result = engine.respond("hello", emptyList(), emptyList())
        assertTrue(result.reply.contains("C@T"))
        assertNull(result.memoryToSave)
    }

    @Test
    fun helpListsOfflineCommands() {
        val result = engine.respond("what can you do", emptyList(), emptyList())
        assertTrue(result.reply.contains("remember"))
        assertTrue(result.reply.contains("recall"))
    }

    @Test
    fun rememberExtractsFact() {
        val result = engine.respond("remember that tea is at 4", emptyList(), emptyList())
        assertEquals("tea is at 4", result.memoryToSave)
        assertTrue(result.reply.contains("tea is at 4"))
    }

    @Test
    fun recallReadsMemory() {
        val result = engine.respond("what do you remember", emptyList(), listOf("tea is at 4"))
        assertTrue(result.reply.contains("tea is at 4"))
    }

    @Test
    fun summarizeUsesThread() {
        val chat = listOf("user" to "plan the fold layout", "assistant" to "use two columns")
        val result = engine.respond("summarize our chat", chat, emptyList())
        assertTrue(result.reply.contains("fold"))
        assertTrue(result.reply.contains("columns"))
    }

    @Test
    fun settingsAndFoldTips() {
        assertTrue(engine.respond("how do settings work", emptyList(), emptyList()).reply.contains("Offline"))
        assertTrue(engine.respond("fold screen tips", emptyList(), emptyList()).reply.contains("600"))
    }

    @Test
    fun keywordUsesMemoryContext() {
        val result = engine.respond(
            "where is the meeting",
            emptyList(),
            listOf("meeting is in the lab")
        )
        assertTrue(result.reply.contains("lab"))
    }

    @Test
    fun prepareStripsSecretsBeforeReply() {
        val prepared = engine.prepare("email ada@example.com and say hello")
        assertTrue(prepared.filtered)
        assertFalse(prepared.text.contains("ada@example.com"))
        val result = engine.respond(prepared.text, emptyList(), emptyList())
        assertFalse(result.reply.contains("ada@example.com"))
        assertFalse(result.memoryToSave.orEmpty().contains("ada@example.com"))
    }

    @Test
    fun slashToolsAndTimeStayLocal() {
        val tools = engine.respond("/tools", emptyList(), emptyList())
        assertTrue(tools.reply.contains("Australia/Perth"))
        assertTrue(tools.reply.contains("Checklist"))
        assertTrue(tools.skipCloud)
        val timed = CopilotEngine(clockMillis = { 0L }).respond("/time", emptyList(), emptyList())
        assertTrue(timed.reply.contains("Perth"))
        assertTrue(timed.reply.contains("8:00:00"))
        assertTrue(timed.skipCloud)
    }

    @Test
    fun slashRememberAndTodo() {
        val remembered = engine.respond("/remember that tea is at 4", emptyList(), emptyList())
        assertEquals("tea is at 4", remembered.memoryToSave)
        val todo = engine.respond("/todo buy milk", emptyList(), emptyList())
        assertEquals("buy milk", todo.todoToAdd)
        assertTrue(engine.respond("/help", emptyList(), emptyList()).reply.contains("/sms"))
    }
}
