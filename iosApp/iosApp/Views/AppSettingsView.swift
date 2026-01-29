import SwiftUI
import shared

// MARK: - App Settings View
struct AppSettingsView: View {
    let sdk: MedistockSDK
    @ObservedObject var session: SessionManager
    @State private var currencySymbol: String = ""
    @State private var isLoading = true
    @State private var isSaving = false
    @State private var showSuccessToast = false
    @State private var errorMessage: String?
    @State private var isDebugMode: Bool = DebugConfig.shared.isDebugEnabled

    var body: some View {
        Form {
            Section(header: Text(Localized.strings.currencySymbolSetting)) {
                VStack(alignment: .leading, spacing: 8) {
                    Text(Localized.strings.currencySymbolDescription)
                        .font(.caption)
                        .foregroundColor(.secondary)

                    TextField("F", text: $currencySymbol)
                        .textFieldStyle(.roundedBorder)
                        .autocapitalization(.none)
                        .disableAutocorrection(true)
                }
            }

            Section(header: Text(Localized.strings.debugMode)) {
                VStack(alignment: .leading, spacing: 8) {
                    Toggle(Localized.strings.debugMode, isOn: $isDebugMode)
                        .onChange(of: isDebugMode) { newValue in
                            if newValue {
                                DebugConfig.shared.enableDebug()
                            } else {
                                DebugConfig.shared.disableDebug()
                            }
                        }
                    Text(Localized.strings.debugModeDescription)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }

            Section {
                Button(action: saveSettings) {
                    HStack {
                        Spacer()
                        if isSaving {
                            ProgressView()
                                .padding(.trailing, 8)
                        }
                        Text(Localized.save)
                            .fontWeight(.semibold)
                        Spacer()
                    }
                }
                .disabled(isSaving || currencySymbol.isEmpty || currencySymbol.count > 5)
            }

            if let error = errorMessage {
                Section {
                    Text(error)
                        .foregroundColor(.red)
                        .font(.caption)
                }
            }
        }
        .navigationTitle(Localized.strings.appSettings)
        .overlay {
            if isLoading {
                ProgressView()
            }
        }
        .overlay {
            if showSuccessToast {
                VStack {
                    Spacer()
                    Text(Localized.strings.settingsSavedSuccessfully)
                        .padding()
                        .background(Color.green.opacity(0.9))
                        .foregroundColor(.white)
                        .cornerRadius(10)
                        .padding()
                }
                .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .task {
            await loadSettings()
        }
    }

    @MainActor
    private func loadSettings() async {
        isLoading = true
        errorMessage = nil

        do {
            // Load from local DB first
            let localSymbol = try await sdk.appConfigRepository.getCurrencySymbol()
            currencySymbol = localSymbol

            // Try to sync from Supabase if configured
            if SupabaseService.shared.isConfigured {
                do {
                    let configs: [AppConfigDTO] = try await SupabaseService.shared.fetch(
                        from: "app_config",
                        filter: ["key": "eq.currency_symbol"]
                    )
                    if let remoteConfig = configs.first, let remoteValue = remoteConfig.value {
                        currencySymbol = remoteValue
                        // Update local cache
                        try await sdk.appConfigRepository.setCurrencySymbol(
                            symbol: remoteValue,
                            updatedBy: session.userId ?? "system"
                        )
                    }
                } catch {
                    debugLog("AppSettingsView", "Failed to fetch config from Supabase: \(error)")
                }
            }
        } catch {
            errorMessage = "\(Localized.error): \(error.localizedDescription)"
        }

        isLoading = false
    }

    @MainActor
    private func saveSettings() {
        guard !currencySymbol.isEmpty && currencySymbol.count <= 5 else {
            errorMessage = Localized.strings.invalidCurrencySymbol
            return
        }

        isSaving = true
        errorMessage = nil

        Task {
            do {
                let userId = session.userId ?? "system"

                // Save to local DB
                try await sdk.appConfigRepository.setCurrencySymbol(
                    symbol: currencySymbol,
                    updatedBy: userId
                )

                // Sync to Supabase if configured
                if SupabaseService.shared.isConfigured {
                    do {
                        let now = Int64(Date().timeIntervalSince1970 * 1000)
                        let configDto = AppConfigDTO(
                            key: "currency_symbol",
                            value: currencySymbol,
                            description: "Currency symbol for prices display",
                            updatedAt: now,
                            updatedBy: userId
                        )
                        try await SupabaseService.shared.upsert(into: "app_config", record: configDto)
                    } catch {
                        debugLog("AppSettingsView", "Failed to sync config to Supabase: \(error)")
                        // Don't fail - local save succeeded
                    }
                }

                // Show success toast
                withAnimation {
                    showSuccessToast = true
                }

                // Hide toast after 2 seconds
                try? await Task.sleep(nanoseconds: 2_000_000_000)
                withAnimation {
                    showSuccessToast = false
                }

            } catch {
                errorMessage = "\(Localized.error): \(error.localizedDescription)"
            }

            isSaving = false
        }
    }
}

// MARK: - App Config DTO
struct AppConfigDTO: Codable {
    let key: String
    let value: String?
    let description: String?
    let updatedAt: Int64
    let updatedBy: String?

    enum CodingKeys: String, CodingKey {
        case key
        case value
        case description
        case updatedAt = "updated_at"
        case updatedBy = "updated_by"
    }
}
