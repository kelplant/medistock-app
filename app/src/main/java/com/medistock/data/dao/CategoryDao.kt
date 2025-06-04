package com.medistock.data.dao

import androidx.room.*
import com.medistock.data.entities.Category

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category): Long

    @Query("SELECT * FROM categories")
    suspend fun getAll(): List<Category>
}