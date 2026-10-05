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

    @Test
    fun fixesStatusVersionClearUnlock() {
        assertEquals("status", SoftCorrect.apply("staus").text)
        assertEquals("/version", SoftCorrect.apply("/verson").text)
        assertEquals("clear", SoftCorrect.apply("cler").text)
        assertEquals("unlock help", SoftCorrect.apply("unlok hlep").text)
    }

    @Test
    fun fuzzyFixesNearCommandButNotOrdinaryEnglish() {
        assertEquals("help", SoftCorrect.apply("helpx").text)
        assertEquals("remember the pin rule", SoftCorrect.apply("remeberr the pin rule").text)
        assertEquals("status", SoftCorrect.apply("stattus").text)
        assertFalse(SoftCorrect.apply("clean the kettle").changed)
        assertEquals("clean the kettle", SoftCorrect.apply("clean the kettle").text)
    }

    @Test
    fun extraCommandTyposStayFastAndLocal() {
        assertEquals("status", SoftCorrect.apply("stauts").text)
        assertEquals("/clear", SoftCorrect.apply("/clrrr").text)
        assertEquals("unlock", SoftCorrect.apply("unlokk").text)
        assertFalse(SoftCorrect.apply("the weather looks fine").changed)
    }
}
