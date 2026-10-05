package com.cat.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SoftCorrectTest {
    @Test
    fun fixesHelpTypoWithSlash() {
        val result = SoftCorrect.apply("/hlp")
        assertTrue(result.changed)
        assertEquals("/help", result.text)
        assertTrue(result.note!!.contains("hlp"))
    }

    @Test
    fun leavesRealSentencesAlone() {
        val result = SoftCorrect.apply("please open the notes later")
        assertFalse(result.changed)
        assertEquals("please open the notes later", result.text)
    }

    @Test
    fun keepsArgumentAfterCommand() {
        val result = SoftCorrect.apply("remeber tea at 4")
        assertEquals("remember tea at 4", result.text)
    }
}
