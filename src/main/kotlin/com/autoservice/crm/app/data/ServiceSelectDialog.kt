package com.autoservice.crm.ui.workorder

import com.autoservice.crm.app.ServiceLocator
import com.autoservice.crm.domain.model.ServiceDTO
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import tornadofx.*

class ServiceSelectDialog : View("Выбор услуги") {

    private val serviceRepository = ServiceLocator.serviceRepository
    private val services = FXCollections.observableArrayList<ServiceDTO>()
    val selectedService = SimpleObjectProperty<ServiceDTO?>(null)

    override val root = vbox(15) {
        padding = Insets(20.0)
        prefWidth = 400.0
        prefHeight = 400.0

        label("Выберите услугу из справочника") {
            style {
                fontSize = 16.px
                fontWeight = FontWeight.BOLD
            }
        }

        listview(services) {
            prefHeight = 250.0
            cellFormat { text = "${it.name} — ${it.defaultPrice} ₽" }
            bindSelected(selectedService)
        }

        hbox(10) {
            alignment = Pos.CENTER_RIGHT
            button("Выбрать") {
                style {
                    backgroundColor += Color.web("#1976d2")
                    textFill = Color.WHITE
                }
                action {
                    if (selectedService.get() != null) close()
                    else warning("Внимание", "Выберите услугу")
                }
            }
            button("Отмена") {
                action { selectedService.set(null); close() }
            }
        }
    }

    override fun onDock() {
        services.setAll(serviceRepository.findAll())
    }
}