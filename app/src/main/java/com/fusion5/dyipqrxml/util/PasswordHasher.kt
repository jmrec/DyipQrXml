package com.fusion5.dyipqrxml.util

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

class PasswordHasher {
    private val secureRandom = SecureRandom()

    fun hash(password: String): String {
        require(password.isNotBlank()) { "Password cannot be blank" }
        val salt = ByteArray(SALT_LENGTH).also { secureRandom.nextBytes(it) }
        val hash = digest(password, salt)
        val encoder = Base64.getEncoder()
        return buildString {
            append(encoder.encodeToString(salt))
            append(':')
            append(encoder.encodeToString(hash))
        }
    }

    fun verify(password: String, stored: String): Boolean {
        val parts = stored.split(':')
        if (parts.size != 2) return false
        val decoder = Base64.getDecoder()
        val salt = runCatching { decoder.decode(parts[0]) }.getOrNull() ?: return false
        val expectedHash = runCatching { decoder.decode(parts[1]) }.getOrNull() ?: return false
        val actualHash = digest(password, salt)
        return MessageDigest.isEqual(expectedHash, actualHash)
    }

    private fun digest(password: String, salt: ByteArray): ByteArray {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        messageDigest.update(salt)
        return messageDigest.digest(password.toByteArray(Charsets.UTF_8))
    }

    companion object {
        private const val SALT_LENGTH = 16
    }
}

