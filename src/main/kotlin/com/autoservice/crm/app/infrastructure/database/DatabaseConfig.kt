package com.autoservice.crm.infrastructure.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import java.util.Properties

object DatabaseConfig {

    lateinit var dataSource: HikariDataSource
        private set

    fun init() {
        val props = Properties().apply {
            val stream = Thread.currentThread().contextClassLoader
                .getResourceAsStream("application.properties")
                ?: throw IllegalStateException("application.properties не найден в classpath")
            load(stream)
        }

        val hikariConfig = HikariConfig().apply {
            jdbcUrl = props.getProperty("db.url")
            username = props.getProperty("db.user")
            password = props.getProperty("db.password")
            maximumPoolSize = props.getProperty("db.pool.size", "10").toInt()
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        }

        dataSource = HikariDataSource(hikariConfig)
        Database.connect(dataSource)
    }
}