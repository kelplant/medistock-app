import Foundation
import shared

/// Local authentication result (for backward compatibility and extended cases)
/// Mirrors shared.OnlineFirstAuthResult for consistency across platforms
enum LocalAuthResult {
    case success(user: UserDTO)
    case invalidCredentials
    case userInactive
    case userNotFound
    case networkError(String)
    case notConfigured
    case networkRequired

    /// Parse error message to determine failure type
    /// Mirrors shared parseAuthError() for consistency with Android
    static func parseAuthError(_ errorMessage: String?) -> LocalAuthResult {
        guard let errorMessage = errorMessage else {
            return .networkError("Unknown error")
        }

        let lowercased = errorMessage.lowercased()

        // Invalid credentials checks (matches shared module)
        if lowercased.contains("invalid credentials") ||
           lowercased.contains("invalid password") ||
           lowercased.contains("invalid_grant") {
            return .invalidCredentials
        }

        // User inactive checks
        if lowercased.contains("deactivated") ||
           lowercased.contains("inactive") {
            return .userInactive
        }

        // User not found checks
        if lowercased.contains("not found") ||
           lowercased.contains("user not found") {
            return .userNotFound
        }

        return .networkError(errorMessage)
    }

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

/// Swift implementation of PasswordVerifier for the shared Kotlin AuthService.
/// Bridges to the existing PasswordHasher BCrypt implementation.
class SwiftPasswordVerifier: shared.PasswordVerifier {
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

    private init() {}

    /// Authenticate user with username and password
    /// Flow:
    /// 1. If online + Supabase configured: try Edge Function, fallback to local
    /// 2. Otherwise: try local authentication via shared SDK AuthService
    func authenticate(username: String, password: String, sdk: MedistockSDK) async -> AuthResult {
        let trimmedUsername = username.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedPassword = password.trimmingCharacters(in: .whitespacesAndNewlines)

        // If Supabase is configured and online, try online-first authentication
        if statusManager.isOnline && supabase.isConfigured {
            let onlineResult = await authenticateOnlineFirst(
                username: trimmedUsername,
                password: trimmedPassword,
                sdk: sdk
            )

            switch onlineResult {
            case .success:
                return onlineResult
            case .invalidCredentials, .userInactive:
                // Definitive failures — don't retry locally
                return onlineResult
            case .networkError, .userNotFound, .notConfigured, .networkRequired:
                // Edge function failed or user not in Supabase — fall back to local auth
                debugLog("AuthService", "Online auth failed, falling back to local auth")
                return await authenticateViaSharedSDK(
                    username: trimmedUsername,
                    password: trimmedPassword,
                    sdk: sdk
                )
            }
        }

        // Offline or not configured — try local authentication via shared SDK
        return await authenticateViaSharedSDK(
            username: trimmedUsername,
            password: trimmedPassword,
            sdk: sdk
        )
    }

    /// Authenticate via the shared Kotlin AuthService using a callback-based approach.
    /// This avoids the Kotlin/Native ObjCExportCoroutines crash on Xcode 26.2 where
    /// the suspend-to-async bridge is incompatible. The coroutine runs entirely within
    /// Kotlin; only the result callback crosses the ObjC bridge.
    private func authenticateViaSharedSDK(username: String, password: String, sdk: MedistockSDK) async -> AuthResult {
        return await withCheckedContinuation { continuation in
            sdk.authenticateAsync(
                passwordVerifier: SwiftPasswordVerifier(),
                username: username,
                password: password
            ) { result in
                continuation.resume(returning: LocalAuthResult.from(result))
            }
        }
    }

    /// Check if this is a first-time login
    /// First-time = no real users in local DB (only system admin marker or empty)
    private func isFirstTimeLogin(sdk: MedistockSDK) async -> Bool {
        do {
            let users = try await sdk.userRepository.getAll()

            // No users = first login
            if users.isEmpty { return true }

            // Only system admin marker = first login
            let hasRealUsers = users.contains { user in
                user.createdBy != "LOCAL_SYSTEM_MARKER"
            }
            if !hasRealUsers { return true }

            // Has valid session = not first login
            if keychain.hasAuthTokens && !keychain.areAuthTokensExpired {
                return false
            }

            return false
        } catch {
            debugLog("AuthService", "Failed to check users: \(error)")
            return true // Assume first login on error
        }
    }

    /// Online-first authentication for first-time login
    /// Authenticates directly with Supabase Edge Function, then syncs all data
    private func authenticateOnlineFirst(username: String, password: String, sdk: MedistockSDK) async -> AuthResult {
        debugLog("AuthService", "First-time login: using online-first authentication")

        do {
            let response = try await supabase.migrateUserToAuth(username: username, password: password)

            if response.success, let userData = response.user, let sessionData = response.session {
                // Create user DTO from response
                let user = UserDTO(
                    id: userData.id,
                    username: userData.username,
                    password: "", // Not stored
                    fullName: userData.name,
                    language: nil,
                    isAdmin: userData.isAdmin,
                    isActive: true,
                    createdAt: Int64(Date().timeIntervalSince1970 * 1000),
                    updatedAt: Int64(Date().timeIntervalSince1970 * 1000),
                    createdBy: userData.id,
                    updatedBy: userData.id
                )

                // Sync user to local database
                await syncUserToLocal(user, sdk: sdk)

                // Store session tokens
                keychain.storeAuthTokens(
                    accessToken: sessionData.accessToken,
                    refreshToken: sessionData.refreshToken,
                    expiresAt: sessionData.expiresAt ?? Int64(Date().timeIntervalSince1970 + 3600),
                    userId: userData.id
                )

                // Trigger full sync to get all data
                debugLog("AuthService", "First login successful, triggering full sync")
                Task {
                    await BidirectionalSyncManager.shared.fullSync(sdk: sdk)
                }

                return .success(user: user)
            } else {
                // Parse error using shared logic
                let errorMsg = response.error ?? response.message
                return LocalAuthResult.parseAuthError(errorMsg)
            }
        } catch {
            debugLog("AuthService", "Online-first auth failed: \(error)")
            return .networkError(error.localizedDescription)
        }
    }

