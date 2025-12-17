package com.medistock.data.dao

import androidx.room.*
import com.medistock.data.entities.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBlocking(category: Category): Long

    suspend fun insert(category: Category): Long {
        return insertBlocking(category)
    }

    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun delete(category: Category)

    @Query("SELECT * FROM categories")
    fun getAll(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :categoryId LIMIT 1")
    fun getById(categoryId: Long): Flow<Category?>
}