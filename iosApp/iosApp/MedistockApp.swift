import SwiftUI
import shared

@main
struct MedistockApp: App {

    // Initialize the shared SDK
    let sdk = MedistockSDK(driverFactory: DatabaseDriverFactory())

    init() {
        // Configure SDK provider for sync services
        SDKProvider.shared.configure(sdk: sdk)
    }

    var body: some Scene {
        WindowGroup {
            ContentView(sdk: sdk)
        }
    }
}
