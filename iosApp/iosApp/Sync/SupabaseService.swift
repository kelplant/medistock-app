import Foundation
import Supabase
import Auth
import PostgREST
import Functions

/// Supabase service using the official Swift SDK
/// Replaces the custom SupabaseClient
class SupabaseService {
    static let shared = SupabaseService()

    private var client: SupabaseClient?
    private let keychain = KeychainService.shared

    // Legacy key for migration from UserDefaults
    private let legacyConfigKey = "medistock_supabase_config"

    /// Email domain used for UUID-based auth emails
    static let authEmailDomain = "medistock.local"

    var isConfigured: Bool {
        client != nil
    }

    /// Check if user is authenticated with Supabase Auth
    var isAuthenticated: Bool {
        get async {
            guard let client = client else { return false }
            do {
                _ = try await client.auth.session
                return true
            } catch {
                return false
            }
        }
    }

    private init() {
        migrateFromUserDefaultsIfNeeded()
        loadConfiguration()
    }

    func currentClient() -> SupabaseClient? {
        client
    }

    // MARK: - Configuration

    func configure(url: String, anonKey: String) {
        guard let supabaseURL = URL(string: url) else {
            debugLog("SupabaseService", "Invalid URL: \(url)")
            return
        }

        client = SupabaseClient(
            supabaseURL: supabaseURL,
            supabaseKey: anonKey
        )

        // Save configuration securely to Keychain
        keychain.save(url, for: .supabaseUrl)
        keychain.save(anonKey, for: .supabaseAnonKey)

        debugLog("SupabaseService", "Configured with URL: \(url)")
    }

    func disconnect() {
        client = nil
        keychain.clearAll()
        debugLog("SupabaseService", "Disconnected")
    }

    /// Returns the stored configuration if available
    func getStoredConfig() -> SupabaseConfig? {
        guard let url = keychain.get(.supabaseUrl),
              let anonKey = keychain.get(.supabaseAnonKey) else {
            return nil
        }
        return SupabaseConfig(url: url, anonKey: anonKey)
    }

    private func loadConfiguration() {
        guard let url = keychain.get(.supabaseUrl),
              let anonKey = keychain.get(.supabaseAnonKey) else {
            return
        }
        configure(url: url, anonKey: anonKey)
    }

    /// Migrate credentials from legacy UserDefaults to secure Keychain
    private func migrateFromUserDefaultsIfNeeded() {
        // Check if we have legacy data in UserDefaults
        guard let data = UserDefaults.standard.data(forKey: legacyConfigKey),
              let config = try? JSONDecoder().decode(SupabaseConfig.self, from: data) else {
            return
        }

        // Check if already migrated to Keychain
        if keychain.hasStoredCredentials {
            // Already have Keychain data, just clean up UserDefaults
            UserDefaults.standard.removeObject(forKey: legacyConfigKey)
            debugLog("SupabaseService", "Cleaned up legacy UserDefaults (already migrated)")
            return
        }

        // Migrate to Keychain
        keychain.save(config.url, for: .supabaseUrl)
        keychain.save(config.anonKey, for: .supabaseAnonKey)

        // Remove from UserDefaults
        UserDefaults.standard.removeObject(forKey: legacyConfigKey)

        debugLog("SupabaseService", "Migrated credentials from UserDefaults to Keychain")
    }

    // MARK: - Database Operations

    /// Fetch all records from a table
    func fetchAll<T: Decodable>(from table: String) async throws -> [T] {
        guard let client = client else {
            throw SupabaseServiceError.notConfigured
        }

        let response: [T] = try await client.database
            .from(table)
            .select()
            .execute()
            .value

        return response
    }

    /// Fetch records with filter dictionary
    func fetch<T: Decodable>(from table: String, filter: [String: String]) async throws -> [T] {
        guard let client = client else {
            throw SupabaseServiceError.notConfigured
        }

        var query = client.database.from(table).select()

        for (key, value) in filter {
            query = query.eq(key, value: value)
        }

        let response: [T] = try await query.execute().value
        return response
    }

