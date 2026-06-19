package com.autoservice.crm.domain.model

data class VehicleDTO(
    val id: Long,
    val clientId: Long,  // <-- Убедись, что это поле есть
    val brand: String,
    val model: String,
    val year: Int?,
    val vin: String,
    val licensePlate: String,
    val color: String?,
    val mileage: Int?,
    val notes: String?
)