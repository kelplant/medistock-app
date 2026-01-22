import Foundation
import SwiftUI
import shared

// MARK: - Login View
struct LoginView: View {
    let sdk: MedistockSDK
    @State private var username: String = ""
    @State private var password: String = ""
    @State private var errorMessage: String?
    @State private var isLoading = false
    @State private var isShowingSupabase = false

    let onLogin: (UserDTO) -> Void

    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: "cross.case.fill")
                .resizable()
                .scaledToFit()
                .frame(width: 80, height: 80)
                .foregroundColor(.accentColor)

            Text(Localized.appName)
                .font(.largeTitle)
                .fontWeight(.bold)

            Text(Localized.loginTitle)
                .font(.headline)
                .foregroundColor(.secondary)

            VStack(spacing: 12) {
                TextField(Localized.username, text: $username)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .textFieldStyle(.roundedBorder)

                SecureField(Localized.password, text: $password)
                    .textFieldStyle(.roundedBorder)
            }

            if let errorMessage {
                Text(errorMessage)
                    .foregroundColor(.red)
                    .font(.footnote)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal)
            }

            Button(action: performLogin) {
                if isLoading {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                } else {
                    Text(Localized.login)
                }
            }
            .buttonStyle(.borderedProminent)
            .disabled(isLoading || username.isEmpty || password.isEmpty)

            Button("Configure Supabase") {
                isShowingSupabase = true
            }
            .buttonStyle(.bordered)
            .sheet(isPresented: $isShowingSupabase) {
                SupabaseConfigView(sdk: sdk)
            }

            Spacer()
        }
        .padding()
        .navigationTitle("Authentication")
    }

    private func performLogin() {
        let trimmedUser = username.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedPassword = password.trimmingCharacters(in: .whitespacesAndNewlines)

        guard !trimmedUser.isEmpty, !trimmedPassword.isEmpty else {
            errorMessage = "Please enter your credentials."
            return
        }

        isLoading = true
        errorMessage = nil

        Task {
            let result = await AuthService.shared.authenticate(
                username: trimmedUser,
                password: trimmedPassword,
                sdk: sdk
            )

            await MainActor.run {
                isLoading = false

                switch result {
                case .success(let user):
                    onLogin(user)

                case .invalidCredentials:
                    errorMessage = "Invalid password."

                case .userNotFound:
                    errorMessage = "User not found."

                case .userInactive:
                    errorMessage = "This account is disabled. Contact an administrator."

                case .networkError(let message):
                    errorMessage = "Connection error: \(message)"

                case .notConfigured:
                    errorMessage = "Supabase is not configured and no local user found."
                }
            }
        }
    }
}

// MARK: - Session Manager
class SessionManager: ObservableObject {
    /// Shared singleton instance - all code should use this
    static let shared = SessionManager()

    @Published var isLoggedIn: Bool {
        didSet { UserDefaults.standard.set(isLoggedIn, forKey: "medistock_is_logged_in") }
    }
    @Published var userId: String {
        didSet { UserDefaults.standard.set(userId, forKey: "medistock_user_id") }
    }
    @Published var username: String {
        didSet { UserDefaults.standard.set(username, forKey: "medistock_username") }
    }
    @Published var fullName: String {
        didSet { UserDefaults.standard.set(fullName, forKey: "medistock_fullname") }
    }
    @Published var isAdmin: Bool {
        didSet { UserDefaults.standard.set(isAdmin, forKey: "medistock_is_admin") }
    }
    @Published var currentSiteId: String? {
        didSet { UserDefaults.standard.set(currentSiteId, forKey: "medistock_current_site") }
    }

    private init() {
        self.isLoggedIn = UserDefaults.standard.bool(forKey: "medistock_is_logged_in")
        self.userId = UserDefaults.standard.string(forKey: "medistock_user_id") ?? ""
        self.username = UserDefaults.standard.string(forKey: "medistock_username") ?? ""
        self.fullName = UserDefaults.standard.string(forKey: "medistock_fullname") ?? ""
        self.isAdmin = UserDefaults.standard.bool(forKey: "medistock_is_admin")
        self.currentSiteId = UserDefaults.standard.string(forKey: "medistock_current_site")

        // Load permissions if already logged in
        if isLoggedIn && !userId.isEmpty {
            Task {
                await PermissionManager.shared.loadPermissions(forUserId: userId)
            }
        }
    }

    func login(username: String, fullName: String, isAdmin: Bool) {
        self.username = username
        self.fullName = fullName
        self.isAdmin = isAdmin
        self.isLoggedIn = true
    }

    func logout() {
        self.isLoggedIn = false
        self.userId = ""
        self.username = ""
        self.fullName = ""
        self.isAdmin = false
        self.currentSiteId = nil
        PermissionManager.shared.clearPermissions()
    }
}
