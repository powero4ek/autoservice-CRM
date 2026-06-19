package com.autoservice.crm.ui.workorder

import com.autoservice.crm.app.ServiceLocator
import com.autoservice.crm.app.SessionManager
import com.autoservice.crm.domain.model.WorkOrderDTO
import com.autoservice.crm.domain.model.WorkOrderItemDTO
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.effect.DropShadow
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import tornadofx.*

class WorkOrderDetailView : View("Детали заказ-наряда") {

    private val workOrderService = ServiceLocator.workOrderService

    var workOrder: WorkOrderDTO? = null
        set(value) {
            field = value
            value?.let {
                title = "Наряд ${it.orderNumber}"
                loadItems(it.id)
            }
        }

    private val items = FXCollections.observableArrayList<WorkOrderItemDTO>()

    override val root = borderpane {
        prefWidth = 900.0
        prefHeight = 700.0
        style { backgroundColor += Color.web("#f5f7fa") }

        top = vbox {
            prefHeight = 140.0
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
                    action { close() }
                }

                vbox(5) {
                    label("🔧 Заказ-наряд ${workOrder?.orderNumber ?: ""}") {
                        style {
                            fontSize = 20.px
                            fontWeight = FontWeight.BOLD
                            textFill = Color.WHITE
                        }
                    }
                    label("Статус: ${workOrder?.statusDisplay ?: ""}") {
                        style {
                            fontSize = 14.px
                            textFill = Color.web("#ffe0b2")
                        }
                    }
                }

                region { hgrow = javafx.scene.layout.Priority.ALWAYS }

                label("Сумма: ${workOrder?.totalCost ?: 0.0} ₽") {
                    style {
                        fontSize = 18.px
                        fontWeight = FontWeight.BOLD
                        textFill = Color.WHITE
                    }
                }
            }

            hbox(10) {
                alignment = Pos.CENTER_LEFT
                prefHeight = 50.0
                padding = Insets(0.0, 0.0, 15.0, 0.0)

                if (!SessionManager.isMechanic && workOrder?.status != "CLOSED") {
                    button("➕ Добавить услугу") {
                        prefHeight = 36.0
                        style {
                            fontSize = 13.px
                            fontWeight = FontWeight.BOLD
                            backgroundColor += Color.web("#4caf50")
                            backgroundRadius += box(8.px)
                            textFill = Color.WHITE
                            cursor = Cursor.HAND
                        }
                        action { addServiceItem() }
                    }

                    button("🔄 Сменить статус") {
                        prefHeight = 36.0
                        style {
                            fontSize = 13.px
                            fontWeight = FontWeight.BOLD
                            backgroundColor += Color.web("#9c27b0")
                            backgroundRadius += box(8.px)
                            textFill = Color.WHITE
                            cursor = Cursor.HAND
                        }
                        action { changeStatus() }
                    }
                }
            }
        }

        center = stackpane {
            padding = Insets(20.0, 30.0, 20.0, 30.0)

            tableview(items) {
                style {
                    backgroundColor += Color.WHITE
                    backgroundRadius += box(12.px)
                }
                effect = DropShadow().apply {
                    color = Color.web("#000000", 0.08)
                    radius = 10.0
                    offsetY = 3.0
                }

                column<WorkOrderItemDTO, String>("Тип", "itemType").prefWidth = 100.0
                column<WorkOrderItemDTO, String>("Наименование", "displayName").prefWidth = 250.0
                column<WorkOrderItemDTO, Number>("Кол-во", "quantity").prefWidth = 80.0
                column<WorkOrderItemDTO, Number>("Цена", "unitPrice").prefWidth = 100.0
                column<WorkOrderItemDTO, Number>("Сумма", "totalPrice").prefWidth = 100.0
                column<WorkOrderItemDTO, String>("Примечание", "notes").prefWidth = 200.0

                contextmenu {
                    item("🗑️ Удалить").action {
                        selectedItem?.let { deleteItem(it) }
                    }
                }
            }
        }
    }

    private fun loadItems(orderId: Long) {
        items.setAll(workOrderService.getOrderItems(orderId))
    }

    private fun addServiceItem() {
        val wo = workOrder ?: return
        val dialog = find<ServiceSelectDialog>()
        dialog.openModal(block = true)
        val service = dialog.selectedService.get()
        if (service != null) {
            workOrderService.addServiceItem(wo.id, service.id, service.defaultPrice, null)
            information("✅ Успех", "Услуга «${service.name}» добавлена")
            refreshOrder()
        }
    }

    private fun changeStatus() {
        val wo = workOrder ?: return
        val currentStatus = wo.status
        val nextStatus = when (currentStatus) {
            "CREATED" -> "IN_PROGRESS"
            "IN_PROGRESS" -> "WAITING_PARTS"
            "WAITING_PARTS" -> "COMPLETED"
            "COMPLETED" -> "CLOSED"
            else -> null
        }

        if (nextStatus != null) {
            confirm("Смена статуса", "Перевести наряд в статус «${statusDisplay(nextStatus)}»?") {
                if (workOrderService.changeStatus(wo.id, nextStatus)) {
                    information("✅ Успех", "Статус изменён")
                    refreshOrder()
                }
            }
        } else {
            information("Информация", "Наряд уже закрыт")
        }
    }

    private fun deleteItem(item: WorkOrderItemDTO) {
        confirm("Удаление", "Удалить позицию из наряда?") {
            if (workOrderService.removeItem(item.id)) {
                information("✅ Успех", "Позиция удалена")
                refreshOrder()
            }
        }
    }

    private fun refreshOrder() {
        val wo = workOrder ?: return
        loadItems(wo.id)
        val sum = items.sumOf { it.totalPrice }
        workOrderService.updateOrderTotal(wo.id, sum)
        workOrder = workOrderService.getOrderById(wo.id)
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