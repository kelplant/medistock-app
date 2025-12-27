package com.medistock.data.db

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Installs SQLite triggers to ensure every INSERT/UPDATE/DELETE
 * is mirrored into the audit_history table, even when DAOs are
 * called directly without the higher-level audited repositories.
 */
object AuditTriggerInitializer {

    private data class TableConfig(
        val tableName: String,
        val columns: List<String>,
        val idColumn: String = "id",
        val siteIdColumn: String? = null,
        val userColumns: List<String> = emptyList()
    )

    private val tableConfigs = listOf(
        TableConfig(
            tableName = "categories",
            columns = listOf("id", "name", "createdAt", "updatedAt", "createdBy", "updatedBy"),
            userColumns = listOf("updatedBy", "createdBy")
        ),
        TableConfig(
            tableName = "products",
            columns = listOf(
                "id",
                "name",
                "unit",
                "unitVolume",
                "packagingTypeId",
                "selectedLevel",
                "conversionFactor",
                "categoryId",
                "marginType",
                "marginValue",
                "siteId",
                "minStock",
                "maxStock",
                "createdAt",
                "updatedAt",
                "createdBy",
                "updatedBy"
            ),
            siteIdColumn = "siteId",
            userColumns = listOf("updatedBy", "createdBy")
        ),
        TableConfig(
            tableName = "product_prices",
            columns = listOf(
                "id",
                "productId",
                "effectiveDate",
                "purchasePrice",
                "sellingPrice",
                "source",
                "createdAt",
                "updatedAt",
                "createdBy",
                "updatedBy"
            ),
            userColumns = listOf("updatedBy", "createdBy")
        ),
        TableConfig(
            tableName = "product_sales",
            columns = listOf(
                "id",
                "productId",
                "quantity",
                "priceAtSale",
                "farmerName",
                "date",
                "siteId",
                "createdAt",
                "createdBy"
            ),
            siteIdColumn = "siteId",
            userColumns = listOf("createdBy")
        ),
        TableConfig(
            tableName = "product_transfers",
            columns = listOf(
                "id",
                "productId",
                "quantity",
                "fromSiteId",
                "toSiteId",
                "date",
                "notes",
                "createdAt",
                "updatedAt",
                "createdBy",
                "updatedBy"
            ),
            siteIdColumn = "fromSiteId",
            userColumns = listOf("updatedBy", "createdBy")
        ),
        TableConfig(
            tableName = "stock_movements",
            columns = listOf(
                "id",
                "productId",
                "type",
                "quantity",
                "date",
                "purchasePriceAtMovement",
                "sellingPriceAtMovement",
                "siteId",
                "createdAt",
                "createdBy"
            ),
            siteIdColumn = "siteId",
            userColumns = listOf("createdBy")
        ),
        TableConfig(
            tableName = "purchase_batches",
            columns = listOf(
                "id",
                "productId",
                "siteId",
                "batchNumber",
                "purchaseDate",
                "initialQuantity",
                "remainingQuantity",
                "purchasePrice",
                "supplierName",
                "expiryDate",
                "isExhausted",
                "createdAt",
                "updatedAt",
                "createdBy",
                "updatedBy"
            ),
            siteIdColumn = "siteId",
            userColumns = listOf("updatedBy", "createdBy")
        ),
        TableConfig(
            tableName = "inventories",
            columns = listOf(
                "id",
                "productId",
                "siteId",
                "countDate",
                "countedQuantity",
                "theoreticalQuantity",
                "discrepancy",
                "reason",
                "countedBy",
                "notes",
                "createdAt",
                "createdBy"
            ),
            siteIdColumn = "siteId",
            userColumns = listOf("createdBy")
        ),
        TableConfig(
            tableName = "sales",
            columns = listOf(
                "id",
                "customerName",
                "customerId",
                "date",
                "totalAmount",
                "siteId",
                "createdAt",
                "createdBy"
            ),
            siteIdColumn = "siteId",
            userColumns = listOf("createdBy")
        ),
        TableConfig(
            tableName = "sale_items",
            columns = listOf(
                "id",
                "saleId",
                "productId",
                "productName",
                "unit",
                "quantity",
                "pricePerUnit",
                "subtotal"
            )
        ),
        TableConfig(
            tableName = "sale_batch_allocations",
            columns = listOf(
                "id",
                "saleItemId",
                "batchId",
                "quantityAllocated",
                "purchasePriceAtAllocation",
                "createdAt"
            )
        ),
        TableConfig(
            tableName = "app_users",
            columns = listOf(
                "id",
                "username",
                "password",
                "full_name",
                "is_admin",
                "is_active",
                "created_at",
                "updated_at",
                "created_by",
                "updated_by"
            ),
            userColumns = listOf("updated_by", "created_by")
        ),
        TableConfig(
            tableName = "user_permissions",
            columns = listOf(
                "id",
                "user_id",
                "module",
                "can_view",
                "can_create",
                "can_edit",
                "can_delete",
                "created_at",
                "updated_at",
                "created_by",
                "updated_by"
            ),
            userColumns = listOf("updated_by", "created_by")
        ),
        TableConfig(
            tableName = "customers",
            columns = listOf(
                "id",
                "name",
                "phone",
                "address",
                "notes",
                "siteId",
                "createdAt",
                "createdBy"
            ),
            siteIdColumn = "siteId",
            userColumns = listOf("createdBy")
        ),
        TableConfig(
            tableName = "packaging_types",
            columns = listOf(
                "id",
                "name",
                "level1Name",
                "level2Name",
                "defaultConversionFactor",
                "isActive",
                "displayOrder",
                "createdAt",
                "updatedAt",
                "createdBy",
                "updatedBy"
            ),
            userColumns = listOf("updatedBy", "createdBy")
        ),
        TableConfig(
            tableName = "sites",
            columns = listOf("id", "name", "createdAt", "updatedAt", "createdBy", "updatedBy"),
            userColumns = listOf("updatedBy", "createdBy")
        )
    )

