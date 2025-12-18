package com.medistock.util

import com.medistock.data.dao.UserPermissionDao
import com.medistock.data.entities.UserPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Module names for permission management
 */
object Modules {
    const val STOCK = "STOCK"
    const val SALES = "SALES"
    const val PURCHASES = "PURCHASES"
    const val INVENTORY = "INVENTORY"
    const val ADMIN = "ADMIN"
    const val PRODUCTS = "PRODUCTS"
    const val SITES = "SITES"
    const val CATEGORIES = "CATEGORIES"
    const val USERS = "USERS"
}

/**
 * Manages user permissions
 */
class PermissionManager(
    private val userPermissionDao: UserPermissionDao,
    private val authManager: AuthManager
) {

    /**
     * Check if current user can view a module
     */
    suspend fun canView(module: String): Boolean {
        if (authManager.isAdmin()) return true
        return withContext(Dispatchers.IO) {
            val permission = userPermissionDao.getPermissionForModule(authManager.getUserId(), module)
            permission?.canView ?: false
        }
    }

    /**
     * Check if current user can create in a module
     */
    suspend fun canCreate(module: String): Boolean {
        if (authManager.isAdmin()) return true
        return withContext(Dispatchers.IO) {
            val permission = userPermissionDao.getPermissionForModule(authManager.getUserId(), module)
            permission?.canCreate ?: false
        }
    }

    /**
     * Check if current user can edit in a module
     */
    suspend fun canEdit(module: String): Boolean {
        if (authManager.isAdmin()) return true
        return withContext(Dispatchers.IO) {
            val permission = userPermissionDao.getPermissionForModule(authManager.getUserId(), module)
            permission?.canEdit ?: false
        }
    }

    /**
     * Check if current user can delete in a module
     */
    suspend fun canDelete(module: String): Boolean {
        if (authManager.isAdmin()) return true
        return withContext(Dispatchers.IO) {
            val permission = userPermissionDao.getPermissionForModule(authManager.getUserId(), module)
            permission?.canDelete ?: false
        }
    }

    /**
     * Get all permissions for current user
     */
    suspend fun getUserPermissions(): List<UserPermission> {
        return withContext(Dispatchers.IO) {
            userPermissionDao.getPermissionsForUser(authManager.getUserId())
        }
    }
}
