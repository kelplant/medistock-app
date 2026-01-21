import Foundation
import Supabase

/// Résultat de l'application d'une migration
struct MigrationResult {
    let success: Bool
    let alreadyApplied: Bool
    let message: String
    let executionTimeMs: Int?
    let error: String?
}

/// Résultat de l'exécution des migrations
struct MigrationRunResult {
    let success: Bool
    let migrationsApplied: [String]
    let migrationsFailed: [(String, String)] // name, error message
    let migrationsSkipped: [String]
    let systemNotInstalled: Bool
    let errorMessage: String?
}

/// Résultat de la vérification de compatibilité app/DB
enum CompatibilityResult {
    case compatible
    case appTooOld(appVersion: Int, minRequired: Int, dbVersion: Int)
    case unknown(reason: String)
}

/// DTO pour les migrations stockées dans schema_migrations
struct SchemaMigrationDto: Codable {
    let name: String
    let checksum: String?
    let appliedAt: Int64?
    let appliedBy: String?
    let success: Bool
    let executionTimeMs: Int?
    let errorMessage: String?

    enum CodingKeys: String, CodingKey {
        case name
        case checksum
        case appliedAt = "applied_at"
        case appliedBy = "applied_by"
        case success
        case executionTimeMs = "execution_time_ms"
        case errorMessage = "error_message"
    }
}

/// DTO pour la version du schéma
struct SchemaVersionDto: Codable {
    let schemaVersion: Int
    let minAppVersion: Int
    let updatedAt: Int64?

    enum CodingKeys: String, CodingKey {
        case schemaVersion = "schema_version"
        case minAppVersion = "min_app_version"
        case updatedAt = "updated_at"
    }
}

/// Gestionnaire des migrations de schéma Supabase pour iOS
///
/// Ce gestionnaire:
/// - Charge les fichiers SQL depuis le bundle de l'app
/// - Compare avec les migrations déjà appliquées dans Supabase
/// - Applique les nouvelles migrations dans l'ordre alphabétique
class MigrationManager {

    /// Version du schéma supportée par cette version de l'app
    static let APP_SCHEMA_VERSION = 2

    private let supabase: SupabaseClient

    init() {
        self.supabase = SupabaseService.shared.client
    }

    // MARK: - Compatibility Check

    /// Vérifie si cette version de l'app est compatible avec la base de données
    func checkCompatibility() async -> CompatibilityResult {
        do {
            let response: [SchemaVersionDto] = try await supabase.database
                .rpc("get_schema_version")
                .execute()
                .value

            guard let schemaVersion = response.first else {
                print("⚠️ Système de versioning non installé - compatibilité assumée")
                return .compatible
            }

            let dbVersion = schemaVersion.schemaVersion
            let minAppVersion = schemaVersion.minAppVersion

            if Self.APP_SCHEMA_VERSION < minAppVersion {
                print("❌ App trop ancienne: app=\(Self.APP_SCHEMA_VERSION), min=\(minAppVersion), db=\(dbVersion)")
                return .appTooOld(appVersion: Self.APP_SCHEMA_VERSION, minRequired: minAppVersion, dbVersion: dbVersion)
            } else {
                print("✅ App compatible: app=\(Self.APP_SCHEMA_VERSION), min=\(minAppVersion), db=\(dbVersion)")
                return .compatible
            }
        } catch {
            print("⚠️ Impossible de vérifier la compatibilité: \(error.localizedDescription)")
            return .unknown(reason: error.localizedDescription)
        }
    }

    // MARK: - Load Migrations

    /// Charge toutes les migrations depuis le bundle de l'app
    func loadMigrationsFromBundle() -> [(name: String, sql: String, checksum: String)] {
        var migrations: [(name: String, sql: String, checksum: String)] = []

        guard let bundlePath = Bundle.main.resourcePath else {
            print("❌ Impossible de trouver le chemin du bundle")
            return []
        }

        let migrationsPath = (bundlePath as NSString).appendingPathComponent("migrations")
        let fileManager = FileManager.default

        do {
            let files = try fileManager.contentsOfDirectory(atPath: migrationsPath)
                .filter { $0.hasSuffix(".sql") }
                .sorted()

            for fileName in files {
                let filePath = (migrationsPath as NSString).appendingPathComponent(fileName)
                if let sql = try? String(contentsOfFile: filePath, encoding: .utf8) {
                    let name = String(fileName.dropLast(4)) // Remove .sql
                    let checksum = sql.md5()
                    migrations.append((name: name, sql: sql, checksum: checksum))
                }
            }
        } catch {
            print("❌ Erreur lors du listage des migrations: \(error.localizedDescription)")
        }

        return migrations
    }

    // MARK: - Get Applied Migrations

    /// Récupère les migrations déjà appliquées depuis Supabase
    func getAppliedMigrations() async -> [SchemaMigrationDto] {
        do {
            let response: [SchemaMigrationDto] = try await supabase.database
                .rpc("get_applied_migrations")
                .execute()
                .value
            return response
        } catch {
            print("❌ Erreur lors de la récupération des migrations: \(error.localizedDescription)")
            return []
        }
    }

