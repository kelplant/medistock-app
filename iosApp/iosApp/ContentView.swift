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
        // Start the sync scheduler
        SyncScheduler.shared.start(sdk: sdk)

        // Trigger initial sync if online and configured
        if syncStatus.isOnline && SupabaseService.shared.isConfigured {
            Task {
                await BidirectionalSyncManager.shared.fullSync(sdk: sdk)
            }
        }
    }
}

#Preview {
    ContentView(sdk: MedistockSDK(driverFactory: DatabaseDriverFactory()))
}
