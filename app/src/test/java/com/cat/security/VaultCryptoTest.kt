package com.cat.security

import com.cat.data.TerminalLine
import com.cat.data.TerminalTranscript
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class VaultCryptoTest {
    @Test
    fun roundTripDoesNotStorePlaintext() {
        val key = ByteArray(32) { it.toByte() }
        val lines = listOf(TerminalLine("user", "hello\nfold", 10L))
        val plain = TerminalTranscript.encode(lines).toByteArray()
        val blob = VaultCrypto.encrypt(key, plain)
        assertFalse(String(blob).contains("hello"))
        val back = TerminalTranscript.decode(String(VaultCrypto.decrypt(key, blob)))
        assertEquals(lines, back)
    }
}
