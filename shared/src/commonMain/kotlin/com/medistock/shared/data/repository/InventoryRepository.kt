package com.medistock.shared.data.repository

import com.medistock.shared.db.MedistockDatabase
import com.medistock.shared.domain.model.Inventory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InventoryRepository(private val database: MedistockDatabase) {

    private val queries = database.medistockQueries

    suspend fun getAll(): List<Inventory> = withContext(Dispatchers.Default) {
        queries.getAllInventories().executeAsList().map { it.toModel() }
    }

    suspend fun getById(id: String): Inventory? = withContext(Dispatchers.Default) {
        queries.getInventoryById(id).executeAsOneOrNull()?.toModel()
    }

    suspend fun getBySite(siteId: String): List<Inventory> = withContext(Dispatchers.Default) {
        queries.getInventoriesBySite(siteId).executeAsList().map { it.toModel() }
    }

    suspend fun insert(inventory: Inventory) = withContext(Dispatchers.Default) {
        queries.insertInventory(
            id = inventory.id,
            site_id = inventory.siteId,
            status = inventory.status,
            started_at = inventory.startedAt,
            completed_at = inventory.completedAt,
            notes = inventory.notes,
            created_by = inventory.createdBy
        )
    }

    suspend fun updateStatus(id: String, status: String, completedAt: Long?) = withContext(Dispatchers.Default) {
        queries.updateInventoryStatus(
            status = status,
            completed_at = completedAt,
            id = id
        )
    }

    fun observeAll(): Flow<List<Inventory>> {
        return queries.getAllInventories()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toModel() } }
    }

    fun observeBySite(siteId: String): Flow<List<Inventory>> {
        return queries.getInventoriesBySite(siteId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toModel() } }
    }

    private fun com.medistock.shared.db.Inventories.toModel(): Inventory {
        return Inventory(
            id = id,
            siteId = site_id,
            status = status,
            startedAt = started_at,
            completedAt = completed_at,
            notes = notes,
            createdBy = created_by
        )
    }
}
