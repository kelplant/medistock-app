import SwiftUI
import shared

/// View displayed when the app version is incompatible with the database.
/// This view blocks the app until the user updates.
struct AppUpdateRequiredView: View {
    let appVersion: Int
    let minRequired: Int
    let dbVersion: Int

    var body: some View {
        VStack(spacing: 24) {
            Spacer()

            // Warning icon
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 80))
                .foregroundColor(.orange)

            // Title
            Text("Update Required")
                .font(.largeTitle)
                .fontWeight(.bold)

            // Message
            Text("Your app version is not compatible with the database. Please update the app to continue.")
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)

            // Version info box
            VStack(spacing: 12) {
                HStack {
                    Text("App version:")
                        .foregroundColor(.secondary)
                    Spacer()
                    Text("\(appVersion)")
                        .fontWeight(.semibold)
                        .foregroundColor(.red)
                }

                Divider()

                HStack {
                    Text("Minimum required version:")
                        .foregroundColor(.secondary)
                    Spacer()
                    Text("\(minRequired)")
                        .fontWeight(.semibold)
                        .foregroundColor(.green)
                }

                Divider()

                HStack {
                    Text("Database version:")
                        .foregroundColor(.secondary)
                    Spacer()
                    Text("\(dbVersion)")
                        .fontWeight(.semibold)
                }
            }
            .padding()
            .background(Color(.systemGray6))
            .cornerRadius(12)
            .padding(.horizontal)

            Spacer()

            // Instructions
            VStack(spacing: 8) {
                Text("To update:")
                    .font(.headline)

                Text("Contact your administrator to get the latest version of the app.")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }
            .padding(.horizontal)

            Spacer()
        }
        .padding()
        .background(Color(.systemBackground))
        .interactiveDismissDisabled() // Prevent dismissal
    }
}

/// A wrapper view that checks compatibility and shows either content or update required screen
struct CompatibilityGuardView<Content: View>: View {
    let sdk: MedistockSDK
    @ViewBuilder let content: () -> Content

    @StateObject private var compatibilityManager = CompatibilityManager.shared
    @State private var hasChecked = false

    var body: some View {
        Group {
            if compatibilityManager.isChecking && !hasChecked {
                // Show loading while checking
                VStack(spacing: 16) {
                    ProgressView()
                    Text("Checking compatibility...")
                        .foregroundColor(.secondary)
                }
            } else if let result = compatibilityManager.compatibilityResult,
                      let appTooOld = result as? shared.CompatibilityResult.AppTooOld {
                // Show update required screen
                AppUpdateRequiredView(
                    appVersion: Int(appTooOld.appVersion),
                    minRequired: Int(appTooOld.minRequired),
                    dbVersion: Int(appTooOld.dbVersion)
                )
            } else {
                // Show normal content
                content()
            }
        }
        .task {
            if !hasChecked {
                _ = await compatibilityManager.checkCompatibility()
                hasChecked = true
            }
        }
    }
}

#Preview("Update Required") {
    AppUpdateRequiredView(
        appVersion: 1,
        minRequired: 3,
        dbVersion: 5
    )
}
