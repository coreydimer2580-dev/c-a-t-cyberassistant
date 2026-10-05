package com.cat.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHashTest {
    @Test
    fun formatIsFourToEightDigits() {
        assertTrue(PinHash.isValidFormat("1357"))
        assertTrue(PinHash.isValidFormat("12345678"))
        assertFalse(PinHash.isValidFormat("123"))
        assertFalse(PinHash.isValidFormat("123456789"))
        assertFalse(PinHash.isValidFormat("12ab"))
        assertFalse(PinHash.isValidFormat(""))
    }

    @Test
    fun hashIsSaltedAndVerifies() {
        val salt = PinHash.fromHex("00112233445566778899aabbccddeeff")!!
        val other = PinHash.newSalt()
        val pin = "1357"
        val digest = PinHash.hash(salt, pin)
        assertEquals(64, digest.length)
        assertTrue(PinHash.verify(salt, pin, digest))
        assertFalse(PinHash.verify(salt, "1358", digest))
        assertNotEquals(digest, PinHash.hash(other, pin))
        assertFalse(digest.contains(pin))
    }
}
