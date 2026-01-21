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
/// Online-first: tries Supabase first, falls back to local cache if offline
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

    // MARK: - Permission Loading (online-first)

    /// Load permissions for current user - online first, fallback to local cache
    func loadPermissions(forUserId userId: String) async {
        await MainActor.run { isLoading = true }

        // Try Supabase first if configured
        if supabase.isConfigured {
            do {
                let fetchedPermissions: [UserPermission] = try await supabase.fetch(
                    from: "user_permissions",
                    filter: ["user_id": userId]
                )

                await MainActor.run {
                    self.permissions = fetchedPermissions
                    self.isLoading = false
                    self.lastError = nil
                    // Sync to local cache
                    self.savePermissionsLocally(fetchedPermissions, forUserId: userId)
                }
                return
            } catch {
                await MainActor.run {
                    self.lastError = error.localizedDescription
                }
                // Fall through to local fallback
            }
        }

        // Fallback to local cache if offline or Supabase not configured
        let localPermissions = loadPermissionsLocally(forUserId: userId)
        await MainActor.run {
            self.permissions = localPermissions
            self.isLoading = false
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

    // MARK: - Admin functions (for permission management UI) - online-first

    /// Save a permission - online first, fallback to local if offline
    func savePermission(_ permission: UserPermission) async throws {
        // Try Supabase first if configured
        if supabase.isConfigured {
            do {
                _ = try await supabase.upsert(into: "user_permissions", record: permission)
                // Success - sync to local cache
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
                return
            } catch {
                // If offline, fall through to local-only save
                throw error
            }
        } else {
            throw SupabaseError.notConfigured
        }
    }

    /// Delete a permission - online first, fallback to local if offline
    func deletePermission(_ permission: UserPermission) async throws {
        // Try Supabase first if configured
        if supabase.isConfigured {
            do {
                try await supabase.delete(from: "user_permissions", id: permission.id)
                // Success - sync to local cache
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
                return
            } catch {
                throw error
            }
        } else {
            throw SupabaseError.notConfigured
        }
    }

    /// Get all permissions for a specific user (admin function) - online first
    func getPermissions(forUserId userId: String) async throws -> [UserPermission] {
        // Try Supabase first if configured
        if supabase.isConfigured {
            do {
                let remotePermissions: [UserPermission] = try await supabase.fetch(
                    from: "user_permissions",
                    filter: ["user_id": userId]
                )
                // Sync to local cache
                savePermissionsLocally(remotePermissions, forUserId: userId)
                return remotePermissions
            } catch {
                // Fallback to local cache if offline
                print("Warning: Failed to fetch permissions from Supabase: \(error). Using local cache.")
            }
        }

        // Fallback to local cache
        return loadPermissionsLocally(forUserId: userId)
    }
}
