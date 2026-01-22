package com.medistock.shared.data.repository

import com.medistock.shared.db.MedistockDatabase
import com.medistock.shared.domain.model.Product
import com.medistock.shared.domain.model.ProductWithCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class ProductRepository(private val database: MedistockDatabase) {

    private val queries = database.medistockQueries

    suspend fun getAll(): List<Product> = withContext(Dispatchers.Default) {
        queries.getAllProducts().executeAsList().map { it.toModel() }
    }

    /**
     * Get only active products (for dropdowns/pickers in operational screens).
     */
    suspend fun getActive(): List<Product> = withContext(Dispatchers.Default) {
        queries.getActiveProducts().executeAsList().map { it.toModel() }
    }

    suspend fun getBySite(siteId: String): List<Product> = withContext(Dispatchers.Default) {
        queries.getProductsBySite(siteId).executeAsList().map { it.toModel() }
    }

    /**
     * Get only active products for a site.
     */
    suspend fun getActiveBySite(siteId: String): List<Product> = withContext(Dispatchers.Default) {
        queries.getActiveProductsBySite(siteId).executeAsList().map { it.toModel() }
    }

    suspend fun getById(id: String): Product? = withContext(Dispatchers.Default) {
        queries.getProductById(id).executeAsOneOrNull()?.toModel()
    }

    suspend fun insert(product: Product) = withContext(Dispatchers.Default) {
        queries.insertProduct(
            id = product.id,
            name = product.name,
            unit = product.unit,
            unit_volume = product.unitVolume,
            packaging_type_id = product.packagingTypeId,
            selected_level = product.selectedLevel?.toLong(),
            conversion_factor = product.conversionFactor,
            category_id = product.categoryId,
            margin_type = product.marginType,
            margin_value = product.marginValue,
            description = product.description,
            site_id = product.siteId,
            min_stock = product.minStock,
            max_stock = product.maxStock,
            is_active = if (product.isActive) 1L else 0L,
            created_at = product.createdAt,
            updated_at = product.updatedAt,
            created_by = product.createdBy,
            updated_by = product.updatedBy
        )
    }

    suspend fun update(product: Product) = withContext(Dispatchers.Default) {
        queries.updateProduct(
            name = product.name,
            unit = product.unit,
            unit_volume = product.unitVolume,
            packaging_type_id = product.packagingTypeId,
            selected_level = product.selectedLevel?.toLong(),
            conversion_factor = product.conversionFactor,
            category_id = product.categoryId,
            margin_type = product.marginType,
            margin_value = product.marginValue,
            description = product.description,
            min_stock = product.minStock,
            max_stock = product.maxStock,
            is_active = if (product.isActive) 1L else 0L,
            updated_at = product.updatedAt,
            updated_by = product.updatedBy,
            id = product.id
        )
    }

    suspend fun delete(id: String) = withContext(Dispatchers.Default) {
        queries.deleteProduct(id)
    }

    /**
     * Deactivate a product (soft delete).
     */
    suspend fun deactivate(id: String, updatedBy: String) = withContext(Dispatchers.Default) {
        val now = Clock.System.now().toEpochMilliseconds()
        queries.deactivateProduct(now, updatedBy, id)
    }

    /**
     * Reactivate a previously deactivated product.
     */
    suspend fun activate(id: String, updatedBy: String) = withContext(Dispatchers.Default) {
        val now = Clock.System.now().toEpochMilliseconds()
        queries.activateProduct(now, updatedBy, id)
    }

    /**
     * Upsert (INSERT OR REPLACE) a product.
     * Use this for sync operations to handle both new and existing records.
     */
    suspend fun upsert(product: Product) = withContext(Dispatchers.Default) {
        queries.upsertProduct(
            id = product.id,
            name = product.name,
            unit = product.unit,
            unit_volume = product.unitVolume,
            packaging_type_id = product.packagingTypeId,
            selected_level = product.selectedLevel?.toLong(),
            conversion_factor = product.conversionFactor,
            category_id = product.categoryId,
            margin_type = product.marginType,
            margin_value = product.marginValue,
            description = product.description,
            site_id = product.siteId,
            min_stock = product.minStock,
            max_stock = product.maxStock,
            is_active = if (product.isActive) 1L else 0L,
            created_at = product.createdAt,
            updated_at = product.updatedAt,
            created_by = product.createdBy,
            updated_by = product.updatedBy
        )
    }

    fun observeAll(): Flow<List<Product>> {
        return queries.getAllProducts()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toModel() } }
    }

    fun observeBySite(siteId: String): Flow<List<Product>> {
        return queries.getProductsBySite(siteId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toModel() } }
    }

    /**
     * Observe only active products.
     */
    fun observeActive(): Flow<List<Product>> {
        return queries.getActiveProducts()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toModel() } }
    }

    /**
     * Observe only active products for a specific site.
     */
    fun observeActiveBySite(siteId: String): Flow<List<Product>> {
        return queries.getActiveProductsBySite(siteId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toModel() } }
    }

    suspend fun getAllWithCategory(): List<ProductWithCategory> = withContext(Dispatchers.Default) {
        queries.getProductsWithCategory().executeAsList().map { it.toProductWithCategory() }
    }

    suspend fun getWithCategoryForSite(siteId: String): List<ProductWithCategory> = withContext(Dispatchers.Default) {
        queries.getProductsWithCategoryForSite(siteId).executeAsList().map { it.toProductWithCategory() }
    }

    fun observeAllWithCategory(): Flow<List<ProductWithCategory>> {
        return queries.getProductsWithCategory()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toProductWithCategory() } }
    }

    fun observeWithCategoryForSite(siteId: String): Flow<List<ProductWithCategory>> {
        return queries.getProductsWithCategoryForSite(siteId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toProductWithCategory() } }
    }

    private fun com.medistock.shared.db.GetProductsWithCategory.toProductWithCategory(): ProductWithCategory {
        return ProductWithCategory(
            id = id,
            name = name,
            unit = unit,
            categoryId = category_id,
            categoryName = category_name,
            marginType = margin_type,
            marginValue = margin_value,
            unitVolume = unit_volume,
            description = description,
            siteId = site_id,
            minStock = min_stock,
            maxStock = max_stock
        )
    }

    private fun com.medistock.shared.db.GetProductsWithCategoryForSite.toProductWithCategory(): ProductWithCategory {
        return ProductWithCategory(
            id = id,
            name = name,
            unit = unit,
            categoryId = category_id,
            categoryName = category_name,
            marginType = margin_type,
            marginValue = margin_value,
            unitVolume = unit_volume,
            description = description,
            siteId = site_id,
            minStock = min_stock,
            maxStock = max_stock
        )
    }

    // Extension function to convert SQLDelight generated class to domain model
    private fun com.medistock.shared.db.Products.toModel(): Product {
        return Product(
            id = id,
            name = name,
            unit = unit,
            unitVolume = unit_volume,
            packagingTypeId = packaging_type_id,
            selectedLevel = selected_level?.toInt(),
            conversionFactor = conversion_factor,
            categoryId = category_id,
            marginType = margin_type,
            marginValue = margin_value,
            description = description,
            siteId = site_id,
            minStock = min_stock,
            maxStock = max_stock,
            isActive = is_active == 1L,
            createdAt = created_at,
            updatedAt = updated_at,
            createdBy = created_by,
            updatedBy = updated_by
        )
    }
}
