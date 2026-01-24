package com.medistock.shared

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.driver.native.wrapConnection
import co.touchlab.sqliter.DatabaseConfiguration
import com.medistock.shared.db.MedistockDatabase
import platform.UIKit.UIDevice

class IOSPlatform : Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = MedistockDatabase.Schema,
            name = "medistock.db",
            onConfiguration = { config ->
                config.copy(
                    extendedConfig = DatabaseConfiguration.Extended(
                        foreignKeyConstraints = true
                    ),
                    // Run pre-migration to add missing columns
                    create = { connection ->
                        // First run standard create
                        wrapConnection(connection) { driver ->
                            MedistockDatabase.Schema.create(driver)
                        }
                    },
                    upgrade = { connection, oldVersion, newVersion ->
                        // Add language column if upgrading from older version
                        try {
                            connection.rawExecSql("ALTER TABLE app_users ADD COLUMN language TEXT")
                            println("✅ Added column language to app_users")
                        } catch (e: Exception) {
                            // Column might already exist, ignore
                        }
                        wrapConnection(connection) { driver ->
                            MedistockDatabase.Schema.migrate(driver, oldVersion.toLong(), newVersion.toLong())
                        }
                    }
                )
            }
        )
    }
}
