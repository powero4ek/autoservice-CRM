package com.autoservice.crm.ui.client

import com.autoservice.crm.app.ServiceLocator
import com.autoservice.crm.domain.model.ClientDTO
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.effect.DropShadow
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import tornadofx.*

class ClientEditView : View("Клиент") {

    private val clientService = ServiceLocator.clientService

    var client: ClientDTO? = null
        set(value) {
            field = value
            value?.let {
                fullName.set(it.fullName)
                phone.set(it.phone)
                email.set(it.email ?: "")
                notes.set(it.notes ?: "")
            }
        }

    private val fullName = SimpleStringProperty("")
    private val phone = SimpleStringProperty("")
    private val email = SimpleStringProperty("")
    private val notes = SimpleStringProperty("")
    private val errorMessage = SimpleStringProperty("")

    override val root = borderpane {
        prefWidth = 520.0
        prefHeight = 480.0
        style { backgroundColor += Color.web("#f5f7fa") }

        top = hbox(15) {
            alignment = Pos.CENTER_LEFT
            padding = Insets(0.0, 30.0, 0.0, 30.0)
            prefHeight = 60.0
            style {
                backgroundColor += Color.web("#1976d2")
                backgroundRadius += box(0.px, 0.px, 16.px, 16.px)
            }

            label(if (client == null) "✏️ Новый клиент" else "✏️ Редактирование клиента") {
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
                        field("ФИО *") {
                            textfield(fullName) {
                                promptText = "Иванов Иван Иванович"
                                prefWidth = 370.0
                                prefHeight = 42.0
                                padding = Insets(0.0, 12.0, 0.0, 12.0)
                                style {
                                    fontSize = 14.px
                                    backgroundColor += Color.web("#f5f5f5")
                                    backgroundRadius += box(8.px)
                                    borderColor += box(Color.web("#e0e0e0"))
                                    borderRadius += box(8.px)
                                }
                            }
                        }
                        field("Телефон *") {
                            textfield(phone) {
                                promptText = "+7 (900) 123-45-67"
                                prefWidth = 370.0
                                prefHeight = 42.0
                                padding = Insets(0.0, 12.0, 0.0, 12.0)
                                style {
                                    fontSize = 14.px
                                    backgroundColor += Color.web("#f5f5f5")
                                    backgroundRadius += box(8.px)
                                    borderColor += box(Color.web("#e0e0e0"))
                                    borderRadius += box(8.px)
                                }
                            }
                        }
                        field("Email") {
                            textfield(email) {
                                promptText = "client@example.com"
                                prefWidth = 370.0
                                prefHeight = 42.0
                                padding = Insets(0.0, 12.0, 0.0, 12.0)
                                style {
                                    fontSize = 14.px
                                    backgroundColor += Color.web("#f5f5f5")
                                    backgroundRadius += box(8.px)
                                    borderColor += box(Color.web("#e0e0e0"))
                                    borderRadius += box(8.px)
                                }
                            }
                        }
                        field("Примечание") {
                            textarea(notes) {
                                promptText = "Дополнительная информация о клиенте..."
                                prefWidth = 370.0
                                prefHeight = 80.0
                                padding = Insets(8.0, 12.0, 8.0, 12.0)
                                style {
                                    fontSize = 14.px
                                    backgroundColor += Color.web("#f5f5f5")
                                    backgroundRadius += box(8.px)
                                    borderColor += box(Color.web("#e0e0e0"))
                                    borderRadius += box(8.px)
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
                        effect = DropShadow().apply {
                            color = Color.web("#1976d2", 0.3)
                            radius = 6.0
                            offsetY = 2.0
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

    private fun save() {
        errorMessage.set("")
        val name = fullName.get().trim()
        val phoneNum = phone.get().trim()
        val mail = email.get().trim().ifBlank { null }
        val note = notes.get().trim().ifBlank { null }

        if (name.isBlank() || phoneNum.isBlank()) {
            errorMessage.set("ФИО и телефон обязательны для заполнения")
            return
        }

        try {
            if (client == null) {
                clientService.createClient(name, phoneNum, mail, note)
                information("✅ Успех", "Клиент успешно добавлен в систему")
            } else {
                clientService.updateClient(client!!.id, name, phoneNum, mail, note)
                information("✅ Успех", "Данные клиента обновлены")
            }
            close()
        } catch (e: Exception) {
            errorMessage.set("Ошибка при сохранении: " + e.message)
            e.printStackTrace()
        }
    }
}