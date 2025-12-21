package com.medistock.data.remote.repository

import com.medistock.data.remote.dto.AppUserDto
import com.medistock.data.remote.dto.UserPermissionDto
import io.github.jan.supabase.postgrest.from

class UserSupabaseRepository : BaseSupabaseRepository("app_users") {
    suspend fun getAllUsers(): List<AppUserDto> = getAll()
    suspend fun getUserById(id: Long): AppUserDto? = getById(id)
    suspend fun createUser(user: AppUserDto): AppUserDto = create(user)
    suspend fun updateUser(id: Long, user: AppUserDto): AppUserDto = update(id, user)
    suspend fun deleteUser(id: Long) = delete(id)

    suspend fun getUserByUsername(username: String): AppUserDto? {
        return supabase.from(tableName).select {
            filter { eq("username", username) }
        }.decodeList<AppUserDto>().firstOrNull()
    }

    suspend fun getActiveUsers(): List<AppUserDto> {
        return supabase.from(tableName).select {
            filter { eq("is_active", true) }
        }.decodeList()
    }

    suspend fun getAdminUsers(): List<AppUserDto> {
        return supabase.from(tableName).select {
            filter {
                eq("is_admin", true)
                eq("is_active", true)
            }
        }.decodeList()
    }

    suspend fun usernameExists(username: String): Boolean {
        return getUserByUsername(username) != null
    }
}

class UserPermissionSupabaseRepository : BaseSupabaseRepository("user_permissions") {
    suspend fun getAllPermissions(): List<UserPermissionDto> = getAll()
    suspend fun getPermissionById(id: Long): UserPermissionDto? = getById(id)
    suspend fun createPermission(permission: UserPermissionDto): UserPermissionDto = create(permission)
    suspend fun updatePermission(id: Long, permission: UserPermissionDto): UserPermissionDto = update(id, permission)
    suspend fun deletePermission(id: Long) = delete(id)

    suspend fun getPermissionsByUser(userId: Long): List<UserPermissionDto> {
        return supabase.from(tableName).select {
            filter { eq("user_id", userId) }
        }.decodeList()
    }

    suspend fun getPermissionByUserAndModule(userId: Long, module: String): UserPermissionDto? {
        return supabase.from(tableName).select {
            filter {
                eq("user_id", userId)
                eq("module", module)
            }
        }.decodeList<UserPermissionDto>().firstOrNull()
    }

    suspend fun hasPermission(userId: Long, module: String, action: String): Boolean {
        val permission = getPermissionByUserAndModule(userId, module) ?: return false

        return when (action) {
            "view" -> permission.canView
            "create" -> permission.canCreate
            "edit" -> permission.canEdit
            "delete" -> permission.canDelete
            else -> false
        }
    }

    suspend fun deleteAllUserPermissions(userId: Long) {
        supabase.from(tableName).delete {
            filter { eq("user_id", userId) }
        }
    }
}
