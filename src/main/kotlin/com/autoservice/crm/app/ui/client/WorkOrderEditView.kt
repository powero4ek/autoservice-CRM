package com.autoservice.crm.ui.workorder

import com.autoservice.crm.app.ServiceLocator
import com.autoservice.crm.app.SessionManager
import com.autoservice.crm.domain.model.ClientDTO
import com.autoservice.crm.domain.model.VehicleDTO
import com.autoservice.crm.domain.model.WorkOrderDTO
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.effect.DropShadow
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import tornadofx.*

class WorkOrderEditView : View("Заказ-наряд") {

    private val clientService = ServiceLocator.clientService
    private val vehicleService = ServiceLocator.vehicleService
    private val workOrderService = ServiceLocator.workOrderService

    var workOrder: WorkOrderDTO? = null

    private val selectedClient = SimpleObjectProperty<ClientDTO?>(null)
    private val selectedVehicle = SimpleObjectProperty<VehicleDTO?>(null)
    private val description = SimpleStringProperty("")
    private val selectedMechanic = SimpleObjectProperty<com.autoservice.crm.domain.model.UserDTO?>(null)
    private val errorMessage = SimpleStringProperty("")

    private val clients = FXCollections.observableArrayList<ClientDTO>()
    private val vehicles = FXCollections.observableArrayList<VehicleDTO>()
    private val mechanics = FXCollections.observableArrayList<com.autoservice.crm.domain.model.UserDTO>()

    override val root = borderpane {
        prefWidth = 600.0
        prefHeight = 550.0
        style { backgroundColor += Color.web("#f5f7fa") }

        top = hbox(15) {
            alignment = Pos.CENTER_LEFT
            padding = Insets(0.0, 30.0, 0.0, 30.0)
            prefHeight = 60.0
            style {
                backgroundColor += Color.web("#f57c00")
                backgroundRadius += box(0.px, 0.px, 16.px, 16.px)
            }

            label(if (workOrder == null) "🔧 Новый заказ-наряд" else "🔧 Редактирование наряда") {
                style {
                    fontSize = 18.px
                    fontWeight = FontWeight.BOLD
                    textFill = Color.WHITE
                }
            }
        }

        center = vbox(20) {
            alignment = Pos.CENTER
            padding = Insets(30.0, 40.0, 30.0, 40.0)

            vbox(15) {
                alignment = Pos.CENTER
                padding = Insets(30.0, 35.0, 30.0, 35.0)
                maxWidth = 520.0

                style {
                    backgroundColor += Color.WHITE
                    backgroundRadius += box(12.px)
                }
                effect = DropShadow().apply {
                    color = Color.web("#000000", 0.08)
                    radius = 12.0
                    offsetY = 4.0
                }

                form {
                    fieldset {
                        field("Клиент *") {
                            combobox(selectedClient, clients) {
                                prefWidth = 400.0
                                prefHeight = 42.0
                                promptText = "Выберите клиента"
                                cellFormat { text = it?.fullName ?: "" }
                            }
                        }

                        field("Автомобиль *") {
                            combobox(selectedVehicle, vehicles) {
                                prefWidth = 400.0
                                prefHeight = 42.0
                                promptText = "Сначала выберите клиента"
                                cellFormat { text = it?.let { v -> "${v.brand} ${v.model} (${v.licensePlate})" } ?: "" }
                            }
                        }

                        field("Описание проблемы") {
                            textarea(description) {
                                prefWidth = 400.0
                                prefHeight = 80.0
                                promptText = "Опишите неисправность..."
                            }
                        }

                        if (!SessionManager.isMechanic) {
                            field("Механик") {
                                combobox(selectedMechanic, mechanics) {
                                    prefWidth = 400.0
                                    prefHeight = 42.0
                                    promptText = "Не назначен"
                                    cellFormat { text = it?.fullName ?: "Не назначен" }
                                }
                            }
                        }
                    }
                }

                label(errorMessage) {
                    style {
                        textFill = Color.web("#d32f2f")
                        fontSize = 13.px
                        fontWeight = FontWeight.MEDIUM
                    }
                    visibleWhen { errorMessage.isNotEmpty }
                }

                hbox(15) {
                    alignment = Pos.CENTER

                    button("💾 Сохранить") {
                        prefWidth = 150.0
                        prefHeight = 45.0
                        style {
                            fontSize = 14.px
                            fontWeight = FontWeight.BOLD
                            backgroundColor += Color.web("#1976d2")
                            backgroundRadius += box(10.px)
                            textFill = Color.WHITE
                            cursor = Cursor.HAND
                        }
                        action { save() }  // ← УБЕДИСЬ, что только ОДИН action
                    }

                    button("❌ Отмена") {
                        prefWidth = 150.0
                        prefHeight = 45.0
                        style {
                            fontSize = 14.px
                            fontWeight = FontWeight.BOLD
                            backgroundColor += Color.web("#e0e0e0")
                            backgroundRadius += box(10.px)
                            textFill = Color.web("#555555")
                            cursor = Cursor.HAND
                        }
                        action { close() }
                    }
                }
            }
        }
    }

    override fun onDock() {
        // isInitialized убран — инициализируем каждый раз
        clients.clear()
        vehicles.clear()
        mechanics.clear()
        selectedClient.set(null)
        selectedVehicle.set(null)
        selectedMechanic.set(null)

        clients.setAll(clientService.getAllClients())
        mechanics.setAll(ServiceLocator.userRepository.findAllMechanics())

        selectedClient.addListener { _, _, client ->
            selectedVehicle.set(null)
            vehicles.clear()
            client?.let {
                vehicles.setAll(vehicleService.getClientVehicles(it.id))
            }
        }

        workOrder?.let { wo ->
            val client = clients.find { it.id == wo.clientId }
            selectedClient.set(client)
            client?.let { c ->
                vehicles.setAll(vehicleService.getClientVehicles(c.id))
                val vehicle = vehicles.find { it.id == wo.vehicleId }
                selectedVehicle.set(vehicle)
            }
            description.set(wo.description ?: "")
            val mechanic = mechanics.find { it.id == wo.assignedMechanicId }
            selectedMechanic.set(mechanic)
        }

        isSaving = false  // ← сброс флага при открытии
    }
    private var isSaving = false

    private fun save() {
        if (isSaving) return
        isSaving = true

        errorMessage.set("")
        val client = selectedClient.get()
        val vehicle = selectedVehicle.get()

        if (client == null || vehicle == null) {
            errorMessage.set("Выберите клиента и автомобиль")
            isSaving = false  // ← сброс при ошибке
            return
        }

        try {
            println("DEBUG SAVE START: client=${client.id}, vehicle=${vehicle.id}")

            if (workOrder == null) {
                val orderId = workOrderService.createOrder(
                    client.id,
                    vehicle.id,
                    description.get(),
                    selectedMechanic.get()?.id
                )
                println("DEBUG SAVE CREATED: orderId=$orderId")
                information("✅ Успех", "Заказ-наряд №$orderId создан")
            } else {
                workOrderService.assignMechanic(workOrder!!.id, selectedMechanic.get()?.id)
                information("✅ Успех", "Наряд обновлён")
            }
            close()
        } catch (e: Exception) {
            errorMessage.set("Ошибка: ${e.message}")
            e.printStackTrace()
            isSaving = false  // ← сброс при ошибке
        }
    }
}