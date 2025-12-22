package com.medistock.data.dao

import androidx.room.*
import com.medistock.data.entities.Site
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(site: Site): Long

    @Update
    fun update(site: Site)

    @Delete
    fun delete(site: Site)

    @Query("SELECT * FROM sites")
    fun getAll(): Flow<List<Site>>

    @Query("SELECT * FROM sites WHERE id = :siteId LIMIT 1")
    fun getById(siteId: String): Flow<Site?>
}