    /// Standard authentication flow for subsequent logins
    /// Uses online-first auth to get correct UUID from Supabase, avoiding local/remote UUID mismatches
    private func authenticateStandardFlow(username: String, password: String, sdk: MedistockSDK) async -> AuthResult {
        // If online and Supabase is configured, use online-first auth
        // This ensures we get the correct UUID from Supabase
        if statusManager.isOnline && supabase.isConfigured {
            debugLog("AuthService", "Using online-first auth for user: \(username)")

            do {
                let response = try await supabase.migrateUserToAuth(username: username, password: password)

                if response.success, let userData = response.user, let sessionData = response.session {
                    debugLog("AuthService", "Online-first auth SUCCESS")

                    // Create user DTO with correct UUID from Supabase
                    let user = UserDTO(
                        id: userData.id,
                        username: userData.username,
                        password: "",
                        fullName: userData.name,
                        language: nil,
                        isAdmin: userData.isAdmin,
                        isActive: true,
                        createdAt: Int64(Date().timeIntervalSince1970 * 1000),
                        updatedAt: Int64(Date().timeIntervalSince1970 * 1000),
                        createdBy: userData.id,
                        updatedBy: userData.id
                    )

                    // Update local user with correct UUID from Supabase
                    await syncUserToLocal(user, sdk: sdk)

                    // Store session tokens
                    keychain.storeAuthTokens(
                        accessToken: sessionData.accessToken,
                        refreshToken: sessionData.refreshToken,
                        expiresAt: sessionData.expiresAt ?? Int64(Date().timeIntervalSince1970 + 3600),
                        userId: userData.id
                    )

                    return .success(user: user)
                } else {
                    // Parse error using shared logic
                    let errorMsg = response.error ?? response.message
                    let result = LocalAuthResult.parseAuthError(errorMsg)

                    // If user not found in Supabase, fall back to local auth
                    if case .userNotFound = result {
                        debugLog("AuthService", "User not in Supabase, falling back to local auth")
                    } else {
                        return result
                    }
                }
            } catch {
                debugLog("AuthService", "Online-first auth failed: \(error), falling back to local auth")
            }
        }

        // Fall back to local SDK authentication (offline mode or user not in Supabase)
        return await authenticateLocally(username: username, password: password, sdk: sdk)
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

    /// Authenticate using local SDK database
    /// Uses direct Swift password verification to avoid Kotlin/Native callback issues
    private func authenticateLocally(username: String, password: String, sdk: MedistockSDK) async -> AuthResult {
        do {
            // Use the SDK to get user by username
            let user = try await sdk.userRepository.getByUsername(username: username)

            guard let user = user else {
                return .userNotFound
            }

            // Check if user is active
            guard user.isActive else {
                return .userInactive
            }

            // Validate password using Swift's PasswordHasher (avoids Kotlin->Swift callback)
            guard PasswordHasher.shared.verifyPassword(password, storedPassword: user.password) else {
                return .invalidCredentials
            }

            // Convert to UserDTO format
            let userDTO = UserDTO(from: user)
            return .success(user: userDTO)
        } catch {
            return .networkError(error.localizedDescription)
        }
    }

    /// Sync user to local database
    /// Preserves existing password hash (Edge Function doesn't return it)
    private func syncUserToLocal(_ userDTO: UserDTO, sdk: MedistockSDK) async {
        do {
            // Check if user already exists locally with a password
            if let existingUser = try? await sdk.userRepository.getById(id: userDTO.id),
               !existingUser.password.isEmpty {
                // Preserve the existing password hash
                let userWithPassword = UserDTO(
                    id: userDTO.id,
                    username: userDTO.username,
                    password: existingUser.password,
                    fullName: userDTO.fullName,
                    language: userDTO.language,
                    isAdmin: userDTO.isAdmin,
                    isActive: userDTO.isActive,
                    createdAt: userDTO.createdAt,
                    updatedAt: userDTO.updatedAt,
                    createdBy: userDTO.createdBy,
                    updatedBy: userDTO.updatedBy
                )
                try await sdk.userRepository.upsert(user: userWithPassword.toEntity())
            } else {
                // New user or no existing password
                try await sdk.userRepository.upsert(user: userDTO.toEntity())
            }
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
        self.language = user.language
        self.isLoggedIn = true

        // Update auth session status (tokens may have been stored during auth)
        refreshAuthStatus()

        // Load language from user profile (with fallback to cache/system)
        Localized.loadLanguageFromProfile(user: user.toEntity())

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
