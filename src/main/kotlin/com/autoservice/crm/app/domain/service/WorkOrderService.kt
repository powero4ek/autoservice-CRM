package com.autoservice.crm.domain.service

import com.autoservice.crm.app.SessionManager
import com.autoservice.crm.data.repository.WorkOrderRepository
import com.autoservice.crm.domain.model.WorkOrderDTO
import com.autoservice.crm.domain.model.WorkOrderItemDTO
import java.math.BigDecimal

class WorkOrderService(private val repository: WorkOrderRepository) {

    fun getAllOrders(): List<WorkOrderDTO> {
        return if (SessionManager.isMechanic) {
            SessionManager.currentUser?.id?.let { repository.findByMechanic(it) } ?: emptyList()
        } else {
            repository.findAll()
        }
    }

    fun getOrderById(id: Long): WorkOrderDTO? = repository.findById(id)

    fun createOrder(
        clientId: Long,
        vehicleId: Long,
        description: String?,
        assignedMechanicId: Long?
    ): Long {
        val createdBy = SessionManager.currentUser?.id ?: throw IllegalStateException("Не авторизован")
        return repository.create(clientId, vehicleId, description, assignedMechanicId, createdBy)
    }

    fun changeStatus(orderId: Long, newStatus: String, comment: String? = null): Boolean {
        val userId = SessionManager.currentUser?.id ?: return false
        return repository.updateStatus(orderId, newStatus, userId, comment)
    }

    fun assignMechanic(orderId: Long, mechanicId: Long?): Boolean {
        return repository.assignMechanic(orderId, mechanicId)
    }

    fun addServiceItem(
        orderId: Long,
        serviceId: Long?,
        unitPrice: Double,
        notes: String?
    ): Long {
        return repository.addItem(orderId, "SERVICE", serviceId, null, 1, unitPrice, notes)
    }

    // УДАЛЕНО: addPartItem полностью

    fun removeItem(itemId: Long): Boolean {
        return repository.deleteItem(itemId)
    }

    fun getOrderItems(orderId: Long): List<WorkOrderItemDTO> {
        return repository.getItems(orderId)
    }

    fun updateOrderTotal(orderId: Long, total: Double): Boolean {
        return repository.updateTotalCost(orderId, BigDecimal.valueOf(total))
    }

    fun deleteOrder(orderId: Long): Boolean {
        return repository.deleteOrder(orderId)
    }
}