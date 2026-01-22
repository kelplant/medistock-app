package com.medistock.shared.data.repository

import com.medistock.shared.db.MedistockDatabase
import com.medistock.shared.domain.model.Module
import com.medistock.shared.domain.model.UserPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserPermissionRepository(private val database: MedistockDatabase) {

    private val queries = database.medistockQueries

    suspend fun getAll(): List<UserPermission> = withContext(Dispatchers.Default) {
        queries.getAllPermissions().executeAsList().map { it.toModel() }
    }

    suspend fun getById(id: String): UserPermission? = withContext(Dispatchers.Default) {
        queries.getPermissionById(id).executeAsOneOrNull()?.toModel()
    }

    suspend fun getPermissionsForUser(userId: String): List<UserPermission> = withContext(Dispatchers.Default) {
        queries.getPermissionsForUser(userId).executeAsList().map { it.toModel() }
    }

    suspend fun getPermissionForUserAndModule(userId: String, module: Module): UserPermission? = withContext(Dispatchers.Default) {
        queries.getPermissionForUserAndModule(userId, module.name).executeAsOneOrNull()?.toModel()
    }

    suspend fun getPermissionForUserAndModule(userId: String, moduleName: String): UserPermission? = withContext(Dispatchers.Default) {
        queries.getPermissionForUserAndModule(userId, moduleName).executeAsOneOrNull()?.toModel()
    }

    suspend fun insert(permission: UserPermission) = withContext(Dispatchers.Default) {
        queries.insertPermission(
            id = permission.id,
            user_id = permission.userId,
            module = permission.module,
            can_view = if (permission.canView) 1L else 0L,
            can_create = if (permission.canCreate) 1L else 0L,
            can_edit = if (permission.canEdit) 1L else 0L,
            can_delete = if (permission.canDelete) 1L else 0L,
            created_at = permission.createdAt,
            updated_at = permission.updatedAt,
            created_by = permission.createdBy,
            updated_by = permission.updatedBy
        )
    }

    suspend fun update(permission: UserPermission) = withContext(Dispatchers.Default) {
        queries.updatePermission(
            can_view = if (permission.canView) 1L else 0L,
            can_create = if (permission.canCreate) 1L else 0L,
            can_edit = if (permission.canEdit) 1L else 0L,
            can_delete = if (permission.canDelete) 1L else 0L,
            updated_at = permission.updatedAt,
            updated_by = permission.updatedBy,
            id = permission.id
        )
    }

    suspend fun upsert(permission: UserPermission) = withContext(Dispatchers.Default) {
        queries.upsertPermission(
            id = permission.id,
            user_id = permission.userId,
            module = permission.module,
            can_view = if (permission.canView) 1L else 0L,
            can_create = if (permission.canCreate) 1L else 0L,
            can_edit = if (permission.canEdit) 1L else 0L,
            can_delete = if (permission.canDelete) 1L else 0L,
            created_at = permission.createdAt,
            updated_at = permission.updatedAt,
            created_by = permission.createdBy,
            updated_by = permission.updatedBy
        )
    }

    suspend fun delete(id: String) = withContext(Dispatchers.Default) {
        queries.deletePermission(id)
    }

    suspend fun deletePermissionsForUser(userId: String) = withContext(Dispatchers.Default) {
        queries.deletePermissionsForUser(userId)
    }

    /**
     * Check if user can view a module.
     * Returns true if admin or has canView permission.
     */
    suspend fun canView(userId: String, isAdmin: Boolean, module: Module): Boolean {
        if (isAdmin) return true
        val permission = getPermissionForUserAndModule(userId, module)
        return permission?.canView ?: false
    }

    /**
     * Check if user can create in a module.
     * Returns true if admin or has canCreate permission.
     */
    suspend fun canCreate(userId: String, isAdmin: Boolean, module: Module): Boolean {
        if (isAdmin) return true
        val permission = getPermissionForUserAndModule(userId, module)
        return permission?.canCreate ?: false
    }

    /**
     * Check if user can edit in a module.
     * Returns true if admin or has canEdit permission.
     */
    suspend fun canEdit(userId: String, isAdmin: Boolean, module: Module): Boolean {
        if (isAdmin) return true
        val permission = getPermissionForUserAndModule(userId, module)
        return permission?.canEdit ?: false
    }

    /**
     * Check if user can delete in a module.
     * Returns true if admin or has canDelete permission.
     */
    suspend fun canDelete(userId: String, isAdmin: Boolean, module: Module): Boolean {
        if (isAdmin) return true
        val permission = getPermissionForUserAndModule(userId, module)
        return permission?.canDelete ?: false
    }

    private fun com.medistock.shared.db.User_permissions.toModel(): UserPermission {
        return UserPermission(
            id = id,
            userId = user_id,
            module = module,
            canView = can_view != 0L,
            canCreate = can_create != 0L,
            canEdit = can_edit != 0L,
            canDelete = can_delete != 0L,
            createdAt = created_at,
            updatedAt = updated_at,
            createdBy = created_by,
            updatedBy = updated_by
        )
    }
}
