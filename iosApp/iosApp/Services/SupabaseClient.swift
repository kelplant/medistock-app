import Foundation

/// Supabase client for iOS using URLSession
/// Mirrors the Android SupabaseClientProvider functionality
class SupabaseClient {
    static let shared = SupabaseClient()

    private let urlKey = "medistock_supabase_url"
    private let keyKey = "medistock_supabase_key"
    private let clientIdKey = "medistock_client_id"

    private var session: URLSession

    private init() {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 30
        config.timeoutIntervalForResource = 60
        self.session = URLSession(configuration: config)
    }

    // MARK: - Configuration

    var isConfigured: Bool {
        guard let url = supabaseUrl, let key = supabaseKey else { return false }
        return !url.isEmpty && !key.isEmpty && url != "https://YOUR_PROJECT_ID.supabase.co"
    }

    var supabaseUrl: String? {
        UserDefaults.standard.string(forKey: urlKey)
    }

    var supabaseKey: String? {
        UserDefaults.standard.string(forKey: keyKey)
    }

    var clientId: String {
        if let existing = UserDefaults.standard.string(forKey: clientIdKey) {
            return existing
        }
        let newId = UUID().uuidString
        UserDefaults.standard.set(newId, forKey: clientIdKey)
        return newId
    }

    func configure(url: String, key: String) {
        UserDefaults.standard.set(url, forKey: urlKey)
        UserDefaults.standard.set(key, forKey: keyKey)
    }

    func clearConfiguration() {
        UserDefaults.standard.removeObject(forKey: urlKey)
        UserDefaults.standard.removeObject(forKey: keyKey)
    }

    // MARK: - REST API Methods

