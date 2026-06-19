package com.autoservice.crm.ui.client

import com.autoservice.crm.app.ServiceLocator
import com.autoservice.crm.app.SessionManager
import com.autoservice.crm.domain.model.ClientDTO
import com.autoservice.crm.ui.dashboard.DashboardView
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.effect.DropShadow
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import tornadofx.*

class ClientListView : View("Клиенты автосервиса") {

    private val clientService = ServiceLocator.clientService

    private val clients = FXCollections.observableArrayList<ClientDTO>()
    private val searchPhone = SimpleStringProperty("")

    override val root = borderpane {
        prefWidth = 1000.0
        prefHeight = 700.0
        style { backgroundColor += Color.web("#f5f7fa") }

        top = vbox {
            prefHeight = 130.0
            padding = Insets(0.0, 30.0, 0.0, 30.0)
            style {
                backgroundColor += Color.web("#1a237e")
                backgroundRadius += box(0.px, 0.px, 16.px, 16.px)
            }
            effect = DropShadow().apply {
                color = Color.web("#000000", 0.15)
                radius = 10.0
                offsetY = 3.0
            }

            hbox(15) {
                alignment = Pos.CENTER_LEFT
                prefHeight = 70.0

                button("← Назад") {
                    prefHeight = 36.0
                    prefWidth = 90.0
                    style {
                        fontSize = 13.px
                        fontWeight = FontWeight.BOLD
                        backgroundColor += Color.web("#3949ab")
                        backgroundRadius += box(8.px)
                        textFill = Color.WHITE
                        cursor = Cursor.HAND
                    }
                    action {
                        close()
                        DashboardView().openWindow()
                    }
                }

                label("👥 Клиенты автосервиса") {
                    style {
                        fontSize = 22.px
                        fontWeight = FontWeight.BOLD
                        textFill = Color.WHITE
                    }
                }

                region { hgrow = javafx.scene.layout.Priority.ALWAYS }

                if (!SessionManager.isMechanic) {
                    button("+ Добавить клиента") {
                        prefHeight = 40.0
                        prefWidth = 160.0
                        style {
                            fontSize = 14.px
                            fontWeight = FontWeight.BOLD
                            backgroundColor += Color.web("#4caf50")
                            backgroundRadius += box(10.px)
                            textFill = Color.WHITE
                            cursor = Cursor.HAND
                        }
                        action { openAddDialog() }
                    }
                }
            }

            hbox(10) {
                alignment = Pos.CENTER_LEFT
                prefHeight = 50.0
                padding = Insets(0.0, 0.0, 15.0, 0.0)

                textfield(searchPhone) {
                    promptText = "🔍 Поиск по телефону или ФИО..."
                    prefWidth = 350.0
                    prefHeight = 40.0
                    padding = Insets(0.0, 15.0, 0.0, 15.0)
                    style {
                        fontSize = 14.px
                        backgroundColor += Color.web("#ffffff", 0.95)
                        backgroundRadius += box(10.px)
                        textFill = Color.web("#333333")
                    }
                }

                button("Найти") {
                    prefHeight = 40.0
                    prefWidth = 80.0
                    style {
                        fontSize = 13.px
                        fontWeight = FontWeight.BOLD
                        backgroundColor += Color.web("#1976d2")
                        backgroundRadius += box(10.px)
                        textFill = Color.WHITE
                        cursor = Cursor.HAND
                    }
                    action { search() }
                }

                button("Сброс") {
                    prefHeight = 40.0
                    prefWidth = 80.0
                    style {
                        fontSize = 13.px
                        fontWeight = FontWeight.BOLD
                        backgroundColor += Color.web("#757575")
                        backgroundRadius += box(10.px)
                        textFill = Color.WHITE
                        cursor = Cursor.HAND
                    }
                    action { loadAll() }
                }
            }
        }

        center = stackpane {
            padding = Insets(20.0, 30.0, 20.0, 30.0)

            tableview(clients) {
                style {
                    backgroundColor += Color.WHITE
                    backgroundRadius += box(12.px)
                }
                effect = DropShadow().apply {
                    color = Color.web("#000000", 0.08)
                    radius = 10.0
                    offsetY = 3.0
                }

                column<ClientDTO, String>("ФИО", "fullName").prefWidth = 250.0
                column<ClientDTO, String>("Телефон", "phone").prefWidth = 150.0
                column<ClientDTO, String>("Email", "email").prefWidth = 200.0
                column<ClientDTO, String>("Примечание", "notes").prefWidth = 250.0

                contextmenu {
                    item("🔍 Просмотр").action {
                        selectedItem?.let { openVehicles(it) }
                    }
                    if (!SessionManager.isMechanic) {
                        item("✏️ Редактировать").action {
                            selectedItem?.let { openEditDialog(it) }
                        }
                        item("🚗 Автомобили").action {
                            selectedItem?.let { openVehicles(it) }
                        }
                        separator()
                        item("🗑️ Удалить").action {
                            selectedItem?.let { deleteClient(it) }
                        }
                    }
                }

                rowFactory = javafx.util.Callback {
                    val row = javafx.scene.control.TableRow<ClientDTO>()
                    row.hoverProperty().addListener { _, _, hovered ->
                        if (hovered && !row.isEmpty) {
                            row.style = "-fx-background-color: #e3f2fd;"
                        } else {
                            row.style = ""
                        }
                    }
                    row
                }
            }
        }
    }

    override fun onDock() {
        loadAll()
    }

    private fun loadAll() {
        clients.setAll(clientService.getAllClients())
        searchPhone.set("")
    }

    private fun search() {
        val phone = searchPhone.get()
        if (phone.isBlank()) {
            loadAll()
        } else {
            clients.setAll(clientService.searchClients(phone))
        }
    }

    private fun openAddDialog() {
        val editView = find<ClientEditView>()
        editView.client = null
        editView.openModal(block = true)
        loadAll()
    }

    private fun openEditDialog(client: ClientDTO) {
        val editView = find<ClientEditView>()
        editView.client = client
        editView.openModal(block = true)
        loadAll()
    }

    private fun openVehicles(client: ClientDTO) {
        val vehicleView = find<VehicleListView>()
        vehicleView.client = client
        vehicleView.openModal(block = true)
    }

    private fun deleteClient(client: ClientDTO) {
        confirm("Удаление клиента", "Вы уверены, что хотите удалить ${client.fullName}?") {
            if (clientService.deleteClient(client.id)) {
                information("Успех", "Клиент удалён")
                loadAll()
            } else {
                error("Ошибка", "Нельзя удалить клиента с автомобилями")
            }
        }
    }
}