    fun createCallback(): RoomDatabase.Callback {
        return object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                createTriggers(db)
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                createTriggers(db)
            }
        }
    }

    private fun createTriggers(db: SupportSQLiteDatabase) {
        tableConfigs.forEach { config ->
            db.execSQL(buildInsertTrigger(config))
            db.execSQL(buildUpdateTrigger(config))
            db.execSQL(buildDeleteTrigger(config))
        }
    }

    private fun buildInsertTrigger(config: TableConfig): String {
        return """
            CREATE TRIGGER IF NOT EXISTS audit_${config.tableName}_insert
            AFTER INSERT ON ${config.tableName}
            BEGIN
                INSERT INTO audit_history (entity_type, entity_id, action_type, field_name, old_value, new_value, changed_by, site_id, description, changed_at)
                VALUES (
                    '${config.tableName}',
                    CAST(NEW."${config.idColumn}" AS TEXT),
                    'INSERT',
                    'ALL_FIELDS',
                    NULL,
                    ${buildJsonObject("NEW", config.columns)},
                    ${buildUserExpr("NEW", config.userColumns)},
                    ${buildSiteExpr("NEW", config.siteIdColumn)},
                    'Room trigger audit',
                    (strftime('%s','now') * 1000)
                );
            END;
        """.trimIndent()
    }

    private fun buildUpdateTrigger(config: TableConfig): String {
        return """
            CREATE TRIGGER IF NOT EXISTS audit_${config.tableName}_update
            AFTER UPDATE ON ${config.tableName}
            BEGIN
                INSERT INTO audit_history (entity_type, entity_id, action_type, field_name, old_value, new_value, changed_by, site_id, description, changed_at)
                VALUES (
                    '${config.tableName}',
                    CAST(NEW."${config.idColumn}" AS TEXT),
                    'UPDATE',
                    'ALL_FIELDS',
                    ${buildJsonObject("OLD", config.columns)},
                    ${buildJsonObject("NEW", config.columns)},
                    ${buildUserExpr("NEW", config.userColumns)},
                    ${buildSiteExpr("NEW", config.siteIdColumn)},
                    'Room trigger audit',
                    (strftime('%s','now') * 1000)
                );
            END;
        """.trimIndent()
    }

    private fun buildDeleteTrigger(config: TableConfig): String {
        return """
            CREATE TRIGGER IF NOT EXISTS audit_${config.tableName}_delete
            AFTER DELETE ON ${config.tableName}
            BEGIN
                INSERT INTO audit_history (entity_type, entity_id, action_type, field_name, old_value, new_value, changed_by, site_id, description, changed_at)
                VALUES (
                    '${config.tableName}',
                    CAST(OLD."${config.idColumn}" AS TEXT),
                    'DELETE',
                    'ALL_FIELDS',
                    ${buildJsonObject("OLD", config.columns)},
                    NULL,
                    ${buildUserExpr("OLD", config.userColumns)},
                    ${buildSiteExpr("OLD", config.siteIdColumn)},
                    'Room trigger audit',
                    (strftime('%s','now') * 1000)
                );
            END;
        """.trimIndent()
    }

    private fun buildJsonObject(alias: String, columns: List<String>): String {
        if (columns.isEmpty()) return "NULL"
        val pairs = columns.joinToString(", ") { "'$it', $alias.\"$it\"" }
        return "json_object($pairs)"
    }

    private fun buildUserExpr(alias: String, userColumns: List<String>): String {
        if (userColumns.isEmpty()) return "'system'"
        val coalesceArgs = userColumns.joinToString(", ") { "$alias.\"$it\"" }
        return "COALESCE($coalesceArgs, 'system')"
    }

    private fun buildSiteExpr(alias: String, siteIdColumn: String?): String {
        return if (siteIdColumn != null) "$alias.\"$siteIdColumn\"" else "NULL"
    }
}
