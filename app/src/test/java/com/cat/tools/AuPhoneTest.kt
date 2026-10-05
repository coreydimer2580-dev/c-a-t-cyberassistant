package com.cat.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuPhoneTest {
    @Test
    fun parsesAustralianMobile() {
        assertEquals("0412345678", AuPhone.parse("0412 345 678"))
        assertEquals("+61412345678", AuPhone.parse("+61 412 345 678"))
    }

    @Test
    fun rejectsStarCodesAndShortNumbers() {
        assertNull(AuPhone.parse("*#06#"))
        assertNull(AuPhone.parse("000"))
        assertNull(AuPhone.parse("12345"))
    }

    @Test
    fun callAndSmsCommands() {
        val call = AuPhone.interpret("/call 0412 345 678")
        assertEquals(true, call!!.dial)
        assertEquals("0412345678", call.number)
        assertTrue(call.reply.contains("dialer"))

        val sms = AuPhone.interpret("/sms 0412345678 on my way")
        assertEquals(false, sms!!.dial)
        assertEquals("0412345678", sms.number)
        assertEquals("on my way", sms.body)
        assertTrue(sms.reply.contains("SMS"))

        val blocked = AuPhone.interpret("/sms *#06# hi")
        assertNull(blocked!!.number)
        assertTrue(blocked.reply.contains("normal phone number"))
    }

    @Test
    fun ignoresOrdinaryChat() {
        assertNull(AuPhone.interpret("hello"))
        assertNull(AuPhone.interpret("/help"))
    }
}
