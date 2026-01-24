import Foundation
import SwiftUI
import shared

// MARK: - Supabase Config View
struct SupabaseConfigView: View {
    let sdk: MedistockSDK

    @Environment(\.dismiss) private var dismiss
    @ObservedObject private var syncStatus = SyncStatusManager.shared
    @State private var projectUrl: String = ""
    @State private var anonKey: String = ""
    @State private var isSaving = false
    @State private var isSyncing = false
    @State private var showSuccess = false
    @State private var errorMessage: String?
    @State private var syncMessage: String?
    @State private var hasExistingKey: Bool = false

    private let supabase = SupabaseService.shared
    private let maskedKey = "••••••••••••••••••••••••••••••••"

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text(Localized.supabaseConfiguration)) {
                    VStack(alignment: .leading, spacing: 8) {
                        Text(Localized.projectUrl)
                            .font(.caption)
                            .foregroundColor(.secondary)
                        TextField("https://xxx.supabase.co", text: $projectUrl)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                            .keyboardType(.URL)
                    }

                    VStack(alignment: .leading, spacing: 8) {
                        Text(Localized.anonKey)
                            .font(.caption)
                            .foregroundColor(.secondary)
                        if hasExistingKey && anonKey.isEmpty {
                            HStack {
                                Text(maskedKey)
                                    .foregroundColor(.secondary)
                                Spacer()
                                Button(Localized.edit) {
                                    hasExistingKey = false
                                }
                                .font(.caption)
                            }
                            .padding(8)
                            .background(Color(.systemGray6))
                            .cornerRadius(6)
                        } else {
                            SecureField("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", text: $anonKey)
                                .textInputAutocapitalization(.never)
                                .autocorrectionDisabled()
                        }
                    }
                }

                Section {
                    Button(action: saveConfig) {
                        HStack {
                            Spacer()
                            if isSaving {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle())
                            } else {
                                Text(Localized.save)
                            }
                            Spacer()
                        }
                    }
                    .disabled(projectUrl.isEmpty || (!hasExistingKey && anonKey.isEmpty) || isSaving)
                }

                if showSuccess {
                    Section {
                        HStack {
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundColor(.green)
                            Text(Localized.configSaved)
                                .foregroundColor(.green)
                        }
                    }
                }

                if let errorMessage {
                    Section {
                        Text(errorMessage)
                            .foregroundColor(.red)
                    }
                }

                Section(header: Text(Localized.synchronization)) {
                    Button(action: performSync) {
                        HStack {
                            Spacer()
                            if isSyncing {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle())
                                Text(Localized.syncing)
                            } else {
                                Image(systemName: "arrow.triangle.2.circlepath")
                                Text(Localized.syncData)
                            }
                            Spacer()
                        }
                    }
                    .disabled(!supabase.isConfigured || isSyncing)

                    if let syncMessage {
                        Text(syncMessage)
                            .font(.caption)
                            .foregroundColor(syncMessage.contains("Error") ? .red : .green)
                    }

                    if let lastSync = syncStatus.lastSyncInfo.timestamp {
                        LabeledContentCompat {
                            Text(Localized.lastSync)
                        } content: {
                            Text(lastSync, style: .relative)
                                .foregroundColor(.secondary)
                        }
                    }
                }

                Section(header: Text(Localized.information)) {
                    VStack(alignment: .leading, spacing: 8) {
                        Text(Localized.howToGetInfo)
                            .font(.subheadline)
                            .fontWeight(.medium)

                        Text(Localized.strings.supabaseStep1)
                        Text(Localized.strings.supabaseStep2)
                        Text(Localized.strings.supabaseStep3)
                    }
                    .font(.caption)
                    .foregroundColor(.secondary)
                }

                Section(header: Text(Localized.currentStatus)) {
                    LabeledContentCompat {
                        Text(Localized.configured)
                    } content: {
                        Text(supabase.isConfigured ? Localized.yes : Localized.no)
                            .foregroundColor(supabase.isConfigured ? .green : .red)
                    }
                    LabeledContentCompat {
                        Text(Localized.connection)
                    } content: {
                        HStack {
                            Circle()
                                .fill(syncStatus.isOnline ? Color.green : Color.orange)
                                .frame(width: 8, height: 8)
                            Text(syncStatus.isOnline ? Localized.online : Localized.offline)
                                .foregroundColor(syncStatus.isOnline ? .green : .orange)
                        }
                    }
                    if syncStatus.pendingCount > 0 {
                        LabeledContentCompat {
                            Text(Localized.pending)
                        } content: {
                            Text(Localized.format(Localized.pendingChanges, "count", syncStatus.pendingCount))
                                .foregroundColor(.orange)
                        }
                    }
                }

                Section {
                    Button(action: testConnection) {
                        HStack {
                            Spacer()
                            Image(systemName: "network")
                            Text(Localized.testConnection)
                            Spacer()
                        }
                    }
                    .disabled(!supabase.isConfigured)
                }

                Section {
                    Button(role: .destructive, action: clearConfig) {
                        HStack {
                            Spacer()
                            Text(Localized.clearConfiguration)
                            Spacer()
                        }
                    }
                }
            }
            .navigationTitle(Localized.supabaseConfiguration)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(Localized.close) { dismiss() }
                }
            }
            .onAppear {
                loadConfig()
            }
        }
    }

    private func loadConfig() {
        if let config = supabase.getStoredConfig() {
            projectUrl = config.url
            // Don't load the actual key - just mark that we have one
            hasExistingKey = true
            anonKey = ""
        } else {
            projectUrl = ""
            anonKey = ""
            hasExistingKey = false
        }
    }

    private func saveConfig() {
        isSaving = true
        errorMessage = nil
        showSuccess = false

        // Validate URL format
        guard let url = URL(string: projectUrl), url.scheme == "https" else {
            errorMessage = "URL must be a valid HTTPS URL"
            isSaving = false
            return
        }

        // Determine the key to use
        let keyToSave: String
        if !anonKey.isEmpty {
            // User entered a new key
            keyToSave = anonKey
        } else if hasExistingKey, let existingConfig = supabase.getStoredConfig() {
            // Use existing key
            keyToSave = existingConfig.anonKey
        } else {
            errorMessage = "Anon key is required"
            isSaving = false
            return
        }

        // Validate API key format (Supabase anon keys are JWTs starting with "eyJ")
        if !keyToSave.hasPrefix("eyJ") || !keyToSave.contains(".") || keyToSave.count < 100 {
            errorMessage = "Invalid API key. The key must be a JWT (starts with 'eyJ...')"
            isSaving = false
            return
        }

        // Save configuration
        supabase.configure(url: projectUrl, anonKey: keyToSave)

        // Remove local admin users (with LOCAL_SYSTEM_MARKER) since we now have Supabase
        // This prevents UUID conflicts with remote users
        Task {
            do {
                let removed = try await sdk.defaultAdminService.forceRemoveLocalAdmin()
                if removed.boolValue {
                    debugLog("SupabaseConfig", "Local admin removed after Supabase config")
                }
            } catch {
                debugLog("SupabaseConfig", "Failed to remove local admin: \(error)")
            }
        }

        // Update state
        hasExistingKey = true
        anonKey = ""

        // Start sync scheduler after configuration
        SyncScheduler.shared.start(sdk: sdk)

        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            isSaving = false
            showSuccess = true
        }
    }

    private func clearConfig() {
        // Stop sync services
        SyncScheduler.shared.stop()
        RealtimeSyncService.shared.stop()

        // Clear stored configuration via SupabaseService
        supabase.disconnect()

        projectUrl = ""
        anonKey = ""
        hasExistingKey = false
        showSuccess = false
        errorMessage = nil
        syncMessage = nil
    }

    private func performSync() {
        isSyncing = true
        syncMessage = nil

        Task {
            await BidirectionalSyncManager.shared.fullSync(sdk: sdk)

            await MainActor.run {
                isSyncing = false
                if let error = syncStatus.lastSyncInfo.error {
                    syncMessage = "\(Localized.error): \(error)"
                } else {
                    syncMessage = Localized.syncCompleted
                }
            }
        }
    }

    private func testConnection() {
        errorMessage = nil
        syncMessage = nil

        Task {
            do {
                // Try to fetch a small amount of data to test connection
                let _: [SiteDTO] = try await supabase.fetchAll(from: "sites")
                await MainActor.run {
                    syncMessage = Localized.connectionSuccessful
                }
            } catch {
                await MainActor.run {
                    errorMessage = "\(Localized.connectionError): \(error.localizedDescription)"
                }
            }
        }
    }
}
