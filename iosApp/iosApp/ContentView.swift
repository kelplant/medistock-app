import SwiftUI
import shared

struct ContentView: View {
    let sdk: MedistockSDK
    @ObservedObject private var session = SessionManager.shared

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
                        session.loginWithAuth(user: user)
                    }
                }
            }
        }
        .navigationViewStyle(.stack)
    }
}

#Preview {
    ContentView(sdk: MedistockSDK(driverFactory: DatabaseDriverFactory()))
}
