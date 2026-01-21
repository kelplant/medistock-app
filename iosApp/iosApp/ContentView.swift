import SwiftUI
import shared

struct ContentView: View {
    let sdk: MedistockSDK
    @AppStorage("medistock_is_logged_in") private var isLoggedIn = false
    @AppStorage("medistock_username") private var username = ""
    var body: some View {
        NavigationView {
            Group {
                if isLoggedIn {
                    HomeView(
                        sdk: sdk,
                        username: username,
                        onLogout: {
                            isLoggedIn = false
                        }
                    )
                } else {
                    LoginView { user in
                        username = user
                        isLoggedIn = true
                    }
                }
            }
        }
    }
}

#Preview {
    ContentView(sdk: MedistockSDK(driverFactory: DatabaseDriverFactory()))
}
