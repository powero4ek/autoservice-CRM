package com.autoservice.crm.domain.model

data class UserDTO(
    val id: Long,
    val fullName: String,
    val username: String,
    val passwordHash: String,
    val roleName: String
)