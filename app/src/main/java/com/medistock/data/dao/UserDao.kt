package com.medistock.data.dao

import androidx.room.*

@Dao
interface UserDao {
    @Query("SELECT * FROM app_users ORDER BY full_name ASC")
    fun getAllUsers(): List<com.medistock.data.entities.User>

    @Query("SELECT * FROM app_users WHERE is_active = 1 ORDER BY full_name ASC")
    suspend fun getActiveUsers(): List<com.medistock.data.entities.User>

    @Query("SELECT * FROM app_users WHERE id = :userId")
    suspend fun getUserById(userId: Long): com.medistock.data.entities.User?

    @Query("SELECT * FROM app_users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): com.medistock.data.entities.User?

    @Query("SELECT * FROM app_users WHERE username = :username AND password = :password AND is_active = 1 LIMIT 1")
    suspend fun authenticate(username: String, password: String): com.medistock.data.entities.User?

    @Insert
    suspend fun insertUser(user: com.medistock.data.entities.User): Long

    @Update
    suspend fun updateUser(user: com.medistock.data.entities.User)

    @Delete
    suspend fun deleteUser(user: com.medistock.data.entities.User)

    @Query("UPDATE app_users SET is_active = 0, updated_at = :timestamp, updated_by = :updatedBy WHERE id = :userId")
    suspend fun deactivateUser(userId: Long, timestamp: Long, updatedBy: String)

    @Query("SELECT COUNT(*) FROM app_users WHERE is_admin = 1 AND is_active = 1")
    suspend fun countActiveAdmins(): Int
}
