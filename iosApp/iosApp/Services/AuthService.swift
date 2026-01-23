import Foundation
import shared

/// Local authentication result (for backward compatibility and extended cases)
enum LocalAuthResult {
    case success(user: UserDTO)
    case invalidCredentials
    case userInactive
    case userNotFound
    case networkError(String)
    case notConfigured

    /// Convert from shared AuthResult
    static func from(_ sharedResult: shared.AuthResult, toDTO: ((shared.User) -> UserDTO)? = nil) -> LocalAuthResult {
        switch sharedResult {
        case let success as shared.AuthResult.Success:
            if let converter = toDTO {
                return .success(user: converter(success.user))
            } else {
                return .success(user: UserDTO(from: success.user))
            }
        case is shared.AuthResult.InvalidCredentials:
            return .invalidCredentials
        case is shared.AuthResult.UserInactive:
            return .userInactive
        case is shared.AuthResult.UserNotFound:
            return .userNotFound
        case let error as shared.AuthResult.Error:
            return .networkError(error.message)
        default:
            return .networkError("Unknown error")
        }
    }
}

/// Backward compatibility alias
typealias AuthResult = LocalAuthResult

/// iOS implementation of PasswordVerifier for shared AuthService
class SwiftPasswordVerifier: shared.PasswordVerifier {
    static let instance = SwiftPasswordVerifier()
    private init() {}

    func verify(plainPassword: String, hashedPassword: String) -> Bool {
        return PasswordHasher.shared.verifyPassword(plainPassword, storedPassword: hashedPassword)
    }
}

/// Authentication service that properly validates credentials
/// Uses Supabase Auth with UUID-based emails for online authentication
/// Falls back to local BCrypt authentication when offline
class AuthService {
    static let shared = AuthService()

    private let supabase = SupabaseService.shared
    private let statusManager = SyncStatusManager.shared
    private let keychain = KeychainService.shared

    /// Shared auth service for local authentication (lazy initialized)
    private var sharedAuthService: shared.AuthService?

    private init() {}

    /// Get or create the shared AuthService
    private func getSharedAuthService(sdk: MedistockSDK) -> shared.AuthService {
        if sharedAuthService == nil {
            sharedAuthService = sdk.createAuthService(passwordVerifier: SwiftPasswordVerifier.instance)
        }
        return sharedAuthService!
    }

    /// Authenticate user with username and password
    /// Flow:
    /// 1. Look up username in local DB to get UUID
    /// 2. If online & configured, try Supabase Auth with {uuid}@medistock.local
    /// 3. If auth fails, call migrate-user-to-auth Edge Function for BCrypt migration
    /// 4. Store session tokens securely
    /// 5. Fall back to local auth if offline
    func authenticate(username: String, password: String, sdk: MedistockSDK) async -> AuthResult {
        let trimmedUsername = username.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedPassword = password.trimmingCharacters(in: .whitespacesAndNewlines)

        // First, look up the user locally to get their UUID
        let localUser = await lookupLocalUser(username: trimmedUsername, sdk: sdk)

        // If online and Supabase is configured, use Supabase Auth
        if statusManager.isOnline && supabase.isConfigured {
            let result = await authenticateWithSupabaseAuth(
                username: trimmedUsername,
                password: trimmedPassword,
                localUser: localUser,
                sdk: sdk
            )
            // If we got a definitive answer (not network error), return it
            switch result {
            case .success, .invalidCredentials, .userInactive, .userNotFound:
                return result
            case .networkError, .notConfigured:
                // Fall through to local authentication
                debugLog("AuthService", "Supabase Auth failed, falling back to local auth")
                break
            }
        }

        // Fall back to local SDK authentication
        return await authenticateLocally(username: trimmedUsername, password: trimmedPassword, sdk: sdk)
    }

    /// Look up user in local database by username
    private func lookupLocalUser(username: String, sdk: MedistockSDK) async -> UserDTO? {
        do {
            if let user = try await sdk.userRepository.getByUsername(username: username) {
                return UserDTO(from: user)
            }
        } catch {
            debugLog("AuthService", "Failed to lookup local user: \(error)")
        }
        return nil
    }

    /// Authenticate using Supabase Auth with UUID-based email
    private func authenticateWithSupabaseAuth(username: String, password: String, localUser: UserDTO?, sdk: MedistockSDK) async -> AuthResult {
        // Step 1: Get user UUID - either from local DB or fetch from Supabase
        var user: UserDTO? = localUser

        if user == nil {
            // Try to fetch user from Supabase to get their UUID
            do {
                let users: [UserDTO] = try await supabase.fetchWithFilter(
                    from: "app_users",
                    filter: "username=eq.\(username)"
                )
                user = users.first
            } catch {
                debugLog("AuthService", "Failed to fetch user from Supabase: \(error)")
                return .networkError(error.localizedDescription)
            }
        }

        guard let user = user else {
            return .userNotFound
        }

        // Check if user is active
        guard user.isActive else {
            return .userInactive
        }

        let uuid = user.id

        // Step 2: Try Supabase Auth sign-in
        do {
            debugLog("AuthService", "Attempting Supabase Auth sign-in for UUID: \(uuid)")
            _ = try await supabase.signIn(uuid: uuid, password: password)

            // Auth successful - sync user to local and return
            await syncUserToLocal(user, sdk: sdk)
            return .success(user: user)

        } catch {
            debugLog("AuthService", "Supabase Auth sign-in failed: \(error)")

            // Step 3: Auth failed - try migration via Edge Function
            // This handles users who have BCrypt passwords but no Supabase Auth account yet
            return await migrateAndAuthenticate(user: user, password: password, sdk: sdk)
        }
    }

