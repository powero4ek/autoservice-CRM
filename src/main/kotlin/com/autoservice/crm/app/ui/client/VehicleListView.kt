package com.autoservice.crm.ui.client

import com.autoservice.crm.app.ServiceLocator
import com.autoservice.crm.domain.model.ClientDTO
import com.autoservice.crm.domain.model.VehicleDTO
import com.autoservice.crm.ui.vehicle.VehicleEditView
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import tornadofx.*

class VehicleListView : View("Автомобили клиента") {

    private val vehicleService = ServiceLocator.vehicleService

    var client: ClientDTO? = null
        set(value) {
            field = value
            value?.let { loadVehicles(it.id) }
        }

    private val vehicles = FXCollections.observableArrayList<VehicleDTO>()

    override val root = borderpane {
        prefWidth = 800.0
        prefHeight = 500.0

        top = hbox(15) {
            alignment = Pos.CENTER_LEFT
            paddingAll = 15
            style { backgroundColor += Color.web("#263238") }

            button("← Назад") {
                style { backgroundColor += Color.web("#1976d2"); textFill = Color.WHITE }
                action { close() }
            }

            label((client?.fullName ?: "") + " — Автомобили") {
                style { fontSize = 18.px; fontWeight = FontWeight.BOLD; textFill = Color.WHITE }
            }

            region { hgrow = javafx.scene.layout.Priority.ALWAYS }

            button("+ Добавить авто") {
                style { backgroundColor += Color.web("#388e3c"); textFill = Color.WHITE; fontWeight = FontWeight.BOLD }
                action { openAddDialog() }
            }
        }

        center = tableview(vehicles) {
            column<VehicleDTO, String>("Марка", "brand").prefWidth = 120.0
            column<VehicleDTO, String>("Модель", "model").prefWidth = 120.0
            column<VehicleDTO, Number>("Год", "year").prefWidth = 60.0
            column<VehicleDTO, String>("Гос. номер", "licensePlate").prefWidth = 100.0
            column<VehicleDTO, String>("VIN", "vin").prefWidth = 150.0
            column<VehicleDTO, String>("Цвет", "color").prefWidth = 80.0
            column<VehicleDTO, Number>("Пробег", "mileage").prefWidth = 80.0
            column<VehicleDTO, String>("Примечание", "notes").prefWidth = 150.0  // ← добавлено

            contextmenu {
                item("Редактировать").action {
                    selectedItem?.let { openEditDialog(it) }
                }
                item("Удалить").action {
                    selectedItem?.let { deleteVehicle(it) }
                }
            }
        }
    }

    private fun loadVehicles(clientId: Long) {
        vehicles.setAll(vehicleService.getClientVehicles(clientId))
    }

    private fun openAddDialog() {
        val editView = find<VehicleEditView>()
        editView.clientId = client?.id ?: 0
        editView.vehicle = null  // Это вызовет resetFields()
        editView.openModal(block = true)
        client?.let { loadVehicles(it.id) }
    }

    private fun openEditDialog(vehicle: VehicleDTO) {
        val editView = find<VehicleEditView>()
        editView.clientId = client?.id ?: 0
        editView.vehicle = vehicle
        editView.openModal(block = true)
        client?.let { loadVehicles(it.id) }
    }

    private fun deleteVehicle(vehicle: VehicleDTO) {
        confirm("Удаление", "Удалить ${vehicle.brand} ${vehicle.model}?") {
            if (vehicleService.deleteVehicle(vehicle.id)) {
                information("Успех", "Автомобиль удален")
                client?.let { loadVehicles(it.id) }
            } else {
                error("Ошибка", "Не удалось удалить")
            }
        }
    }
}