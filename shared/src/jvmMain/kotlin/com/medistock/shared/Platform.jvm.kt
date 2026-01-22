package com.medistock.shared

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.medistock.shared.db.MedistockDatabase

class JvmPlatform : Platform {
    override val name: String = "JVM ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JvmPlatform()

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        MedistockDatabase.Schema.create(driver)
        return driver
    }
}
