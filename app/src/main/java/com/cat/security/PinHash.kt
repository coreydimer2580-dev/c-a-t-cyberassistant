package com.cat.security

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Salted PIN digest. Callers never persist the PIN itself.
 */
object PinHash {
    fun isValidFormat(pin: String): Boolean {
        return pin.length in 4..8 && pin.all { it.isDigit() }
    }

    fun newSalt(): ByteArray {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return salt
    }

    fun hash(salt: ByteArray, pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        digest.update(pin.toByteArray(Charsets.UTF_8))
        return hex(digest.digest())
    }

    fun verify(salt: ByteArray, pin: String, expectedHex: String): Boolean {
        if (!isValidFormat(pin) || expectedHex.isBlank()) return false
        return hash(salt, pin).equals(expectedHex, ignoreCase = true)
    }

    fun hex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }

    fun fromHex(hex: String): ByteArray? {
        val clean = hex.trim()
        if (clean.length % 2 != 0 || clean.isEmpty()) return null
        if (!clean.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return null
        return ByteArray(clean.length / 2) { i ->
            clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
