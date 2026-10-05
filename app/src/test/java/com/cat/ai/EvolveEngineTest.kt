package com.cat.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EvolveEngineTest {
    @Test
    fun bestSoFarRanksChatAndMemory() {
        val chat = listOf(
            "user" to "plan the fold layout for meetings",
            "assistant" to "use two columns on the inner display"
        )
        val memories = listOf("meeting is in the lab", "tea is at 4")
        val result = EvolveEngine.bestSoFar(chat, memories)
        assertNotNull(result)
        assertTrue(result!!.summary.startsWith("best so far:"))
        assertTrue(result.statusLine.startsWith("Evolving"))
        assertTrue(result.summary.length <= 200)
    }

    @Test
    fun bestSoFarEmptyReturnsNull() {
        assertNull(EvolveEngine.bestSoFar(emptyList(), emptyList()))
    }

    @Test
    fun followUpIsShortAndWaitsForUser() {
        val q = EvolveEngine.followUpPrompt(
            "when is tea",
            "C@T offline. Using saved memory: tea is at 4",
            listOf("tea is at 4")
        )
        assertTrue(q.length <= 120)
        assertFalse(q.isBlank())
    }

    @Test
    fun bestSoFarKeepsUserSlang() {
        val chat = listOf(
            "user" to "yeah heaps keen for arvo tea",
            "assistant" to "noted"
        )
        val result = EvolveEngine.bestSoFar(chat, emptyList())
        assertNotNull(result)
        assertTrue(result!!.summary.contains("style:"))
        assertTrue(result.summary.contains("heaps"))
        assertTrue(result.summary.contains("arvo"))
        assertTrue(result.summary.length <= 200)
        assertTrue(result.statusLine.contains("style"))
    }

    @Test
    fun styleTokensOnlyReturnsWordsThatWereTyped() {
        val hits = EvolveEngine.styleTokens("yeah heaps keen for arvo tea")
        assertTrue(hits.contains("heaps"))
        assertTrue(hits.contains("arvo"))
        assertFalse(hits.contains("mate"))
    }

    @Test
    fun captureStyleIgnoresPlainSentences() {
        val style = EvolveEngine.captureStyle(listOf("the meeting is on tuesday"))
        assertTrue(style.isEmpty())
    }

    @Test
    fun rankTopicsSkipsStopWords() {
        val ranked = EvolveEngine.rankTopics(
            listOf("the meeting is in the lab", "meeting notes for lab")
        )
        assertTrue(ranked.contains("meeting") || ranked.contains("lab"))
        assertFalse(ranked.contains("the"))
        assertFalse(ranked.contains("for"))
    }
}
