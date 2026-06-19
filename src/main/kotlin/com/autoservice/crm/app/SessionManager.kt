package com.autoservice.crm.app

import com.autoservice.crm.domain.model.UserDTO

object SessionManager {
    var currentUser: UserDTO? = null

    val isAdmin: Boolean
        get() = currentUser?.roleName == "Администратор"

    val isMechanic: Boolean
        get() = currentUser?.roleName == "Механик"

    fun clear() {
        currentUser = null
    }
}