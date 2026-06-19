package com.autoservice.crm.data.repository

import com.autoservice.crm.data.entity.*
import com.autoservice.crm.domain.model.WorkOrderDTO
import com.autoservice.crm.domain.model.WorkOrderItemDTO
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime
import java.math.BigDecimal

class WorkOrderRepository {

    fun findAll(): List<WorkOrderDTO> = transaction {
        WorkOrders
            .innerJoin(Clients)
            .leftJoin(Users, { WorkOrders.assignedMechanicId }, { Users.id })
            .selectAll()
            .orderBy(WorkOrders.createdAt to SortOrder.DESC)
            .map { row -> mapToWorkOrderDTO(row) }
    }

    fun findById(id: Long): WorkOrderDTO? = transaction {
        WorkOrders.innerJoin(Clients).innerJoin(Vehicles)
            .leftJoin(Users, { WorkOrders.assignedMechanicId }, { Users.id })
            .selectAll().where { WorkOrders.id eq EntityID(id, WorkOrders) }
            .map { row -> mapToWorkOrderDTO(row) }
            .singleOrNull()
    }

    fun findByMechanic(mechanicId: Long): List<WorkOrderDTO> = transaction {
        WorkOrders.innerJoin(Clients).innerJoin(Vehicles)
            .leftJoin(Users, { WorkOrders.assignedMechanicId }, { Users.id })
            .selectAll().where { WorkOrders.assignedMechanicId eq EntityID(mechanicId, Users) }
            .orderBy(WorkOrders.createdAt to SortOrder.DESC)
            .map { row -> mapToWorkOrderDTO(row) }
    }

    fun findByStatus(status: String): List<WorkOrderDTO> = transaction {
        WorkOrders.innerJoin(Clients).innerJoin(Vehicles)
            .leftJoin(Users, { WorkOrders.assignedMechanicId }, { Users.id })
            .selectAll().where { WorkOrders.status eq status }
            .orderBy(WorkOrders.createdAt to SortOrder.DESC)
            .map { row -> mapToWorkOrderDTO(row) }
    }

    fun create(
        clientId: Long,
        vehicleId: Long,
        description: String?,
        assignedMechanicId: Long?,
        createdBy: Long
    ): Long = transaction {
        val now = LocalDateTime.now()
        WorkOrders.insert {
            it[WorkOrders.clientId] = EntityID(clientId, Clients)
            it[WorkOrders.vehicleId] = EntityID(vehicleId, Vehicles)  // ← УБЕДИСЬ, что vehicleId
            it[WorkOrders.status] = "CREATED"
            it[WorkOrders.description] = description
            it[WorkOrders.totalCost] = BigDecimal.ZERO
            it[WorkOrders.assignedMechanicId] = assignedMechanicId?.let { mid -> EntityID(mid, Users) }
            it[WorkOrders.createdBy] = EntityID(createdBy, Users)
            it[WorkOrders.createdAt] = now
        } get WorkOrders.id
    }.value

    fun updateStatus(id: Long, newStatus: String, changedBy: Long, comment: String?): Boolean = transaction {
        val oldStatus = WorkOrders.selectAll()
            .where { WorkOrders.id eq EntityID(id, WorkOrders) }
            .map { it[WorkOrders.status] }
            .singleOrNull() ?: return@transaction false

        WorkOrders.update({ WorkOrders.id eq EntityID(id, WorkOrders) }) {
            it[status] = newStatus
            if (newStatus == "COMPLETED") {
                it[completedAt] = LocalDateTime.now()
            }
        }

        StatusHistory.insert {
            it[StatusHistory.workOrderId] = EntityID(id, WorkOrders)
            it[StatusHistory.oldStatus] = oldStatus
            it[StatusHistory.newStatus] = newStatus
            it[StatusHistory.changedBy] = EntityID(changedBy, Users)
            it[StatusHistory.changedAt] = LocalDateTime.now()
            it[StatusHistory.comment] = comment
        }

        true
    }

    fun assignMechanic(id: Long, mechanicId: Long?): Boolean = transaction {
        WorkOrders.update({ WorkOrders.id eq EntityID(id, WorkOrders) }) {
            it[assignedMechanicId] = mechanicId?.let { mid -> EntityID(mid, Users) }
        } > 0
    }

    fun updateTotalCost(id: Long, totalCost: BigDecimal): Boolean = transaction {
        WorkOrders.update({ WorkOrders.id eq EntityID(id, WorkOrders) }) {
            it[WorkOrders.totalCost] = totalCost
        } > 0
    }

