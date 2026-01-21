import Foundation

/// Module names for permission management - mirrors Android Modules object
enum Module: String, CaseIterable {
    case stock = "STOCK"
    case sales = "SALES"
    case purchases = "PURCHASES"
    case inventory = "INVENTORY"
    case transfers = "TRANSFERS"
    case admin = "ADMIN"
    case products = "PRODUCTS"
    case sites = "SITES"
    case categories = "CATEGORIES"
    case users = "USERS"
    case customers = "CUSTOMERS"
    case audit = "AUDIT"
    case packagingTypes = "PACKAGING_TYPES"

    var displayName: String {
        switch self {
        case .stock: return "Stock"
        case .sales: return "Ventes"
        case .purchases: return "Achats"
        case .inventory: return "Inventaire"
        case .transfers: return "Transferts"
        case .admin: return "Administration"
        case .products: return "Produits"
        case .sites: return "Sites"
        case .categories: return "Catégories"
        case .users: return "Utilisateurs"
        case .customers: return "Clients"
        case .audit: return "Audit"
        case .packagingTypes: return "Types d'emballage"
        }
    }
}

/// User permission model - mirrors Android UserPermission entity
struct UserPermission: Codable, Identifiable {
    let id: String
    let userId: String
    let module: String
    let canView: Bool
    let canCreate: Bool
    let canEdit: Bool
    let canDelete: Bool
    let createdAt: Int64
    let updatedAt: Int64
    let createdBy: String
    let updatedBy: String

    init(
        id: String = UUID().uuidString,
        userId: String,
        module: String,
        canView: Bool = false,
        canCreate: Bool = false,
        canEdit: Bool = false,
        canDelete: Bool = false,
        createdAt: Int64 = Int64(Date().timeIntervalSince1970 * 1000),
        updatedAt: Int64 = Int64(Date().timeIntervalSince1970 * 1000),
        createdBy: String = "",
        updatedBy: String = ""
    ) {
        self.id = id
        self.userId = userId
        self.module = module
        self.canView = canView
        self.canCreate = canCreate
        self.canEdit = canEdit
        self.canDelete = canDelete
        self.createdAt = createdAt
        self.updatedAt = updatedAt
        self.createdBy = createdBy
        self.updatedBy = updatedBy
    }
}

/// Manages user permissions - mirrors Android PermissionManager
/// Uses UserDefaults as local primary store for offline-first behavior
class PermissionManager: ObservableObject {
    static let shared = PermissionManager()

    @Published private(set) var permissions: [UserPermission] = []
    @Published private(set) var isLoading = false
    @Published private(set) var lastError: String?

    private let supabase = SupabaseClient.shared
    private let cacheKeyPrefix = "medistock_permissions_"

    private init() {
        loadCachedPermissions()
    }

    private func cacheKey(forUserId userId: String) -> String {
        return "\(cacheKeyPrefix)\(userId)"
    }

    // MARK: - Permission Checking (mirrors Android PermissionManager)

    /// Check if current user can view a module
    func canView(_ module: Module) -> Bool {
        if SessionManager.shared.isAdmin { return true }
        return getPermission(for: module)?.canView ?? false
    }

    /// Check if current user can create in a module
    func canCreate(_ module: Module) -> Bool {
        if SessionManager.shared.isAdmin { return true }
        return getPermission(for: module)?.canCreate ?? false
    }

    /// Check if current user can edit in a module
    func canEdit(_ module: Module) -> Bool {
        if SessionManager.shared.isAdmin { return true }
        return getPermission(for: module)?.canEdit ?? false
    }

    /// Check if current user can delete in a module
    func canDelete(_ module: Module) -> Bool {
        if SessionManager.shared.isAdmin { return true }
        return getPermission(for: module)?.canDelete ?? false
    }

    /// Get permission for a specific module
    func getPermission(for module: Module) -> UserPermission? {
        permissions.first { $0.module == module.rawValue }
    }

    // MARK: - Permission Loading (offline-first)

