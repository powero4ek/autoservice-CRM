package com.autoservice.crm.ui.workorder

import com.autoservice.crm.app.ServiceLocator
import com.autoservice.crm.app.SessionManager
import com.autoservice.crm.domain.model.WorkOrderDTO
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

class WorkOrderListView : View("Заказ-наряды") {

    private val workOrderService = ServiceLocator.workOrderService

    private val orders = FXCollections.observableArrayList<WorkOrderDTO>()
    private val filterStatus = SimpleStringProperty("Все")

    override val root = borderpane {
        prefWidth = 1100.0
        prefHeight = 700.0
        style { backgroundColor += Color.web("#f5f7fa") }

        top = vbox {
            prefHeight = 130.0
            padding = Insets(0.0, 30.0, 0.0, 30.0)
            style {
                backgroundColor += Color.web("#f57c00")
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
                        backgroundColor += Color.web("#ef6c00")
                        backgroundRadius += box(8.px)
                        textFill = Color.WHITE
                        cursor = Cursor.HAND
                    }
                    action {
                        close()
                        DashboardView().openWindow()
                    }
                }

                label("🔧 Заказ-наряды") {
                    style {
                        fontSize = 22.px
                        fontWeight = FontWeight.BOLD
                        textFill = Color.WHITE
                    }
                }

                region { hgrow = javafx.scene.layout.Priority.ALWAYS }

                if (!SessionManager.isMechanic) {
                    button("+ Новый наряд") {
                        prefHeight = 40.0
                        prefWidth = 150.0
                        style {
                            fontSize = 14.px
                            fontWeight = FontWeight.BOLD
                            backgroundColor += Color.web("#4caf50")
                            backgroundRadius += box(10.px)
                            textFill = Color.WHITE
                            cursor = Cursor.HAND
                        }
                        action { openCreateDialog() }
                    }
                }
            }

            hbox(10) {
                alignment = Pos.CENTER_LEFT
                prefHeight = 50.0
                padding = Insets(0.0, 0.0, 15.0, 0.0)

                val statusCombo = combobox(filterStatus, listOf("Все", "Создан", "В работе", "Ожидание запчастей", "Выполнен", "Закрыт")) {
                    prefWidth = 200.0
                    prefHeight = 40.0
                    style {
                        fontSize = 14.px
                        backgroundColor += Color.web("#ffffff", 0.95)
                        backgroundRadius += box(10.px)
                    }
                }

                statusCombo.valueProperty().addListener { _, _, _ ->
                    loadOrders()
                }
            }
        }

        center = stackpane {
            padding = Insets(20.0, 30.0, 20.0, 30.0)

            tableview(orders) {
                style {
                    backgroundColor += Color.WHITE
                    backgroundRadius += box(12.px)
                }
                effect = DropShadow().apply {
                    color = Color.web("#000000", 0.08)
                    radius = 10.0
                    offsetY = 3.0
                }

                column<WorkOrderDTO, String>("Номер", "orderNumber").prefWidth = 120.0
                column<WorkOrderDTO, String>("Клиент", "clientName").prefWidth = 180.0
                column<WorkOrderDTO, String>("Автомобиль", "vehicleInfo").prefWidth = 200.0
                column<WorkOrderDTO, String>("Статус", "statusDisplay").prefWidth = 150.0
                column<WorkOrderDTO, Number>("Сумма", "totalCost").prefWidth = 100.0
                column<WorkOrderDTO, String>("Дата", "createdAt").prefWidth = 150.0
                column<WorkOrderDTO, String>("Механик", "assignedMechanicName").prefWidth = 150.0

                contextmenu {
                    item("🔍 Просмотр").action {
                        selectedItem?.let { openDetail(it) }
                    }
                    if (!SessionManager.isMechanic) {
                        item("✏️ Изменить статус").action {
                            selectedItem?.let { openStatusDialog(it) }
                        }
                        item("👤 Назначить механика").action {
                            selectedItem?.let { openAssignDialog(it) }
                        }
                        separator()
                        item("🗑️ Удалить наряд").action {
                            selectedItem?.let { deleteOrder(it) }
                        }
                    }
                }

                rowFactory = javafx.util.Callback {
                    val row = javafx.scene.control.TableRow<WorkOrderDTO>()
                    row.hoverProperty().addListener { _, _, hovered ->
                        if (hovered && !row.isEmpty) {
                            row.style = "-fx-background-color: #fff3e0;"
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
        loadOrders()
    }

    private fun loadOrders() {
        val status = filterStatus.get()
        orders.setAll(
            if (status == "Все") {
                workOrderService.getAllOrders()
            } else {
                val statusCode = when (status) {
                    "Создан" -> "CREATED"
                    "В работе" -> "IN_PROGRESS"
                    "Ожидание запчастей" -> "WAITING_PARTS"
                    "Выполнен" -> "COMPLETED"
                    "Закрыт" -> "CLOSED"
                    else -> status
                }
                workOrderService.getAllOrders().filter { it.status == statusCode }
            }
        )
    }

    private fun openCreateDialog() {
        val editView = find<WorkOrderEditView>()
        editView.workOrder = null
        editView.openModal(block = true)
        loadOrders()
    }

    private fun openDetail(order: WorkOrderDTO) {
        val detailView = find<WorkOrderDetailView>()
        detailView.workOrder = order
        detailView.openModal(block = true)
        loadOrders()
    }

    private fun openStatusDialog(order: WorkOrderDTO) {
        val next = when (order.status) {
            "CREATED" -> "IN_PROGRESS"
            "IN_PROGRESS" -> "WAITING_PARTS"
            "WAITING_PARTS" -> "COMPLETED"
            "COMPLETED" -> "CLOSED"
            else -> null
        }
        if (next != null) {
            confirm("Смена статуса", "Перевести «${order.orderNumber}» в «${statusDisplay(next)}»?") {
                if (workOrderService.changeStatus(order.id, next)) {
                    information("✅ Успех", "Статус обновлён")
                    loadOrders()
                }
            }
        } else {
            information("Информация", "Наряд уже закрыт")
        }
    }

    private fun openAssignDialog(order: WorkOrderDTO) {
        val dialog = find<MechanicAssignDialog>()
        dialog.openModal(block = true)
        val mechanic = dialog.selectedMechanic.get()
        if (mechanic != null) {
            if (workOrderService.assignMechanic(order.id, mechanic.id)) {
                information("✅ Успех", "Механик ${mechanic.fullName} назначен на ${order.orderNumber}")
                loadOrders()
            }
        }
    }

    private fun deleteOrder(order: WorkOrderDTO) {
        confirm("Удаление наряда", "Удалить заказ-наряд ${order.orderNumber}?") {
            if (workOrderService.deleteOrder(order.id)) {
                information("✅ Успех", "Наряд удалён")
                loadOrders()
            } else {
                error("Ошибка", "Не удалось удалить наряд")
            }
        }
    }

    private fun statusDisplay(status: String): String = when (status) {
        "CREATED" -> "Создан"
        "IN_PROGRESS" -> "В работе"
        "WAITING_PARTS" -> "Ожидание запчастей"
        "COMPLETED" -> "Выполнен"
        "CLOSED" -> "Закрыт"
        else -> status
    }
}