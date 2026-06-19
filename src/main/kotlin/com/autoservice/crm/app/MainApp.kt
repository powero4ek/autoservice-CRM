package com.autoservice.crm.app

import com.autoservice.crm.infrastructure.database.DatabaseConfig
import com.autoservice.crm.ui.login.LoginView
import org.flywaydb.core.Flyway
import tornadofx.App
import tornadofx.launch

class MainApp : App(LoginView::class) {

    init {
        DatabaseConfig.init()
        val flyway = Flyway.configure()
            .dataSource(DatabaseConfig.dataSource)
            .locations("classpath:db/migration")
            .load()
        flyway.migrate()
    }
}


fun main(args: Array<String>) {
    launch<MainApp>(args)
}