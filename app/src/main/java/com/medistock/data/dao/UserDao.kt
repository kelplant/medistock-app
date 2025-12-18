package com.medistock.data.dao

import androidx.room.*
import com.medistock.data.entities.User

@Dao
interface UserDao {
    @Query("SELECT * FROM app_users ORDER BY fullName ASC")
    suspend fun getAllUsers(): List<User>

    @Query("SELECT * FROM app_users WHERE isActive = 1 ORDER BY fullName ASC")
    suspend fun getActiveUsers(): List<User>

    @Query("SELECT * FROM app_users WHERE id = :userId")
    suspend fun getUserById(userId: Long): User?

    @Query("SELECT * FROM app_users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM app_users WHERE username = :username AND password = :password AND isActive = 1 LIMIT 1")
    suspend fun authenticate(username: String, password: String): User?

    @Insert
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)

    @Query("UPDATE app_users SET isActive = 0, updatedAt = :timestamp, updatedBy = :updatedBy WHERE id = :userId")
    suspend fun deactivateUser(userId: Long, timestamp: Long, updatedBy: String)

    @Query("SELECT COUNT(*) FROM app_users WHERE isAdmin = 1 AND isActive = 1")
    suspend fun countActiveAdmins(): Int
}
