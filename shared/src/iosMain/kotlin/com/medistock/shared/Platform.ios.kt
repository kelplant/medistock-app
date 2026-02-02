package com.medistock.shared

import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
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

    /**
     * LOCAL SQLite schema wrapper (NOT related to Supabase schema_version).
     *
     * Bumps the local SQLite PRAGMA user_version from 1 to 2 so that
     * existing on-device databases trigger the upgrade path where we add
     * missing tables/columns (app_config, suppliers, purchase_batches.supplier_id).
     *
     * Local SQLite version history:
     * - Version 1: Original schema created by SQLDelight
     * - Version 2: Added app_config, suppliers tables + app_users.language column
     * - Version 3: Added products.selected_level + products.conversion_factor columns
     * - Version 4: Added sale_items.base_quantity column
     * - Version 5: Added sale_items.batch_id column (user-selected batch)
     */
    private val migratingSchema = object : SqlSchema<QueryResult.Value<Unit>> {
        override val version: Long get() = 5
        override fun create(driver: SqlDriver) = MedistockDatabase.Schema.create(driver)
        override fun migrate(
            driver: SqlDriver,
            oldVersion: Long,
            newVersion: Long,
            vararg callbacks: AfterVersion
        ) = MedistockDatabase.Schema.migrate(driver, oldVersion, newVersion, *callbacks)
    }

    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = migratingSchema,
            name = "medistock.db",
            onConfiguration = { config ->
                config.copy(
                    extendedConfig = DatabaseConfiguration.Extended(
                        foreignKeyConstraints = true
                    ),
                    create = { connection ->
                        wrapConnection(connection) { driver ->
                            MedistockDatabase.Schema.create(driver)
                        }
                    },
                    upgrade = { connection, oldVersion, newVersion ->
                        // Add missing columns and tables for schema evolution (v1 → v2)
                        val migrations = listOf(
                            // Rename current_stock_table -> current_stock (must run BEFORE CREATE TABLE IF NOT EXISTS)
                            "ALTER TABLE current_stock_table RENAME TO current_stock",
                            // Products: add columns introduced when 'unit' was removed
                            "ALTER TABLE products ADD COLUMN selected_level INTEGER NOT NULL DEFAULT 1",
                            "ALTER TABLE products ADD COLUMN conversion_factor REAL",
                            "ALTER TABLE app_users ADD COLUMN language TEXT",
                            """CREATE TABLE IF NOT EXISTS app_config (
                                key TEXT NOT NULL PRIMARY KEY,
                                value TEXT,
                                description TEXT,
                                updated_at INTEGER NOT NULL DEFAULT 0,
                                updated_by TEXT NOT NULL DEFAULT ''
                            )""",
                            """CREATE TABLE IF NOT EXISTS suppliers (
                                id TEXT NOT NULL PRIMARY KEY,
                                name TEXT NOT NULL,
                                phone TEXT,
                                email TEXT,
                                address TEXT,
                                notes TEXT,
                                is_active INTEGER NOT NULL DEFAULT 1,
                                created_at INTEGER NOT NULL DEFAULT 0,
                                updated_at INTEGER NOT NULL DEFAULT 0,
                                created_by TEXT NOT NULL DEFAULT '',
                                updated_by TEXT NOT NULL DEFAULT ''
                            )""",
                            // Add supplier_id to purchase_batches (added at the end to match ALTER TABLE behavior)
                            "ALTER TABLE purchase_batches ADD COLUMN supplier_id TEXT",
                            // Add base_quantity to sale_items (level 1 equivalent of display quantity)
                            "ALTER TABLE sale_items ADD COLUMN base_quantity REAL",
                            // Add batch_id to sale_items (user-selected batch for the sale item)
                            "ALTER TABLE sale_items ADD COLUMN batch_id TEXT",
                            // Create current_stock (materialized stock view)
                            """CREATE TABLE IF NOT EXISTS current_stock (
                                id TEXT NOT NULL PRIMARY KEY,
                                product_id TEXT NOT NULL,
                                site_id TEXT NOT NULL,
                                quantity REAL NOT NULL DEFAULT 0,
                                last_movement_id TEXT,
                                last_updated_at INTEGER NOT NULL DEFAULT 0,
                                FOREIGN KEY (product_id) REFERENCES products(id),
                                FOREIGN KEY (site_id) REFERENCES sites(id),
                                FOREIGN KEY (last_movement_id) REFERENCES stock_movements(id)
                            )""",
                            "CREATE UNIQUE INDEX IF NOT EXISTS idx_current_stock_product_site ON current_stock(product_id, site_id)",
                            "CREATE INDEX IF NOT EXISTS idx_current_stock_quantity ON current_stock(quantity)",
                        )
                        for (sql in migrations) {
                            try {
                                connection.rawExecSql(sql)
                            } catch (_: Exception) {
                                // Column/table might already exist, ignore
                            }
                        }
                    }
                )
            }
        )
    }
}
