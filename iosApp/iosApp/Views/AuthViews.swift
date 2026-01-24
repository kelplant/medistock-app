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

            Button(Localized.configureSupabase) {
                isShowingSupabase = true
            }
            .buttonStyle(.bordered)
            .sheet(isPresented: $isShowingSupabase) {
                SupabaseConfigView(sdk: sdk)
            }

            Spacer()
        }
        .padding()
        .navigationTitle(Localized.authentication)
    }

    private func performLogin() {
        let trimmedUser = username.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedPassword = password.trimmingCharacters(in: .whitespacesAndNewlines)

        guard !trimmedUser.isEmpty, !trimmedPassword.isEmpty else {
            errorMessage = Localized.enterCredentials
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
                    errorMessage = Localized.invalidPassword

                case .userNotFound:
                    errorMessage = Localized.userNotFound

                case .userInactive:
                    errorMessage = Localized.accountDisabled

                case .networkError(let message):
                    errorMessage = "\(Localized.connectionError): \(message)"

                case .notConfigured:
                    errorMessage = Localized.supabaseNotConfigured

                case .networkRequired:
                    errorMessage = Localized.firstLoginRequiresInternet
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
    @Published var language: String? {
        didSet { UserDefaults.standard.set(language, forKey: "medistock_user_language") }
    }

    /// Whether the user has a valid Supabase Auth session
    @Published var hasAuthSession: Bool = false

    private let keychain = KeychainService.shared

    private init() {
        self.isLoggedIn = UserDefaults.standard.bool(forKey: "medistock_is_logged_in")
        self.userId = UserDefaults.standard.string(forKey: "medistock_user_id") ?? ""
        self.username = UserDefaults.standard.string(forKey: "medistock_username") ?? ""
        self.fullName = UserDefaults.standard.string(forKey: "medistock_fullname") ?? ""
        self.isAdmin = UserDefaults.standard.bool(forKey: "medistock_is_admin")
        self.currentSiteId = UserDefaults.standard.string(forKey: "medistock_current_site")
        self.language = UserDefaults.standard.string(forKey: "medistock_user_language")

        // Check for stored auth tokens
        self.hasAuthSession = keychain.hasAuthTokens && !keychain.areAuthTokensExpired

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
        // Update auth session status
        self.hasAuthSession = keychain.hasAuthTokens && !keychain.areAuthTokensExpired
    }

    func logout() {
        self.isLoggedIn = false
        self.userId = ""
        self.username = ""
        self.fullName = ""
        self.isAdmin = false
        self.currentSiteId = nil
        self.language = nil
        self.hasAuthSession = false
        PermissionManager.shared.clearPermissions()

        // Sign out from Supabase Auth
        Task {
            await AuthService.shared.signOut()
        }
    }

    /// Refresh auth session status
    func refreshAuthStatus() {
        hasAuthSession = keychain.hasAuthTokens && !keychain.areAuthTokensExpired
    }

    /// Check if we need to re-authenticate (token expired)
    var needsReauthentication: Bool {
        return isLoggedIn && !hasAuthSession && SupabaseService.shared.isConfigured
    }
}