    private func buildRequest(endpoint: String, method: String = "GET", body: Data? = nil, query: [String: String]? = nil) throws -> URLRequest {
        guard let baseUrl = supabaseUrl, let apiKey = supabaseKey else {
            throw SupabaseError.notConfigured
        }

        var urlString = "\(baseUrl)/rest/v1/\(endpoint)"

        if let query = query, !query.isEmpty {
            let queryString = query.map { "\($0.key)=\($0.value)" }.joined(separator: "&")
            urlString += "?\(queryString)"
        }

        guard let url = URL(string: urlString) else {
            throw SupabaseError.invalidURL
        }

        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue(apiKey, forHTTPHeaderField: "apikey")
        request.setValue("Bearer \(apiKey)", forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("return=representation", forHTTPHeaderField: "Prefer")

        if let body = body {
            request.httpBody = body
        }

        return request
    }

    /// Fetch all records from a table
    func fetchAll<T: Decodable>(from table: String, query: [String: String]? = nil) async throws -> [T] {
        let request = try buildRequest(endpoint: table, query: query)
        let (data, response) = try await session.data(for: request)

        try validateResponse(response)

        let decoder = JSONDecoder()
        decoder.keyDecodingStrategy = .convertFromSnakeCase
        return try decoder.decode([T].self, from: data)
    }

    /// Fetch a single record by ID
    func fetchById<T: Decodable>(from table: String, id: String) async throws -> T? {
        let query = ["id": "eq.\(id)"]
        let results: [T] = try await fetchAll(from: table, query: query)
        return results.first
    }

    /// Fetch records with a filter
    func fetch<T: Decodable>(from table: String, filter: [String: String]) async throws -> [T] {
        let query = filter.mapValues { "eq.\($0)" }
        return try await fetchAll(from: table, query: query)
    }

    /// Insert a new record
    func insert<T: Codable>(into table: String, record: T) async throws -> T {
        var recordDict = try encodeToDict(record)
        recordDict["client_id"] = clientId

        let body = try JSONSerialization.data(withJSONObject: recordDict)
        let request = try buildRequest(endpoint: table, method: "POST", body: body)
        let (data, response) = try await session.data(for: request)

        try validateResponse(response)

        let decoder = JSONDecoder()
        decoder.keyDecodingStrategy = .convertFromSnakeCase
        let results = try decoder.decode([T].self, from: data)
        guard let result = results.first else {
            throw SupabaseError.noData
        }
        return result
    }

    /// Update a record by ID
    func update<T: Codable>(table: String, id: String, record: T) async throws -> T {
        var recordDict = try encodeToDict(record)
        recordDict["client_id"] = clientId

        let body = try JSONSerialization.data(withJSONObject: recordDict)
        var request = try buildRequest(endpoint: table, method: "PATCH", body: body, query: ["id": "eq.\(id)"])

        let (data, response) = try await session.data(for: request)

        try validateResponse(response)

        let decoder = JSONDecoder()
        decoder.keyDecodingStrategy = .convertFromSnakeCase
        let results = try decoder.decode([T].self, from: data)
        guard let result = results.first else {
            throw SupabaseError.noData
        }
        return result
    }

    /// Upsert a record (insert or update)
    func upsert<T: Codable>(into table: String, record: T, includeClientId: Bool = true) async throws -> T {
        var recordDict = try encodeToDict(record)
        if includeClientId {
            recordDict["client_id"] = clientId
        }

        let body = try JSONSerialization.data(withJSONObject: recordDict)
        var request = try buildRequest(endpoint: table, method: "POST", body: body)
        // Combine return=representation (to get response body) with resolution=merge-duplicates (for upsert)
        request.setValue("return=representation,resolution=merge-duplicates", forHTTPHeaderField: "Prefer")

        let (data, response) = try await session.data(for: request)

        try validateResponse(response)

        let decoder = JSONDecoder()
        decoder.keyDecodingStrategy = .convertFromSnakeCase
        let results = try decoder.decode([T].self, from: data)
        guard let result = results.first else {
            throw SupabaseError.noData
        }
        return result
    }

    /// Delete a record by ID
    func delete(from table: String, id: String) async throws {
        let request = try buildRequest(endpoint: table, method: "DELETE", query: ["id": "eq.\(id)"])
        let (_, response) = try await session.data(for: request)
        try validateResponse(response)
    }

    // MARK: - Helpers

    private func validateResponse(_ response: URLResponse) throws {
        guard let httpResponse = response as? HTTPURLResponse else {
            throw SupabaseError.invalidResponse
        }

        switch httpResponse.statusCode {
        case 200...299:
            return
        case 401:
            throw SupabaseError.unauthorized
        case 404:
            throw SupabaseError.notFound
        case 409:
            throw SupabaseError.conflict
        default:
            throw SupabaseError.serverError(statusCode: httpResponse.statusCode)
        }
    }

    private func encodeToDict<T: Encodable>(_ value: T) throws -> [String: Any] {
        let encoder = JSONEncoder()
        encoder.keyEncodingStrategy = .convertToSnakeCase
        let data = try encoder.encode(value)
        guard let dict = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            throw SupabaseError.encodingError
        }
        return dict
    }
}

// MARK: - Errors

enum SupabaseError: LocalizedError {
    case notConfigured
    case invalidURL
    case invalidResponse
    case unauthorized
    case notFound
    case conflict
    case noData
    case encodingError
    case serverError(statusCode: Int)

    var errorDescription: String? {
        switch self {
        case .notConfigured:
            return "Supabase n'est pas configuré. Veuillez configurer l'URL et la clé API."
        case .invalidURL:
            return "URL invalide"
        case .invalidResponse:
            return "Réponse invalide du serveur"
        case .unauthorized:
            return "Non autorisé. Vérifiez votre clé API."
        case .notFound:
            return "Ressource non trouvée"
        case .conflict:
            return "Conflit lors de la création/mise à jour"
        case .noData:
            return "Aucune donnée retournée"
        case .encodingError:
            return "Erreur d'encodage des données"
        case .serverError(let code):
            return "Erreur serveur (code: \(code))"
        }
    }
}