    /// Attempt to migrate user to Supabase Auth and authenticate
    private func migrateAndAuthenticate(user: UserDTO, password: String, sdk: MedistockSDK) async -> AuthResult {
        do {
            debugLog("AuthService", "Attempting user migration for username: \(user.username)")
            let response = try await supabase.migrateUserToAuth(username: user.username, password: password)

            if response.success {
                debugLog("AuthService", "Migration successful")
                // Sync user to local and return success
                await syncUserToLocal(user, sdk: sdk)
                return .success(user: user)
            } else {
                // Migration failed - likely invalid credentials
                debugLog("AuthService", "Migration failed: \(response.message ?? "Unknown error")")
                if response.message?.lowercased().contains("invalid") == true ||
                   response.message?.lowercased().contains("credentials") == true {
                    return .invalidCredentials
                }
                return .networkError(response.message ?? "Migration failed")
            }
        } catch {
            debugLog("AuthService", "Migration Edge Function call failed: \(error)")
            return .networkError(error.localizedDescription)
        }
    }

    /// Authenticate using local SDK database via shared AuthService
    private func authenticateLocally(username: String, password: String, sdk: MedistockSDK) async -> AuthResult {
        let authService = getSharedAuthService(sdk: sdk)

        do {
            // Use shared AuthService for authentication
            let result = try await authService.authenticate(username: username, password: password)

            // Convert shared AuthResult to local AuthResult
            return LocalAuthResult.from(result) { sharedUser in
                UserDTO(from: sharedUser)
            }
        } catch {
            return .networkError(error.localizedDescription)
        }
    }

    /// Sync user to local database
    private func syncUserToLocal(_ userDTO: UserDTO, sdk: MedistockSDK) async {
        do {
            // Use upsert (INSERT OR REPLACE) to handle both new and existing records
            try await sdk.userRepository.upsert(user: userDTO.toEntity())
            debugLog("AuthService", "User synced to local: \(userDTO.username)")
        } catch {
            debugLog("AuthService", "Failed to sync user to local: \(error)")
        }
    }

    /// Sign out the current user from Supabase Auth
    func signOut() async {
        do {
            try await supabase.signOut()
        } catch {
            debugLog("AuthService", "Sign out failed: \(error)")
        }
        // Clear local tokens regardless of sign out success
        keychain.clearAuthTokens()
    }

    /// Check if user has valid auth tokens
    var hasValidSession: Bool {
        return keychain.hasAuthTokens && !keychain.areAuthTokensExpired
    }
}

// MARK: - Session Management Extension

extension SessionManager {
    /// Login with proper authentication and trigger initial sync
    func loginWithAuth(user: UserDTO, sdk: MedistockSDK) {
        self.userId = user.id
        self.username = user.username
        self.fullName = user.fullName
        self.isAdmin = user.isAdmin
        self.isLoggedIn = true

        // Update auth session status (tokens may have been stored during auth)
        refreshAuthStatus()

        // Load permissions for this user
        Task {
            await PermissionManager.shared.loadPermissions(forUserId: user.id)
        }

        // Start sync scheduler
        SyncScheduler.shared.start(sdk: sdk)

        // Trigger initial full sync if online
        if SyncStatusManager.shared.isOnline && SupabaseService.shared.isConfigured {
            Task {
                await BidirectionalSyncManager.shared.fullSync(sdk: sdk)
            }
        }

        // Run pending migrations if authenticated
        if hasAuthSession {
            Task {
                await runPendingMigrationsIfNeeded()
            }
        }
    }

    /// Run pending database migrations if user is authenticated
    private func runPendingMigrationsIfNeeded() async {
        guard SupabaseService.shared.isConfigured else { return }

        guard let migrationManager = MigrationManager() else {
            debugLog("SessionManager", "Could not initialize MigrationManager")
            return
        }
        let result = await migrationManager.runPendingMigrations(appliedBy: "ios_app_\(userId)")

        if result.success {
            if !result.migrationsApplied.isEmpty {
                debugLog("SessionManager", "Applied \(result.migrationsApplied.count) migrations")
            }
        } else if !result.systemNotInstalled {
            debugLog("SessionManager", "Migration failed: \(result.errorMessage ?? "Unknown error")")
        }
    }

    /// Logout and clear all session data
    func logoutWithCleanup() {
        // Stop sync scheduler first
        SyncScheduler.shared.stop()

        self.isLoggedIn = false
        self.userId = ""
        self.username = ""
        self.fullName = ""
        self.isAdmin = false
        self.currentSiteId = nil
        self.hasAuthSession = false

        // Clear permissions
        PermissionManager.shared.clearPermissions()

        // Sign out from Supabase Auth and clear tokens
        Task {
            await AuthService.shared.signOut()
        }
    }
}
