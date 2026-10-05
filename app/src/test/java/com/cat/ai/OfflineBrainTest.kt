package com.cat.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineBrainTest {
    private fun a(q: String, todos: List<String> = emptyList()) = OfflineBrain.answer(q, emptyList(), todos, 0L)

    @Test
    fun mathIsEvaluatedSafely() {
        assertTrue(a("what is 12 * 7")!!.text.endsWith("= 84"))
        assertTrue(a("(2+3)^2")!!.text.endsWith("= 25"))
        assertTrue(a("12 times 3 plus 4")!!.text.endsWith("= 40"))
        assertTrue(a("15% of 80")!!.text.contains("= 12"))
        assertTrue(a("gst on 110")!!.text.contains("\$10"))
    }

    @Test
    fun conversionsUseWords() {
        assertTrue(a("10 km to miles")!!.text.contains("6.2137"))
        assertTrue(a("100 celsius in fahrenheit")!!.text.contains("212"))
    }

    @Test
    fun dateUsesPerth() {
        assertTrue(a("what's the date")!!.text.contains("1 January 1970"))
    }

    @Test
    fun intentsGiveStructuredAnswers() {
        assertEquals("plan", a("plan my day", listOf("open: gym", "open: email boss"))!!.intent)
        assertTrue(a("plan my day", listOf("open: gym"))!!.text.contains("gym"))
        assertEquals("decide", a("should i rent or buy")!!.intent)
        assertTrue(a("should i rent or buy")!!.text.contains("rent"))
        assertTrue(a("give me options for dinner")!!.text.contains("Stir-fry"))
        assertTrue(a("what is a vpn")!!.text.contains("encrypts"))
        assertTrue(a("how do i save money")!!.text.contains("50/30/20"))
        assertTrue(a("help me write an email to sam about the invoice")!!.text.contains("Hi Sam"))
        assertTrue(a("i'm so stressed")!!.text.contains("13 11 14"))
        assertEquals("compare", a("iphone vs pixel")!!.intent)
    }

    @Test
    fun followUpReusesLastTopic() {
        val chat = listOf("user" to "how do i sleep better", "assistant" to "…")
        val more = OfflineBrain.answer("more", chat, emptyList(), 0L)
        assertNotNull(more)
        assertTrue(more!!.text.contains("Going further"))
    }

    @Test
    fun plainStatementsFallThrough() {
        assertNull(a("my cat is called Max"))
        assertFalse(a("what is 2 + 2")!!.text.contains("offline knowledge"))
    }
}
