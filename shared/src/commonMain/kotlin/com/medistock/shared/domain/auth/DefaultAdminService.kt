package com.medistock.shared.domain.auth

import com.medistock.shared.data.repository.UserPermissionRepository
import com.medistock.shared.data.repository.UserRepository
import com.medistock.shared.domain.model.Module
import com.medistock.shared.domain.model.User
import com.medistock.shared.domain.model.UserPermission
import com.medistock.shared.config.DebugConfig
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Service for managing the local default admin user.
 *
 * Creates a local admin/admin user when the database is empty,
 * allowing the app to work offline without Supabase configuration.
 *
 * The local admin is automatically removed when real users are synced
 * from Supabase.
 */
class DefaultAdminService(
    private val userRepository: UserRepository,
    private val userPermissionRepository: UserPermissionRepository
) {
    companion object {
        /** Marker to identify the local system-created admin user */
        const val LOCAL_SYSTEM_MARKER = "LOCAL_SYSTEM_ADMIN"

        /** Default admin username */
        const val DEFAULT_ADMIN_USERNAME = "admin"

        /** Default admin full name */
        const val DEFAULT_ADMIN_FULLNAME = "Administrator"
    }

    /**
     * Creates a default local admin user if no users exist in the database.
     *
     * @param hashedPassword The BCrypt-hashed password for the admin user.
     *                       Platform-specific hashing should be done before calling this.
     * @param currentTimeMillis The current timestamp in milliseconds.
     * @return true if a default admin was created, false if users already exist.
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun createDefaultAdminIfNeeded(
        hashedPassword: String,
        currentTimeMillis: Long
    ): Boolean {
        val existingUsers = userRepository.getAll()

        if (existingUsers.isNotEmpty()) {
            DebugConfig.log("DefaultAdminService") {
                "Users already exist (${existingUsers.size}), skipping default admin creation"
            }
            return false
        }

        DebugConfig.log("DefaultAdminService") { "No users found, creating default local admin" }

        val adminUserId = Uuid.random().toString()

        // Create admin user with LOCAL_SYSTEM_MARKER to identify it as local-only
        val adminUser = User(
            id = adminUserId,
            username = DEFAULT_ADMIN_USERNAME,
            password = hashedPassword,
            fullName = DEFAULT_ADMIN_FULLNAME,
            isAdmin = true,
            isActive = true,
            createdAt = currentTimeMillis,
            updatedAt = currentTimeMillis,
            createdBy = LOCAL_SYSTEM_MARKER,
            updatedBy = LOCAL_SYSTEM_MARKER
        )

        userRepository.insert(adminUser)
        DebugConfig.log("DefaultAdminService") { "Default admin user created with id: $adminUserId" }

        // Create full permissions for all modules
        Module.entries.forEach { module ->
            val permission = UserPermission(
                id = Uuid.random().toString(),
                userId = adminUserId,
                module = module.name,
                canView = true,
                canCreate = true,
                canEdit = true,
                canDelete = true,
                createdAt = currentTimeMillis,
                updatedAt = currentTimeMillis,
                createdBy = LOCAL_SYSTEM_MARKER,
                updatedBy = LOCAL_SYSTEM_MARKER
            )
            userPermissionRepository.insert(permission)
        }

        DebugConfig.log("DefaultAdminService") {
            "Created ${Module.entries.size} permissions for default admin"
        }

        return true
    }

    /**
     * Checks if the local system admin exists.
     *
     * @return true if the local system admin exists.
     */
    suspend fun hasLocalSystemAdmin(): Boolean {
        val users = userRepository.getAll()
        return users.any { it.createdBy == LOCAL_SYSTEM_MARKER }
    }

    /**
     * Removes the local system admin if real (non-local) users exist.
     *
     * This should be called after syncing users from Supabase to clean up
     * the temporary local admin.
     *
     * @return true if the local admin was removed, false if not found or
     *         no real users exist yet.
     */
    suspend fun removeLocalAdminIfRemoteUsersExist(): Boolean {
        val allUsers = userRepository.getAll()

        val localAdmin = allUsers.find { it.createdBy == LOCAL_SYSTEM_MARKER }
        val realUsers = allUsers.filter { it.createdBy != LOCAL_SYSTEM_MARKER }

        if (localAdmin == null) {
            DebugConfig.log("DefaultAdminService") { "No local system admin to remove" }
            return false
        }

        if (realUsers.isEmpty()) {
            DebugConfig.log("DefaultAdminService") {
                "No real users synced yet, keeping local admin"
            }
            return false
        }

        DebugConfig.log("DefaultAdminService") {
            "Found ${realUsers.size} real user(s), removing local system admin"
        }

        // Delete local admin's permissions first
        val adminPermissions = userPermissionRepository.getPermissionsForUser(localAdmin.id)
        adminPermissions.forEach { permission ->
            userPermissionRepository.delete(permission.id)
        }

        // Delete local admin user
        userRepository.delete(localAdmin.id)

        DebugConfig.log("DefaultAdminService") {
            "Local system admin removed successfully"
        }

        return true
    }

    /**
     * Forces removal of the local system admin regardless of other users.
     * Use this when you want to ensure the local admin is gone (e.g., after
     * successful Supabase authentication).
     *
     * @return true if removed, false if not found.
     */
    suspend fun forceRemoveLocalAdmin(): Boolean {
        val allUsers = userRepository.getAll()
        val localAdmin = allUsers.find { it.createdBy == LOCAL_SYSTEM_MARKER }

        if (localAdmin == null) {
            return false
        }

        // Delete permissions
        val adminPermissions = userPermissionRepository.getPermissionsForUser(localAdmin.id)
        adminPermissions.forEach { permission ->
            userPermissionRepository.delete(permission.id)
        }

        // Delete user
        userRepository.delete(localAdmin.id)

        DebugConfig.log("DefaultAdminService") {
            "Local system admin force-removed"
        }

        return true
    }
}
