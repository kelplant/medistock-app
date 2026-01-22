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
                Section(header: Text("Supabase Configuration")) {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Project URL")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        TextField("https://xxx.supabase.co", text: $projectUrl)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                            .keyboardType(.URL)
                    }

                    VStack(alignment: .leading, spacing: 8) {
                        Text("Anon Key")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        if hasExistingKey && anonKey.isEmpty {
                            HStack {
                                Text(maskedKey)
                                    .foregroundColor(.secondary)
                                Spacer()
                                Button("Edit") {
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
                                Text("Save")
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
                            Text("Configuration saved successfully")
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

                Section(header: Text("Synchronization")) {
                    Button(action: performSync) {
                        HStack {
                            Spacer()
                            if isSyncing {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle())
                                Text("Syncing...")
                            } else {
                                Image(systemName: "arrow.triangle.2.circlepath")
                                Text("Sync data")
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
                            Text("Last sync")
                        } content: {
                            Text(lastSync, style: .relative)
                                .foregroundColor(.secondary)
                        }
                    }
                }

                Section(header: Text("Information")) {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("How to get this information:")
                            .font(.subheadline)
                            .fontWeight(.medium)

                        Text("1. Log in to your Supabase project")
                        Text("2. Go to Settings > API")
                        Text("3. Copy the project URL and anon key")
                    }
                    .font(.caption)
                    .foregroundColor(.secondary)
                }

                Section(header: Text("Current Status")) {
                    LabeledContentCompat {
                        Text("Configured")
                    } content: {
                        Text(supabase.isConfigured ? "Yes" : "No")
                            .foregroundColor(supabase.isConfigured ? .green : .red)
                    }
                    LabeledContentCompat {
                        Text("Connection")
                    } content: {
                        HStack {
                            Circle()
                                .fill(syncStatus.isOnline ? Color.green : Color.orange)
                                .frame(width: 8, height: 8)
                            Text(syncStatus.isOnline ? "Online" : "Offline")
                                .foregroundColor(syncStatus.isOnline ? .green : .orange)
                        }
                    }
                    if syncStatus.pendingCount > 0 {
                        LabeledContentCompat {
                            Text("Pending")
                        } content: {
                            Text("\(syncStatus.pendingCount) change(s)")
                                .foregroundColor(.orange)
                        }
                    }
                }

                Section {
                    Button(action: testConnection) {
                        HStack {
                            Spacer()
                            Image(systemName: "network")
                            Text("Test connection")
                            Spacer()
                        }
                    }
                    .disabled(!supabase.isConfigured)
                }

                Section {
                    Button(role: .destructive, action: clearConfig) {
                        HStack {
                            Spacer()
                            Text("Clear configuration")
                            Spacer()
                        }
                    }
                }
            }
            .navigationTitle("Supabase Configuration")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Close") { dismiss() }
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

        // Save configuration
        supabase.configure(url: projectUrl, anonKey: keyToSave)

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
                    syncMessage = "Error: \(error)"
                } else {
                    syncMessage = "Sync completed successfully!"
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
                    syncMessage = "Connection successful!"
                }
            } catch {
                await MainActor.run {
                    errorMessage = "Connection error: \(error.localizedDescription)"
                }
            }
        }
    }
}
