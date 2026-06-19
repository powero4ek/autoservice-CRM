package com.autoservice.crm.infrastructure.security

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

object PasswordHasher {
    private val encoder = BCryptPasswordEncoder()

    fun hash(plainPassword: String): String {
        return encoder.encode(plainPassword)
    }

    fun check(plainPassword: String, hashedPassword: String): Boolean {
        return encoder.matches(plainPassword, hashedPassword)
    }
}