    /// Fetch records with raw PostgREST filter string (e.g., "username=eq.john")
    func fetchWithFilter<T: Decodable>(from table: String, filter: String) async throws -> [T] {
        guard let client = client else {
            throw SupabaseServiceError.notConfigured
        }

        // Parse the filter string (format: "column=op.value")
        let parts = filter.split(separator: "=", maxSplits: 1)
        guard parts.count == 2 else {
            throw SupabaseServiceError.serverError("Invalid filter format")
        }

        let column = String(parts[0])
        let opValue = String(parts[1])

        // Parse operator and value (format: "op.value")
        let opParts = opValue.split(separator: ".", maxSplits: 1)
        guard opParts.count == 2 else {
            throw SupabaseServiceError.serverError("Invalid filter operator format")
        }

        let op = String(opParts[0])
        let value = String(opParts[1])

        var query = client.database.from(table).select()

        switch op {
        case "eq":
            query = query.eq(column, value: value)
        case "neq":
            query = query.neq(column, value: value)
        case "gt":
            query = query.gt(column, value: value)
        case "lt":
            query = query.lt(column, value: value)
        case "gte":
            query = query.gte(column, value: value)
        case "lte":
            query = query.lte(column, value: value)
        case "like":
            query = query.like(column, pattern: value)
        case "ilike":
            query = query.ilike(column, pattern: value)
        default:
            throw SupabaseServiceError.serverError("Unsupported filter operator: \(op)")
        }

        let response: [T] = try await query.execute().value
        return response
    }

    /// Fetch a single record by ID
    func fetchById<T: Decodable>(from table: String, id: String) async throws -> T? {
        guard let client = client else {
            throw SupabaseServiceError.notConfigured
        }

        let response: [T] = try await client.database
            .from(table)
            .select()
            .eq("id", value: id)
            .limit(1)
            .execute()
            .value

        return response.first
    }

    /// Insert a record
    func insert<T: Encodable>(into table: String, record: T) async throws {
        guard let client = client else {
            throw SupabaseServiceError.notConfigured
        }

        try await client.database
            .from(table)
            .insert(record)
            .execute()
    }

    /// Upsert a record (insert or update)
    func upsert<T: Encodable>(into table: String, record: T) async throws {
        guard let client = client else {
            throw SupabaseServiceError.notConfigured
        }

        try await client.database
            .from(table)
            .upsert(record)
            .execute()
    }

    /// Update a record by ID
    func update<T: Encodable>(table: String, id: String, record: T) async throws {
        guard let client = client else {
            throw SupabaseServiceError.notConfigured
        }

        try await client.database
            .from(table)
            .update(record)
            .eq("id", value: id)
            .execute()
    }

    /// Delete a record by ID
    func delete(from table: String, id: String) async throws {
        guard let client = client else {
            throw SupabaseServiceError.notConfigured
        }

        try await client.database
            .from(table)
            .delete()
            .eq("id", value: id)
            .execute()
    }

    // MARK: - RPC Functions

    /// Call a Supabase RPC function
    func rpc<T: Decodable, P: Encodable>(function: String, params: P) async throws -> T? {
        guard let client = client else {
            throw SupabaseServiceError.notConfigured
        }

        let response: T = try await client.database
            .rpc(function, params: params)
            .execute()
            .value

        return response
    }

    /// Call a Supabase RPC function with no parameters
    func rpc<T: Decodable>(function: String) async throws -> T? {
        guard let client = client else {
            throw SupabaseServiceError.notConfigured
        }

        let response: T = try await client.database
            .rpc(function)
            .execute()
            .value

        return response
    }

    // MARK: - Realtime

    /// Get the Supabase client for realtime subscriptions
    var realtimeClient: SupabaseClient? {
        client
    }

    // MARK: - Direct Database Access

