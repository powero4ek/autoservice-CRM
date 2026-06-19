package com.autoservice.crm.data.repository

import com.autoservice.crm.data.entity.Clients
import com.autoservice.crm.data.entity.Vehicles
import com.autoservice.crm.domain.model.VehicleDTO
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.update

class VehicleRepository {

    fun getClientVehicles(clientId: Long): List<VehicleDTO> = transaction {
        Vehicles.selectAll()
            .where { Vehicles.clientId eq EntityID(clientId, Clients) }
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

    fun findAll(): List<VehicleDTO> = transaction {
        Vehicles.selectAll().map { row ->
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

    fun findById(id: Long): VehicleDTO? = transaction {
        Vehicles.selectAll()
            .where { Vehicles.id eq EntityID(id, Vehicles) }
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
            .singleOrNull()
    }

    fun create(
        clientId: Long,
        brand: String,
        model: String,
        year: Int?,
        vin: String,
        licensePlate: String,
        color: String?,
        mileage: Int?,
        notes: String?
    ): Long = transaction {
        Vehicles.insert {
            it[Vehicles.clientId] = EntityID(clientId, Clients)
            it[Vehicles.brand] = brand
            it[Vehicles.model] = model
            it[Vehicles.year] = year
            it[Vehicles.vin] = vin
            it[Vehicles.licensePlate] = licensePlate
            it[Vehicles.color] = color
            it[Vehicles.mileage] = mileage
            it[Vehicles.notes] = notes
        } get Vehicles.id
    }.value

    fun update(
        id: Long,
        brand: String,
        model: String,
        year: Int?,
        vin: String,
        licensePlate: String,
        color: String?,
        mileage: Int?,
        notes: String?
    ): Boolean = transaction {
        Vehicles.update({ Vehicles.id eq EntityID(id, Vehicles) }) {
            it[Vehicles.brand] = brand
            it[Vehicles.model] = model
            it[Vehicles.year] = year
            it[Vehicles.vin] = vin
            it[Vehicles.licensePlate] = licensePlate
            it[Vehicles.color] = color
            it[Vehicles.mileage] = mileage
            it[Vehicles.notes] = notes
        } > 0
    }

    fun delete(id: Long): Boolean = transaction {
        Vehicles.deleteWhere { Vehicles.id eq EntityID(id, Vehicles) } > 0
    }
}