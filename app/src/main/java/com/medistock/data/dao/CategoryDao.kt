package com.medistock.data.dao

import androidx.room.*
import com.medistock.data.entities.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(category: Category): Long

    @Update
    fun update(category: Category)

    @Delete
    fun delete(category: Category)

    @Query("SELECT * FROM categories")
    fun getAll(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :categoryId LIMIT 1")
    fun getById(categoryId: String): Flow<Category?>
}
