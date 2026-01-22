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
/// Mirrors Android AuthManager functionality with Supabase integration
class AuthService {
    static let shared = AuthService()

    private let supabase = SupabaseService.shared
    private let statusManager = SyncStatusManager.shared

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
    func authenticate(username: String, password: String, sdk: MedistockSDK) async -> AuthResult {
        let trimmedUsername = username.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedPassword = password.trimmingCharacters(in: .whitespacesAndNewlines)

        // First, try to authenticate against Supabase if configured and online
        if statusManager.isOnline && supabase.isConfigured {
            let result = await authenticateWithSupabase(username: trimmedUsername, password: trimmedPassword, sdk: sdk)
            // If we got a definitive answer (not network error), return it
            switch result {
            case .success, .invalidCredentials, .userInactive, .userNotFound:
                return result
            case .networkError, .notConfigured:
                // Fall through to local authentication
                break
            }
        }

        // Fall back to local SDK authentication
        return await authenticateLocally(username: trimmedUsername, password: trimmedPassword, sdk: sdk)
    }

    /// Authenticate against Supabase
    private func authenticateWithSupabase(username: String, password: String, sdk: MedistockSDK) async -> AuthResult {
        do {
            // Fetch user by username using filter
            let users: [UserDTO] = try await supabase.fetchWithFilter(
                from: "app_users",
                filter: "username=eq.\(username)"
            )

            guard let user = users.first else {
                return .userNotFound
            }

            // Check if user is active
            guard user.isActive else {
                return .userInactive
            }

            // Validate password using BCrypt
            guard PasswordHasher.shared.verifyPassword(password, storedPassword: user.password) else {
                return .invalidCredentials
            }

            // Sync user to local database
            await syncUserToLocal(user, sdk: sdk)

            return .success(user: user)

        } catch {
            print("[AuthService] Supabase authentication failed: \(error)")
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
            let localUser = userDTO.toEntity()
            let existing = try? await sdk.userRepository.getById(id: userDTO.id)
            if existing != nil {
                try await sdk.userRepository.update(user: localUser)
            } else {
                try await sdk.userRepository.insert(user: localUser)
            }
            print("[AuthService] User synced to local: \(userDTO.username)")
        } catch {
            print("[AuthService] Failed to sync user to local: \(error)")
        }
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

        // Clear permissions
        PermissionManager.shared.clearPermissions()
    }
}
