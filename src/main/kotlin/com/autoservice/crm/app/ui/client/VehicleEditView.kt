package com.autoservice.crm.ui.vehicle

import com.autoservice.crm.app.ServiceLocator
import com.autoservice.crm.domain.model.VehicleDTO
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.effect.DropShadow
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import tornadofx.*

class VehicleEditView : View("Автомобиль") {

    private val vehicleService = ServiceLocator.vehicleService

    var vehicle: VehicleDTO? = null
    var clientId: Long = 0

    private val brand = SimpleStringProperty("")
    private val model = SimpleStringProperty("")
    private val year = SimpleStringProperty("")
    private val vin = SimpleStringProperty("")
    private val licensePlate = SimpleStringProperty("")
    private val color = SimpleStringProperty("")
    private val mileage = SimpleStringProperty("")
    private val notes = SimpleStringProperty("")
    private val errorMessage = SimpleStringProperty("")

    override val root = borderpane {
        prefWidth = 520.0
        prefHeight = 550.0
        style { backgroundColor += Color.web("#f5f7fa") }

        top = hbox(15) {
            alignment = Pos.CENTER_LEFT
            padding = Insets(0.0, 30.0, 0.0, 30.0)
            prefHeight = 60.0
            style {
                backgroundColor += Color.web("#1976d2")
                backgroundRadius += box(0.px, 0.px, 16.px, 16.px)
            }

            label(if (vehicle == null) "🚗 Новый автомобиль" else "🚗 Редактирование автомобиля") {
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
                maxWidth = 440.0

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
                        field("Марка *") {
                            textfield(brand) {
                                promptText = "Toyota"
                                prefWidth = 370.0
                                prefHeight = 42.0
                            }
                        }
                        field("Модель *") {
                            textfield(model) {
                                promptText = "Camry"
                                prefWidth = 370.0
                                prefHeight = 42.0
                            }
                        }
                        field("Год выпуска") {
                            textfield(year) {
                                promptText = "2020"
                                prefWidth = 370.0
                                prefHeight = 42.0
                                filterInput { it.controlNewText.isInt() }
                            }
                        }
                        field("VIN *") {
                            textfield(vin) {
                                promptText = "XWB3K32EDMA123456"
                                prefWidth = 370.0
                                prefHeight = 42.0
                            }
                        }
                        field("Гос. номер *") {
                            textfield(licensePlate) {
                                promptText = "А123БВ777"
                                prefWidth = 370.0
                                prefHeight = 42.0
                            }
                        }
                        field("Цвет") {
                            textfield(color) {
                                promptText = "Чёрный"
                                prefWidth = 370.0
                                prefHeight = 42.0
                            }
                        }
                        field("Пробег") {
                            textfield(mileage) {
                                promptText = "50000"
                                prefWidth = 370.0
                                prefHeight = 42.0
                                filterInput { it.controlNewText.isInt() }
                            }
                        }
                        field("Примечание") {
                            textarea(notes) {
                                promptText = "Дополнительная информация..."
                                prefWidth = 370.0
                                prefHeight = 60.0
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
                        action { save() }
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
        vehicle?.let {
            brand.set(it.brand)
            model.set(it.model)
            year.set(it.year?.toString() ?: "")
            vin.set(it.vin)
            licensePlate.set(it.licensePlate)
            color.set(it.color ?: "")
            mileage.set(it.mileage?.toString() ?: "")
            notes.set(it.notes ?: "")
        }
    }

    private fun save() {
        errorMessage.set("")

        val brandVal = brand.get().trim()
        val modelVal = model.get().trim()
        val vinVal = vin.get().trim()
        val plateVal = licensePlate.get().trim()

        if (brandVal.isBlank() || modelVal.isBlank() || vinVal.isBlank() || plateVal.isBlank()) {
            errorMessage.set("Марка, модель, VIN и гос. номер обязательны")
            return
        }

        val yearVal = year.get().trim().toIntOrNull()
        val mileageVal = mileage.get().trim().toIntOrNull()

        try {
            if (vehicle == null) {
                vehicleService.createVehicle(
                    clientId,
                    brandVal,
                    modelVal,
                    yearVal,
                    vinVal,        // <-- String, не String?
                    plateVal,      // <-- String, не String?
                    color.get().trim().ifBlank { null },
                    mileageVal,
                    notes.get().trim().ifBlank { null }
                )
                information("✅ Успех", "Автомобиль добавлен")
            } else {
                vehicleService.updateVehicle(
                    vehicle!!.id,
                    brandVal,
                    modelVal,
                    yearVal,
                    vinVal,
                    plateVal,
                    color.get().trim().ifBlank { null },
                    mileageVal,
                    notes.get().trim().ifBlank { null }
                )
                information("✅ Успех", "Автомобиль обновлён")
            }
            close()
        } catch (e: Exception) {
            errorMessage.set("Ошибка: ${e.message}")
            e.printStackTrace()
        }
    }
}