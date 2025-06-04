package com.medistock.data.dao

import androidx.room.*
import com.medistock.data.entities.Site

@Dao
interface SiteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(site: Site): Long

    @Query("SELECT * FROM sites")
    suspend fun getAll(): List<Site>
}