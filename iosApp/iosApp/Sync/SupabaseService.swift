import Foundation
import Supabase

/// Supabase service using the official Swift SDK
/// Replaces the custom SupabaseClient
class SupabaseService {
    static let shared = SupabaseService()

    private var client: SupabaseClient?
    private let configKey = "medistock_supabase_config"

    var isConfigured: Bool {
        client != nil
    }

    private init() {
        loadConfiguration()
    }

    func currentClient() -> SupabaseClient? {
        client
    }

    // MARK: - Configuration

    func configure(url: String, anonKey: String) {
        guard let supabaseURL = URL(string: url) else {
            print("[SupabaseService] Invalid URL: \(url)")
            return
        }

        client = SupabaseClient(
            supabaseURL: supabaseURL,
            supabaseKey: anonKey
        )

        // Save configuration
        let config = SupabaseConfig(url: url, anonKey: anonKey)
        if let data = try? JSONEncoder().encode(config) {
            UserDefaults.standard.set(data, forKey: configKey)
        }

        print("[SupabaseService] Configured with URL: \(url)")
    }

    func disconnect() {
        client = nil
        UserDefaults.standard.removeObject(forKey: configKey)
        print("[SupabaseService] Disconnected")
    }

    /// Returns the stored configuration if available
    func getStoredConfig() -> SupabaseConfig? {
        guard let data = UserDefaults.standard.data(forKey: configKey),
              let config = try? JSONDecoder().decode(SupabaseConfig.self, from: data) else {
            return nil
        }
        return config
    }

    private func loadConfiguration() {
        guard let data = UserDefaults.standard.data(forKey: configKey),
              let config = try? JSONDecoder().decode(SupabaseConfig.self, from: data) else {
            return
        }
        configure(url: config.url, anonKey: config.anonKey)
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

    // MARK: - Realtime

    /// Get the Supabase client for realtime subscriptions
    var realtimeClient: SupabaseClient? {
        client
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
