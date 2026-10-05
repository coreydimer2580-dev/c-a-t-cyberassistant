package com.cat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductGuideTest {
    @Test
    fun pathAndSurfacesStayOneStory() {
        assertEquals("YOU → SoftCorrect → MEMORY → REPLY → Evolve", ProductGuide.PATH)
        val names = ProductGuide.surfaces().map { it.name }
        assertEquals(listOf("Terminal", "Chat", "Online", "Wheel", "Memory", "Settings"), names)
        assertTrue(ProductGuide.SPEECH.contains("Chat only"))
        assertTrue(ProductGuide.SAFE.contains("No shell"))
        assertTrue(ProductGuide.VERSION_LABEL.startsWith("v1.16"))
    }
}
