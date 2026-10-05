package com.cat.security

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** AES-GCM blob: 12-byte IV then ciphertext. Key bytes come from the caller. */
object VaultCrypto {
    private const val IV_LEN = 12
    private const val TAG_BITS = 128

    fun encrypt(key: ByteArray, plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
        val iv = cipher.iv ?: ByteArray(IV_LEN).also { SecureRandom().nextBytes(it) }
        val ct = cipher.doFinal(plain)
        return iv + ct
    }

    fun decrypt(key: ByteArray, blob: ByteArray): ByteArray {
        require(blob.size > IV_LEN) { "vault blob too short" }
        val iv = blob.copyOfRange(0, IV_LEN)
        val ct = blob.copyOfRange(IV_LEN, blob.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
        return cipher.doFinal(ct)
    }
}
