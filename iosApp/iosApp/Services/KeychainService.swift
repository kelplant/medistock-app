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
}
