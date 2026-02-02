import SwiftUI
import UIKit
import shared

struct ContentView: View {
    let sdk: MedistockSDK
    @ObservedObject private var session = SessionManager.shared
    @ObservedObject private var syncStatus = SyncStatusManager.shared
    @ObservedObject private var compatibilityManager = CompatibilityManager.shared

    @State private var hasCheckedCompatibility = false
    @State private var isCheckingCompatibility = false
    @State private var updateAvailable: (currentVersion: String, newVersion: String)? = nil
    @State private var showUpdateDialog = false

    var body: some View {
        Group {
            // Check if app requires update
            if let appTooOld = compatibilityManager.compatibilityResult as? shared.CompatibilityResult.AppTooOld {
                // Show update required screen (blocking)
                AppUpdateRequiredView(
                    appVersion: Int(appTooOld.appVersion),
                    minRequired: Int(appTooOld.minRequired),
                    dbVersion: Int(appTooOld.dbVersion)
                )
            } else if let dbTooOld = compatibilityManager.compatibilityResult as? shared.CompatibilityResult.DbTooOld {
                // Show database too old screen (blocking)
                VStack(spacing: 20) {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .font(.system(size: 50))
                        .foregroundColor(.orange)
                    Text("Base de donnees obsolete")
                        .font(.title2)
                        .fontWeight(.bold)
                    Text("La base de donnees doit etre mise a jour (version \(dbTooOld.dbSchemaVersion), minimum requis: \(dbTooOld.minRequired)).")
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)
                    Text("Contactez l'administrateur pour appliquer les migrations.")
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)
                }
                .padding()
            } else if isCheckingCompatibility && !hasCheckedCompatibility {
                // Show loading while checking compatibility
                VStack(spacing: 16) {
                    ProgressView()
                    Text(Localized.strings.checkingCompatibility)
                        .foregroundColor(.secondary)
                }
            } else {
                // Normal app flow
                NavigationView {
                    Group {
                        if session.isLoggedIn {
                            HomeView(
                                sdk: sdk,
                                session: session
                            )
                        } else {
                            LoginView(sdk: sdk) { user in
                                session.loginWithAuth(user: user, sdk: sdk)
                            }
                        }
                    }
                }
                .navigationViewStyle(.stack)
            }
        }
        .onAppear {
            // Initialize Supabase from stored configuration
            initializeSupabase()

            Task {
                // Step 1: Non-blocking GitHub update check
                await checkGitHubUpdate()

                // Step 2: Run migrations BEFORE compatibility check
                if session.isLoggedIn {
                    await SupabaseService.shared.restoreSessionIfNeeded()
                    await runMigrationsIfNeeded()
                }

                // Step 3: Check compatibility (bidirectional - blocking if mismatch)
                await checkCompatibilityIfNeeded()

                // Step 4: Create default admin if needed
                await createDefaultAdminIfNeeded()

                // Step 5: Start sync if logged in
                if session.isLoggedIn {
                    SyncScheduler.shared.start(sdk: sdk)
                    if syncStatus.isOnline && SupabaseService.shared.isConfigured {
                        await BidirectionalSyncManager.shared.fullSync(sdk: sdk)
                    }
                }
            }
        }
        .alert("Mise a jour disponible", isPresented: $showUpdateDialog) {
            Button("Plus tard", role: .cancel) { }
            Button("Voir") {
                if let url = URL(string: "https://github.com/kelplant/medistock-app/releases/latest") {
                    UIApplication.shared.open(url)
                }
            }
        } message: {
            if let update = updateAvailable {
                Text("Version \(update.newVersion) disponible (actuelle: \(update.currentVersion))")
            }
        }
    }

    private func checkCompatibilityIfNeeded() async {
        guard !hasCheckedCompatibility else { return }
            guard SupabaseService.shared.isConfigured else {
            hasCheckedCompatibility = true
            return
        }

        isCheckingCompatibility = true
        _ = await compatibilityManager.checkCompatibility()
        hasCheckedCompatibility = true
        isCheckingCompatibility = false
    }

    private func initializeSupabase() {
        // Load stored Supabase configuration
        let defaults = UserDefaults.standard
        if let url = defaults.string(forKey: "supabase_url"),
           let key = defaults.string(forKey: "supabase_key"),
           !url.isEmpty, !key.isEmpty {
            SupabaseService.shared.configure(url: url, anonKey: key)
        }
    }

    /// Creates a default local admin user if no users exist in the database.
    /// Uses the shared DefaultAdminService from the KMM module.
    private func createDefaultAdminIfNeeded() async {
        guard let hashedPassword = PasswordHasher.shared.hashPassword("admin") else {
            debugLog("ContentView", "Failed to hash default admin password")
            return
        }

        let currentTime = Int64(Date().timeIntervalSince1970 * 1000)

        do {
            let created = try await sdk.defaultAdminService.createDefaultAdminIfNeeded(
                hashedPassword: hashedPassword,
                currentTimeMillis: currentTime
            )
            if created.boolValue {
                debugLog("ContentView", "Default local admin user created")
            }
        } catch {
            debugLog("ContentView", "Error creating default admin: \(error)")
        }
    }

    private func runMigrationsIfNeeded() async {
#if canImport(Supabase)
        guard SupabaseService.shared.isConfigured else {
            print("⚠️ Supabase non configuré - migrations ignorées")
            return
        }

        // Check compatibility using shared CompatibilityManager
        // If app is too old, the UI will show the blocking screen
        if compatibilityManager.requiresUpdate {
            print("❌ App trop ancienne - migrations ignorées")
            return
        }

        guard let migrationManager = MigrationManager() else {
            print("⚠️ MigrationManager non initialisé - migrations ignorées")
            return
        }

        // Run pending migrations
        let result = await migrationManager.runPendingMigrations(appliedBy: "ios_app")

        if result.systemNotInstalled {
            print("⚠️ Système de migration non installé dans Supabase")
        } else if !result.migrationsApplied.isEmpty {
            print("✅ \(result.migrationsApplied.count) migration(s) appliquée(s):")
            result.migrationsApplied.forEach { print("   - \($0)") }
        } else if !result.migrationsFailed.isEmpty {
            print("❌ \(result.migrationsFailed.count) migration(s) échouée(s):")
            result.migrationsFailed.forEach { print("   - \($0.0): \($0.1)") }
        } else {
            print("✅ Aucune nouvelle migration à appliquer")
        }
#else
        print("⚠️ Supabase indisponible - migrations ignorées")
#endif
    }

    private func checkGitHubUpdate() async {
        guard let currentVersion = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String else {
            return
        }

        do {
            let url = URL(string: "https://api.github.com/repos/kelplant/medistock-app/releases/latest")!
            var request = URLRequest(url: url)
            request.setValue("application/vnd.github.v3+json", forHTTPHeaderField: "Accept")
            request.setValue("Medistock-iOS", forHTTPHeaderField: "User-Agent")
            request.timeoutInterval = 10

            let (data, response) = try await URLSession.shared.data(for: request)

            guard let httpResponse = response as? HTTPURLResponse,
                  httpResponse.statusCode == 200 else {
                return
            }

            let release = try JSONDecoder().decode(GitHubReleaseResponse.self, from: data)

            guard !release.draft, !release.prerelease else { return }

            let newVersion = release.tagName.replacingOccurrences(of: "v", with: "")

            if isNewerVersion(current: currentVersion, new: newVersion) {
                print("🆕 Nouvelle version disponible: \(newVersion) (actuelle: \(currentVersion))")
                await MainActor.run {
                    updateAvailable = (currentVersion: currentVersion, newVersion: newVersion)
                    showUpdateDialog = true
                }
            }
        } catch {
            print("⚠️ Verification GitHub echouee: \(error.localizedDescription)")
        }
    }

    private func isNewerVersion(current: String, new: String) -> Bool {
        let currentParts = current.split(separator: ".").compactMap { Int($0) }
        let newParts = new.split(separator: ".").compactMap { Int($0) }
        let maxCount = max(currentParts.count, newParts.count)

        for i in 0..<maxCount {
            let c = i < currentParts.count ? currentParts[i] : 0
            let n = i < newParts.count ? newParts[i] : 0
            if n > c { return true }
            if n < c { return false }
        }
        return false
    }
}

/// Minimal GitHub Release response for update checking
private struct GitHubReleaseResponse: Codable {
    let tagName: String
    let name: String
    let draft: Bool
    let prerelease: Bool

    enum CodingKeys: String, CodingKey {
        case tagName = "tag_name"
        case name
        case draft
        case prerelease
    }
}

#Preview {
    ContentView(sdk: MedistockSDK(driverFactory: DatabaseDriverFactory()))
}
