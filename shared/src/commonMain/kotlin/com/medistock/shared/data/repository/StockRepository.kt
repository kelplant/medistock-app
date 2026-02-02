package com.medistock.shared.data.repository

import com.medistock.shared.db.MedistockDatabase
import com.medistock.shared.domain.model.CurrentStock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Repository for accessing and managing stock data.
 *
 * Uses the materialized current_stock for O(1) lookups instead of
 * computing stock from stock_movements aggregation.
 */
@OptIn(ExperimentalUuidApi::class)
class StockRepository(private val database: MedistockDatabase) {

    private val queries = database.medistockQueries

    // ============================================
    // READ OPERATIONS - O(1) lookups from current_stock
    // ============================================

    /**
     * Get the stock quantity for a specific product at a specific site.
     * O(1) lookup from the materialized current_stock.
     */
    suspend fun getStockQuantity(productId: String, siteId: String): Double = withContext(Dispatchers.Default) {
        queries.getStockQuantity(productId, siteId).executeAsOneOrNull() ?: 0.0
    }

    /**
     * Get current stock with full product details for a product at a site.
     * Uses a single JOIN query — no N+1.
     */
    suspend fun getCurrentStockByProductAndSite(productId: String, siteId: String): CurrentStock? = withContext(Dispatchers.Default) {
        queries.getStockForProductAndSite(productId, siteId).executeAsOneOrNull()?.let { row ->
            CurrentStock(
                productId = row.product_id,
                productName = row.product_name ?: "",
                unit = row.unit ?: "",
                categoryName = row.category_name ?: "",
                siteId = row.site_id,
                siteName = row.site_name ?: "",
                quantityOnHand = row.quantity,
                minStock = row.min_stock ?: 0.0,
                maxStock = row.max_stock ?: 0.0
            )
        }
    }

    /**
     * Get all current stock entries for a site.
     * Uses a single JOIN query that includes min_stock/max_stock — no N+1.
     */
    suspend fun getCurrentStockForSite(siteId: String): List<CurrentStock> = withContext(Dispatchers.Default) {
        val site = queries.getSiteById(siteId).executeAsOneOrNull()
        val siteName = site?.name ?: ""

        queries.getStockForSite(siteId).executeAsList().map { row ->
            CurrentStock(
                productId = row.product_id,
                productName = row.product_name ?: "",
                unit = row.unit ?: "",
                categoryName = row.category_name ?: "",
                siteId = row.site_id,
                siteName = siteName,
                quantityOnHand = row.quantity,
                minStock = row.min_stock ?: 0.0,
                maxStock = row.max_stock ?: 0.0
            )
        }
    }

    /**
     * Get all current stock entries across all sites.
     * Uses a single JOIN query that includes min_stock/max_stock — no N+1.
     */
    suspend fun getAllCurrentStock(): List<CurrentStock> = withContext(Dispatchers.Default) {
        queries.getAllStock().executeAsList().map { row ->
            CurrentStock(
                productId = row.product_id,
                productName = row.product_name ?: "",
                unit = row.unit ?: "",
                categoryName = row.category_name ?: "",
                siteId = row.site_id,
                siteName = row.site_name ?: "",
                quantityOnHand = row.quantity,
                minStock = row.min_stock ?: 0.0,
                maxStock = row.max_stock ?: 0.0
            )
        }
    }

    /**
     * Observe current stock for a site as a Flow.
     * Uses the materialized current_stock with min_stock/max_stock in the JOIN.
     */
    fun observeCurrentStockForSite(siteId: String): Flow<List<CurrentStock>> {
        return queries.getStockForSite(siteId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list ->
                val site = queries.getSiteById(siteId).executeAsOneOrNull()
                val siteName = site?.name ?: ""
                list.map { row ->
                    CurrentStock(
                        productId = row.product_id,
                        productName = row.product_name ?: "",
                        unit = row.unit ?: "",
                        categoryName = row.category_name ?: "",
                        siteId = row.site_id,
                        siteName = siteName,
                        quantityOnHand = row.quantity,
                        minStock = row.min_stock ?: 0.0,
                        maxStock = row.max_stock ?: 0.0
                    )
                }
            }
    }

