import Foundation
import shared

/// Remote user model for Supabase - matches the app_users table
struct RemoteUser: Codable, Identifiable {
    let id: String
    let username: String
    let password: String
    let fullName: String
    let isAdmin: Bool
    let isActive: Bool
    let createdAt: Int64
    let updatedAt: Int64
    let createdBy: String
    let updatedBy: String
}

/// Authentication result
enum AuthResult {
    case success(user: RemoteUser)
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

    private let supabase = SupabaseClient.shared

    private init() {}

    /// Authenticate user with username and password
    /// This replaces the previous "demo mode" that allowed any login
    func authenticate(username: String, password: String, sdk: MedistockSDK) async -> AuthResult {
        let trimmedUsername = username.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedPassword = password.trimmingCharacters(in: .whitespacesAndNewlines)

        // First, try to authenticate against Supabase if configured
        if supabase.isConfigured {
            return await authenticateWithSupabase(username: trimmedUsername, password: trimmedPassword)
        }

        // Fall back to local SDK authentication
        return await authenticateLocally(username: trimmedUsername, password: trimmedPassword, sdk: sdk)
    }

    /// Authenticate against Supabase
    private func authenticateWithSupabase(username: String, password: String) async -> AuthResult {
        do {
            // Fetch user by username
            let users: [RemoteUser] = try await supabase.fetch(
                from: "app_users",
                filter: ["username": username]
            )

            guard let user = users.first else {
                return .userNotFound
            }

            // Check if user is active
            guard user.isActive else {
                return .userInactive
            }

            // Validate password (in production, this should be done server-side with proper hashing)
            // For now, we compare directly but this should be improved
            guard validatePassword(input: password, stored: user.password) else {
                return .invalidCredentials
            }

            return .success(user: user)

        } catch let error as SupabaseError {
            switch error {
            case .notConfigured:
                return .notConfigured
            default:
                return .networkError(error.localizedDescription)
            }
        } catch {
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

            // Validate password
            guard validatePassword(input: password, stored: user.password) else {
                return .invalidCredentials
            }

            // Convert to RemoteUser format for consistency
            let remoteUser = RemoteUser(
                id: user.id,
                username: user.username,
                password: user.password,
                fullName: user.fullName,
                isAdmin: user.isAdmin,
                isActive: user.isActive,
                createdAt: user.createdAt,
                updatedAt: user.updatedAt,
                createdBy: user.createdBy,
                updatedBy: user.updatedBy
            )

            return .success(user: remoteUser)
        } catch {
            return .networkError(error.localizedDescription)
        }
    }

    /// Validate password
    /// In a real production app, passwords should be hashed server-side
    /// and validation should happen on the server
    private func validatePassword(input: String, stored: String) -> Bool {
        // Direct comparison for now
        // TODO: Implement proper password hashing (bcrypt, argon2, etc.)
        return input == stored
    }

    /// Sync local user with Supabase (for offline-first approach)
    func syncUserFromSupabase(userId: String) async throws -> RemoteUser? {
        guard supabase.isConfigured else { return nil }

        return try await supabase.fetchById(from: "app_users", id: userId)
    }
}

// MARK: - Session Management Extension

extension SessionManager {
    /// Login with proper authentication
    func loginWithAuth(user: RemoteUser) {
        self.userId = user.id
        self.username = user.username
        self.fullName = user.fullName
        self.isAdmin = user.isAdmin
        self.isLoggedIn = true

        // Load permissions for this user
        Task {
            await PermissionManager.shared.loadPermissions(forUserId: user.id)
        }
    }

    /// Logout and clear all session data
    func logoutWithCleanup() {
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
