package com.autoservice.crm.domain.model

data class WorkOrderItemDTO(
    val id: Long,
    val itemType: String,
    val serviceId: Long?,
    val serviceName: String?,
    val partId: Long?,
    val partName: String?,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double,
    val notes: String?
) {
    val displayName: String = serviceName ?: partName ?: "—"
}