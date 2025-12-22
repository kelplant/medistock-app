package com.medistock.data.repository

import android.content.Context
import com.medistock.data.db.AppDatabase
import com.medistock.data.entities.Product
import com.medistock.util.AuditLogger
import com.medistock.util.AuthManager
import kotlinx.coroutines.flow.Flow

/**
 * Repository for Product operations with integrated audit logging
 */
class AuditedProductRepository(private val context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val productDao = database.productDao()
    private val auditLogger = AuditLogger.getInstance(context)
    private val authManager = AuthManager.getInstance(context)

    fun getAll(): Flow<List<Product>> = productDao.getAll()

    fun getById(productId: String): Flow<Product?> = productDao.getById(productId)

    fun getProductsForSite(siteId: String): Flow<List<Product>> = productDao.getProductsForSite(siteId)

    fun insert(product: Product): String {
        productDao.insert(product)
        val productId = product.id

        // Log the insert action
        auditLogger.logInsert(
            entityType = "Product",
            entityId = productId,
            newValues = mapOf(
                "name" to product.name,
                "unit" to product.unit,
                "unitVolume" to product.unitVolume.toString(),
                "categoryId" to (product.categoryId?.toString() ?: "null"),
                "marginType" to (product.marginType ?: "null"),
                "marginValue" to (product.marginValue?.toString() ?: "null"),
                "siteId" to product.siteId.toString()
            ),
            username = authManager.getUsername(),
            siteId = product.siteId,
            description = "Product created: ${product.name}"
        )

        return productId
    }

    fun update(oldProduct: Product, newProduct: Product) {
        productDao.update(newProduct)

        // Track changes
        val changes = mutableMapOf<String, Pair<String?, String?>>()

        if (oldProduct.name != newProduct.name) {
            changes["name"] = Pair(oldProduct.name, newProduct.name)
        }
        if (oldProduct.unit != newProduct.unit) {
            changes["unit"] = Pair(oldProduct.unit, newProduct.unit)
        }
        if (oldProduct.unitVolume != newProduct.unitVolume) {
            changes["unitVolume"] = Pair(oldProduct.unitVolume.toString(), newProduct.unitVolume.toString())
        }
        if (oldProduct.categoryId != newProduct.categoryId) {
            changes["categoryId"] = Pair(oldProduct.categoryId?.toString(), newProduct.categoryId?.toString())
        }
        if (oldProduct.marginType != newProduct.marginType) {
            changes["marginType"] = Pair(oldProduct.marginType, newProduct.marginType)
        }
        if (oldProduct.marginValue != newProduct.marginValue) {
            changes["marginValue"] = Pair(oldProduct.marginValue?.toString(), newProduct.marginValue?.toString())
        }
        if (oldProduct.minStock != newProduct.minStock) {
            changes["minStock"] = Pair(oldProduct.minStock?.toString(), newProduct.minStock?.toString())
        }
        if (oldProduct.maxStock != newProduct.maxStock) {
            changes["maxStock"] = Pair(oldProduct.maxStock?.toString(), newProduct.maxStock?.toString())
        }

        if (changes.isNotEmpty()) {
            auditLogger.logUpdate(
                entityType = "Product",
                entityId = newProduct.id,
                changes = changes,
                username = authManager.getUsername(),
                siteId = newProduct.siteId,
                description = "Product updated: ${newProduct.name}"
            )
        }
    }

    fun delete(product: Product) {
        productDao.delete(product)

        auditLogger.logDelete(
            entityType = "Product",
            entityId = product.id,
            oldValues = mapOf(
                "name" to product.name,
                "unit" to product.unit,
                "unitVolume" to product.unitVolume.toString(),
                "categoryId" to (product.categoryId?.toString() ?: "null"),
                "siteId" to product.siteId.toString()
            ),
            username = authManager.getUsername(),
            siteId = product.siteId,
            description = "Product deleted: ${product.name}"
        )
    }
}