    /// Access the database from() method directly
    /// Use this for complex queries that need direct builder access
    func from(_ table: String) -> PostgrestQueryBuilder? {
        return client?.from(table)
    }

    // MARK: - Auth

    /// Generate UUID-based email for Supabase Auth
    /// - Parameter uuid: The user's UUID from the local database
    /// - Returns: Email in format {uuid}@medistock.local
    static func authEmail(for uuid: String) -> String {
        return "\(uuid)@\(authEmailDomain)"
    }

    /// Sign in with UUID-based email and password using Supabase Auth
    /// - Parameters:
    ///   - uuid: The user's UUID from the local database
    ///   - password: The user's password
    /// - Returns: The authenticated session
    func signIn(uuid: String, password: String) async throws -> Session {
        guard let client = client else {
            throw SupabaseServiceError.notConfigured
        }

        let email = Self.authEmail(for: uuid)
        debugLog("SupabaseService", "Signing in with email: \(email)")

        let session = try await client.auth.signIn(email: email, password: password)

        // Store tokens in Keychain
        keychain.storeAuthTokens(
            accessToken: session.accessToken,
            refreshToken: session.refreshToken,
            expiresAt: Int64(session.expiresAt),
            userId: session.user.id.uuidString
        )

        debugLog("SupabaseService", "Sign in successful for user: \(session.user.id)")
        return session
    }

    /// Sign out the current user
    func signOut() async throws {
        guard let client = client else {
            throw SupabaseServiceError.notConfigured
        }

        try await client.auth.signOut()
        keychain.clearAuthTokens()
        debugLog("SupabaseService", "Signed out successfully")
    }

    /// Get the current session if authenticated
    func getSession() async throws -> Session? {
        guard let client = client else {
            return nil
        }

        do {
            return try await client.auth.session
        } catch {
            debugLog("SupabaseService", "No active session: \(error)")
            return nil
        }
    }

    /// Refresh the current session token
    func refreshSession() async throws -> Session {
        guard let client = client else {
            throw SupabaseServiceError.notConfigured
        }

        let session = try await client.auth.refreshSession()

        // Update stored tokens
        keychain.storeAuthTokens(
            accessToken: session.accessToken,
            refreshToken: session.refreshToken,
            expiresAt: Int64(session.expiresAt),
            userId: session.user.id.uuidString
        )

        debugLog("SupabaseService", "Session refreshed successfully")
        return session
    }

    /// Get the current access token, refreshing if needed
    func getAccessToken() async throws -> String {
        guard let client = client else {
            throw SupabaseServiceError.notConfigured
        }

        // Check if we need to refresh
        if keychain.areAuthTokensExpired {
            debugLog("SupabaseService", "Token expired, refreshing...")
            let session = try await refreshSession()
            return session.accessToken
        }

        // Return current session token
        let session = try await client.auth.session
        return session.accessToken
    }

    // MARK: - Edge Functions

    /// Response structure for migrate-user-to-auth Edge Function
    struct MigrateUserResponse: Codable {
        let success: Bool
        let message: String?
        let accessToken: String?
        let refreshToken: String?
        let expiresAt: Int64?
        let userId: String?

        enum CodingKeys: String, CodingKey {
            case success, message
            case accessToken = "access_token"
            case refreshToken = "refresh_token"
            case expiresAt = "expires_at"
            case userId = "user_id"
        }
    }

    /// Request structure for migrate-user-to-auth Edge Function
    struct MigrateUserRequest: Codable {
        let username: String
        let password: String
    }

