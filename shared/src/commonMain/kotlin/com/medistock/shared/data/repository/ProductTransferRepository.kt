package com.medistock.shared.data.repository

import com.medistock.shared.db.MedistockDatabase
import com.medistock.shared.domain.model.ProductTransfer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProductTransferRepository(private val database: MedistockDatabase) {

    private val queries = database.medistockQueries

    suspend fun getAll(): List<ProductTransfer> = withContext(Dispatchers.Default) {
        queries.getAllTransfers().executeAsList().map { it.toModel() }
    }

    suspend fun getById(id: String): ProductTransfer? = withContext(Dispatchers.Default) {
        queries.getTransferById(id).executeAsOneOrNull()?.toModel()
    }

    suspend fun getByStatus(status: String): List<ProductTransfer> = withContext(Dispatchers.Default) {
        queries.getTransfersByStatus(status).executeAsList().map { it.toModel() }
    }

    suspend fun insert(transfer: ProductTransfer) = withContext(Dispatchers.Default) {
        queries.insertTransfer(
            id = transfer.id,
            product_id = transfer.productId,
            from_site_id = transfer.fromSiteId,
            to_site_id = transfer.toSiteId,
            quantity = transfer.quantity,
            status = transfer.status,
            notes = transfer.notes,
            created_at = transfer.createdAt,
            updated_at = transfer.updatedAt,
            created_by = transfer.createdBy,
            updated_by = transfer.updatedBy
        )
    }

    suspend fun updateStatus(id: String, status: String, updatedAt: Long, updatedBy: String) = withContext(Dispatchers.Default) {
        queries.updateTransferStatus(
            status = status,
            updated_at = updatedAt,
            updated_by = updatedBy,
            id = id
        )
    }

    fun observeAll(): Flow<List<ProductTransfer>> {
        return queries.getAllTransfers()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toModel() } }
    }

    private fun com.medistock.shared.db.Product_transfers.toModel(): ProductTransfer {
        return ProductTransfer(
            id = id,
            productId = product_id,
            fromSiteId = from_site_id,
            toSiteId = to_site_id,
            quantity = quantity,
            status = status,
            notes = notes,
            createdAt = created_at,
            updatedAt = updated_at,
            createdBy = created_by,
            updatedBy = updated_by
        )
    }
}
