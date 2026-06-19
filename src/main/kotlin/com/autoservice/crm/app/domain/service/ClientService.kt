package com.autoservice.crm.domain.service

import com.autoservice.crm.data.repository.ClientRepository
import com.autoservice.crm.domain.model.ClientDTO
import com.autoservice.crm.domain.model.VehicleDTO

class ClientService(private val repository: ClientRepository) {

    fun getAllClients(): List<ClientDTO> = repository.findAll()

    fun searchClients(phone: String): List<ClientDTO> = repository.findByPhone(phone)

    fun createClient(fullName: String, phone: String, email: String?, notes: String?): Long {
        require(fullName.isNotBlank()) { "ФИО обязательно" }
        require(phone.isNotBlank()) { "Телефон обязателен" }
        return repository.create(fullName, phone, email, notes)
    }
    fun getClientVehicles(clientId: Long): List<VehicleDTO> = repository.getVehicles(clientId)
    fun updateClient(id: Long, fullName: String, phone: String, email: String?, notes: String?): Boolean {
        require(fullName.isNotBlank()) { "ФИО обязательно" }
        require(phone.isNotBlank()) { "Телефон обязателен" }
        return repository.update(id, fullName, phone, email, notes)
    }

    fun deleteClient(id: Long): Boolean = repository.delete(id)
}