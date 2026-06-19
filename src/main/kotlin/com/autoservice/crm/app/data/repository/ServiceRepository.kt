package com.autoservice.crm.app.data.repository

import com.autoservice.crm.data.entity.Services
import com.autoservice.crm.domain.model.ServiceDTO
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class ServiceRepository {
    fun findAll(): List<ServiceDTO> = transaction {
        Services.selectAll().map { row ->
            ServiceDTO(
                id = row[Services.id].value,
                name = row[Services.name],
                category = row.getOrNull(Services.category),
                defaultPrice = row[Services.defaultPrice].toDouble()
            )
        }
    }
}