package com.cat.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiPresetTest {
    @Test
    fun geminiPresetIsARealHttpsAddress() {
        val url = CopilotPrefs.GEMINI_BASE_URL
        assertTrue(url.startsWith("https://"))
        assertTrue(CopilotPrefs.looksLikeUrl(url))
        assertFalse(CopilotPrefs.isLocalOnlyUrl(url))
        assertTrue(CopilotPrefs.isGeminiUrl(url))
    }

    @Test
    fun loopbackStillRefused() {
        assertTrue(CopilotPrefs.isLocalOnlyUrl("http://10.0.2.2:11434/v1"))
        assertTrue(CopilotPrefs.isLocalOnlyUrl("http://localhost:8080"))
        assertFalse(CopilotPrefs.isGeminiUrl("http://10.0.2.2:11434/v1"))
    }

    @Test
    fun nonGeminiModelSwapsToGeminiAliasOnlyForGemini() {
        val g = CopilotPrefs.GEMINI_BASE_URL
        assertEquals(CopilotPrefs.GEMINI_MODEL, CopilotPrefs.modelFor(g, "llama3.2"))
        assertEquals(CopilotPrefs.GEMINI_MODEL, CopilotPrefs.modelFor(g, ""))
        assertEquals("gemini-2.5-pro", CopilotPrefs.modelFor(g, "gemini-2.5-pro"))
        assertEquals("llama3.2", CopilotPrefs.modelFor("https://api.groq.com/openai/v1", "llama3.2"))
    }
}
