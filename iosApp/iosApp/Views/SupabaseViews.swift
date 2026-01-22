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
                Section(header: Text("Configuration Supabase")) {
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
                                Button("Modifier") {
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
                                Text("Enregistrer")
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
                            Text("Configuration enregistrée avec succès")
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

                Section(header: Text("Synchronisation")) {
                    Button(action: performSync) {
                        HStack {
                            Spacer()
                            if isSyncing {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle())
                                Text("Synchronisation...")
                            } else {
                                Image(systemName: "arrow.triangle.2.circlepath")
                                Text("Synchroniser les données")
                            }
                            Spacer()
                        }
                    }
                    .disabled(!supabase.isConfigured || isSyncing)

                    if let syncMessage {
                        Text(syncMessage)
                            .font(.caption)
                            .foregroundColor(syncMessage.contains("Erreur") ? .red : .green)
                    }

                    if let lastSync = syncStatus.lastSyncInfo.timestamp {
                        LabeledContentCompat {
                            Text("Dernière sync")
                        } content: {
                            Text(lastSync, style: .relative)
                                .foregroundColor(.secondary)
                        }
                    }
                }

                Section(header: Text("Informations")) {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Comment obtenir ces informations:")
                            .font(.subheadline)
                            .fontWeight(.medium)

                        Text("1. Connectez-vous à votre projet Supabase")
                        Text("2. Allez dans Settings > API")
                        Text("3. Copiez l'URL du projet et la clé anon")
                    }
                    .font(.caption)
                    .foregroundColor(.secondary)
                }

                Section(header: Text("État actuel")) {
                    LabeledContentCompat {
                        Text("Configuré")
                    } content: {
                        Text(supabase.isConfigured ? "Oui" : "Non")
                            .foregroundColor(supabase.isConfigured ? .green : .red)
                    }
                    LabeledContentCompat {
                        Text("Connexion")
                    } content: {
                        HStack {
                            Circle()
                                .fill(syncStatus.isOnline ? Color.green : Color.orange)
                                .frame(width: 8, height: 8)
                            Text(syncStatus.isOnline ? "En ligne" : "Hors ligne")
                                .foregroundColor(syncStatus.isOnline ? .green : .orange)
                        }
                    }
                    if syncStatus.pendingCount > 0 {
                        LabeledContentCompat {
                            Text("En attente")
                        } content: {
                            Text("\(syncStatus.pendingCount) modification(s)")
                                .foregroundColor(.orange)
                        }
                    }
                }

                Section {
                    Button(action: testConnection) {
                        HStack {
                            Spacer()
                            Image(systemName: "network")
                            Text("Tester la connexion")
                            Spacer()
                        }
                    }
                    .disabled(!supabase.isConfigured)
                }

                Section {
                    Button(role: .destructive, action: clearConfig) {
                        HStack {
                            Spacer()
                            Text("Effacer la configuration")
                            Spacer()
                        }
                    }
                }
            }
            .navigationTitle("Configuration Supabase")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Fermer") { dismiss() }
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
            errorMessage = "L'URL doit être une URL HTTPS valide"
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
            errorMessage = "La clé Anon est requise"
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
                    syncMessage = "Erreur: \(error)"
                } else {
                    syncMessage = "Synchronisation réussie!"
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
                    syncMessage = "Connexion réussie!"
                }
            } catch {
                await MainActor.run {
                    errorMessage = "Erreur de connexion: \(error.localizedDescription)"
                }
            }
        }
    }
}
