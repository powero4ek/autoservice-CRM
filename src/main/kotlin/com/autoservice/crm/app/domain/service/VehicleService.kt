package com.autoservice.crm.domain.service

import com.autoservice.crm.data.entity.Vehicles
import com.autoservice.crm.data.repository.VehicleRepository
import com.autoservice.crm.domain.model.VehicleDTO
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.dao.id.EntityID
import com.autoservice.crm.data.entity.Clients

class VehicleService(private val repository: VehicleRepository) {

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
                    licensePlate = row.getOrNull(Vehicles.licensePlate) ?: "",
                    color = row.getOrNull(Vehicles.color),
                    mileage = row.getOrNull(Vehicles.mileage),
                    notes = row.getOrNull(Vehicles.notes)
                )
            }
    }

    fun getAllVehicles(): List<VehicleDTO> = transaction {
        Vehicles.selectAll().map { row ->
            VehicleDTO(
                id = row[Vehicles.id].value,
                clientId = row[Vehicles.clientId].value,
                brand = row[Vehicles.brand],
                model = row[Vehicles.model],
                year = row.getOrNull(Vehicles.year),
                vin = row.getOrNull(Vehicles.vin) ?: "",
                licensePlate = row.getOrNull(Vehicles.licensePlate) ?: "",
                color = row.getOrNull(Vehicles.color),
                mileage = row.getOrNull(Vehicles.mileage),
                notes = row.getOrNull(Vehicles.notes)
            )
        }
    }

    fun getVehicleById(id: Long): VehicleDTO? = transaction {
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
                    licensePlate = row.getOrNull(Vehicles.licensePlate) ?: "",
                    color = row.getOrNull(Vehicles.color),
                    mileage = row.getOrNull(Vehicles.mileage),
                    notes = row.getOrNull(Vehicles.notes)
                )
            }
            .singleOrNull()
    }

    fun createVehicle(
        clientId: Long,
        brand: String,
        model: String,
        year: Int?,
        vin: String,
        licensePlate: String,
        color: String?,
        mileage: Int?,
        notes: String?
    ): Long {
        return repository.create(clientId, brand, model, year, vin, licensePlate, color, mileage, notes)
    }

    fun updateVehicle(
        id: Long,
        brand: String,
        model: String,
        year: Int?,
        vin: String,
        licensePlate: String,
        color: String?,
        mileage: Int?,
        notes: String?
    ): Boolean {
        return repository.update(id, brand, model, year, vin, licensePlate, color, mileage, notes)
    }

    fun deleteVehicle(id: Long): Boolean {
        return repository.delete(id)
    }
}