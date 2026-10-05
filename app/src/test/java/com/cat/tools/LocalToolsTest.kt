package com.cat.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalToolsTest {
    @Test
    fun perthEpochIsEightAm() {
        val report = LocalTools.clockReport(0L)
        assertTrue(report.contains("Perth"))
        assertTrue(report.contains("8:00:00"))
        assertTrue(report.contains("am"))
    }

    @Test
    fun convertsKilometresAndCelsius() {
        val miles = LocalTools.convert(10.0, "km", "mi")
        assertEquals(6.2137, miles!!, 0.001)
        val fahrenheit = LocalTools.convert(0.0, "C", "F")
        assertEquals(32.0, fahrenheit!!, 0.001)
    }

    @Test
    fun strengthAndHashAndJson() {
        assertEquals("Weak", LocalTools.passphraseStrength("abc").label)
        assertEquals("Strong", LocalTools.passphraseStrength("Horse-Battery-9").label)
        assertEquals(
            "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
            LocalTools.sha256("hello")
        )
        assertTrue(LocalTools.prettyJson("{\"a\":1}").contains("\n"))
        assertEquals("hi", LocalTools.base64Decode(LocalTools.base64Encode("hi")))
    }
}
