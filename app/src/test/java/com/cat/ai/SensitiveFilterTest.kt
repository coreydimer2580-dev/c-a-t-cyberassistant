package com.cat.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SensitiveFilterTest {
    private val filter = SensitiveFilter()

    @Test
    fun redactsEmail() {
        val out = filter.sanitize("mail me at ada@example.com please")
        assertFalse(out.contains("ada@example.com"))
        assertEquals("mail me at [FILTERED] please", out)
    }

    @Test
    fun redactsSsn() {
        val out = filter.sanitize("ssn 123-45-6789")
        assertFalse(out.contains("123-45-6789"))
    }
}
