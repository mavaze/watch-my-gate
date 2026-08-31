package com.mavaze.mygate.auth

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16

    fun hash(password: String): String {
        require(password.isNotEmpty()) {
            "Password cannot be empty"
        }

        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)

        val hash = derive(password, salt)

        return buildString {
            append("pbkdf2_sha256$")
            append(ITERATIONS)
            append("$")
            append(salt.toHex())
            append("$")
            append(hash.toHex())
        }
    }

    fun verify(password: String, storedHash: String): Boolean {
        return try {
            val parts = storedHash.split("$")

            if (parts.size != 4) return false
            if (parts[0] != "pbkdf2_sha256") return false

            val iterations = parts[1].toInt()
            val salt = parts[2].hexToBytes()
            val expected = parts[3].hexToBytes()

            val actual = derive(password, salt, iterations)

            MessageDigest.isEqual(expected, actual)
        } catch (_: Exception) {
            false
        }
    }

    private fun derive(
        password: String,
        salt: ByteArray,
        iterations: Int = ITERATIONS
    ): ByteArray {

        val spec = PBEKeySpec(
            password.toCharArray(),
            salt,
            iterations,
            KEY_LENGTH
        )

        return try {
            SecretKeyFactory
                .getInstance(ALGORITHM)
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun ByteArray.toHex(): String =
        joinToString("") { "%02x".format(it) }

    private fun String.hexToBytes(): ByteArray {
        require(length % 2 == 0)

        return chunked(2).map {
            it.toInt(16).toByte()
        }.toByteArray()
    }
}