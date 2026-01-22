package com.medistock.shared.data.repository

import com.medistock.shared.db.MedistockDatabase
import com.medistock.shared.domain.model.InventoryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for managing individual inventory counts (product-level).
 * This corresponds to the Room Inventory entity used in InventoryListActivity.
 */
class InventoryItemRepository(private val database: MedistockDatabase) {

    private val queries = database.medistockQueries

    suspend fun getAll(): List<InventoryItem> = withContext(Dispatchers.Default) {
        queries.getAllInventoryItems().executeAsList().map { it.toModel() }
    }

    suspend fun getBySite(siteId: String): List<InventoryItem> = withContext(Dispatchers.Default) {
        queries.getInventoryItemsBySite(siteId).executeAsList().map { it.toModel() }
    }

    suspend fun getForProduct(productId: String, siteId: String): List<InventoryItem> = withContext(Dispatchers.Default) {
        queries.getInventoryItemsForProduct(productId, siteId).executeAsList().map { it.toModel() }
    }

    suspend fun getRecent(limit: Long = 100): List<InventoryItem> = withContext(Dispatchers.Default) {
        queries.getRecentInventoryItems(limit).executeAsList().map { it.toModel() }
    }

    suspend fun getWithDiscrepancies(siteId: String): List<InventoryItem> = withContext(Dispatchers.Default) {
        queries.getInventoryItemsWithDiscrepancies(siteId).executeAsList().map { it.toModel() }
    }

    suspend fun getById(id: String): InventoryItem? = withContext(Dispatchers.Default) {
        queries.getInventoryItemById(id).executeAsOneOrNull()?.toModel()
    }

    suspend fun insert(item: InventoryItem) = withContext(Dispatchers.Default) {
        queries.insertInventoryItem(
            id = item.id,
            inventory_id = item.inventoryId,
            product_id = item.productId,
            site_id = item.siteId,
            count_date = item.countDate,
            counted_quantity = item.countedQuantity,
            theoretical_quantity = item.theoreticalQuantity,
            discrepancy = item.discrepancy,
            reason = item.reason,
            counted_by = item.countedBy,
            notes = item.notes,
            created_at = item.createdAt,
            created_by = item.createdBy
        )
    }

    suspend fun update(item: InventoryItem) = withContext(Dispatchers.Default) {
        queries.updateInventoryItem(
            counted_quantity = item.countedQuantity,
            theoretical_quantity = item.theoreticalQuantity,
            discrepancy = item.discrepancy,
            reason = item.reason,
            notes = item.notes,
            id = item.id
        )
    }

    suspend fun delete(id: String) = withContext(Dispatchers.Default) {
        queries.deleteInventoryItem(id)
    }

    fun observeAll(): Flow<List<InventoryItem>> {
        return queries.getAllInventoryItems()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toModel() } }
    }

    fun observeBySite(siteId: String): Flow<List<InventoryItem>> {
        return queries.getInventoryItemsBySite(siteId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toModel() } }
    }

    private fun com.medistock.shared.db.Inventory_items.toModel(): InventoryItem {
        return InventoryItem(
            id = id,
            inventoryId = inventory_id,
            productId = product_id,
            siteId = site_id,
            countDate = count_date,
            countedQuantity = counted_quantity,
            theoreticalQuantity = theoretical_quantity,
            discrepancy = discrepancy,
            reason = reason,
            countedBy = counted_by,
            notes = notes,
            createdAt = created_at,
            createdBy = created_by
        )
    }
}
