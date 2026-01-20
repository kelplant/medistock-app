import SwiftUI
import shared

@main
struct MedistockApp: App {

    // Initialize the shared SDK
    let sdk = MedistockSDK(driverFactory: DatabaseDriverFactory())

    var body: some Scene {
        WindowGroup {
            ContentView(sdk: sdk)
        }
    }
}
