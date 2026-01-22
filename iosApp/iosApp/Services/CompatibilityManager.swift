import Foundation
import shared

// Type aliases to avoid conflict with CompatibilityManager.shared property
typealias SharedCompatibilityResult = shared.CompatibilityResult
typealias SharedCompatibilityChecker = shared.CompatibilityChecker
typealias SharedSchemaVersion = shared.SchemaVersion

/// Schema version information from Supabase RPC
struct SchemaVersionResponse: Codable {
    let schemaVersion: Int
    let minAppVersion: Int
    let updatedAt: Int64?

    enum CodingKeys: String, CodingKey {
        case schemaVersion = "schema_version"
        case minAppVersion = "min_app_version"
        case updatedAt = "updated_at"
    }
}

/// Manages app/database compatibility checks
class CompatibilityManager: ObservableObject {
    static let shared = CompatibilityManager()

    @Published var compatibilityResult: SharedCompatibilityResult?
    @Published var isChecking = false

    private let supabase = SupabaseService.shared

    private init() {}

    /// Check app/database compatibility
    @MainActor
    func checkCompatibility() async -> SharedCompatibilityResult {
        isChecking = true
        defer { isChecking = false }

        // If Supabase is not configured, assume compatible
        guard supabase.isConfigured else {
            let result = SharedCompatibilityResult.Compatible()
            compatibilityResult = result
            return result
        }

        // If offline, assume compatible (we'll check again when online)
        guard SyncStatusManager.shared.isOnline else {
            let result = SharedCompatibilityResult.Compatible()
            compatibilityResult = result
            return result
        }

        do {
            // Fetch schema version from Supabase RPC
            let schemaVersion = try await fetchSchemaVersion()

            // Convert to shared model and use shared checker
            let convertedSchemaVersion: SharedSchemaVersion?
            if let sv = schemaVersion {
                convertedSchemaVersion = SharedSchemaVersion(
                    schemaVersion: Int32(sv.schemaVersion),
                    minAppVersion: Int32(sv.minAppVersion),
                    updatedAt: sv.updatedAt.map { KotlinLong(value: $0) }
                )
            } else {
                convertedSchemaVersion = nil
            }

            let result = SharedCompatibilityChecker.shared.checkCompatibility(schemaVersion: convertedSchemaVersion)

            // Log the result
            let info = SharedCompatibilityChecker.shared.formatCompatibilityInfo(result: result)
            print("[CompatibilityManager] \(info)")

            compatibilityResult = result
            return result
        } catch {
            print("[CompatibilityManager] Error checking compatibility: \(error)")
            let result = SharedCompatibilityResult.Unknown(reason: error.localizedDescription)
            compatibilityResult = result
            return result
        }
    }

    /// Fetch schema version from Supabase RPC
    private func fetchSchemaVersion() async throws -> SchemaVersionResponse? {
        // Call get_schema_version RPC function
        // The RPC returns a single object, not an array
        let response: SchemaVersionResponse? = try await supabase.rpc(function: "get_schema_version")
        return response
    }

    /// Returns true if the app requires an update
    var requiresUpdate: Bool {
        guard let result = compatibilityResult else { return false }
        return result is SharedCompatibilityResult.AppTooOld
    }

    /// Returns the compatibility info for display
    var compatibilityInfo: (appVersion: Int, minRequired: Int, dbVersion: Int)? {
        guard let result = compatibilityResult as? SharedCompatibilityResult.AppTooOld else {
            return nil
        }
        return (
            appVersion: Int(result.appVersion),
            minRequired: Int(result.minRequired),
            dbVersion: Int(result.dbVersion)
        )
    }

    /// Get the current app schema version from shared
    var appSchemaVersion: Int {
        Int(SharedCompatibilityChecker.shared.APP_SCHEMA_VERSION)
    }
}
