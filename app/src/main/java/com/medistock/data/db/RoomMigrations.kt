package com.medistock.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database migrations.
 *
 * IMPORTANT: When modifying the database schema:
 * 1. Increment the version number in AppDatabase
 * 2. Create a new migration here (e.g., MIGRATION_13_14)
 * 3. Add it to ALL_MIGRATIONS list
 *
 * This prevents data loss during app updates.
 */
object RoomMigrations {

    /**
     * Migration from version 12 to 13
     * Added sync_queue table for offline sync
     */
    val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create sync_queue table if not exists
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS sync_queue (
                    id TEXT NOT NULL PRIMARY KEY,
                    tableName TEXT NOT NULL,
                    recordId TEXT NOT NULL,
                    operation TEXT NOT NULL,
                    data TEXT NOT NULL,
                    createdAt INTEGER NOT NULL DEFAULT 0,
                    retryCount INTEGER NOT NULL DEFAULT 0,
                    lastError TEXT,
                    status TEXT NOT NULL DEFAULT 'pending'
                )
            """.trimIndent())
        }
    }

    /**
     * Migration from version 11 to 12
     * Added product_transfers table
     */
    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS product_transfers (
                    id TEXT NOT NULL PRIMARY KEY,
                    productId TEXT NOT NULL,
                    fromSiteId TEXT NOT NULL,
                    toSiteId TEXT NOT NULL,
                    quantity REAL NOT NULL,
                    status TEXT NOT NULL DEFAULT 'pending',
                    notes TEXT,
                    createdAt INTEGER NOT NULL DEFAULT 0,
                    updatedAt INTEGER NOT NULL DEFAULT 0,
                    createdBy TEXT NOT NULL DEFAULT '',
                    updatedBy TEXT NOT NULL DEFAULT ''
                )
            """.trimIndent())
        }
    }

    /**
     * Migration from version 10 to 11
     * Added audit_history table
     */
    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS audit_history (
                    id TEXT NOT NULL PRIMARY KEY,
                    tableName TEXT NOT NULL,
                    recordId TEXT NOT NULL,
                    action TEXT NOT NULL,
                    oldValues TEXT,
                    newValues TEXT,
                    userId TEXT NOT NULL,
                    timestamp INTEGER NOT NULL
                )
            """.trimIndent())

            // Create index for faster queries
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_audit_table ON audit_history(tableName)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_history(timestamp)")
        }
    }

    /**
     * Migration from version 9 to 10
     * Added packaging_types table
     */
    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS packaging_types (
                    id TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    level1Name TEXT NOT NULL,
                    level2Name TEXT,
                    level2Quantity INTEGER,
                    createdAt INTEGER NOT NULL DEFAULT 0,
                    updatedAt INTEGER NOT NULL DEFAULT 0,
                    createdBy TEXT NOT NULL DEFAULT '',
                    updatedBy TEXT NOT NULL DEFAULT ''
                )
            """.trimIndent())

            // Add packaging columns to products if not exist
            db.execSQL("ALTER TABLE products ADD COLUMN packagingTypeId TEXT")
            db.execSQL("ALTER TABLE products ADD COLUMN selectedLevel INTEGER")
            db.execSQL("ALTER TABLE products ADD COLUMN conversionFactor REAL")
        }
    }

    /**
     * List of all migrations in order.
     * Add new migrations here when created.
     */
    val ALL_MIGRATIONS = arrayOf(
        MIGRATION_9_10,
        MIGRATION_10_11,
        MIGRATION_11_12,
        MIGRATION_12_13
    )
}
