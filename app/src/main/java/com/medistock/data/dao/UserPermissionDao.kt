package com.medistock.data.dao

import androidx.room.*

@Dao
interface UserPermissionDao {
    @Query("SELECT * FROM user_permissions WHERE user_id = :userId")
    suspend fun getPermissionsForUser(userId: Long): List<com.medistock.data.entities.UserPermission>

    @Query("SELECT * FROM user_permissions WHERE user_id = :userId AND module = :module LIMIT 1")
    suspend fun getPermissionForModule(userId: Long, module: String): com.medistock.data.entities.UserPermission?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPermission(permission: com.medistock.data.entities.UserPermission): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPermissions(permissions: List<com.medistock.data.entities.UserPermission>)

    @Update
    suspend fun updatePermission(permission: com.medistock.data.entities.UserPermission)

    @Delete
    suspend fun deletePermission(permission: com.medistock.data.entities.UserPermission)

    @Query("DELETE FROM user_permissions WHERE user_id = :userId")
    suspend fun deleteAllPermissionsForUser(userId: Long)

    @Query("DELETE FROM user_permissions WHERE user_id = :userId AND module = :module")
    suspend fun deletePermissionForModule(userId: Long, module: String)
}
