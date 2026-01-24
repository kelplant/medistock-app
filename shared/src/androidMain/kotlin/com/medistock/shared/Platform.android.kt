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

            // Add language column to app_users if missing
            addColumnIfNotExists(db, "app_users", "language", "TEXT")

        } catch (e: Exception) {
            println("⚠️ Pre-migration failed: ${e.message}")
        } finally {
            db?.close()
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
