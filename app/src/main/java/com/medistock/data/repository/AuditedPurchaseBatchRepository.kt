package com.medistock.data.repository

import android.content.Context
import com.medistock.data.db.AppDatabase
import com.medistock.data.entities.PurchaseBatch
import com.medistock.util.AuditLogger
import com.medistock.util.AuthManager

/**
 * Repository for PurchaseBatch operations with integrated audit logging
 */
class AuditedPurchaseBatchRepository(private val context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val purchaseBatchDao = database.purchaseBatchDao()
    private val auditLogger = AuditLogger.getInstance(context)
    private val authManager = AuthManager.getInstance(context)

    fun insert(batch: PurchaseBatch): String {
        purchaseBatchDao.insert(batch)
        val batchId = batch.id

        // Log the insert action
        auditLogger.logInsert(
            entityType = "PurchaseBatch",
            entityId = batchId,
            newValues = mapOf(
                "productId" to batch.productId.toString(),
                "batchNumber" to (batch.batchNumber ?: "auto-generated"),
                "initialQuantity" to batch.initialQuantity.toString(),
                "remainingQuantity" to batch.remainingQuantity.toString(),
                "purchasePrice" to batch.purchasePrice.toString(),
                "supplierName" to batch.supplierName,
                "siteId" to batch.siteId.toString()
            ),
            username = authManager.getUsername(),
            siteId = batch.siteId,
            description = "Purchase batch created: ${batch.supplierName} - Qty: ${batch.initialQuantity}"
        )

        return batchId
    }

    fun update(oldBatch: PurchaseBatch, newBatch: PurchaseBatch) {
        purchaseBatchDao.update(newBatch)

        // Track changes
        val changes = mutableMapOf<String, Pair<String?, String?>>()

        if (oldBatch.remainingQuantity != newBatch.remainingQuantity) {
            changes["remainingQuantity"] = Pair(
                oldBatch.remainingQuantity.toString(),
                newBatch.remainingQuantity.toString()
            )
        }
        if (oldBatch.isExhausted != newBatch.isExhausted) {
            changes["isExhausted"] = Pair(
                oldBatch.isExhausted.toString(),
                newBatch.isExhausted.toString()
            )
        }
        if (oldBatch.supplierName != newBatch.supplierName) {
            changes["supplierName"] = Pair(oldBatch.supplierName, newBatch.supplierName)
        }
        if (oldBatch.purchasePrice != newBatch.purchasePrice) {
            changes["purchasePrice"] = Pair(
                oldBatch.purchasePrice.toString(),
                newBatch.purchasePrice.toString()
            )
        }

        if (changes.isNotEmpty()) {
            auditLogger.logUpdate(
                entityType = "PurchaseBatch",
                entityId = newBatch.id,
                changes = changes,
                username = authManager.getUsername(),
                siteId = newBatch.siteId,
                description = "Purchase batch updated: ${newBatch.batchNumber ?: newBatch.id}"
            )
        }
    }

    fun delete(batch: PurchaseBatch) {
        purchaseBatchDao.deleteById(batch.id)

        auditLogger.logDelete(
            entityType = "PurchaseBatch",
            entityId = batch.id,
            oldValues = mapOf(
                "batchNumber" to (batch.batchNumber ?: batch.id.toString()),
                "remainingQuantity" to batch.remainingQuantity.toString(),
                "supplierName" to batch.supplierName
            ),
            username = authManager.getUsername(),
            siteId = batch.siteId,
            description = "Purchase batch deleted: ${batch.batchNumber ?: batch.id}"
        )
    }
}
