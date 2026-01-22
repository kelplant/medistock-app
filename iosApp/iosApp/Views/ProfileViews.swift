import Foundation
import SwiftUI
import shared

// MARK: - Profile Badge View
/// Badge showing user initial with sync status border (green/yellow/red)
struct ProfileBadgeView: View {
    @ObservedObject var session: SessionManager
    @ObservedObject private var syncStatus = SyncStatusManager.shared

    var initial: String {
        let name = session.fullName.isEmpty ? session.username : session.fullName
        return String(name.prefix(1)).uppercased()
    }

    var statusColor: Color {
        switch syncStatus.indicatorColor {
        case .synced:
            return .green
        case .syncing:
            return .blue
        case .pending:
            return .orange
        case .offline:
            return .gray
        case .error:
            return .red
        }
    }

    var backgroundColor: Color {
        switch syncStatus.indicatorColor {
        case .synced:
            return Color(red: 232/255, green: 245/255, blue: 233/255)
        case .syncing:
            return Color(red: 227/255, green: 242/255, blue: 253/255)
        case .pending:
            return Color(red: 255/255, green: 248/255, blue: 225/255)
        case .offline:
            return Color(red: 245/255, green: 245/255, blue: 245/255)
        case .error:
            return Color(red: 255/255, green: 235/255, blue: 238/255)
        }
    }

    var textColor: Color {
        switch syncStatus.indicatorColor {
        case .synced:
            return Color(red: 46/255, green: 125/255, blue: 50/255)
        case .syncing:
            return Color(red: 25/255, green: 118/255, blue: 210/255)
        case .pending:
            return Color(red: 255/255, green: 143/255, blue: 0/255)
        case .offline:
            return Color(red: 117/255, green: 117/255, blue: 117/255)
        case .error:
            return Color(red: 198/255, green: 40/255, blue: 40/255)
        }
    }

    var body: some View {
        ZStack(alignment: .topTrailing) {
            Text(initial)
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(textColor)
                .frame(width: 30, height: 30)
                .background(backgroundColor)
                .clipShape(Circle())
                .overlay(
                    Circle()
                        .stroke(statusColor, lineWidth: 2.5)
                )

            // Pending count badge
            if syncStatus.pendingCount > 0 {
                Text("\(min(syncStatus.pendingCount, 99))")
                    .font(.system(size: 9, weight: .bold))
                    .foregroundColor(.white)
                    .frame(width: 16, height: 16)
                    .background(Color.orange)
                    .clipShape(Circle())
                    .offset(x: 4, y: -4)
            }

            // Conflict indicator
            if syncStatus.conflictCount > 0 {
                Image(systemName: "exclamationmark.triangle.fill")
                    .font(.system(size: 10))
                    .foregroundColor(.red)
                    .offset(x: -20, y: -4)
            }
        }
        .padding(2)
        .accessibilityLabel(statusAccessibilityLabel)
    }

    private var statusAccessibilityLabel: String {
        var label = "Profil - "
        switch syncStatus.indicatorColor {
        case .synced:
            label += "Synchronisé"
        case .syncing:
            label += "Synchronisation en cours"
        case .pending:
            label += "\(syncStatus.pendingCount) modifications en attente"
        case .offline:
            label += "Mode hors ligne"
        case .error:
            label += "Erreur de synchronisation"
        }
        return label
    }
}

// MARK: - Profile Menu View
struct ProfileMenuView: View {
    let sdk: MedistockSDK
    @ObservedObject var session: SessionManager
    @ObservedObject private var syncStatus = SyncStatusManager.shared
    @ObservedObject private var syncManager = BidirectionalSyncManager.shared
    @ObservedObject private var realtimeService = RealtimeSyncService.shared
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

