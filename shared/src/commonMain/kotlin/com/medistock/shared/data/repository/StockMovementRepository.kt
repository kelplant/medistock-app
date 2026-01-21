package com.medistock.shared.data.repository

import com.medistock.shared.db.MedistockDatabase
import com.medistock.shared.domain.model.StockMovement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StockMovementRepository(private val database: MedistockDatabase) {

    private val queries = database.medistockQueries

    suspend fun getAll(): List<StockMovement> = withContext(Dispatchers.Default) {
        queries.getAllStockMovements().executeAsList().map { it.toModel() }
    }

    suspend fun getByProduct(productId: String): List<StockMovement> = withContext(Dispatchers.Default) {
        queries.getStockMovementsByProduct(productId).executeAsList().map { it.toModel() }
    }

    suspend fun getBySite(siteId: String): List<StockMovement> = withContext(Dispatchers.Default) {
        queries.getStockMovementsBySite(siteId).executeAsList().map { it.toModel() }
    }

    suspend fun insert(movement: StockMovement) = withContext(Dispatchers.Default) {
        queries.insertStockMovement(
            id = movement.id,
            product_id = movement.productId,
            site_id = movement.siteId,
            quantity = movement.quantity,
            movement_type = movement.movementType,
            reference_id = movement.referenceId,
            notes = movement.notes,
            created_at = movement.createdAt,
            created_by = movement.createdBy
        )
    }

    fun observeAll(): Flow<List<StockMovement>> {
        return queries.getAllStockMovements()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toModel() } }
    }

    fun observeBySite(siteId: String): Flow<List<StockMovement>> {
        return queries.getStockMovementsBySite(siteId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toModel() } }
    }

    private fun com.medistock.shared.db.Stock_movements.toModel(): StockMovement {
        return StockMovement(
            id = id,
            productId = product_id,
            siteId = site_id,
            quantity = quantity,
            movementType = movement_type,
            referenceId = reference_id,
            notes = notes,
            createdAt = created_at,
            createdBy = created_by
        )
    }
}
