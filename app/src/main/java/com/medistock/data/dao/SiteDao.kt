package com.medistock.data.dao

import androidx.room.*
import com.medistock.data.entities.Site
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBlocking(site: Site): Long

    suspend fun insert(site: Site): Long {
        return insertBlocking(site)
    }

    @Query("SELECT * FROM sites")
    fun getAll(): Flow<List<Site>>
}