package com.autoservice.crm.ui.dashboard

import com.autoservice.crm.app.ServiceLocator
import com.autoservice.crm.app.SessionManager
import com.autoservice.crm.ui.client.ClientListView
import com.autoservice.crm.ui.login.LoginView
import com.autoservice.crm.ui.workorder.WorkOrderListView
import javafx.animation.ScaleTransition
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.effect.DropShadow
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.paint.CycleMethod
import javafx.scene.paint.LinearGradient
import javafx.scene.paint.Stop
import javafx.scene.shape.Rectangle
import javafx.scene.text.FontWeight
import javafx.util.Duration
import tornadofx.*

class DashboardView : View("CRM Автосервис") {

    private val authService = ServiceLocator.authService

    override val root = borderpane {
        prefWidth = 1100.0
        prefHeight = 750.0

        style {
            backgroundColor += LinearGradient(
                0.0, 0.0, 1.0, 1.0, true,
                CycleMethod.NO_CYCLE,
                Stop(0.0, Color.web("#f5f7fa")),
                Stop(1.0, Color.web("#e4e8ec"))
            )
        }

        // ===== ВЕРХНЯЯ ПАНЕЛЬ =====
        top = hbox(15) {
            alignment = Pos.CENTER_LEFT
            padding = Insets(0.0, 30.0, 0.0, 30.0)
            prefHeight = 70.0
            style {
                backgroundColor += Color.web("#1a237e")
                backgroundRadius += box(0.px, 0.px, 16.px, 16.px)
            }
            effect = DropShadow().apply {
                color = Color.web("#000000", 0.15)
                radius = 10.0
                offsetY = 3.0
            }

            label("🚗") { style { fontSize = 28.px } }

            label("CRM Автосервис") {
                style {
                    fontSize = 22.px
                    fontWeight = FontWeight.BOLD
                    textFill = Color.WHITE
                }
            }

            region { hgrow = Priority.ALWAYS }

            hbox(10) {
                alignment = Pos.CENTER
                circle {
                    radius = 18.0
                    fill = Color.web("#3949ab")
                }
                vbox(2) {
                    alignment = Pos.CENTER_LEFT
                    label(SessionManager.currentUser?.fullName ?: "Неизвестно") {
                        style {
                            fontSize = 14.px
                            fontWeight = FontWeight.BOLD
                            textFill = Color.WHITE
                        }
                    }
                    label(SessionManager.currentUser?.roleName ?: "") {
                        style {
                            fontSize = 11.px
                            textFill = Color.web("#b0bec5")
                        }
                    }
                }
            }

            button("Выйти") {
                prefHeight = 36.0
                prefWidth = 90.0
                style {
                    fontSize = 13.px
                    fontWeight = FontWeight.BOLD
                    backgroundColor += Color.web("#d32f2f", 0.9)
                    backgroundRadius += box(8.px)
                    textFill = Color.WHITE
                    cursor = Cursor.HAND
                }
                action {
                    authService.logout()
                    close()
                    LoginView().openWindow()
                }
            }
        }

        // ===== ЦЕНТР — МОДУЛИ =====
        center = scrollpane {
            isFitToWidth = true  // ← ИСПРАВЛЕНО: isFitToWidth вместо fitToWidth
            style { backgroundColor += Color.TRANSPARENT }
            padding = Insets(30.0, 40.0, 30.0, 40.0)

            content = vbox(25) {
                alignment = Pos.TOP_CENTER

                label("Модули системы") {
                    style {
                        fontSize = 24.px
                        fontWeight = FontWeight.BOLD
                        textFill = Color.web("#1a237e")
                    }
                }

                // --- Ряд 1 ---
                hbox(20) {
                    alignment = Pos.CENTER

                    add(createModuleCard(
                        "👥 Клиенты и автомобили",
                        "База клиентов, автопарк, история обслуживания",
                        Color.web("#1976d2")
                    ) {
                        close()
                        ClientListView().openWindow()
                    })

                    if (!SessionManager.isMechanic) {
                        add(createModuleCard(
                            "🔧 Заказ-наряды",
                            "Создание, назначение, статусы, калькуляция",
                            Color.web("#f57c00")
                        ) {
                            close()
                            WorkOrderListView().openWindow()
                        })
                    } else {
                        add(createModuleCard(
                            "🔧 Мои заказ-наряды",
                            "Просмотр назначенных нарядов",
                            Color.web("#f57c00")
                        ) {
                            close()
                            WorkOrderListView().openWindow()
                        })
                    }

                    if (SessionManager.isAdmin) {
                        add(createModuleCard(
                            "📦 Склад запчастей",
                            "Остатки, приход, расход, критические запасы",
                            Color.web("#388e3c")
                        ) { /* TODO */ })
                    } else {
                        region { prefWidth = 280.0; prefHeight = 160.0 }
                    }
                }

                // --- Ряд 2 ---
                hbox(20) {
                    alignment = Pos.CENTER

                    if (SessionManager.isAdmin) {
                        add(createModuleCard(
                            "📋 Справочники",
                            "Услуги, запчасти, поставщики",
                            Color.web("#7b1fa2")
                        ) { /* TODO */ })

                        add(createModuleCard(
                            "📊 Отчёты",
                            "Выручка, загрузка, статистика",
                            Color.web("#00796b")
                        ) { /* TODO */ })

                        add(createModuleCard(
                            "⚙️ Пользователи",
                            "Учётные записи и роли",
                            Color.web("#5d4037")
                        ) { /* TODO */ })
                    }
                }

                region { vgrow = Priority.ALWAYS }
            }
        }
    }

