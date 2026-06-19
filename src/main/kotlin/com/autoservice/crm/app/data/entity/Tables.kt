package com.autoservice.crm.data.entity

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import java.math.BigDecimal

/**
 * Таблицы БД в нотации Exposed DSL.
 * Имена и типы полей строго соответствуют SQL-схеме.
 */

object Roles : LongIdTable("roles") {
    val name = varchar("name", 50).uniqueIndex()
    val description = text("description")
    val permissions = text("permissions") // JSON-строка с правами
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}

object Users : LongIdTable("users") {
    val username = varchar("username", 50).uniqueIndex()
    val fullName = varchar("full_name", 100)
    val passwordHash = varchar("password_hash", 255)
    val roleName = varchar("role_name", 50)  // ← УБЕДИСЬ, что это есть
}

object Clients : LongIdTable("clients") {
    val fullName = varchar("full_name", 150)
    val phone = varchar("phone", 20)
    val email = varchar("email", 100).nullable()
    val notes = text("notes").nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}

object Vehicles : LongIdTable("vehicles") {
    val clientId = reference("client_id", Clients)
    val brand = varchar("brand", 50)
    val model = varchar("model", 50)
    val year = integer("year").nullable()
    val vin = varchar("vin", 17).nullable().uniqueIndex()
    val licensePlate = varchar("license_plate", 20)
    val color = varchar("color", 30).nullable()
    val mileage = integer("mileage").nullable()
    val notes = text("notes").nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}

object Suppliers : LongIdTable("suppliers") {
    val name = varchar("name", 150)
    val phone = varchar("phone", 20).nullable()
    val email = varchar("email", 100).nullable()
    val address = varchar("address", 255).nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}

object Services : LongIdTable("services") {
    val name = varchar("name", 150)
    val category = varchar("category", 50).nullable()
    val defaultPrice = decimal("default_price", 12, 2)
    val estimatedHours = decimal("estimated_hours", 4, 1).nullable()
    val isActive = bool("is_active").default(true)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}

object Parts : LongIdTable("parts") {
    val name = varchar("name", 150)
    val sku = varchar("sku", 50).nullable().uniqueIndex()
    val category = varchar("category", 50).nullable()
    val stockQuantity = decimal("quantity_in_stock", 10, 0).default(BigDecimal.ZERO)
    val minStockLevel = integer("min_stock_level").default(5)
    val purchasePrice = decimal("purchase_price", 12, 2).nullable()
    val salePrice = decimal("sale_price", 12, 2).nullable()
    val supplierId = reference("supplier_id", Suppliers).nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}

object WorkOrders : LongIdTable("work_orders") {
    val orderNumber = varchar("order_number", 20).uniqueIndex()
    val vehicleId = reference("vehicle_id", Vehicles)
    val clientId = reference("client_id", Clients)
    val status = varchar("status", 20).default("CREATED") // <-- изменено
    val description = text("description").nullable()
    val totalCost = decimal("total_cost", 12, 2).default(BigDecimal.ZERO)
    val assignedMechanicId = reference("assigned_mechanic_id", Users).nullable()
    val createdBy = reference("created_by", Users)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val completedAt = datetime("completed_at").nullable()
    val closedBy = reference("closed_by", Users).nullable()
    val closedAt = datetime("closed_at").nullable()
}

object WorkOrderItems : LongIdTable("work_order_items") {
    val workOrderId = reference("work_order_id", WorkOrders)
    val itemType = varchar("item_type", 20)
    val serviceId = reference("service_id", Services).nullable()
    val partId = reference("part_id", Parts).nullable()
    val quantity = decimal("quantity", 10, 0)
    val unitPrice = decimal("unit_price", 12, 2)
    val totalPrice = decimal("total_price", 12, 2)  // ← без generatedAlways!
    val notes = text("notes").nullable()
}

object InventoryTransactions : LongIdTable("inventory_transactions") {
    val partId = reference("part_id", Parts)
    val transactionType = varchar("transaction_type", 20) // IN, OUT, ADJUSTMENT
    val quantity = decimal("quantity", 8, 2)
    val reason = varchar("reason", 255).nullable()
    val workOrderId = reference("work_order_id", WorkOrders).nullable()
    val createdBy = reference("created_by", Users)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}

object StatusHistory : LongIdTable("status_history") {
    val workOrderId = reference("work_order_id", WorkOrders)
    val oldStatus = varchar("old_status", 20).nullable()
    val newStatus = varchar("new_status", 20)
    val changedBy = reference("changed_by", Users)
    val changedAt = datetime("changed_at").defaultExpression(CurrentDateTime)
    val comment = varchar("comment", 255).nullable()
}

object UserSessions : LongIdTable("user_sessions") {
    val userId = reference("user_id", Users)
    val sessionToken = varchar("session_token", 255).uniqueIndex()
    val ipAddress = varchar("ip_address", 45).nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val expiresAt = datetime("expires_at")
}
