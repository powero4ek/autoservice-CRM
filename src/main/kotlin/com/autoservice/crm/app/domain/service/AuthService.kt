package com.autoservice.crm.domain.service

import com.autoservice.crm.app.SessionManager
import com.autoservice.crm.data.repository.UserRepository


class AuthService(private val userRepository: UserRepository) {

    fun login(username: String, password: String): Boolean {
        val user = userRepository.findByUsername(username) ?: return false
        return if (org.springframework.security.crypto.bcrypt.BCrypt.checkpw(password, user.passwordHash)) {
            SessionManager.currentUser = user
            true
        } else {
            false
        }
    }

    fun logout() {
        SessionManager.clear()
    }
}