package com.medistock.data.remote.repository

import com.medistock.data.remote.dto.AppUserDto
import com.medistock.data.remote.dto.UserPermissionDto

/**
 * Repository pour les utilisateurs
 */
class UserSupabaseRepository : BaseSupabaseRepository("app_users") {

    suspend fun getAllUsers(): List<AppUserDto> = getAll()

    suspend fun getUserById(id: Long): AppUserDto? = getById(id)

    suspend fun createUser(user: AppUserDto): AppUserDto = create(user)

    suspend fun updateUser(id: Long, user: AppUserDto): AppUserDto = update(id, user)

    suspend fun deleteUser(id: Long) = delete(id)

    /**
     * Récupère un utilisateur par nom d'utilisateur
     */
    suspend fun getUserByUsername(username: String): AppUserDto? {
        return getWithFilter<AppUserDto> {
            eq("username", username)
        }.firstOrNull()
    }

    /**
     * Récupère les utilisateurs actifs uniquement
     */
    suspend fun getActiveUsers(): List<AppUserDto> {
        return getWithFilter {
            eq("is_active", true)
        }
    }

    /**
     * Récupère les administrateurs
     */
    suspend fun getAdminUsers(): List<AppUserDto> {
        return getWithFilter {
            eq("is_admin", true)
            eq("is_active", true)
        }
    }

    /**
     * Vérifie si un nom d'utilisateur existe
     */
    suspend fun usernameExists(username: String): Boolean {
        return getUserByUsername(username) != null
    }
}

/**
 * Repository pour les permissions utilisateur
 */
class UserPermissionSupabaseRepository : BaseSupabaseRepository("user_permissions") {

    suspend fun getAllPermissions(): List<UserPermissionDto> = getAll()

    suspend fun getPermissionById(id: Long): UserPermissionDto? = getById(id)

    suspend fun createPermission(permission: UserPermissionDto): UserPermissionDto = create(permission)

    suspend fun updatePermission(id: Long, permission: UserPermissionDto): UserPermissionDto = update(id, permission)

    suspend fun deletePermission(id: Long) = delete(id)

    /**
     * Récupère toutes les permissions d'un utilisateur
     */
    suspend fun getPermissionsByUser(userId: Long): List<UserPermissionDto> {
        return getWithFilter {
            eq("user_id", userId)
        }
    }

    /**
     * Récupère les permissions d'un utilisateur pour un module spécifique
     */
    suspend fun getPermissionByUserAndModule(userId: Long, module: String): UserPermissionDto? {
        return getWithFilter<UserPermissionDto> {
            eq("user_id", userId)
            eq("module", module)
        }.firstOrNull()
    }

    /**
     * Vérifie si un utilisateur a une permission spécifique
     */
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

    /**
     * Supprime toutes les permissions d'un utilisateur
     */
    suspend fun deleteAllUserPermissions(userId: Long) {
        supabase.from("user_permissions").delete {
            filter {
                eq("user_id", userId)
            }
        }
    }
}