    /// Call the migrate-user-to-auth Edge Function
    /// This handles transparent migration of users from BCrypt to Supabase Auth
    /// - Parameters:
    ///   - username: The user's username
    ///   - password: The user's plain text password
    /// - Returns: The migration response with session tokens if successful
    func migrateUserToAuth(username: String, password: String) async throws -> MigrateUserResponse {
        guard let client = client else {
            throw SupabaseServiceError.notConfigured
        }

        let request = MigrateUserRequest(username: username, password: password)
        debugLog("SupabaseService", "Calling migrate-user-to-auth for username: \(username)")

        let response: MigrateUserResponse = try await client.functions.invoke(
            "migrate-user-to-auth",
            options: FunctionInvokeOptions(body: request)
        )

        if response.success {
            // Store the tokens if migration was successful
            if let accessToken = response.accessToken,
               let refreshToken = response.refreshToken,
               let expiresAt = response.expiresAt,
               let userId = response.userId {
                keychain.storeAuthTokens(
                    accessToken: accessToken,
                    refreshToken: refreshToken,
                    expiresAt: expiresAt,
                    userId: userId
                )
                debugLog("SupabaseService", "Migration successful, tokens stored")
            }
        }

        return response
    }

    /// Response structure for apply-migration Edge Function
    struct ApplyMigrationResponse: Codable {
        let success: Bool
        let alreadyApplied: Bool
        let message: String
        let executionTimeMs: Int?
        let error: String?

        enum CodingKeys: String, CodingKey {
            case success
            case alreadyApplied = "already_applied"
            case message
            case executionTimeMs = "execution_time_ms"
            case error
        }
    }

    /// Request structure for apply-migration Edge Function
    struct ApplyMigrationRequest: Codable {
        let name: String
        let sql: String
        let checksum: String?
        let appliedBy: String

        enum CodingKeys: String, CodingKey {
            case name, sql, checksum
            case appliedBy = "applied_by"
        }
    }

    /// Call the apply-migration Edge Function (requires auth)
    /// - Parameters:
    ///   - name: Migration name
    ///   - sql: SQL content to execute
    ///   - checksum: Optional checksum for integrity verification
    ///   - appliedBy: Identifier of who is applying the migration
    /// - Returns: The migration result
    func applyMigration(name: String, sql: String, checksum: String?, appliedBy: String) async throws -> ApplyMigrationResponse {
        guard let client = client else {
            throw SupabaseServiceError.notConfigured
        }

        let request = ApplyMigrationRequest(
            name: name,
            sql: sql,
            checksum: checksum,
            appliedBy: appliedBy
        )

        debugLog("SupabaseService", "Calling apply-migration for: \(name)")

        let response: ApplyMigrationResponse = try await client.functions.invoke(
            "apply-migration",
            options: FunctionInvokeOptions(body: request)
        )

        return response
    }

    /// Response structure for Edge Functions that just return success/error
    struct EdgeFunctionResponse: Codable {
        let success: Bool
        let message: String?
        let error: String?
    }

    /// Generic method to invoke an Edge Function with typed request/response
    func invokeFunction<Request: Encodable, Response: Decodable>(
        _ name: String,
        request: Request
    ) async throws -> Response {
        guard let client = client else {
            throw SupabaseServiceError.notConfigured
        }

        debugLog("SupabaseService", "Invoking Edge Function: \(name)")

        let response: Response = try await client.functions.invoke(
            name,
            options: FunctionInvokeOptions(body: request)
        )

        return response
    }

    /// Invoke an Edge Function with no request body
    func invokeFunction<Response: Decodable>(_ name: String) async throws -> Response {
        guard let client = client else {
            throw SupabaseServiceError.notConfigured
        }

        debugLog("SupabaseService", "Invoking Edge Function: \(name)")

        let response: Response = try await client.functions.invoke(name)

        return response
    }
}

// MARK: - Supporting Types

struct SupabaseConfig: Codable {
    let url: String
    let anonKey: String
}

enum SupabaseServiceError: LocalizedError {
    case notConfigured
    case networkError(String)
    case serverError(String)

    var errorDescription: String? {
        switch self {
        case .notConfigured:
            return "Supabase n'est pas configuré"
        case .networkError(let message):
            return "Erreur réseau: \(message)"
        case .serverError(let message):
            return "Erreur serveur: \(message)"
        }
    }
}
