import Foundation
import SwiftUI
import shared

// MARK: - Supabase Config View
struct SupabaseConfigView: View {
    let sdk: MedistockSDK

    @Environment(\.dismiss) private var dismiss
    @State private var projectUrl: String = ""
    @State private var anonKey: String = ""
    @State private var isSaving = false
    @State private var isSyncing = false
    @State private var showSuccess = false
    @State private var errorMessage: String?
    @State private var syncMessage: String?

    private let supabase = SupabaseClient.shared

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
                        SecureField("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", text: $anonKey)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
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
                    .disabled(projectUrl.isEmpty || anonKey.isEmpty || isSaving)
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

                    if let lastSync = SyncService.shared.lastSyncDate {
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
                        Text("URL configurée")
                    } content: {
                        Text(supabase.supabaseUrl?.isEmpty == false ? "Oui" : "Non")
                            .foregroundColor(supabase.supabaseUrl?.isEmpty == false ? .green : .red)
                    }
                    LabeledContentCompat {
                        Text("Clé configurée")
                    } content: {
                        Text(supabase.supabaseKey?.isEmpty == false ? "Oui" : "Non")
                            .foregroundColor(supabase.supabaseKey?.isEmpty == false ? .green : .red)
                    }
                    LabeledContentCompat {
                        Text("Statut")
                    } content: {
                        HStack {
                            Circle()
                                .fill(supabase.isConfigured ? Color.green : Color.orange)
                                .frame(width: 8, height: 8)
                            Text(supabase.isConfigured ? "Connecté" : "Non configuré")
                                .foregroundColor(supabase.isConfigured ? .green : .orange)
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
        projectUrl = supabase.supabaseUrl ?? ""
        anonKey = supabase.supabaseKey ?? ""
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

        // Save configuration
        supabase.configure(url: projectUrl, key: anonKey)

        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            isSaving = false
            showSuccess = true
        }
    }

    private func clearConfig() {
        supabase.clearConfiguration()
        projectUrl = ""
        anonKey = ""
        showSuccess = false
        errorMessage = nil
        syncMessage = nil
    }

    private func performSync() {
        isSyncing = true
        syncMessage = nil

        Task {
            await SyncService.shared.performFullSync(sdk: sdk)

            await MainActor.run {
                isSyncing = false
                if let error = SyncService.shared.lastError {
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
                let _: [RemoteSite] = try await supabase.fetchAll(from: "sites", query: ["limit": "1"])
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
