package com.medistock.shared.data.repository

import com.medistock.shared.db.MedistockDatabase
import com.medistock.shared.domain.model.PurchaseBatch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PurchaseBatchRepository(private val database: MedistockDatabase) {

    private val queries = database.medistockQueries

    suspend fun getAll(): List<PurchaseBatch> = withContext(Dispatchers.Default) {
        queries.getAllBatches().executeAsList().map { it.toModel() }
    }

    suspend fun getById(id: String): PurchaseBatch? = withContext(Dispatchers.Default) {
        queries.getBatchById(id).executeAsOneOrNull()?.toModel()
    }

    suspend fun getByProduct(productId: String): List<PurchaseBatch> = withContext(Dispatchers.Default) {
        queries.getBatchesByProduct(productId).executeAsList().map { it.toModel() }
    }

    suspend fun getByProductAndSite(productId: String, siteId: String): List<PurchaseBatch> = withContext(Dispatchers.Default) {
        queries.getBatchesByProductAndSite(productId, siteId).executeAsList().map { it.toModel() }
    }

    suspend fun insert(batch: PurchaseBatch) = withContext(Dispatchers.Default) {
        queries.insertBatch(
            id = batch.id,
            product_id = batch.productId,
            site_id = batch.siteId,
            batch_number = batch.batchNumber,
            purchase_date = batch.purchaseDate,
            initial_quantity = batch.initialQuantity,
            remaining_quantity = batch.remainingQuantity,
            purchase_price = batch.purchasePrice,
            supplier_name = batch.supplierName,
            expiry_date = batch.expiryDate,
            is_exhausted = if (batch.isExhausted) 1L else 0L,
            created_at = batch.createdAt,
            updated_at = batch.updatedAt,
            created_by = batch.createdBy,
            updated_by = batch.updatedBy
        )
    }

    suspend fun updateQuantity(id: String, remainingQuantity: Double, isExhausted: Boolean, updatedAt: Long, updatedBy: String) = withContext(Dispatchers.Default) {
        queries.updateBatchQuantity(
            remaining_quantity = remainingQuantity,
            is_exhausted = if (isExhausted) 1L else 0L,
            updated_at = updatedAt,
            updated_by = updatedBy,
            id = id
        )
    }

    fun observeAll(): Flow<List<PurchaseBatch>> {
        return queries.getAllBatches()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toModel() } }
    }

    private fun com.medistock.shared.db.Purchase_batches.toModel(): PurchaseBatch {
        return PurchaseBatch(
            id = id,
            productId = product_id,
            siteId = site_id,
            batchNumber = batch_number,
            purchaseDate = purchase_date,
            initialQuantity = initial_quantity,
            remainingQuantity = remaining_quantity,
            purchasePrice = purchase_price,
            supplierName = supplier_name,
            expiryDate = expiry_date,
            isExhausted = is_exhausted != 0L,
            createdAt = created_at,
            updatedAt = updated_at,
            createdBy = created_by,
            updatedBy = updated_by
        )
    }
}
