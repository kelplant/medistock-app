package com.medistock.util

import com.medistock.shared.MedistockSDK
import com.medistock.shared.domain.model.Module
import com.medistock.shared.domain.model.User
import com.medistock.shared.domain.model.UserPermission
import java.util.UUID

/**
 * Utility class to seed test users for Maestro E2E permission tests.
 *
 * Usage: Call TestUserSeeder.seedTestUsers(sdk) during test setup or from a debug menu.
 *
 * All test users have the password: Test123!
 */
object TestUserSeeder {

    private const val TEST_PASSWORD = "Test123!"
    private const val SEEDER_ID = "test-seeder"

    /**
     * Test user configurations.
     * Each entry: username -> (fullName, isAdmin, list of modules with permissions)
     */
    private data class TestUserConfig(
        val username: String,
        val fullName: String,
        val isAdmin: Boolean = false,
        val permissions: Map<Module, PermissionFlags> = emptyMap()
    )

    private data class PermissionFlags(
        val canView: Boolean = false,
        val canCreate: Boolean = false,
        val canEdit: Boolean = false,
        val canDelete: Boolean = false
    ) {
        companion object {
            val FULL_CRUD = PermissionFlags(true, true, true, true)
            val VIEW_ONLY = PermissionFlags(true, false, false, false)
        }
    }

    private val testUsers = listOf(
        // No permissions user
        TestUserConfig(
            username = "test_no_permission",
            fullName = "Test No Permission"
        ),

        // Single module users - full CRUD
        TestUserConfig(
            username = "test_sites_only",
            fullName = "Test Sites Only",
            permissions = mapOf(Module.SITES to PermissionFlags.FULL_CRUD)
        ),
        TestUserConfig(
            username = "test_products_only",
            fullName = "Test Products Only",
            permissions = mapOf(Module.PRODUCTS to PermissionFlags.FULL_CRUD)
        ),
        TestUserConfig(
            username = "test_categories_only",
            fullName = "Test Categories Only",
            permissions = mapOf(Module.CATEGORIES to PermissionFlags.FULL_CRUD)
        ),
        TestUserConfig(
            username = "test_customers_only",
            fullName = "Test Customers Only",
            permissions = mapOf(Module.CUSTOMERS to PermissionFlags.FULL_CRUD)
        ),
        TestUserConfig(
            username = "test_packaging_only",
            fullName = "Test Packaging Only",
            permissions = mapOf(Module.PACKAGING_TYPES to PermissionFlags.FULL_CRUD)
        ),
        TestUserConfig(
            username = "test_users_only",
            fullName = "Test Users Only",
            permissions = mapOf(Module.USERS to PermissionFlags.FULL_CRUD)
        ),

        // Single module users - view only
        TestUserConfig(
            username = "test_stock_only",
            fullName = "Test Stock View Only",
            permissions = mapOf(Module.STOCK to PermissionFlags.VIEW_ONLY)
        ),
        TestUserConfig(
            username = "test_audit_only",
            fullName = "Test Audit View Only",
            permissions = mapOf(Module.AUDIT to PermissionFlags.VIEW_ONLY)
        ),

        // Operation users - full CRUD
        TestUserConfig(
            username = "test_purchases_only",
            fullName = "Test Purchases Only",
            permissions = mapOf(Module.PURCHASES to PermissionFlags.FULL_CRUD)
        ),
        TestUserConfig(
            username = "test_sales_only",
            fullName = "Test Sales Only",
            permissions = mapOf(Module.SALES to PermissionFlags.FULL_CRUD)
        ),
        TestUserConfig(
            username = "test_transfers_only",
            fullName = "Test Transfers Only",
            permissions = mapOf(Module.TRANSFERS to PermissionFlags.FULL_CRUD)
        ),
        TestUserConfig(
            username = "test_inventory_only",
            fullName = "Test Inventory Only",
            permissions = mapOf(Module.INVENTORY to PermissionFlags.FULL_CRUD)
        ),

        // CRUD granularity users (for Products module)
        TestUserConfig(
            username = "test_products_view",
            fullName = "Test Products View Only",
            permissions = mapOf(Module.PRODUCTS to PermissionFlags.VIEW_ONLY)
        ),
        TestUserConfig(
            username = "test_products_create",
            fullName = "Test Products Create",
            permissions = mapOf(Module.PRODUCTS to PermissionFlags(true, true, false, false))
        ),
        TestUserConfig(
            username = "test_products_edit",
            fullName = "Test Products Edit",
            permissions = mapOf(Module.PRODUCTS to PermissionFlags(true, false, true, false))
        ),
        TestUserConfig(
            username = "test_products_delete",
            fullName = "Test Products Delete",
            permissions = mapOf(Module.PRODUCTS to PermissionFlags(true, false, false, true))
        ),

        // Multi-permission user
        TestUserConfig(
            username = "test_multi_perm",
            fullName = "Test Multi Permission",
            permissions = mapOf(
                Module.SITES to PermissionFlags.FULL_CRUD,
                Module.PRODUCTS to PermissionFlags.FULL_CRUD,
                Module.SALES to PermissionFlags.FULL_CRUD
            )
        ),

        // Admin user (for reference)
        TestUserConfig(
            username = "test_admin",
            fullName = "Test Admin",
            isAdmin = true
        )
    )

