import Foundation
import Security

/// Secure storage service using iOS Keychain
/// Replaces UserDefaults for sensitive data like Supabase credentials
class KeychainService {
    static let shared = KeychainService()

    private let service = "com.medistock.supabase"

    enum KeychainKey: String {
        case supabaseUrl = "supabase_url"
        case supabaseAnonKey = "supabase_anon_key"
        case authAccessToken = "auth_access_token"
        case authRefreshToken = "auth_refresh_token"
        case authExpiresAt = "auth_expires_at"
        case authUserId = "auth_user_id"
    }

    private init() {}

    // MARK: - Public API

    /// Save a string value to Keychain
    /// - Parameters:
    ///   - value: The string value to save
    ///   - key: The key to save under
    /// - Returns: true if successful, false otherwise
    @discardableResult
    func save(_ value: String, for key: KeychainKey) -> Bool {
        guard let data = value.data(using: .utf8) else {
            debugLog("KeychainService", "Failed to encode value for key: \(key.rawValue)")
            return false
        }

        // Delete existing value first
        delete(key)

        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key.rawValue,
            kSecValueData as String: data,
            // Accessible after first unlock - good balance of security and usability
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlock
        ]

        let status = SecItemAdd(query as CFDictionary, nil)

        if status == errSecSuccess {
            debugLog("KeychainService", "Saved value for key: \(key.rawValue)")
            return true
        } else {
            debugLog("KeychainService", "Failed to save value for key: \(key.rawValue), status: \(status)")
            return false
        }
    }

    /// Retrieve a string value from Keychain
    /// - Parameter key: The key to retrieve
    /// - Returns: The stored value, or nil if not found
    func get(_ key: KeychainKey) -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key.rawValue,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]

        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)

        guard status == errSecSuccess,
              let data = result as? Data,
              let value = String(data: data, encoding: .utf8) else {
            if status != errSecItemNotFound {
                debugLog("KeychainService", "Failed to get value for key: \(key.rawValue), status: \(status)")
            }
            return nil
        }

        return value
    }

    /// Delete a value from Keychain
    /// - Parameter key: The key to delete
    @discardableResult
    func delete(_ key: KeychainKey) -> Bool {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key.rawValue
        ]

        let status = SecItemDelete(query as CFDictionary)
        return status == errSecSuccess || status == errSecItemNotFound
    }

    /// Clear all Supabase credentials from Keychain
    func clearAll() {
        delete(.supabaseUrl)
        delete(.supabaseAnonKey)
        debugLog("KeychainService", "Cleared all credentials")
    }

    /// Check if Supabase credentials are stored
    var hasStoredCredentials: Bool {
        return get(.supabaseUrl) != nil && get(.supabaseAnonKey) != nil
    }

    // MARK: - Auth Token Management

    /// Store auth session tokens securely
    /// - Parameters:
    ///   - accessToken: The JWT access token
    ///   - refreshToken: The refresh token for token renewal
    ///   - expiresAt: Token expiration timestamp (Unix epoch)
    ///   - userId: The Supabase Auth user ID
    func storeAuthTokens(accessToken: String, refreshToken: String, expiresAt: Int64, userId: String) {
        save(accessToken, for: .authAccessToken)
        save(refreshToken, for: .authRefreshToken)
        save(String(expiresAt), for: .authExpiresAt)
        save(userId, for: .authUserId)
        debugLog("KeychainService", "Stored auth tokens for user: \(userId)")
    }

    /// Retrieve stored auth tokens
    /// - Returns: Tuple of (accessToken, refreshToken, expiresAt, userId) or nil if not stored
    func getAuthTokens() -> (accessToken: String, refreshToken: String, expiresAt: Int64, userId: String)? {
        guard let accessToken = get(.authAccessToken),
              let refreshToken = get(.authRefreshToken),
              let expiresAtStr = get(.authExpiresAt),
              let expiresAt = Int64(expiresAtStr),
              let userId = get(.authUserId) else {
            return nil
        }
        return (accessToken, refreshToken, expiresAt, userId)
    }

    /// Clear auth tokens from Keychain
    func clearAuthTokens() {
        delete(.authAccessToken)
        delete(.authRefreshToken)
        delete(.authExpiresAt)
        delete(.authUserId)
        debugLog("KeychainService", "Cleared auth tokens")
    }

    /// Check if auth tokens are stored
    var hasAuthTokens: Bool {
        return get(.authAccessToken) != nil && get(.authRefreshToken) != nil
    }

    /// Check if stored auth tokens are expired
    var areAuthTokensExpired: Bool {
        guard let expiresAtStr = get(.authExpiresAt),
              let expiresAt = Int64(expiresAtStr) else {
            return true
        }
        // Add 300 second (5 minute) buffer to account for clock skew and match Android behavior
        return Int64(Date().timeIntervalSince1970) >= (expiresAt - 300)
    }
}
