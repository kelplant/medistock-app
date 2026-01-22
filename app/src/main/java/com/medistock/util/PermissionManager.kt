package com.medistock.util

import com.medistock.shared.data.repository.UserPermissionRepository
import com.medistock.shared.domain.model.Module
import com.medistock.shared.domain.model.UserPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages user permissions
 */
class PermissionManager(
    private val userPermissionRepository: UserPermissionRepository,
    private val authManager: AuthManager
) {

    /**
     * Check if current user can view a module
     */
    suspend fun canView(module: Module): Boolean {
        val userId = authManager.getUserId() ?: return false
        val isAdmin = authManager.isAdmin()
        return userPermissionRepository.canView(userId, isAdmin, module)
    }

    /**
     * Check if current user can view a module (string-based, for backward compatibility)
     */
    suspend fun canView(module: String): Boolean {
        if (authManager.isAdmin()) return true
        return withContext(Dispatchers.IO) {
            val userId = authManager.getUserId() ?: return@withContext false
            val permission = userPermissionRepository.getPermissionForUserAndModule(userId, module)
            permission?.canView ?: false
        }
    }

    /**
     * Check if current user can create in a module
     */
    suspend fun canCreate(module: Module): Boolean {
        val userId = authManager.getUserId() ?: return false
        val isAdmin = authManager.isAdmin()
        return userPermissionRepository.canCreate(userId, isAdmin, module)
    }

    /**
     * Check if current user can create in a module (string-based, for backward compatibility)
     */
    suspend fun canCreate(module: String): Boolean {
        if (authManager.isAdmin()) return true
        return withContext(Dispatchers.IO) {
            val userId = authManager.getUserId() ?: return@withContext false
            val permission = userPermissionRepository.getPermissionForUserAndModule(userId, module)
            permission?.canCreate ?: false
        }
    }

    /**
     * Check if current user can edit in a module
     */
    suspend fun canEdit(module: Module): Boolean {
        val userId = authManager.getUserId() ?: return false
        val isAdmin = authManager.isAdmin()
        return userPermissionRepository.canEdit(userId, isAdmin, module)
    }

    /**
     * Check if current user can edit in a module (string-based, for backward compatibility)
     */
    suspend fun canEdit(module: String): Boolean {
        if (authManager.isAdmin()) return true
        return withContext(Dispatchers.IO) {
            val userId = authManager.getUserId() ?: return@withContext false
            val permission = userPermissionRepository.getPermissionForUserAndModule(userId, module)
            permission?.canEdit ?: false
        }
    }

    /**
     * Check if current user can delete in a module
     */
    suspend fun canDelete(module: Module): Boolean {
        val userId = authManager.getUserId() ?: return false
        val isAdmin = authManager.isAdmin()
        return userPermissionRepository.canDelete(userId, isAdmin, module)
    }

    /**
     * Check if current user can delete in a module (string-based, for backward compatibility)
     */
    suspend fun canDelete(module: String): Boolean {
        if (authManager.isAdmin()) return true
        return withContext(Dispatchers.IO) {
            val userId = authManager.getUserId() ?: return@withContext false
            val permission = userPermissionRepository.getPermissionForUserAndModule(userId, module)
            permission?.canDelete ?: false
        }
    }

    /**
     * Get all permissions for current user
     */
    suspend fun getUserPermissions(): List<UserPermission> {
        return withContext(Dispatchers.IO) {
            val userId = authManager.getUserId() ?: return@withContext emptyList()
            userPermissionRepository.getPermissionsForUser(userId)
        }
    }
}
