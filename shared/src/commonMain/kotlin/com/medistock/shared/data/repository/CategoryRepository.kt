package com.medistock.shared.data.repository

import com.medistock.shared.db.MedistockDatabase
import com.medistock.shared.domain.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class CategoryRepository(private val database: MedistockDatabase) {

    private val queries = database.medistockQueries

    suspend fun getAll(): List<Category> = withContext(Dispatchers.Default) {
        queries.getAllCategories().executeAsList().map { it.toModel() }
    }

    /**
     * Get only active categories (for dropdowns/pickers in operational screens).
     */
    suspend fun getActive(): List<Category> = withContext(Dispatchers.Default) {
        queries.getActiveCategories().executeAsList().map { it.toModel() }
    }

    suspend fun getById(id: String): Category? = withContext(Dispatchers.Default) {
        queries.getCategoryById(id).executeAsOneOrNull()?.toModel()
    }

    suspend fun insert(category: Category) = withContext(Dispatchers.Default) {
        queries.insertCategory(
            id = category.id,
            name = category.name,
            is_active = if (category.isActive) 1L else 0L,
            created_at = category.createdAt,
            updated_at = category.updatedAt,
            created_by = category.createdBy,
            updated_by = category.updatedBy
        )
    }

    suspend fun update(category: Category) = withContext(Dispatchers.Default) {
        queries.updateCategory(
            name = category.name,
            is_active = if (category.isActive) 1L else 0L,
            updated_at = category.updatedAt,
            updated_by = category.updatedBy,
            id = category.id
        )
    }

    suspend fun delete(id: String) = withContext(Dispatchers.Default) {
        queries.deleteCategory(id)
    }

    /**
     * Deactivate a category (soft delete).
     */
    suspend fun deactivate(id: String, updatedBy: String) = withContext(Dispatchers.Default) {
        val now = Clock.System.now().toEpochMilliseconds()
        queries.deactivateCategory(now, updatedBy, id)
    }

    /**
     * Reactivate a previously deactivated category.
     */
    suspend fun activate(id: String, updatedBy: String) = withContext(Dispatchers.Default) {
        val now = Clock.System.now().toEpochMilliseconds()
        queries.activateCategory(now, updatedBy, id)
    }

    /**
     * Upsert (INSERT OR REPLACE) a category.
     * Use this for sync operations to handle both new and existing records.
     */
    suspend fun upsert(category: Category) = withContext(Dispatchers.Default) {
        queries.upsertCategory(
            id = category.id,
            name = category.name,
            is_active = if (category.isActive) 1L else 0L,
            created_at = category.createdAt,
            updated_at = category.updatedAt,
            created_by = category.createdBy,
            updated_by = category.updatedBy
        )
    }

    fun observeAll(): Flow<List<Category>> {
        return queries.getAllCategories()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toModel() } }
    }

    /**
     * Observe only active categories.
     */
    fun observeActive(): Flow<List<Category>> {
        return queries.getActiveCategories()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toModel() } }
    }

    private fun com.medistock.shared.db.Categories.toModel(): Category {
        return Category(
            id = id,
            name = name,
            isActive = is_active == 1L,
            createdAt = created_at,
            updatedAt = updated_at,
            createdBy = created_by,
            updatedBy = updated_by
        )
    }
}
