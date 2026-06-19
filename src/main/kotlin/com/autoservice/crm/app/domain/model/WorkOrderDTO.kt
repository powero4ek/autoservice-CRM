package com.autoservice.crm.domain.model

import java.time.LocalDateTime

data class WorkOrderDTO(
    val id: Long,
    val orderNumber: String,
    val clientId: Long,
    val clientName: String,
    val vehicleId: Long,
    val vehicleInfo: String,
    val status: String,
    val statusDisplay: String,
    val createdAt: LocalDateTime,
    val completedAt: LocalDateTime?,
    val totalCost: Double,
    val assignedMechanicId: Long?,
    val assignedMechanicName: String?,
    val description: String?,
    val items: List<WorkOrderItemDTO> = emptyList()
)