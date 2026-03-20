package com.remitos.app.data

import java.security.MessageDigest

object PasswordHasher {
    private const val SALT = "remitos_local_v1_"

    fun hash(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest((SALT + password).toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verify(password: String, storedHash: String): Boolean {
        return hash(password) == storedHash
    }
}
