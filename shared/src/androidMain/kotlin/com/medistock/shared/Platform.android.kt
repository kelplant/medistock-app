package com.medistock.shared

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.medistock.shared.db.MedistockDatabase

class AndroidPlatform : Platform {
    override val name: String = "Android ${android.os.Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        // Pre-migrate the database BEFORE SQLDelight opens it
        // This ensures the schema matches what SQLDelight expects
        preMigrateDatabase(context)

        return AndroidSqliteDriver(
            schema = MedistockDatabase.Schema,
            context = context,
            name = "medistock.db"
        )
    }

    /**
     * Opens the database directly to add missing columns before SQLDelight accesses it.
     * This is necessary because SQLDelight expects the schema to match at open time.
     */
    private fun preMigrateDatabase(context: Context) {
        val dbFile = context.getDatabasePath("medistock.db")
        if (!dbFile.exists()) {
            // Database doesn't exist yet, SQLDelight will create it
            return
        }

        var db: SQLiteDatabase? = null
        try {
            db = SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READWRITE
            )

            // Products: add columns introduced when 'unit' was removed
            addColumnIfNotExists(db, "products", "selected_level", "INTEGER NOT NULL DEFAULT 1")
            addColumnIfNotExists(db, "products", "conversion_factor", "REAL")

            // Add language column to app_users if missing
            addColumnIfNotExists(db, "app_users", "language", "TEXT")

            // Create app_config table if missing (added in a later schema revision)
            createTableIfNotExists(db, "app_config", """
                CREATE TABLE app_config (
                    key TEXT NOT NULL PRIMARY KEY,
                    value TEXT,
                    description TEXT,
                    updated_at INTEGER NOT NULL DEFAULT 0,
                    updated_by TEXT NOT NULL DEFAULT ''
                )
            """.trimIndent())

            // Create suppliers table BEFORE adding supplier_id FK column
            createTableIfNotExists(db, "suppliers", """
                CREATE TABLE suppliers (
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
                )
            """.trimIndent())

            // Add supplier_id column to purchase_batches if missing
            // NOTE: Must be added AFTER suppliers table exists. No FK reference
            // in ALTER TABLE since SQLite doesn't enforce it anyway and it avoids
            // ordering issues.
            addColumnIfNotExists(db, "purchase_batches", "supplier_id", "TEXT")

            // Add base_quantity column to sale_items (level 1 equivalent of display quantity)
            addColumnIfNotExists(db, "sale_items", "base_quantity", "REAL")

            // Add batch_id column to sale_items (user-selected batch for the sale item)
            addColumnIfNotExists(db, "sale_items", "batch_id", "TEXT")

            // Rename current_stock_table -> current_stock (must run BEFORE CREATE TABLE IF NOT EXISTS)
            try {
                db.execSQL("ALTER TABLE current_stock_table RENAME TO current_stock")
                println("Pre-migration: Renamed current_stock_table to current_stock")
            } catch (e: Exception) {
                // Table may not exist yet or already renamed, ignore
            }

            // Create current_stock if missing (new materialized stock view)
            createTableIfNotExists(db, "current_stock", """
                CREATE TABLE current_stock (
                    id TEXT NOT NULL PRIMARY KEY,
                    product_id TEXT NOT NULL,
                    site_id TEXT NOT NULL,
                    quantity REAL NOT NULL DEFAULT 0,
                    last_movement_id TEXT,
                    last_updated_at INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY (product_id) REFERENCES products(id),
                    FOREIGN KEY (site_id) REFERENCES sites(id),
                    FOREIGN KEY (last_movement_id) REFERENCES stock_movements(id)
                )
            """.trimIndent())

            // Create indexes for current_stock
            try {
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_current_stock_product_site ON current_stock(product_id, site_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_current_stock_quantity ON current_stock(quantity)")
            } catch (e: Exception) {
                println("Pre-migration: Failed to create current_stock indexes: ${e.message}")
            }

        } catch (e: Exception) {
            println("⚠️ Pre-migration failed: ${e.message}")
        } finally {
            db?.close()
        }
    }

    private fun createTableIfNotExists(
        db: SQLiteDatabase,
        table: String,
        createSql: String
    ) {
        val cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
            arrayOf(table)
        )
        val exists = cursor.count > 0
        cursor.close()

        if (!exists) {
            try {
                db.execSQL(createSql)
                println("Pre-migration: Created table $table")
            } catch (e: Exception) {
                println("Pre-migration: Failed to create table $table: ${e.message}")
            }
        }
    }

    private fun addColumnIfNotExists(
        db: SQLiteDatabase,
        table: String,
        column: String,
        type: String
    ) {
        val cursor = db.rawQuery("PRAGMA table_info($table)", null)
        var columnExists = false
        while (cursor.moveToNext()) {
            val nameIndex = cursor.getColumnIndex("name")
            if (nameIndex >= 0 && cursor.getString(nameIndex) == column) {
                columnExists = true
                break
            }
        }
        cursor.close()

        if (!columnExists) {
            try {
                db.execSQL("ALTER TABLE $table ADD COLUMN $column $type")
                println("✅ Pre-migration: Added column $column to $table")
            } catch (e: Exception) {
                println("⚠️ Pre-migration: Failed to add column $column to $table: ${e.message}")
            }
        }
    }
}
