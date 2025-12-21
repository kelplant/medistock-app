package com.medistock.data.dao

import androidx.room.*
import com.medistock.data.entities.Customer
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(customer: Customer): Long

    @Update
    fun update(customer: Customer)

    @Delete
    fun delete(customer: Customer)

    @Query("SELECT * FROM customers WHERE id = :id")
    fun getById(id: Long): Flow<Customer?>

    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAll(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE siteId = :siteId ORDER BY name ASC")
    fun getAllForSite(siteId: Long): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE siteId = :siteId AND name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchByName(siteId: Long, query: String): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE siteId = :siteId AND (name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%') ORDER BY name ASC")
    fun search(siteId: Long, query: String): Flow<List<Customer>>

    @Query("DELETE FROM customers WHERE id = :id")
    fun deleteById(id: Long)
}
