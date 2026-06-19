package com.autoservice.crm.domain.model

data class ClientDTO(
    val id: Long,
    val fullName: String,
    val phone: String,
    val email: String?,
    val notes: String?,
    val vehiclesCount: Int = 0
)