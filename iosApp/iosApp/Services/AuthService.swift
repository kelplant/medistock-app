import Foundation
import shared

/// Authentication result
enum AuthResult {
    case success(user: UserDTO)
    case invalidCredentials
    case userInactive
    case userNotFound
    case networkError(String)
    case notConfigured
}

/// Authentication service that properly validates credentials
/// Mirrors Android AuthManager functionality with Supabase integration
class AuthService {
    static let shared = AuthService()

    private let supabase = SupabaseService.shared
    private let statusManager = SyncStatusManager.shared

    private init() {}

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

    /// Authenticate using local SDK database
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

            // Validate password using BCrypt
            guard PasswordHasher.shared.verifyPassword(password, storedPassword: user.password) else {
                return .invalidCredentials
            }

            // Convert to UserDTO format for consistency
            let userDTO = UserDTO(from: user)

            return .success(user: userDTO)
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
