import SwiftUI
import shared

@main
struct MedistockApp: App {

    // Initialize the shared SDK
    let sdk = MedistockSDK(driverFactory: DatabaseDriverFactory())

    init() {
        // Configure SDK provider for sync services
        SDKProvider.shared.configure(sdk: sdk)

        // Load saved language preference
        Localized.loadSavedLanguage()
    }

    var body: some Scene {
        WindowGroup {
            ContentView(sdk: sdk)
                .onOpenURL { url in
                    handleDeepLink(url)
                }
        }
    }

    /// Handle deep links for debug actions
    /// - medistock://debug/seed-test-users - Seeds test users for Maestro E2E tests
    /// - medistock://debug/cleanup-test-users - Removes test users after Maestro E2E tests
    private func handleDeepLink(_ url: URL) {
        guard url.scheme == "medistock" else { return }

        let path = url.host.map { "\($0)\(url.path)" } ?? url.path

        switch path {
        case "debug/seed-test-users":
            Task {
                let count = await TestUserSeeder.instance.seedTestUsers(sdk: sdk)
                print("MedistockApp: Seeded \(count) test users via deep link")
            }
        case "debug/cleanup-test-users":
            Task {
                let count = await TestUserSeeder.instance.removeTestUsers(sdk: sdk)
                print("MedistockApp: Removed \(count) test users via deep link")
            }
        default:
            print("MedistockApp: Unknown deep link path: \(path)")
        }
    }
}
