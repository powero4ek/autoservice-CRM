package com.autoservice.crm.ui.workorder

import com.autoservice.crm.app.ServiceLocator
import com.autoservice.crm.domain.model.UserDTO
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import tornadofx.*

class MechanicAssignDialog : View("Назначить механика") {

    private val userRepository = ServiceLocator.userRepository
    private val mechanics = FXCollections.observableArrayList<UserDTO>()
    val selectedMechanic = SimpleObjectProperty<UserDTO?>(null)

    override val root = vbox(15) {
        padding = Insets(20.0)
        prefWidth = 350.0
        prefHeight = 350.0

        label("Выберите механика") {
            style {
                fontSize = 16.px
                fontWeight = FontWeight.BOLD
            }
        }

        listview(mechanics) {
            prefHeight = 220.0
            cellFormat { text = it.fullName }
            bindSelected(selectedMechanic)
        }

        hbox(10) {
            alignment = Pos.CENTER_RIGHT
            button("Назначить") {
                style {
                    backgroundColor += Color.web("#1976d2")
                    textFill = Color.WHITE
                }
                action { close() }
            }
            button("Отмена") {
                action { selectedMechanic.set(null); close() }
            }
        }
    }

    override fun onDock() {
        mechanics.setAll(userRepository.findAllMechanics())
    }
}