    /// Load permissions for current user - local first, then sync from Supabase
    func loadPermissions(forUserId userId: String) async {
        // 1. Load from local storage first (immediate)
        let localPermissions = loadPermissionsLocally(forUserId: userId)
        await MainActor.run {
            self.permissions = localPermissions
        }

        // 2. Try to sync from Supabase in background
        guard supabase.isConfigured else { return }

        await MainActor.run { isLoading = true }

        do {
            let fetchedPermissions: [UserPermission] = try await supabase.fetch(
                from: "user_permissions",
                filter: ["user_id": userId]
            )

            await MainActor.run {
                self.permissions = fetchedPermissions
                self.isLoading = false
                self.lastError = nil
                self.savePermissionsLocally(fetchedPermissions, forUserId: userId)
            }
        } catch {
            await MainActor.run {
                self.isLoading = false
                self.lastError = error.localizedDescription
                // Keep using local permissions (already loaded)
            }
        }
    }

    /// Clear current user's permissions (on logout)
    func clearPermissions() {
        let userId = SessionManager.shared.userId
        if !userId.isEmpty {
            UserDefaults.standard.removeObject(forKey: cacheKey(forUserId: userId))
        }
        permissions = []
    }

    // MARK: - Local Storage (offline-first)

    private func savePermissionsLocally(_ permissions: [UserPermission], forUserId userId: String) {
        if let data = try? JSONEncoder().encode(permissions) {
            UserDefaults.standard.set(data, forKey: cacheKey(forUserId: userId))
        }
    }

    private func loadPermissionsLocally(forUserId userId: String) -> [UserPermission] {
        guard let data = UserDefaults.standard.data(forKey: cacheKey(forUserId: userId)),
              let cached = try? JSONDecoder().decode([UserPermission].self, from: data) else {
            return []
        }
        return cached
    }

    private func loadCachedPermissions() {
        let userId = SessionManager.shared.userId
        guard !userId.isEmpty else { return }
        permissions = loadPermissionsLocally(forUserId: userId)
    }

    // MARK: - Admin functions (for permission management UI) - offline-first

    /// Save a permission - local first, then sync to Supabase
    func savePermission(_ permission: UserPermission) async throws {
        // 1. Save locally first
        var userPermissions = loadPermissionsLocally(forUserId: permission.userId)
        if let index = userPermissions.firstIndex(where: { $0.id == permission.id }) {
            userPermissions[index] = permission
        } else {
            userPermissions.append(permission)
        }
        savePermissionsLocally(userPermissions, forUserId: permission.userId)

        // Update current user's permissions if applicable
        let currentUserId = SessionManager.shared.userId
        if permission.userId == currentUserId {
            await MainActor.run {
                self.permissions = userPermissions
            }
        }

        // 2. Sync to Supabase in background (non-blocking)
        if supabase.isConfigured {
            Task {
                do {
                    _ = try await supabase.upsert(into: "user_permissions", record: permission)
                } catch {
                    print("Warning: Failed to sync permission to Supabase: \(error). Will sync on next sync cycle.")
                }
            }
        }
    }

    /// Delete a permission - local first, then sync to Supabase
    func deletePermission(_ permission: UserPermission) async throws {
        // 1. Delete locally first
        var userPermissions = loadPermissionsLocally(forUserId: permission.userId)
        userPermissions.removeAll { $0.id == permission.id }
        savePermissionsLocally(userPermissions, forUserId: permission.userId)

        // Update current user's permissions if applicable
        let currentUserId = SessionManager.shared.userId
        if permission.userId == currentUserId {
            await MainActor.run {
                self.permissions = userPermissions
            }
        }

        // 2. Sync to Supabase in background (non-blocking)
        if supabase.isConfigured {
            Task {
                do {
                    try await supabase.delete(from: "user_permissions", id: permission.id)
                } catch {
                    print("Warning: Failed to delete permission from Supabase: \(error). Will sync on next sync cycle.")
                }
            }
        }
    }

    /// Get all permissions for a specific user (admin function) - offline-first
    func getPermissions(forUserId userId: String) async throws -> [UserPermission] {
        // 1. Load from local first
        var localPermissions = loadPermissionsLocally(forUserId: userId)

        // 2. Try to fetch from Supabase and merge
        if supabase.isConfigured {
            do {
                let remotePermissions: [UserPermission] = try await supabase.fetch(
                    from: "user_permissions",
                    filter: ["user_id": userId]
                )
                // Use remote as source of truth if available
                localPermissions = remotePermissions
                savePermissionsLocally(remotePermissions, forUserId: userId)
            } catch {
                // Fall back to local permissions
                print("Warning: Failed to fetch permissions from Supabase: \(error). Using local cache.")
            }
        }

        return localPermissions
    }
}