    /// Vérifie si le système de migration est installé
    func isMigrationSystemInstalled() async -> Bool {
        do {
            let _: [SchemaMigrationDto] = try await supabase.database
                .rpc("get_applied_migrations")
                .execute()
                .value
            return true
        } catch {
            print("⚠️ Système de migration non installé: \(error.localizedDescription)")
            return false
        }
    }

    // MARK: - Apply Migration

    /// Applique une migration
    func applyMigration(name: String, sql: String, checksum: String?, appliedBy: String = "ios_app") async -> MigrationResult {
        do {
            let params: [String: AnyJSON] = [
                "p_name": .string(name),
                "p_sql": .string(sql),
                "p_checksum": checksum.map { .string($0) } ?? .null,
                "p_applied_by": .string(appliedBy)
            ]

            let response = try await supabase.database
                .rpc("apply_migration", params: params)
                .execute()

            // Parse JSON response
            if let data = response.data,
               let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
                return MigrationResult(
                    success: json["success"] as? Bool ?? false,
                    alreadyApplied: json["already_applied"] as? Bool ?? false,
                    message: json["message"] as? String ?? "Unknown result",
                    executionTimeMs: json["execution_time_ms"] as? Int,
                    error: json["error"] as? String
                )
            }

            return MigrationResult(success: false, alreadyApplied: false, message: "Failed to parse response", executionTimeMs: nil, error: nil)
        } catch {
            print("❌ Erreur lors de l'application de la migration \(name): \(error.localizedDescription)")
            return MigrationResult(
                success: false,
                alreadyApplied: false,
                message: "Failed to apply migration: \(error.localizedDescription)",
                executionTimeMs: nil,
                error: error.localizedDescription
            )
        }
    }

    // MARK: - Run Pending Migrations

    /// Récupère les migrations en attente
    func getPendingMigrations() async -> [(name: String, sql: String, checksum: String)] {
        let allMigrations = loadMigrationsFromBundle()
        let appliedMigrations = await getAppliedMigrations()
            .filter { $0.success }
            .map { $0.name }

        return allMigrations.filter { !appliedMigrations.contains($0.name) }
    }

    /// Exécute toutes les migrations en attente
    func runPendingMigrations(appliedBy: String = "ios_app") async -> MigrationRunResult {
        // Vérifier si le système est installé
        let systemInstalled = await isMigrationSystemInstalled()
        if !systemInstalled {
            return MigrationRunResult(
                success: false,
                migrationsApplied: [],
                migrationsFailed: [],
                migrationsSkipped: [],
                systemNotInstalled: true,
                errorMessage: "Migration system not installed in Supabase. Please run 2026011701_migration_system.sql first."
            )
        }

        let pendingMigrations = await getPendingMigrations()

        if pendingMigrations.isEmpty {
            return MigrationRunResult(
                success: true,
                migrationsApplied: [],
                migrationsFailed: [],
                migrationsSkipped: [],
                systemNotInstalled: false,
                errorMessage: nil
            )
        }

        print("📦 \(pendingMigrations.count) migration(s) en attente")

        var applied: [String] = []
        var failed: [(String, String)] = []
        var skipped: [String] = []

        for migration in pendingMigrations {
            print("⏳ Application de \(migration.name)...")

            let result = await applyMigration(
                name: migration.name,
                sql: migration.sql,
                checksum: migration.checksum,
                appliedBy: appliedBy
            )

            if result.alreadyApplied {
                print("⏭️ \(migration.name) déjà appliquée")
                skipped.append(migration.name)
            } else if result.success {
                print("✅ \(migration.name) appliquée en \(result.executionTimeMs ?? 0)ms")
                applied.append(migration.name)
            } else {
                print("❌ \(migration.name) échouée: \(result.error ?? "Unknown error")")
                failed.append((migration.name, result.error ?? "Unknown error"))

                // Arrêter en cas d'échec
                return MigrationRunResult(
                    success: false,
                    migrationsApplied: applied,
                    migrationsFailed: failed,
                    migrationsSkipped: skipped,
                    systemNotInstalled: false,
                    errorMessage: "Migration \(migration.name) failed: \(result.error ?? "Unknown error")"
                )
            }
        }

        return MigrationRunResult(
            success: true,
            migrationsApplied: applied,
            migrationsFailed: failed,
            migrationsSkipped: skipped,
            systemNotInstalled: false,
            errorMessage: nil
        )
    }
}

// MARK: - String MD5 Extension

extension String {
    func md5() -> String {
        let data = Data(self.utf8)
        var digest = [UInt8](repeating: 0, count: Int(CC_MD5_DIGEST_LENGTH))
        _ = data.withUnsafeBytes {
            CC_MD5($0.baseAddress, CC_LONG(data.count), &digest)
        }
        return digest.map { String(format: "%02x", $0) }.joined()
    }
}

// Import CommonCrypto for MD5
import CommonCrypto
