package com.autoservice.crm.domain.model

data class ServiceDTO(
    val id: Long,
    val name: String,
    val category: String?,
    val defaultPrice: Double
)