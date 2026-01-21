import Foundation
import SwiftUI
import shared

// MARK: - Profile Badge View
/// Badge showing user initial with realtime status border (green/yellow/red)
struct ProfileBadgeView: View {
    @ObservedObject var session: SessionManager
    @ObservedObject var realtimeService = RealtimeService.shared

    var initial: String {
        let name = session.fullName.isEmpty ? session.username : session.fullName
        return String(name.prefix(1)).uppercased()
    }

    var statusColor: Color {
        switch realtimeService.status {
        case .connected:
            return .green
        case .connecting:
            return .orange
        case .disconnected:
            return .red
        }
    }

    var backgroundColor: Color {
        switch realtimeService.status {
        case .connected:
            return Color(red: 232/255, green: 245/255, blue: 233/255)
        case .connecting:
            return Color(red: 255/255, green: 248/255, blue: 225/255)
        case .disconnected:
            return Color(red: 255/255, green: 235/255, blue: 238/255)
        }
    }

    var textColor: Color {
        switch realtimeService.status {
        case .connected:
            return Color(red: 46/255, green: 125/255, blue: 50/255)
        case .connecting:
            return Color(red: 255/255, green: 143/255, blue: 0/255)
        case .disconnected:
            return Color(red: 198/255, green: 40/255, blue: 40/255)
        }
    }

    var body: some View {
        Text(initial)
            .font(.system(size: 16, weight: .bold))
            .foregroundColor(textColor)
            .frame(width: 36, height: 36)
            .background(backgroundColor)
            .clipShape(Circle())
            .overlay(
                Circle()
                    .stroke(statusColor, lineWidth: 3)
            )
            .accessibilityLabel(statusAccessibilityLabel)
    }

    private var statusAccessibilityLabel: String {
        switch realtimeService.status {
        case .connected:
            return "Profil - Realtime connecté"
        case .connecting:
            return "Profil - Connexion Realtime..."
        case .disconnected:
            return "Profil - Realtime déconnecté"
        }
    }
}

// MARK: - Profile Menu View
struct ProfileMenuView: View {
    let sdk: MedistockSDK
    @ObservedObject var session: SessionManager
    @ObservedObject var realtimeService = RealtimeService.shared
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationView {
            List {
                Section(header: Text("Informations")) {
                    LabeledContentCompat {
                        Text("Nom d'utilisateur")
                    } content: {
                        Text(session.username)
                    }

                    LabeledContentCompat {
                        Text("Nom complet")
                    } content: {
                        Text(session.fullName)
                    }

                    LabeledContentCompat {
                        Text("Rôle")
                    } content: {
                        Text(session.isAdmin ? "Administrateur" : "Utilisateur")
                    }
                }

                Section(header: Text("Statut Supabase")) {
                    HStack {
                        Circle()
                            .fill(realtimeService.status == .connected ? Color.green :
                                    realtimeService.status == .connecting ? Color.orange : Color.red)
                            .frame(width: 12, height: 12)

                        Text(statusText)
                            .foregroundColor(.secondary)
                    }

                    if let error = realtimeService.lastError {
                        Text(error)
                            .font(.caption)
                            .foregroundColor(.red)
                    }
                }

                Section {
                    NavigationLink(destination: ChangePasswordView(session: session)) {
                        Label("Changer le mot de passe", systemImage: "key")
                    }
                }

                Section {
                    Button(role: .destructive) {
                        session.logout()
                        dismiss()
                    } label: {
                        Label("Déconnexion", systemImage: "rectangle.portrait.and.arrow.right")
                    }
                }
            }
            .navigationTitle("Mon profil")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Fermer") { dismiss() }
                }
            }
        }
    }

    private var statusText: String {
        switch realtimeService.status {
        case .connected:
            return "Connecté en temps réel"
        case .connecting:
            return "Connexion en cours..."
        case .disconnected:
            return "Déconnecté"
        }
    }
}

// MARK: - Change Password View
struct ChangePasswordView: View {
    @ObservedObject var session: SessionManager
    @Environment(\.dismiss) private var dismiss

    @State private var currentPassword = ""
    @State private var newPassword = ""
    @State private var confirmPassword = ""
    @State private var isLoading = false
    @State private var errorMessage: String?
    @State private var successMessage: String?

    var body: some View {
        Form {
            Section(header: Text("Mot de passe actuel")) {
                SecureField("Mot de passe actuel", text: $currentPassword)
            }

            Section(header: Text("Nouveau mot de passe")) {
                SecureField("Nouveau mot de passe", text: $newPassword)
                SecureField("Confirmer le mot de passe", text: $confirmPassword)
            }

            if let errorMessage {
                Section {
                    Text(errorMessage)
                        .foregroundColor(.red)
                }
            }

            if let successMessage {
                Section {
                    Text(successMessage)
                        .foregroundColor(.green)
                }
            }

            Section {
                Button(action: changePassword) {
                    if isLoading {
                        ProgressView()
                    } else {
                        Text("Changer le mot de passe")
                    }
                }
                .disabled(isLoading || !isFormValid)
            }
        }
        .navigationTitle("Changer le mot de passe")
    }

    private var isFormValid: Bool {
        !currentPassword.isEmpty &&
        !newPassword.isEmpty &&
        newPassword.count >= 4 &&
        newPassword == confirmPassword
    }

    private func changePassword() {
        guard isFormValid else {
            if newPassword != confirmPassword {
                errorMessage = "Les mots de passe ne correspondent pas"
            } else if newPassword.count < 4 {
                errorMessage = "Le mot de passe doit contenir au moins 4 caractères"
            }
            return
        }

        isLoading = true
        errorMessage = nil
        successMessage = nil

        Task {
            do {
                // Update password via Supabase if configured
                if SupabaseClient.shared.isConfigured {
                    try await updatePasswordOnSupabase()
                }

                await MainActor.run {
                    isLoading = false
                    successMessage = "Mot de passe modifié avec succès"
                    currentPassword = ""
                    newPassword = ""
                    confirmPassword = ""

                    // Auto-dismiss after success
                    DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                        dismiss()
                    }
                }
            } catch {
                await MainActor.run {
                    isLoading = false
                    errorMessage = error.localizedDescription
                }
            }
        }
    }

    private func updatePasswordOnSupabase() async throws {
        // Build update request
        guard let baseUrl = SupabaseClient.shared.supabaseUrl,
              let apiKey = SupabaseClient.shared.supabaseKey else {
            throw SupabaseError.notConfigured
        }

        let urlString = "\(baseUrl)/rest/v1/app_users?id=eq.\(session.userId)"
        guard let url = URL(string: urlString) else {
            throw SupabaseError.invalidURL
        }

        var request = URLRequest(url: url)
        request.httpMethod = "PATCH"
        request.setValue(apiKey, forHTTPHeaderField: "apikey")
        request.setValue("Bearer \(apiKey)", forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        let body: [String: Any] = [
            "password": newPassword,
            "updated_at": Int64(Date().timeIntervalSince1970 * 1000),
            "updated_by": session.userId
        ]
        request.httpBody = try JSONSerialization.data(withJSONObject: body)

        let (_, response) = try await URLSession.shared.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse,
              (200...299).contains(httpResponse.statusCode) else {
            throw SupabaseError.serverError(statusCode: (response as? HTTPURLResponse)?.statusCode ?? 500)
        }
    }
}
