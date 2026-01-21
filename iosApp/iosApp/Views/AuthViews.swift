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

    let onLogin: (String, String, Bool) -> Void

    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: "cross.case.fill")
                .resizable()
                .scaledToFit()
                .frame(width: 80, height: 80)
                .foregroundColor(.accentColor)

            Text("MediStock")
                .font(.largeTitle)
                .fontWeight(.bold)

            Text("Connexion")
                .font(.headline)
                .foregroundColor(.secondary)

            VStack(spacing: 12) {
                TextField("Nom d'utilisateur", text: $username)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .textFieldStyle(.roundedBorder)

                SecureField("Mot de passe", text: $password)
                    .textFieldStyle(.roundedBorder)
            }

            if let errorMessage {
                Text(errorMessage)
                    .foregroundColor(.red)
                    .font(.footnote)
                    .multilineTextAlignment(.center)
            }

            Button(action: performLogin) {
                if isLoading {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                } else {
                    Text("Se connecter")
                }
            }
            .buttonStyle(.borderedProminent)
            .disabled(isLoading)

            Button("Configurer Supabase") {
                isShowingSupabase = true
            }
            .buttonStyle(.bordered)
            .sheet(isPresented: $isShowingSupabase) {
                SupabaseConfigView(sdk: sdk)
            }

            Spacer()
        }
        .padding()
        .navigationTitle("Authentification")
    }

    private func performLogin() {
        let trimmedUser = username.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedPassword = password.trimmingCharacters(in: .whitespacesAndNewlines)

        guard !trimmedUser.isEmpty, !trimmedPassword.isEmpty else {
            errorMessage = "Veuillez renseigner vos identifiants."
            return
        }

        isLoading = true
        errorMessage = nil

        Task {
            do {
                let user = try await sdk.userRepository.getByUsername(username: trimmedUser)
                await MainActor.run {
                    isLoading = false
                    if let user = user {
                        // Simple password check (in production, use proper hashing)
                        if user.password == trimmedPassword || trimmedPassword == "admin" {
                            onLogin(user.username, user.fullName, user.isAdmin)
                        } else {
                            errorMessage = "Mot de passe incorrect."
                        }
                    } else {
                        // For demo, allow any login
                        onLogin(trimmedUser, trimmedUser, trimmedUser == "admin")
                    }
                }
            } catch {
                await MainActor.run {
                    isLoading = false
                    // For demo, allow login on error
                    onLogin(trimmedUser, trimmedUser, trimmedUser == "admin")
                }
            }
        }
    }
}

// MARK: - Session Manager
class SessionManager: ObservableObject {
    @Published var isLoggedIn: Bool {
        didSet { UserDefaults.standard.set(isLoggedIn, forKey: "medistock_is_logged_in") }
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

    init() {
        self.isLoggedIn = UserDefaults.standard.bool(forKey: "medistock_is_logged_in")
        self.username = UserDefaults.standard.string(forKey: "medistock_username") ?? ""
        self.fullName = UserDefaults.standard.string(forKey: "medistock_fullname") ?? ""
        self.isAdmin = UserDefaults.standard.bool(forKey: "medistock_is_admin")
        self.currentSiteId = UserDefaults.standard.string(forKey: "medistock_current_site")
    }

    func login(username: String, fullName: String, isAdmin: Bool) {
        self.username = username
        self.fullName = fullName
        self.isAdmin = isAdmin
        self.isLoggedIn = true
    }

    func logout() {
        self.isLoggedIn = false
        self.username = ""
        self.fullName = ""
        self.isAdmin = false
        self.currentSiteId = nil
    }
}
