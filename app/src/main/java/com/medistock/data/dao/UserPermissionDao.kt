package com.medistock.data.dao

import androidx.room.*

@Dao
interface UserPermissionDao {
    @Query("SELECT * FROM user_permissions WHERE user_id = :userId")
    fun getPermissionsForUser(userId: String): List<com.medistock.data.entities.UserPermission>

    @Query("SELECT * FROM user_permissions WHERE user_id = :userId AND module = :module LIMIT 1")
    fun getPermissionForModule(userId: String, module: String): com.medistock.data.entities.UserPermission?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPermission(permission: com.medistock.data.entities.UserPermission): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPermissions(permissions: List<com.medistock.data.entities.UserPermission>)

    @Update
    fun updatePermission(permission: com.medistock.data.entities.UserPermission)

    @Delete
    fun deletePermission(permission: com.medistock.data.entities.UserPermission)

    @Query("DELETE FROM user_permissions WHERE user_id = :userId")
    fun deleteAllPermissionsForUser(userId: String)

    @Query("DELETE FROM user_permissions WHERE user_id = :userId AND module = :module")
    fun deletePermissionForModule(userId: String, module: String)
}