                Section(header: Text("Synchronisation")) {
                    // Status indicator
                    HStack {
                        Circle()
                            .fill(statusColor)
                            .frame(width: 12, height: 12)

                        Text(syncStatus.statusSummary)
                            .foregroundColor(.secondary)

                        Spacer()

                        if syncStatus.isSyncing {
                            ProgressView()
                                .scaleEffect(0.8)
                        }
                    }

                    // Pending operations
                    if syncStatus.pendingCount > 0 {
                        HStack {
                            Image(systemName: "arrow.triangle.2.circlepath")
                                .foregroundColor(.orange)
                            Text("\(syncStatus.pendingCount) modification(s) en attente")
                                .font(.subheadline)
                        }
                    }

                    // Conflicts
                    if syncStatus.conflictCount > 0 {
                        HStack {
                            Image(systemName: "exclamationmark.triangle.fill")
                                .foregroundColor(.red)
                            Text("\(syncStatus.conflictCount) conflit(s) à résoudre")
                                .font(.subheadline)
                                .foregroundColor(.red)
                        }
                    }

                    // Network status
                    HStack {
                        Image(systemName: syncStatus.isOnline ? "wifi" : "wifi.slash")
                            .foregroundColor(syncStatus.isOnline ? .green : .gray)
                        Text(syncStatus.isOnline ? "En ligne" : "Hors ligne")
                            .font(.subheadline)
                    }

                    // Realtime status
                    HStack {
                        Image(systemName: realtimeService.isConnected ? "bolt.fill" : "bolt.slash")
                            .foregroundColor(realtimeService.isConnected ? .green : .gray)
                        Text(realtimeService.isConnected ? "Realtime connecté" : "Realtime déconnecté")
                            .font(.subheadline)
                    }

                    // Manual sync button
                    Button(action: triggerSync) {
                        HStack {
                            Image(systemName: "arrow.clockwise")
                            Text("Synchroniser maintenant")
                        }
                    }
                    .disabled(!syncStatus.isOnline || syncStatus.isSyncing)
                }

                if let error = syncStatus.lastSyncInfo.error {
                    Section(header: Text("Dernière erreur")) {
                        Text(error)
                            .font(.caption)
                            .foregroundColor(.red)
                    }
                }

                Section {
                    NavigationLink(destination: ChangePasswordView(sdk: sdk, session: session)) {
                        Label("Changer le mot de passe", systemImage: "key")
                    }
                }

                Section {
                    Button(role: .destructive) {
                        logout()
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

    private var statusColor: Color {
        switch syncStatus.indicatorColor {
        case .synced: return .green
        case .syncing: return .blue
        case .pending: return .orange
        case .offline: return .gray
        case .error: return .red
        }
    }

    private func triggerSync() {
        Task {
            await syncManager.fullSync(sdk: sdk)
        }
    }

    private func logout() {
        SyncScheduler.shared.stop()
        session.logout()
        dismiss()
    }
}

// MARK: - Change Password View
struct ChangePasswordView: View {
    let sdk: MedistockSDK
    @ObservedObject var session: SessionManager
    @ObservedObject private var syncStatus = SyncStatusManager.shared
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
                // 1. Get current user from local database
                guard let user = try await sdk.userRepository.getById(id: session.userId) else {
                    throw PasswordChangeError.userNotFound
                }

                // 2. Verify current password using BCrypt
                guard PasswordHasher.shared.verifyPassword(currentPassword, storedPassword: user.password) else {
                    throw PasswordChangeError.incorrectPassword
                }

                // 3. Hash new password with BCrypt
                guard let hashedPassword = PasswordHasher.shared.hashPassword(newPassword) else {
                    throw PasswordChangeError.hashingFailed
                }

                let now = Int64(Date().timeIntervalSince1970 * 1000)

                // 4. Create updated user
                let updatedUser = User(
                    id: user.id,
                    username: user.username,
                    password: hashedPassword,
                    fullName: user.fullName,
                    isAdmin: user.isAdmin,
                    isActive: user.isActive,
                    createdAt: user.createdAt,
                    updatedAt: now,
                    createdBy: user.createdBy,
                    updatedBy: session.userId
                )

                // 5. Online-first: try Supabase first
                if syncStatus.isOnline && SupabaseService.shared.isConfigured {
                    let dto = UserDTO(from: updatedUser)
                    try await SupabaseService.shared.upsert(into: "app_users", record: dto)
                }

                // 6. Update local database
                try await sdk.userRepository.update(user: updatedUser)

                // 7. Queue for sync if offline
                if !syncStatus.isOnline {
                    SyncQueueHelper.shared.enqueueUserUpdate(updatedUser, userId: session.userId, lastKnownRemoteUpdatedAt: user.updatedAt)
                }

                await MainActor.run {
                    isLoading = false
                    successMessage = "Mot de passe modifié avec succès"
                    currentPassword = ""
                    newPassword = ""
                    confirmPassword = ""

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
}

// MARK: - Password Change Errors
enum PasswordChangeError: LocalizedError {
    case userNotFound
    case incorrectPassword
    case hashingFailed

    var errorDescription: String? {
        switch self {
        case .userNotFound:
            return "Utilisateur non trouvé"
        case .incorrectPassword:
            return "Mot de passe actuel incorrect"
        case .hashingFailed:
            return "Erreur lors du hachage du mot de passe"
        }
    }
}
