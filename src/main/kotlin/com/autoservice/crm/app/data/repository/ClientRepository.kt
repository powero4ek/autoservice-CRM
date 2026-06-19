package com.autoservice.crm.data.repository

import com.autoservice.crm.data.entity.Clients
import com.autoservice.crm.data.entity.Vehicles
import com.autoservice.crm.domain.model.ClientDTO
import com.autoservice.crm.domain.model.VehicleDTO
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class ClientRepository {

    fun findAll(): List<ClientDTO> = transaction {
        Clients.selectAll()
            .orderBy(Clients.fullName to SortOrder.ASC)
            .map { row ->
                ClientDTO(
                    id = row[Clients.id].value,
                    fullName = row[Clients.fullName],
                    phone = row[Clients.phone],
                    email = row[Clients.email],
                    notes = row[Clients.notes]
                )
            }
    }

    fun findById(id: Long): ClientDTO? = transaction {
        Clients.select { Clients.id eq id }
            .map { row ->
                ClientDTO(
                    id = row[Clients.id].value,
                    fullName = row[Clients.fullName],
                    phone = row[Clients.phone],
                    email = row[Clients.email],
                    notes = row[Clients.notes]
                )
            }
            .singleOrNull()
    }

    fun findByPhone(phone: String): List<ClientDTO> = transaction {
        Clients.select { Clients.phone like "%$phone%" }
            .orderBy(Clients.fullName to SortOrder.ASC)
            .map { row ->
                ClientDTO(
                    id = row[Clients.id].value,
                    fullName = row[Clients.fullName],
                    phone = row[Clients.phone],
                    email = row[Clients.email],
                    notes = row[Clients.notes]
                )
            }
    }

    fun create(fullName: String, phone: String, email: String?, notes: String?): Long = transaction {
        Clients.insert {
            it[Clients.fullName] = fullName
            it[Clients.phone] = phone
            it[Clients.email] = email
            it[Clients.notes] = notes
        } get Clients.id
    }.value

    fun update(id: Long, fullName: String, phone: String, email: String?, notes: String?): Boolean = transaction {
        Clients.update({ Clients.id eq id }) {
            it[Clients.fullName] = fullName
            it[Clients.phone] = phone
            it[Clients.email] = email
            it[Clients.notes] = notes
        } > 0
    }

    fun delete(id: Long): Boolean = transaction {
        val hasVehicles = Vehicles.select { Vehicles.clientId eq id }.count() > 0
        if (hasVehicles) return@transaction false
        Clients.deleteWhere { Clients.id eq id } > 0
    }

    fun getVehicles(clientId: Long): List<VehicleDTO> = transaction {
        Vehicles.select { Vehicles.clientId eq clientId }
            .map { row ->
                VehicleDTO(
                    id = row[Vehicles.id].value,
                    clientId = row[Vehicles.clientId].value,
                    brand = row[Vehicles.brand],
                    model = row[Vehicles.model],
                    year = row.getOrNull(Vehicles.year),
                    vin = row.getOrNull(Vehicles.vin) ?: "",
                    licensePlate = row[Vehicles.licensePlate],
                    color = row.getOrNull(Vehicles.color),
                    mileage = row.getOrNull(Vehicles.mileage),
                    notes = row.getOrNull(Vehicles.notes)
                )
            }
    }
}