    /**
     * Observe all current stock across all sites as a Flow.
     * Uses the materialized current_stock with min_stock/max_stock in the JOIN.
     */
    fun observeAllCurrentStock(): Flow<List<CurrentStock>> {
        return queries.getAllStock()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list ->
                list.map { row ->
                    CurrentStock(
                        productId = row.product_id,
                        productName = row.product_name ?: "",
                        unit = row.unit ?: "",
                        categoryName = row.category_name ?: "",
                        siteId = row.site_id,
                        siteName = row.site_name ?: "",
                        quantityOnHand = row.quantity,
                        minStock = row.min_stock ?: 0.0,
                        maxStock = row.max_stock ?: 0.0
                    )
                }
            }
    }

    // ============================================
    // WRITE OPERATIONS - Update current_stock
    // ============================================

    /**
     * Upsert stock entry - insert or update if exists.
     * Use this when you want to set an absolute quantity.
     *
     * @param productId The product ID
     * @param siteId The site ID
     * @param quantity The absolute quantity to set
     * @param lastMovementId Optional reference to the movement that caused this update
     */
    suspend fun upsertStock(
        productId: String,
        siteId: String,
        quantity: Double,
        lastMovementId: String? = null
    ) = withContext(Dispatchers.Default) {
        val now = Clock.System.now().toEpochMilliseconds()
        val id = Uuid.random().toString()
        // Parameters: productId (for lookup), siteId (for lookup), id (new id if not exists),
        //             productId, siteId, quantity, lastMovementId, lastUpdatedAt
        queries.upsertStock(
            productId,  // for COALESCE lookup
            siteId,     // for COALESCE lookup
            id,         // new id if not exists
            productId,
            siteId,
            quantity,
            lastMovementId,
            now
        )
    }

    /**
     * Update stock by adding a delta (positive or negative).
     * Uses a transaction for atomicity — no race condition between check and write.
     *
     * @param productId The product ID
     * @param siteId The site ID
     * @param delta The quantity to add (positive) or subtract (negative)
     * @param lastMovementId Optional reference to the movement that caused this update
     * @return true always (entry is created if it doesn't exist)
     */
    suspend fun updateStockDelta(
        productId: String,
        siteId: String,
        delta: Double,
        lastMovementId: String? = null
    ): Boolean = withContext(Dispatchers.Default) {
        val now = Clock.System.now().toEpochMilliseconds()
        database.transaction {
            val existing = queries.getStockEntry(productId, siteId).executeAsOneOrNull()
            if (existing != null) {
                queries.updateStockDelta(delta, lastMovementId, now, productId, siteId)
            } else {
                val id = Uuid.random().toString()
                queries.insertStock(id, productId, siteId, delta, lastMovementId, now)
            }
        }
        true
    }

    /**
     * Set absolute stock quantity (for inventory adjustments).
     * Uses a transaction for atomicity — creates the entry if it doesn't exist.
     *
     * @param productId The product ID
     * @param siteId The site ID
     * @param quantity The absolute quantity to set
     * @param lastMovementId Optional reference to the movement that caused this update
     * @return true always (entry is created if it doesn't exist)
     */
    suspend fun setStockQuantity(
        productId: String,
        siteId: String,
        quantity: Double,
        lastMovementId: String? = null
    ): Boolean = withContext(Dispatchers.Default) {
        val now = Clock.System.now().toEpochMilliseconds()
        database.transaction {
            val existing = queries.getStockEntry(productId, siteId).executeAsOneOrNull()
            if (existing != null) {
                queries.setStockQuantity(quantity, lastMovementId, now, productId, siteId)
            } else {
                val id = Uuid.random().toString()
                queries.insertStock(id, productId, siteId, quantity, lastMovementId, now)
            }
        }
        true
    }

    /**
     * Insert a new stock entry.
     * Use this when creating initial stock for a product at a site.
     *
     * @param productId The product ID
     * @param siteId The site ID
     * @param quantity The initial quantity
     * @param lastMovementId Optional reference to the movement that caused this
     */
    suspend fun insertStock(
        productId: String,
        siteId: String,
        quantity: Double,
        lastMovementId: String? = null
    ) = withContext(Dispatchers.Default) {
        val now = Clock.System.now().toEpochMilliseconds()
        val id = Uuid.random().toString()
        queries.insertStock(
            id,
            productId,
            siteId,
            quantity,
            lastMovementId,
            now
        )
    }

    /**
     * Delete a stock entry for a product at a site.
     */
    suspend fun deleteStock(productId: String, siteId: String) = withContext(Dispatchers.Default) {
        queries.deleteStock(productId, siteId)
    }

    /**
     * Check if a stock entry exists for a product at a site.
     */
    suspend fun hasStockEntry(productId: String, siteId: String): Boolean = withContext(Dispatchers.Default) {
        queries.getStockEntry(productId, siteId).executeAsOneOrNull() != null
    }

    /**
     * Ensure a stock entry exists for a product at a site.
     * Creates one with quantity 0 if it doesn't exist.
     * Uses a transaction for atomicity.
     */
    suspend fun ensureStockEntry(productId: String, siteId: String) = withContext(Dispatchers.Default) {
        database.transaction {
            val existing = queries.getStockEntry(productId, siteId).executeAsOneOrNull()
            if (existing == null) {
                val id = Uuid.random().toString()
                val now = Clock.System.now().toEpochMilliseconds()
                queries.insertStock(id, productId, siteId, 0.0, null, now)
            }
        }
    }
}