    /**
     * Seed all test users into the database.
     * Existing users with the same username will be skipped.
     *
     * @param sdk The MedistockSDK instance
     * @return Number of users created
     */
    suspend fun seedTestUsers(sdk: MedistockSDK): Int {
        val currentTime = System.currentTimeMillis()
        val hashedPassword = PasswordHasher.hashPassword(TEST_PASSWORD)
        var createdCount = 0

        for (config in testUsers) {
            // Check if user already exists
            val existingUser = sdk.userRepository.getByUsername(config.username)
            if (existingUser != null) {
                println("TestUserSeeder: User ${config.username} already exists, skipping")
                continue
            }

            // Create user
            val userId = UUID.randomUUID().toString()
            val user = User(
                id = userId,
                username = config.username,
                password = hashedPassword,
                fullName = config.fullName,
                isAdmin = config.isAdmin,
                isActive = true,
                createdAt = currentTime,
                updatedAt = currentTime,
                createdBy = SEEDER_ID,
                updatedBy = SEEDER_ID
            )

            sdk.userRepository.insert(user)
            println("TestUserSeeder: Created user ${config.username}")

            // Create permissions (only for non-admin users)
            if (!config.isAdmin) {
                for ((module, flags) in config.permissions) {
                    val permission = UserPermission(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        module = module.name,
                        canView = flags.canView,
                        canCreate = flags.canCreate,
                        canEdit = flags.canEdit,
                        canDelete = flags.canDelete,
                        createdAt = currentTime,
                        updatedAt = currentTime,
                        createdBy = SEEDER_ID,
                        updatedBy = SEEDER_ID
                    )
                    sdk.userPermissionRepository.insert(permission)
                }
                println("TestUserSeeder: Created ${config.permissions.size} permissions for ${config.username}")
            }

            createdCount++
        }

        println("TestUserSeeder: Seeding complete. Created $createdCount users.")
        return createdCount
    }

    /**
     * Remove all test users from the database.
     * Only removes users created by the seeder (identified by username prefix).
     *
     * @param sdk The MedistockSDK instance
     * @return Number of users removed
     */
    suspend fun removeTestUsers(sdk: MedistockSDK): Int {
        var removedCount = 0

        for (config in testUsers) {
            val existingUser = sdk.userRepository.getByUsername(config.username)
            if (existingUser != null) {
                // Delete permissions first
                sdk.userPermissionRepository.deletePermissionsForUser(existingUser.id)
                // Delete user
                sdk.userRepository.delete(existingUser.id)
                println("TestUserSeeder: Removed user ${config.username}")
                removedCount++
            }
        }

        println("TestUserSeeder: Cleanup complete. Removed $removedCount users.")
        return removedCount
    }

    /**
     * Get the list of test usernames.
     */
    fun getTestUsernames(): List<String> = testUsers.map { it.username }

    /**
     * Get the test password (for documentation/test scripts).
     */
    fun getTestPassword(): String = TEST_PASSWORD
}
