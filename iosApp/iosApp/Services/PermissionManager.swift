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
class PermissionManager: ObservableObject {
    static let shared = PermissionManager()

    @Published private(set) var permissions: [UserPermission] = []
    @Published private(set) var isLoading = false
    @Published private(set) var lastError: String?

    private let supabase = SupabaseClient.shared
    private let cacheKey = "medistock_permissions_cache"

    private init() {
        loadCachedPermissions()
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

    // MARK: - Permission Loading

    /// Load permissions for current user from Supabase
    func loadPermissions(forUserId userId: String) async {
        guard supabase.isConfigured else {
            // Load from cache if Supabase not configured
            loadCachedPermissions()
            return
        }

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
                self.cachePermissions(fetchedPermissions)
            }
        } catch {
            await MainActor.run {
                self.isLoading = false
                self.lastError = error.localizedDescription
                // Fall back to cached permissions
                self.loadCachedPermissions()
            }
        }
    }

    /// Clear all permissions (on logout)
    func clearPermissions() {
        permissions = []
        UserDefaults.standard.removeObject(forKey: cacheKey)
    }

    // MARK: - Caching

    private func cachePermissions(_ permissions: [UserPermission]) {
        if let data = try? JSONEncoder().encode(permissions) {
            UserDefaults.standard.set(data, forKey: cacheKey)
        }
    }

    private func loadCachedPermissions() {
        guard let data = UserDefaults.standard.data(forKey: cacheKey),
              let cached = try? JSONDecoder().decode([UserPermission].self, from: data) else {
            return
        }
        permissions = cached
    }

    // MARK: - Admin functions (for permission management UI)

    /// Save a permission to Supabase
    func savePermission(_ permission: UserPermission) async throws {
        guard supabase.isConfigured else {
            throw SupabaseError.notConfigured
        }

        _ = try await supabase.upsert(into: "user_permissions", record: permission)

        // Only reload permissions if we're editing the current user's permissions
        // to avoid overwriting the current user's cache with another user's permissions
        let currentUserId = SessionManager.shared.userId
        if permission.userId == currentUserId {
            await loadPermissions(forUserId: permission.userId)
        }
    }

    /// Delete a permission
    func deletePermission(_ permission: UserPermission) async throws {
        guard supabase.isConfigured else {
            throw SupabaseError.notConfigured
        }

        try await supabase.delete(from: "user_permissions", id: permission.id)

        // Only reload permissions if we're editing the current user's permissions
        let currentUserId = SessionManager.shared.userId
        if permission.userId == currentUserId {
            await loadPermissions(forUserId: permission.userId)
        }
    }

    /// Get all permissions for a specific user (admin function)
    func getPermissions(forUserId userId: String) async throws -> [UserPermission] {
        guard supabase.isConfigured else {
            throw SupabaseError.notConfigured
        }

        return try await supabase.fetch(from: "user_permissions", filter: ["user_id": userId])
    }
}
