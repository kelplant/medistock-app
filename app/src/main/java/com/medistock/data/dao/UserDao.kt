package com.medistock.data.dao

import androidx.room.*
import com.medistock.data.entities.User

@Dao
interface UserDao {
    @Query("SELECT * FROM app_users ORDER BY full_name ASC")
    suspend fun getAllUsers(): List<User>

    @Query("SELECT * FROM app_users WHERE is_active = 1 ORDER BY full_name ASC")
    suspend fun getActiveUsers(): List<User>

    @Query("SELECT * FROM app_users WHERE id = :userId")
    suspend fun getUserById(userId: Long): User?

    @Query("SELECT * FROM app_users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM app_users WHERE username = :username AND password = :password AND is_active = 1 LIMIT 1")
    suspend fun authenticate(username: String, password: String): User?

    @Insert
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)

    @Query("UPDATE app_users SET is_active = 0, updated_at = :timestamp, updated_by = :updatedBy WHERE id = :userId")
    suspend fun deactivateUser(userId: Long, timestamp: Long, updatedBy: String)

    @Query("SELECT COUNT(*) FROM app_users WHERE is_admin = 1 AND is_active = 1")
    suspend fun countActiveAdmins(): Int
}
