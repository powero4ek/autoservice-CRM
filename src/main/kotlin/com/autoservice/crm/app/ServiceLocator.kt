package com.autoservice.crm.app

import com.autoservice.crm.app.data.repository.ServiceRepository
import com.autoservice.crm.data.repository.*
import com.autoservice.crm.domain.service.*

object ServiceLocator {
    val userRepository = UserRepository()
    val authService = AuthService(userRepository)

    val clientRepository = ClientRepository()
    val clientService = ClientService(clientRepository)

    val vehicleRepository = VehicleRepository()
    val vehicleService = VehicleService(vehicleRepository)

    // НОВОЕ
    val serviceRepository = ServiceRepository()

    val workOrderRepository = WorkOrderRepository()
    val workOrderService = WorkOrderService(workOrderRepository)
}