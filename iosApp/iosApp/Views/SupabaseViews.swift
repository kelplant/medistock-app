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
    @State private var showSuccess = false
    @State private var errorMessage: String?

    private let urlKey = "medistock_supabase_url"
    private let keyKey = "medistock_supabase_key"

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
                    LabeledContent("URL configurée") {
                        Text(UserDefaults.standard.string(forKey: urlKey)?.isEmpty == false ? "Oui" : "Non")
                            .foregroundColor(UserDefaults.standard.string(forKey: urlKey)?.isEmpty == false ? .green : .red)
                    }
                    LabeledContent("Clé configurée") {
                        Text(UserDefaults.standard.string(forKey: keyKey)?.isEmpty == false ? "Oui" : "Non")
                            .foregroundColor(UserDefaults.standard.string(forKey: keyKey)?.isEmpty == false ? .green : .red)
                    }
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
        projectUrl = UserDefaults.standard.string(forKey: urlKey) ?? ""
        anonKey = UserDefaults.standard.string(forKey: keyKey) ?? ""
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

        // Save to UserDefaults
        UserDefaults.standard.set(projectUrl, forKey: urlKey)
        UserDefaults.standard.set(anonKey, forKey: keyKey)

        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            isSaving = false
            showSuccess = true
        }
    }

    private func clearConfig() {
        UserDefaults.standard.removeObject(forKey: urlKey)
        UserDefaults.standard.removeObject(forKey: keyKey)
        projectUrl = ""
        anonKey = ""
        showSuccess = false
        errorMessage = nil
    }
}
