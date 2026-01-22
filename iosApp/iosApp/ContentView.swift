import SwiftUI
import shared

struct ContentView: View {
    let sdk: MedistockSDK
    @ObservedObject private var session = SessionManager.shared
    @ObservedObject private var syncStatus = SyncStatusManager.shared
    @ObservedObject private var compatibilityManager = CompatibilityManager.shared

    @State private var hasCheckedCompatibility = false
    @State private var isCheckingCompatibility = false

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
            } else if isCheckingCompatibility && !hasCheckedCompatibility {
                // Show loading while checking compatibility
                VStack(spacing: 16) {
                    ProgressView()
                    Text("Vérification de la compatibilité...")
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

            // Check compatibility and create default admin if needed
            Task {
                await checkCompatibilityIfNeeded()
                await createDefaultAdminIfNeeded()
            }

            // If user is already logged in (session restored), start sync
            if session.isLoggedIn {
                startSyncForRestoredSession()
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
            if created {
                debugLog("ContentView", "Default local admin user created")
            }
        } catch {
            debugLog("ContentView", "Error creating default admin: \(error)")
        }
    }

    private func startSyncForRestoredSession() {
        // Run migrations BEFORE starting sync
        Task {
            await runMigrationsIfNeeded()

            // Start the sync scheduler after migrations
            SyncScheduler.shared.start(sdk: sdk)

            // Trigger initial sync if online and configured
            if syncStatus.isOnline && SupabaseService.shared.isConfigured {
                await BidirectionalSyncManager.shared.fullSync(sdk: sdk)
            }
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

        let migrationManager = MigrationManager()

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
}

#Preview {
    ContentView(sdk: MedistockSDK(driverFactory: DatabaseDriverFactory()))
}
