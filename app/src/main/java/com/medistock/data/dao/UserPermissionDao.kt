package com.medistock.data.dao

import androidx.room.*
import com.medistock.data.entities.UserPermission

@Dao
interface UserPermissionDao {
    @Query("SELECT * FROM user_permissions WHERE userId = :userId")
    suspend fun getPermissionsForUser(userId: Long): List<UserPermission>

    @Query("SELECT * FROM user_permissions WHERE userId = :userId AND module = :module LIMIT 1")
    suspend fun getPermissionForModule(userId: Long, module: String): UserPermission?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPermission(permission: UserPermission): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPermissions(permissions: List<UserPermission>)

    @Update
    suspend fun updatePermission(permission: UserPermission)

    @Delete
    suspend fun deletePermission(permission: UserPermission)

    @Query("DELETE FROM user_permissions WHERE userId = :userId")
    suspend fun deleteAllPermissionsForUser(userId: Long)

    @Query("DELETE FROM user_permissions WHERE userId = :userId AND module = :module")
    suspend fun deletePermissionForModule(userId: Long, module: String)
}
