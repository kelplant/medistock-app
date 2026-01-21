import SwiftUI
import shared

struct ContentView: View {
    let sdk: MedistockSDK
    @ObservedObject private var session = SessionManager.shared
    @ObservedObject private var syncStatus = SyncStatusManager.shared

    var body: some View {
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
        .onAppear {
            // Initialize Supabase from stored configuration
            initializeSupabase()

            // If user is already logged in (session restored), start sync
            if session.isLoggedIn {
                startSyncForRestoredSession()
            }
        }
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

        let migrationManager = MigrationManager()

        // Check compatibility first
        let compat = await migrationManager.checkCompatibility()
        switch compat {
        case .compatible:
            print("✅ App compatible avec la base de données")
        case .appTooOld(let appVersion, let minRequired, let dbVersion):
            print("❌ App trop ancienne: app=\(appVersion), min=\(minRequired), db=\(dbVersion)")
            // TODO: Show update required screen
            return
        case .unknown(let reason):
            print("⚠️ Impossible de vérifier la compatibilité: \(reason)")
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
}

#Preview {
    ContentView(sdk: MedistockSDK(driverFactory: DatabaseDriverFactory()))
}
