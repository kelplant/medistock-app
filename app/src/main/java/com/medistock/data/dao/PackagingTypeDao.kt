package com.medistock.data.dao

import androidx.room.*
import com.medistock.data.entities.PackagingType
import kotlinx.coroutines.flow.Flow

@Dao
interface PackagingTypeDao {

    @Query("SELECT * FROM packaging_types WHERE isActive = 1 ORDER BY displayOrder ASC, name ASC")
    fun getAllActive(): Flow<List<PackagingType>>

    @Query("SELECT * FROM packaging_types ORDER BY displayOrder ASC, name ASC")
    fun getAll(): Flow<List<PackagingType>>

    @Query("SELECT * FROM packaging_types WHERE id = :id")
    fun getById(id: Long): Flow<PackagingType?>

    @Query("SELECT * FROM packaging_types WHERE id = :id")
    suspend fun getByIdSync(id: Long): PackagingType?

    @Query("SELECT * FROM packaging_types WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): PackagingType?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(packagingType: PackagingType): Long

    @Update
    suspend fun update(packagingType: PackagingType)

    @Delete
    suspend fun delete(packagingType: PackagingType)

    @Query("UPDATE packaging_types SET isActive = 0 WHERE id = :id")
    suspend fun deactivate(id: Long)

    @Query("UPDATE packaging_types SET isActive = 1 WHERE id = :id")
    suspend fun activate(id: Long)

    @Query("SELECT COUNT(*) FROM packaging_types WHERE isActive = 1")
    suspend fun getActiveCount(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM products WHERE packagingTypeId = :packagingTypeId LIMIT 1)")
    suspend fun isUsedByProducts(packagingTypeId: Long): Boolean
}
