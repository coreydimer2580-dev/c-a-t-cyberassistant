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

    @Test
    fun shortNoteImprovesOfflineAnswer() {
        val result = engine.respond("when is tea", emptyList(), listOf("tea is at 4"))
        assertTrue(result.reply.contains("tea is at 4"))
        assertTrue(result.reply.contains("saved memory"))
    }

    @Test
    fun recallSaysNotesDoNotExpire() {
        val result = engine.respond("/recall", emptyList(), listOf("tea is at 4"))
        assertTrue(result.reply.contains("no expiry"))
        assertTrue(result.reply.contains("tea is at 4"))
    }

    @Test
    fun taggedMemoryMatchNamesTheUserTag() {
        val result = engine.respond(
            "where is the meeting",
            emptyList(),
            listOf("meeting is in the lab"),
            memoryTags = listOf("True")
        )
        assertTrue(result.reply.contains("[True]"))
        assertTrue(result.reply.contains("lab"))
        assertFalse(result.reply.contains("lie detector"))
    }

    @Test
    fun rememberQueuesEvolvingNoteAsUnsureWordsOnly() {
        val result = engine.respond("remember that tea is at 4", emptyList(), emptyList())
        assertEquals("tea is at 4", result.memoryToSave)
        assertEquals("tea is at 4", result.learnToSave)
        assertTrue(result.reply.contains("Unsure"))
        assertFalse(result.reply.contains("lie detector"))
    }

    @Test
    fun confidenceKeywordQueuesLearnWithoutScoringTruth() {
        val result = engine.respond("I know the gate code is 9", emptyList(), emptyList())
        assertEquals("I know the gate code is 9", result.learnToSave)
        assertNull(result.memoryToSave)
        assertTrue(result.reply.contains("evolving memory"))
        assertFalse(result.reply.contains("lie detector"))
    }

    @Test
    fun plainChatDoesNotAutoLearn() {
        val result = engine.respond("where is the kettle", emptyList(), emptyList())
        assertNull(result.learnToSave)
        assertNull(result.memoryToSave)
    }

    @Test
    fun terminalHelpIsEnglishNotAShell() {
        val outcome = engine.respondForTerminal("help", emptyList(), emptyList(), versionName = "1.9", versionCode = 14)
        assertTrue(outcome.reply.contains("Not a system shell"))
        assertTrue(outcome.reply.contains("remember"))
        assertTrue(outcome.reply.contains("Analyst lens"))
        assertTrue(outcome.reply.contains("Private"))
        assertTrue(outcome.skipCloud)
        assertTrue(outcome.reply.contains("memories:"))
        assertTrue(outcome.reply.contains("vault:"))
    }

    @Test
    fun terminalReplyCarriesMemoryAndVault() {
        val outcome = engine.respondForTerminal(
            "where is tea",
            listOf("user" to "tea later", "assistant" to "noted"),
            listOf("tea is at 4"),
            memoryTags = listOf("True")
        )
        assertTrue(outcome.reply.contains("tea is at 4"))
        assertTrue(outcome.reply.contains("[True]"))
        assertTrue(outcome.reply.contains("vault"))
        assertTrue(outcome.reply.contains("Analyst lens"))
    }

    @Test
    fun terminalKeepsNamedPersona() {
        val outcome = engine.respondForTerminal("hello", emptyList(), emptyList(), persona = AiPersona.CODER)
        assertTrue(outcome.reply.contains("Coder lens"))
        assertFalse(outcome.reply.contains("Analyst lens"))
    }

    @Test
    fun terminalStatusAndPrivateOffline() {
        val status = engine.respondForTerminal(
            "status",
            listOf("user" to "hi"),
            listOf("a fact"),
            versionName = "1.9",
            versionCode = 14,
            online = true,
            privateMode = true
        )
        assertTrue(status.reply.contains("private"))
        assertTrue(status.reply.contains("1.9"))
        assertTrue(status.reply.contains("memories: 1"))
        assertTrue(status.skipCloud)
        val open = engine.respondForTerminal(
            "where is the kettle",
            emptyList(),
            emptyList(),
            privateMode = true
        )
        assertTrue(open.skipCloud)
        assertTrue(open.reply.contains("Private is on"))
    }

    @Test
    fun terminalRanksMatchingMemoryWithItsTag() {
        val outcome = engine.respondForTerminal(
            "where is tea",
            emptyList(),
            listOf("the kettle is blue", "tea is at 4"),
            memoryTags = listOf("False", "True")
        )
        assertTrue(outcome.reply.contains("[True] tea is at 4"))
        assertFalse(outcome.reply.contains("[True] the kettle"))
        assertFalse(outcome.reply.contains("[False] tea is at 4"))
    }

    @Test
    fun terminalClearAsksThenConfirms() {
        val ask = engine.respondForTerminal("/clear", listOf("user" to "secret"), emptyList())
        assertFalse(ask.clearVault)
        assertTrue(ask.reply.contains("/clear yes"))
        val outcome = engine.respondForTerminal("/clear yes", listOf("user" to "secret"), emptyList())
        assertTrue(outcome.clearVault)
        assertTrue(outcome.reply.contains("cleared"))
        assertFalse(outcome.reply.contains("secret"))
    }

    @Test
    fun terminalRanksSharedSlangAboveAnUnrelatedNote() {
        val outcome = engine.respondForTerminal(
            "yeah heaps keen",
            emptyList(),
            listOf("the kettle is blue", "arvo tea is heaps good"),
            memoryTags = listOf("Unsure", "Unsure")
        )
        val slang = outcome.reply.indexOf("heaps good")
        val kettle = outcome.reply.indexOf("kettle")
        assertTrue(slang >= 0)
        if (kettle >= 0) assertTrue(slang < kettle)
    }

    @Test
    fun terminalUsedMemoryHintRanksRelevantNote() {
        val outcome = engine.respondForTerminal(
            "where is tea",
            emptyList(),
            listOf("the kettle is blue", "tea is at 4", "rain tomorrow"),
            memoryTags = listOf("False", "True", "Unsure")
        )
        assertTrue(outcome.reply.contains("used "))
        assertTrue(outcome.reply.contains("memories"))
        val used = outcome.reply.substringAfter("used ").substringBefore(" memories")
        assertTrue(used.toInt() >= 1)
        val tea = outcome.reply.indexOf("tea is at 4")
        val kettle = outcome.reply.indexOf("kettle")
        assertTrue(tea >= 0)
        if (kettle >= 0) assertTrue(tea < kettle)
    }

}