    // ===== КАРТОЧКА МОДУЛЯ =====
    // Обычная функция, возвращающая Node — используем add() в hbox/vbox
    private fun createModuleCard(
        title: String,
        description: String,
        accentColor: Color,
        onClick: () -> Unit
    ): Node {
        return VBox(12.0).apply {
            alignment = Pos.TOP_LEFT
            prefWidth = 280.0
            prefHeight = 160.0
            padding = Insets(20.0)

            style = "-fx-background-color: white; -fx-background-radius: 12px;"

            effect = DropShadow().apply {
                color = Color.web("#000000", 0.08)
                radius = 12.0
                offsetY = 4.0
            }

            // Цветная полоса сверху
            children.add(Rectangle(240.0, 4.0, accentColor))

            // Заголовок
            children.add(javafx.scene.control.Label(title).apply {
                style = "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333333;"
            })

            // Описание
            children.add(javafx.scene.control.Label(description).apply {
                style = "-fx-font-size: 12px; -fx-text-fill: #666666;"
                isWrapText = true
                prefWidth = 240.0
            })

            // Растягивающийся регион
            children.add(javafx.scene.layout.Region().apply {
                VBox.setVgrow(this, Priority.ALWAYS)
            })

            // Стрелка
            children.add(javafx.scene.layout.HBox().apply {
                alignment = Pos.CENTER_RIGHT
                children.add(javafx.scene.control.Label("→ Перейти").apply {
                    style = "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: ${toHex(accentColor)};"
                })
            })

            cursor = Cursor.HAND

            // Анимация наведения
            hoverProperty().addListener { _, _, hovered ->
                ScaleTransition(Duration.millis(200.0), this).apply {
                    toX = if (hovered) 1.03 else 1.0
                    toY = if (hovered) 1.03 else 1.0
                    play()
                }
                effect = DropShadow().apply {
                    color = Color.web("#000000", if (hovered) 0.15 else 0.08)
                    radius = if (hovered) 16.0 else 12.0
                    offsetY = if (hovered) 6.0 else 4.0
                }
            }

            setOnMouseClicked { onClick() }
        }
    }

    private fun toHex(color: Color): String {
        return String.format("#%02X%02X%02X",
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt())
    }
}