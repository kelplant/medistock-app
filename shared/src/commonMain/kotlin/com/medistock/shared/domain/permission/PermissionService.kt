package com.medistock.shared.domain.permission

import com.medistock.shared.data.repository.UserPermissionRepository
import com.medistock.shared.domain.model.Module
import com.medistock.shared.domain.model.UserPermission

/**
 * Permission checking result with all CRUD permissions for a module.
 */
data class ModulePermissions(
    val module: Module,
    val canView: Boolean,
    val canCreate: Boolean,
    val canEdit: Boolean,
    val canDelete: Boolean
) {
    companion object {
        /**
         * Create permissions for an admin (all permissions granted).
         */
        fun forAdmin(module: Module) = ModulePermissions(
            module = module,
            canView = true,
            canCreate = true,
            canEdit = true,
            canDelete = true
        )

        /**
         * Create permissions with no access.
         */
        fun noAccess(module: Module) = ModulePermissions(
            module = module,
            canView = false,
            canCreate = false,
            canEdit = false,
            canDelete = false
        )

        /**
         * Create from UserPermission model.
         */
        fun fromPermission(module: Module, permission: UserPermission?) = ModulePermissions(
            module = module,
            canView = permission?.canView ?: false,
            canCreate = permission?.canCreate ?: false,
            canEdit = permission?.canEdit ?: false,
            canDelete = permission?.canDelete ?: false
        )
    }
}

/**
 * Shared permission service for checking user permissions.
 * This service encapsulates the permission checking logic used by both Android and iOS.
 *
 * Usage:
 * ```
 * val service = PermissionService(repository)
 * val canEdit = service.canEdit(userId, isAdmin, Module.PRODUCTS)
 * ```
 */
class PermissionService(private val repository: UserPermissionRepository) {

    /**
     * Check if user can view a module.
     * @param userId The user's ID
     * @param isAdmin Whether the user is an admin
     * @param module The module to check
     * @return true if the user can view the module
     */
    suspend fun canView(userId: String, isAdmin: Boolean, module: Module): Boolean {
        return repository.canView(userId, isAdmin, module)
    }

    /**
     * Check if user can create in a module.
     * @param userId The user's ID
     * @param isAdmin Whether the user is an admin
     * @param module The module to check
     * @return true if the user can create in the module
     */
    suspend fun canCreate(userId: String, isAdmin: Boolean, module: Module): Boolean {
        return repository.canCreate(userId, isAdmin, module)
    }

    /**
     * Check if user can edit in a module.
     * @param userId The user's ID
     * @param isAdmin Whether the user is an admin
     * @param module The module to check
     * @return true if the user can edit in the module
     */
    suspend fun canEdit(userId: String, isAdmin: Boolean, module: Module): Boolean {
        return repository.canEdit(userId, isAdmin, module)
    }

    /**
     * Check if user can delete in a module.
     * @param userId The user's ID
     * @param isAdmin Whether the user is an admin
     * @param module The module to check
     * @return true if the user can delete in the module
     */
    suspend fun canDelete(userId: String, isAdmin: Boolean, module: Module): Boolean {
        return repository.canDelete(userId, isAdmin, module)
    }

    /**
     * Get all permissions for a module at once.
     * @param userId The user's ID
     * @param isAdmin Whether the user is an admin
     * @param module The module to check
     * @return ModulePermissions with all CRUD permissions
     */
    suspend fun getModulePermissions(userId: String, isAdmin: Boolean, module: Module): ModulePermissions {
        if (isAdmin) {
            return ModulePermissions.forAdmin(module)
        }
        val permission = repository.getPermissionForUserAndModule(userId, module)
        return ModulePermissions.fromPermission(module, permission)
    }

    /**
     * Get all permissions for a user.
     * @param userId The user's ID
     * @return List of UserPermission objects
     */
    suspend fun getUserPermissions(userId: String): List<UserPermission> {
        return repository.getPermissionsForUser(userId)
    }

    /**
     * Get permissions for all modules for a user.
     * @param userId The user's ID
     * @param isAdmin Whether the user is an admin
     * @return Map of Module to ModulePermissions
     */
    suspend fun getAllModulePermissions(userId: String, isAdmin: Boolean): Map<Module, ModulePermissions> {
        if (isAdmin) {
            return Module.entries.associateWith { ModulePermissions.forAdmin(it) }
        }

        val permissions = repository.getPermissionsForUser(userId)
        val permissionsByModule = permissions.associateBy { Module.fromName(it.module) }

        return Module.entries.associateWith { module ->
            val permission = permissionsByModule[module]
            ModulePermissions.fromPermission(module, permission)
        }
    }

    /**
     * Check if user has any permission (view, create, edit, or delete) for a module.
     * @param userId The user's ID
     * @param isAdmin Whether the user is an admin
     * @param module The module to check
     * @return true if the user has any permission for the module
     */
    suspend fun hasAnyPermission(userId: String, isAdmin: Boolean, module: Module): Boolean {
        if (isAdmin) return true
        val permissions = getModulePermissions(userId, isAdmin, module)
        return permissions.canView || permissions.canCreate || permissions.canEdit || permissions.canDelete
    }
}
