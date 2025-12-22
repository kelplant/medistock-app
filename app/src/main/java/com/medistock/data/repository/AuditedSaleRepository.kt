package com.medistock.data.repository

import android.content.Context
import com.medistock.data.db.AppDatabase
import com.medistock.data.entities.Sale
import com.medistock.util.AuditLogger
import com.medistock.util.AuthManager

/**
 * Repository for Sale operations with integrated audit logging
 */
class AuditedSaleRepository(private val context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val saleDao = database.saleDao()
    private val auditLogger = AuditLogger.getInstance(context)
    private val authManager = AuthManager.getInstance(context)

    fun insert(sale: Sale): String {
        saleDao.insert(sale)
        val saleId = sale.id

        // Log the insert action
        auditLogger.logInsert(
            entityType = "Sale",
            entityId = saleId,
            newValues = mapOf(
                "customerName" to sale.customerName,
                "customerId" to (sale.customerId?.toString() ?: "null"),
                "totalAmount" to sale.totalAmount.toString(),
                "siteId" to sale.siteId.toString()
            ),
            username = authManager.getUsername(),
            siteId = sale.siteId,
            description = "Sale created for customer: ${sale.customerName}"
        )

        return saleId
    }

    fun update(oldSale: Sale, newSale: Sale) {
        saleDao.update(newSale)

        // Track changes
        val changes = mutableMapOf<String, Pair<String?, String?>>()

        if (oldSale.customerName != newSale.customerName) {
            changes["customerName"] = Pair(oldSale.customerName, newSale.customerName)
        }
        if (oldSale.customerId != newSale.customerId) {
            changes["customerId"] = Pair(oldSale.customerId?.toString(), newSale.customerId?.toString())
        }
        if (oldSale.totalAmount != newSale.totalAmount) {
            changes["totalAmount"] = Pair(oldSale.totalAmount.toString(), newSale.totalAmount.toString())
        }

        if (changes.isNotEmpty()) {
            auditLogger.logUpdate(
                entityType = "Sale",
                entityId = newSale.id,
                changes = changes,
                username = authManager.getUsername(),
                siteId = newSale.siteId,
                description = "Sale updated for customer: ${newSale.customerName}"
            )
        }
    }

    fun delete(sale: Sale) {
        saleDao.delete(sale)

        auditLogger.logDelete(
            entityType = "Sale",
            entityId = sale.id,
            oldValues = mapOf(
                "customerName" to sale.customerName,
                "totalAmount" to sale.totalAmount.toString()
            ),
            username = authManager.getUsername(),
            siteId = sale.siteId,
            description = "Sale deleted for customer: ${sale.customerName}"
        )
    }
}
