package com.medistock.shared.data.repository

import com.medistock.shared.db.MedistockDatabase
import com.medistock.shared.domain.model.Supplier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class SupplierRepository(private val database: MedistockDatabase) {

    private val queries = database.medistockQueries

    suspend fun getAll(): List<Supplier> = withContext(Dispatchers.Default) {
        queries.getAllSuppliers().executeAsList().map { it.toModel() }
    }

    /**
     * Get only active suppliers (for dropdowns/pickers in operational screens).
     */
    suspend fun getActive(): List<Supplier> = withContext(Dispatchers.Default) {
        queries.getActiveSuppliers().executeAsList().map { it.toModel() }
    }

    suspend fun getById(id: String): Supplier? = withContext(Dispatchers.Default) {
        queries.getSupplierById(id).executeAsOneOrNull()?.toModel()
    }

    suspend fun insert(supplier: Supplier) = withContext(Dispatchers.Default) {
        queries.insertSupplier(
            id = supplier.id,
            name = supplier.name,
            phone = supplier.phone,
            email = supplier.email,
            address = supplier.address,
            notes = supplier.notes,
            is_active = if (supplier.isActive) 1L else 0L,
            created_at = supplier.createdAt,
            updated_at = supplier.updatedAt,
            created_by = supplier.createdBy,
            updated_by = supplier.updatedBy
        )
    }

    suspend fun update(supplier: Supplier) = withContext(Dispatchers.Default) {
        queries.updateSupplier(
            name = supplier.name,
            phone = supplier.phone,
            email = supplier.email,
            address = supplier.address,
            notes = supplier.notes,
            is_active = if (supplier.isActive) 1L else 0L,
            updated_at = supplier.updatedAt,
            updated_by = supplier.updatedBy,
            id = supplier.id
        )
    }

    suspend fun search(query: String): List<Supplier> = withContext(Dispatchers.Default) {
        queries.searchSuppliers(query, query).executeAsList().map { it.toModel() }
    }

    suspend fun delete(id: String) = withContext(Dispatchers.Default) {
        queries.deleteSupplier(id)
    }

    /**
     * Deactivate a supplier (soft delete).
     */
    suspend fun deactivate(id: String, updatedBy: String) = withContext(Dispatchers.Default) {
        val now = Clock.System.now().toEpochMilliseconds()
        queries.deactivateSupplier(now, updatedBy, id)
    }

    /**
     * Reactivate a previously deactivated supplier.
     */
    suspend fun activate(id: String, updatedBy: String) = withContext(Dispatchers.Default) {
        val now = Clock.System.now().toEpochMilliseconds()
        queries.activateSupplier(now, updatedBy, id)
    }

    /**
     * Upsert (INSERT OR REPLACE) a supplier.
     * Use this for sync operations to handle both new and existing records.
     */
    suspend fun upsert(supplier: Supplier) = withContext(Dispatchers.Default) {
        queries.upsertSupplier(
            id = supplier.id,
            name = supplier.name,
            phone = supplier.phone,
            email = supplier.email,
            address = supplier.address,
            notes = supplier.notes,
            is_active = if (supplier.isActive) 1L else 0L,
            created_at = supplier.createdAt,
            updated_at = supplier.updatedAt,
            created_by = supplier.createdBy,
            updated_by = supplier.updatedBy
        )
    }

    fun observeAll(): Flow<List<Supplier>> {
        return queries.getAllSuppliers()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toModel() } }
    }

    /**
     * Observe only active suppliers.
     */
    fun observeActive(): Flow<List<Supplier>> {
        return queries.getActiveSuppliers()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toModel() } }
    }

    private fun com.medistock.shared.db.Suppliers.toModel(): Supplier {
        return Supplier(
            id = id,
            name = name,
            phone = phone,
            email = email,
            address = address,
            notes = notes,
            isActive = is_active == 1L,
            createdAt = created_at,
            updatedAt = updated_at,
            createdBy = created_by,
            updatedBy = updated_by
        )
    }
}