    fun getItems(workOrderId: Long): List<WorkOrderItemDTO> = transaction {
        WorkOrderItems
            .leftJoin(Services, { WorkOrderItems.serviceId }, { Services.id })
            .leftJoin(Parts, { WorkOrderItems.partId }, { Parts.id })
            .selectAll()
            .where { WorkOrderItems.workOrderId eq EntityID(workOrderId, WorkOrders) }
            .map { row ->
                WorkOrderItemDTO(
                    id = row[WorkOrderItems.id].value,
                    itemType = row[WorkOrderItems.itemType],
                    serviceId = row[WorkOrderItems.serviceId]?.value,
                    serviceName = row.getOrNull(Services.name),  // ← из JOIN Services
                    partId = row[WorkOrderItems.partId]?.value,
                    partName = row.getOrNull(Parts.name),      // ← из JOIN Parts
                    quantity = row[WorkOrderItems.quantity]?.toInt() ?: 0,
                    unitPrice = row[WorkOrderItems.unitPrice]?.toDouble() ?: 0.0,
                    totalPrice = row[WorkOrderItems.totalPrice]?.toDouble() ?: 0.0,
                    notes = row[WorkOrderItems.notes]
                )
            }
    }

    fun addItem(
        workOrderId: Long,
        itemType: String,
        serviceId: Long?,
        partId: Long?,
        quantity: Int,
        unitPrice: Double,
        notes: String?
    ): Long = transaction {
        WorkOrderItems.insert {
            it[WorkOrderItems.workOrderId] = EntityID(workOrderId, WorkOrders)
            it[WorkOrderItems.itemType] = itemType
            it[WorkOrderItems.serviceId] = serviceId?.let { sid -> EntityID(sid, Services) }
            it[WorkOrderItems.partId] = partId?.let { pid -> EntityID(pid, Parts) }
            it[WorkOrderItems.quantity] = BigDecimal.valueOf(quantity.toLong())
            it[WorkOrderItems.unitPrice] = BigDecimal.valueOf(unitPrice)
            it[WorkOrderItems.totalPrice] = BigDecimal.valueOf(quantity * unitPrice)
            it[WorkOrderItems.notes] = notes
        } get WorkOrderItems.id
    }.value

    fun deleteItem(itemId: Long): Boolean = transaction {
        WorkOrderItems.deleteWhere { id eq EntityID(itemId, WorkOrderItems) } > 0
    }
    fun deleteOrder(id: Long): Boolean = transaction {
        // Сначала удалить позиции наряда
        WorkOrderItems.deleteWhere { workOrderId eq EntityID(id, WorkOrders) }
        // Потом удалить сам наряд
        WorkOrders.deleteWhere { WorkOrders.id eq EntityID(id, WorkOrders) } > 0
    }
    private fun mapToWorkOrderDTO(row: ResultRow): WorkOrderDTO {
        val status = row[WorkOrders.status]
        val vehicleId = row[WorkOrders.vehicleId].value

        // Получаем авто отдельно
        val vehicleInfo = transaction {
            Vehicles.selectAll()
                .where { Vehicles.id eq EntityID(vehicleId, Vehicles) }
                .map { "${it[Vehicles.brand]} ${it[Vehicles.model]} (${it[Vehicles.licensePlate]})" }
                .singleOrNull()
        } ?: "Неизвестно"

        return WorkOrderDTO(
            id = row[WorkOrders.id].value,
            orderNumber = row[WorkOrders.orderNumber],
            clientId = row[WorkOrders.clientId].value,
            clientName = row[Clients.fullName],
            vehicleId = vehicleId,
            vehicleInfo = vehicleInfo,
            status = status,
            statusDisplay = when (status) {
                "CREATED" -> "Создан"
                "IN_PROGRESS" -> "В работе"
                "WAITING_PARTS" -> "Ожидание запчастей"
                "COMPLETED" -> "Выполнен"
                "CLOSED" -> "Закрыт"
                else -> status
            },
            createdAt = row[WorkOrders.createdAt],
            completedAt = row[WorkOrders.completedAt],
            totalCost = row[WorkOrders.totalCost].toDouble(),
            assignedMechanicId = row[WorkOrders.assignedMechanicId]?.value,
            assignedMechanicName = row.getOrNull(Users.fullName),
            description = row[WorkOrders.description]
        )
    }
}