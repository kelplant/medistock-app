package com.medistock.shared.data.repository

import com.medistock.shared.db.MedistockDatabase
import com.medistock.shared.domain.model.PackagingType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PackagingTypeRepository(private val database: MedistockDatabase) {

    private val queries = database.medistockQueries

    suspend fun getAll(): List<PackagingType> = withContext(Dispatchers.Default) {
        queries.getAllPackagingTypes().executeAsList().map { it.toModel() }
    }

    suspend fun getById(id: String): PackagingType? = withContext(Dispatchers.Default) {
        queries.getPackagingTypeById(id).executeAsOneOrNull()?.toModel()
    }

    suspend fun insert(packagingType: PackagingType) = withContext(Dispatchers.Default) {
        queries.insertPackagingType(
            id = packagingType.id,
            name = packagingType.name,
            level1_name = packagingType.level1Name,
            level2_name = packagingType.level2Name,
            level2_quantity = packagingType.level2Quantity?.toLong(),
            default_conversion_factor = packagingType.defaultConversionFactor,
            is_active = if (packagingType.isActive) 1L else 0L,
            display_order = packagingType.displayOrder.toLong(),
            created_at = packagingType.createdAt,
            updated_at = packagingType.updatedAt,
            created_by = packagingType.createdBy,
            updated_by = packagingType.updatedBy
        )
    }

    suspend fun update(packagingType: PackagingType) = withContext(Dispatchers.Default) {
        queries.updatePackagingType(
            name = packagingType.name,
            level1_name = packagingType.level1Name,
            level2_name = packagingType.level2Name,
            level2_quantity = packagingType.level2Quantity?.toLong(),
            default_conversion_factor = packagingType.defaultConversionFactor,
            is_active = if (packagingType.isActive) 1L else 0L,
            display_order = packagingType.displayOrder.toLong(),
            updated_at = packagingType.updatedAt,
            updated_by = packagingType.updatedBy,
            id = packagingType.id
        )
    }

    suspend fun delete(id: String) = withContext(Dispatchers.Default) {
        queries.deletePackagingType(id)
    }

    suspend fun getActive(): List<PackagingType> = withContext(Dispatchers.Default) {
        queries.getActivePackagingTypes().executeAsList().map { it.toModel() }
    }

    suspend fun setActive(id: String, isActive: Boolean, updatedAt: Long, updatedBy: String) = withContext(Dispatchers.Default) {
        queries.setPackagingTypeActive(
            is_active = if (isActive) 1L else 0L,
            updated_at = updatedAt,
            updated_by = updatedBy,
            id = id
        )
    }

    fun observeAll(): Flow<List<PackagingType>> {
        return queries.getAllPackagingTypes()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toModel() } }
    }

    fun observeActive(): Flow<List<PackagingType>> {
        return queries.getActivePackagingTypes()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toModel() } }
    }

    suspend fun isUsedByProducts(id: String): Boolean = withContext(Dispatchers.Default) {
        queries.isPackagingTypeUsedByProducts(id).executeAsOne()
    }

    private fun com.medistock.shared.db.Packaging_types.toModel(): PackagingType {
        return PackagingType(
            id = id,
            name = name,
            level1Name = level1_name,
            level2Name = level2_name,
            level2Quantity = level2_quantity?.toInt(),
            defaultConversionFactor = default_conversion_factor,
            isActive = is_active == 1L,
            displayOrder = display_order.toInt(),
            createdAt = created_at,
            updatedAt = updated_at,
            createdBy = created_by,
            updatedBy = updated_by
        )
    }
}
