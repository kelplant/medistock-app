package com.medistock.data.dao

import androidx.room.*

@Dao
interface UserDao {
    @Query("SELECT * FROM app_users ORDER BY full_name ASC")
    fun getAllUsers(): List<com.medistock.data.entities.User>

    @Query("SELECT * FROM app_users WHERE is_active = 1 ORDER BY full_name ASC")
    fun getActiveUsers(): List<com.medistock.data.entities.User>

    @Query("SELECT * FROM app_users WHERE id = :userId")
    fun getUserById(userId: String): com.medistock.data.entities.User?

    @Query("SELECT * FROM app_users WHERE username = :username LIMIT 1")
    fun getUserByUsername(username: String): com.medistock.data.entities.User?

    @Query("SELECT * FROM app_users WHERE username = :username AND is_active = 1 LIMIT 1")
    fun getUserForAuth(username: String): com.medistock.data.entities.User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUser(user: com.medistock.data.entities.User): Long

    @Update
    fun updateUser(user: com.medistock.data.entities.User)

    @Delete
    fun deleteUser(user: com.medistock.data.entities.User)

    @Query("UPDATE app_users SET is_active = 0, updated_at = :timestamp, updated_by = :updatedBy WHERE id = :userId")
    fun deactivateUser(userId: String, timestamp: Long, updatedBy: String)

    @Query("SELECT COUNT(*) FROM app_users WHERE is_admin = 1 AND is_active = 1")
    fun countActiveAdmins(): Int
}
