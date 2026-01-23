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
        var label = "\(Localized.profile) - "
        switch syncStatus.indicatorColor {
        case .synced:
            label += Localized.synced
        case .syncing:
            label += Localized.syncing
        case .pending:
            label += Localized.format(Localized.pendingChanges, "count", syncStatus.pendingCount)
        case .offline:
            label += Localized.offlineMode
        case .error:
            label += Localized.strings.syncError
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
                Section(header: Text(Localized.information)) {
                    LabeledContentCompat {
                        Text(Localized.username)
                    } content: {
                        Text(session.username)
                    }

                    LabeledContentCompat {
                        Text(Localized.fullName)
                    } content: {
                        Text(session.fullName)
                    }

                    LabeledContentCompat {
                        Text(Localized.role)
                    } content: {
                        Text(session.isAdmin ? Localized.admin : Localized.user)
                    }
                }

                Section(header: Text(Localized.strings.syncSettings)) {
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
                            Text(Localized.format(Localized.pendingChanges, "count", syncStatus.pendingCount))
                                .font(.subheadline)
                        }
                    }

                    // Conflicts
                    if syncStatus.conflictCount > 0 {
                        HStack {
                            Image(systemName: "exclamationmark.triangle.fill")
                                .foregroundColor(.red)
                            Text(Localized.format(Localized.conflictsToResolve, "count", syncStatus.conflictCount))
                                .font(.subheadline)
                                .foregroundColor(.red)
                        }
                    }

                    // Network status
                    HStack {
                        Image(systemName: syncStatus.isOnline ? "wifi" : "wifi.slash")
                            .foregroundColor(syncStatus.isOnline ? .green : .gray)
                        Text(syncStatus.isOnline ? Localized.online : Localized.offline)
                            .font(.subheadline)
                    }

                    // Realtime status
                    HStack {
                        Image(systemName: realtimeService.isConnected ? "bolt.fill" : "bolt.slash")
                            .foregroundColor(realtimeService.isConnected ? .green : .gray)
                        Text(realtimeService.isConnected ? Localized.realtimeConnected : Localized.realtimeDisconnected)
                            .font(.subheadline)
                    }

                    // Manual sync button
                    Button(action: triggerSync) {
                        HStack {
                            Image(systemName: "arrow.clockwise")
                            Text(Localized.syncNow)
                        }
                    }
                    .disabled(!syncStatus.isOnline || syncStatus.isSyncing)
                }

                if let error = syncStatus.lastSyncInfo.error {
                    Section(header: Text(Localized.lastError)) {
                        Text(error)
                            .font(.caption)
                            .foregroundColor(.red)
                    }
                }

                Section {
                    NavigationLink(destination: ChangePasswordView(sdk: sdk, session: session)) {
                        Label(Localized.strings.changePassword, systemImage: "key")
                    }
                }

                Section(header: Text(Localized.settings)) {
                    NavigationLink(destination: LanguagePickerView()) {
                        HStack {
                            Label(Localized.language, systemImage: "globe")
                            Spacer()
                            Text(Localized.currentLanguageDisplayName)
                                .foregroundColor(.secondary)
                        }
                    }
                }

                Section {
                    Button(role: .destructive) {
                        logout()
                    } label: {
                        Label(Localized.logout, systemImage: "rectangle.portrait.and.arrow.right")
                    }
                }
            }
            .navigationTitle(Localized.myProfile)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(Localized.close) { dismiss() }
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
            Section(header: Text(Localized.currentPassword)) {
                SecureField(Localized.currentPassword, text: $currentPassword)
            }

            Section(header: Text(Localized.newPassword)) {
                SecureField(Localized.newPassword, text: $newPassword)
                SecureField(Localized.confirmPassword, text: $confirmPassword)
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
                        Text(Localized.changePassword)
                    }
                }
                .disabled(isLoading || !isFormValid)
            }
        }
        .navigationTitle(Localized.changePassword)
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
                errorMessage = Localized.passwordsDoNotMatch
            } else if newPassword.count < 4 {
                errorMessage = Localized.strings.passwordTooShort
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
                    successMessage = Localized.passwordChangedSuccessfully
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
            return Localized.userNotFound
        case .incorrectPassword:
            return Localized.incorrectPassword
        case .hashingFailed:
            return Localized.error
        }
    }
}

// MARK: - Language Picker View
struct LanguagePickerView: View {
    @State private var selectedLanguage: AppLanguage = AppLanguage.from(code: Localized.currentLanguageCode)
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        List {
            ForEach(AppLanguage.allCases) { language in
                Button(action: {
                    selectLanguage(language)
                }) {
                    HStack {
                        Text(language.displayName)
                            .foregroundColor(.primary)

                        Spacer()

                        if language == selectedLanguage {
                            Image(systemName: "checkmark")
                                .foregroundColor(.accentColor)
                        }
                    }
                }
            }
        }
        .navigationTitle(Localized.selectLanguage)
    }

    private func selectLanguage(_ language: AppLanguage) {
        selectedLanguage = language
        Localized.setLanguage(language)

        // Dismiss to refresh the parent view with new language
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            dismiss()
        }
    }
}
