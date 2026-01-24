import Foundation
import shared

/// Utility class to seed test users for Maestro E2E permission tests.
///
/// Usage: Call TestUserSeeder.seedTestUsers(sdk:) during test setup.
///
/// All test users have the password: Test123!
final class TestUserSeeder {

    static let instance = TestUserSeeder()

    private let testPassword = "Test123!"
    private let seederId = "test-seeder"

    private init() {}

    // MARK: - Test User Configurations

    private struct TestUserConfig {
        let username: String
        let fullName: String
        let isAdmin: Bool
        let permissions: [String: PermissionFlags]

        init(username: String, fullName: String, isAdmin: Bool = false, permissions: [String: PermissionFlags] = [:]) {
            self.username = username
            self.fullName = fullName
            self.isAdmin = isAdmin
            self.permissions = permissions
        }
    }

    private struct PermissionFlags {
        let canView: Bool
        let canCreate: Bool
        let canEdit: Bool
        let canDelete: Bool

        static let fullCrud = PermissionFlags(canView: true, canCreate: true, canEdit: true, canDelete: true)
        static let viewOnly = PermissionFlags(canView: true, canCreate: false, canEdit: false, canDelete: false)
    }

    private let testUsers: [TestUserConfig] = [
        // No permissions user
        TestUserConfig(username: "test_no_permission", fullName: "Test No Permission"),

        // Single module users - full CRUD
        TestUserConfig(username: "test_sites_only", fullName: "Test Sites Only", permissions: ["SITES": .fullCrud]),
        TestUserConfig(username: "test_products_only", fullName: "Test Products Only", permissions: ["PRODUCTS": .fullCrud]),
        TestUserConfig(username: "test_categories_only", fullName: "Test Categories Only", permissions: ["CATEGORIES": .fullCrud]),
        TestUserConfig(username: "test_customers_only", fullName: "Test Customers Only", permissions: ["CUSTOMERS": .fullCrud]),
        TestUserConfig(username: "test_packaging_only", fullName: "Test Packaging Only", permissions: ["PACKAGING_TYPES": .fullCrud]),
        TestUserConfig(username: "test_users_only", fullName: "Test Users Only", permissions: ["USERS": .fullCrud]),

        // Single module users - view only
        TestUserConfig(username: "test_stock_only", fullName: "Test Stock View Only", permissions: ["STOCK": .viewOnly]),
        TestUserConfig(username: "test_audit_only", fullName: "Test Audit View Only", permissions: ["AUDIT": .viewOnly]),

        // Operation users - full CRUD
        TestUserConfig(username: "test_purchases_only", fullName: "Test Purchases Only", permissions: ["PURCHASES": .fullCrud]),
        TestUserConfig(username: "test_sales_only", fullName: "Test Sales Only", permissions: ["SALES": .fullCrud]),
        TestUserConfig(username: "test_transfers_only", fullName: "Test Transfers Only", permissions: ["TRANSFERS": .fullCrud]),
        TestUserConfig(username: "test_inventory_only", fullName: "Test Inventory Only", permissions: ["INVENTORY": .fullCrud]),

        // CRUD granularity users (for Products module)
        TestUserConfig(username: "test_products_view", fullName: "Test Products View Only", permissions: ["PRODUCTS": .viewOnly]),
        TestUserConfig(username: "test_products_create", fullName: "Test Products Create", permissions: ["PRODUCTS": PermissionFlags(canView: true, canCreate: true, canEdit: false, canDelete: false)]),
        TestUserConfig(username: "test_products_edit", fullName: "Test Products Edit", permissions: ["PRODUCTS": PermissionFlags(canView: true, canCreate: false, canEdit: true, canDelete: false)]),
        TestUserConfig(username: "test_products_delete", fullName: "Test Products Delete", permissions: ["PRODUCTS": PermissionFlags(canView: true, canCreate: false, canEdit: false, canDelete: true)]),

        // Multi-permission user
        TestUserConfig(username: "test_multi_perm", fullName: "Test Multi Permission", permissions: [
            "SITES": .fullCrud,
            "PRODUCTS": .fullCrud,
            "SALES": .fullCrud
        ]),

        // Admin user (for reference)
        TestUserConfig(username: "test_admin", fullName: "Test Admin", isAdmin: true)
    ]

    // MARK: - Public Methods

    /// Seed all test users into the database.
    /// Existing users with the same username will be skipped.
    ///
    /// - Parameter sdk: The MedistockSDK instance
    /// - Returns: Number of users created
    func seedTestUsers(sdk: MedistockSDK) async -> Int {
        let currentTime = Int64(Date().timeIntervalSince1970 * 1000)
        guard let hashedPassword = PasswordHasher.shared.hashPassword(testPassword) else {
            print("TestUserSeeder: Failed to hash password")
            return 0
        }

        var createdCount = 0

        for config in testUsers {
            do {
                // Check if user already exists
                let existingUser = try await sdk.userRepository.getByUsername(username: config.username)
                if existingUser != nil {
                    print("TestUserSeeder: User \(config.username) already exists, skipping")
                    continue
                }

                // Create user
                let userId = UUID().uuidString
                let user = User(
                    id: userId,
                    username: config.username,
                    password: hashedPassword,
                    fullName: config.fullName,
                    language: nil,
                    isAdmin: config.isAdmin,
                    isActive: true,
                    createdAt: currentTime,
                    updatedAt: currentTime,
                    createdBy: seederId,
                    updatedBy: seederId
                )

                try await sdk.userRepository.insert(user: user)
                print("TestUserSeeder: Created user \(config.username)")

                // Create permissions (only for non-admin users)
                if !config.isAdmin {
                    for (module, flags) in config.permissions {
                        let permission = shared.UserPermission(
                            id: UUID().uuidString,
                            userId: userId,
                            module: module,
                            canView: flags.canView,
                            canCreate: flags.canCreate,
                            canEdit: flags.canEdit,
                            canDelete: flags.canDelete,
                            createdAt: currentTime,
                            updatedAt: currentTime,
                            createdBy: seederId,
                            updatedBy: seederId
                        )
                        try await sdk.userPermissionRepository.insert(permission: permission)
                    }
                    print("TestUserSeeder: Created \(config.permissions.count) permissions for \(config.username)")
                }

                createdCount += 1
            } catch {
                print("TestUserSeeder: Error creating user \(config.username): \(error)")
            }
        }

        print("TestUserSeeder: Seeding complete. Created \(createdCount) users.")
        return createdCount
    }

    /// Remove all test users from the database.
    /// Only removes users created by the seeder (identified by username prefix).
    ///
    /// - Parameter sdk: The MedistockSDK instance
    /// - Returns: Number of users removed
    func removeTestUsers(sdk: MedistockSDK) async -> Int {
        var removedCount = 0

        for config in testUsers {
            do {
                let existingUser = try await sdk.userRepository.getByUsername(username: config.username)
                if let user = existingUser {
                    // Delete permissions first
                    try await sdk.userPermissionRepository.deletePermissionsForUser(userId: user.id)
                    // Delete user
                    try await sdk.userRepository.delete(id: user.id)
                    print("TestUserSeeder: Removed user \(config.username)")
                    removedCount += 1
                }
            } catch {
                print("TestUserSeeder: Error removing user \(config.username): \(error)")
            }
        }

        print("TestUserSeeder: Cleanup complete. Removed \(removedCount) users.")
        return removedCount
    }

    /// Get the list of test usernames.
    func getTestUsernames() -> [String] {
        testUsers.map { $0.username }
    }

    /// Get the test password (for documentation/test scripts).
    func getTestPassword() -> String {
        testPassword
    }
}
