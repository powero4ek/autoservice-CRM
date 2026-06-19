package com.autoservice.crm.ui.login

import com.autoservice.crm.app.ServiceLocator
import com.autoservice.crm.ui.dashboard.DashboardView
import javafx.animation.FadeTransition
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.effect.DropShadow
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import javafx.util.Duration
import tornadofx.*

class LoginView : View("Авторизация") {

    private val authService = ServiceLocator.authService

    private val username = SimpleStringProperty("")
    private val password = SimpleStringProperty("")
    private val errorMessage = SimpleStringProperty("")

    override val root = borderpane {
        prefWidth = 500.0
        prefHeight = 600.0
        style { backgroundColor += Color.web("#f5f7fa") }

        center = vbox(25) {
            alignment = Pos.CENTER
            padding = Insets(40.0, 50.0, 40.0, 50.0)

            vbox(10) {
                alignment = Pos.CENTER
                label("🔧 CRM Автосервис") {
                    style {
                        fontSize = 28.px
                        fontWeight = FontWeight.BOLD
                        textFill = Color.web("#1a237e")
                    }
                }
                label("Вход в систему") {
                    style {
                        fontSize = 14.px
                        textFill = Color.web("#666666")
                    }
                }
            }

            vbox(20) {
                alignment = Pos.CENTER
                maxWidth = 380.0
                padding = Insets(35.0, 40.0, 35.0, 40.0)
                style {
                    backgroundColor += Color.WHITE
                    backgroundRadius += box(16.px)
                }
                effect = DropShadow().apply {
                    color = Color.web("#000000", 0.1)
                    radius = 20.0
                    offsetY = 8.0
                }

                vbox(8) {
                    label("Логин") {
                        style {
                            fontSize = 13.px
                            fontWeight = FontWeight.BOLD
                            textFill = Color.web("#333333")
                        }
                    }
                    textfield(username) {
                        promptText = "Введите логин"
                        prefHeight = 45.0
                        prefWidth = 300.0
                        padding = Insets(0.0, 15.0, 0.0, 15.0)
                        style {
                            fontSize = 14.px
                            backgroundColor += Color.web("#f8f9fa")
                            backgroundRadius += box(10.px)
                            borderColor += box(Color.web("#e0e0e0"))
                            borderRadius += box(10.px)
                            borderWidth += box(1.px)
                        }
                    }
                }

                vbox(8) {
                    label("Пароль") {
                        style {
                            fontSize = 13.px
                            fontWeight = FontWeight.BOLD
                            textFill = Color.web("#333333")
                        }
                    }
                    passwordfield(password) {
                        promptText = "Введите пароль"
                        prefHeight = 45.0
                        prefWidth = 300.0
                        padding = Insets(0.0, 15.0, 0.0, 15.0)
                        style {
                            fontSize = 14.px
                            backgroundColor += Color.web("#f8f9fa")
                            backgroundRadius += box(10.px)
                            borderColor += box(Color.web("#e0e0e0"))
                            borderRadius += box(10.px)
                            borderWidth += box(1.px)
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

                button("Войти") {
                    prefWidth = 300.0
                    prefHeight = 50.0
                    style {
                        fontSize = 15.px
                        fontWeight = FontWeight.BOLD
                        backgroundColor += Color.web("#1976d2")
                        backgroundRadius += box(12.px)
                        textFill = Color.WHITE
                        cursor = Cursor.HAND
                    }
                    effect = DropShadow().apply {
                        color = Color.web("#1976d2", 0.3)
                        radius = 8.0
                        offsetY = 4.0
                    }
                    action { attemptLogin() }
                }
            }

            // УДАЛЕНО: подпись "Менеджер" полностью
            // Оставлена только техническая информация
            label("Доступные роли: Администратор, Механик") {
                style {
                    fontSize = 11.px
                    textFill = Color.web("#999999")
                }
            }
        }
    }

    private fun attemptLogin() {
        errorMessage.set("")

        val user = username.get().trim()
        val pass = password.get().trim()

        if (user.isBlank() || pass.isBlank()) {
            errorMessage.set("Введите логин и пароль")
            return
        }

        if (authService.login(user, pass)) {
            val fade = FadeTransition(Duration.millis(300.0), root)
            fade.toValue = 0.0
            fade.setOnFinished {
                close()
                DashboardView().openWindow()
            }
            fade.play()
        } else {
            errorMessage.set("Неверный логин или пароль")
        }
    }
}