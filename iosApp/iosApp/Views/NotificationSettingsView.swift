import Foundation
import SwiftUI
import shared

// MARK: - Notification Settings View
struct NotificationSettingsView: View {
    let sdk: MedistockSDK
    @ObservedObject var session: SessionManager

    @State private var isLoading = true
    @State private var isSaving = false
    @State private var errorMessage: String?
    @State private var showSuccessAlert = false

    // Settings state
    @State private var expiryAlertEnabled = true
    @State private var expiryWarningDays = "7"
    @State private var lowStockAlertEnabled = true

    var body: some View {
        Form {
            // Expiry Alerts Section
            Section(header: Text(Localized.notificationExpiryAlerts)) {
                Toggle(Localized.notificationEnableExpiry, isOn: $expiryAlertEnabled)

                HStack {
                    Text(Localized.notificationWarningDays)
                    Spacer()
                    TextField("7", text: $expiryWarningDays)
                        .keyboardType(.numberPad)
                        .multilineTextAlignment(.trailing)
                        .frame(width: 60)
                }

                Text(Localized.notificationExpiryDescription)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            // Low Stock Alerts Section
            Section(header: Text(Localized.notificationLowStockAlerts)) {
                Toggle(Localized.notificationEnableLowStock, isOn: $lowStockAlertEnabled)

                Text(Localized.notificationLowStockDescription)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            // Error message
            if let errorMessage = errorMessage {
                Section {
                    Text(errorMessage)
                        .foregroundColor(.red)
                }
            }

            // Save button
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
                .disabled(!canSave || isSaving)
            }
        }
        .navigationTitle(Localized.notificationSettings)
        .task {
            await loadSettings()
        }
        .alert(Localized.success, isPresented: $showSuccessAlert) {
            Button(Localized.ok, role: .cancel) {}
        } message: {
            Text(Localized.settingsSaved)
        }
    }

    private var canSave: Bool {
        guard let days = Int(expiryWarningDays), days >= 1, days <= 365 else { return false }
        return SupabaseService.shared.isConfigured
    }

    @MainActor
    private func loadSettings() async {
        isLoading = true
        errorMessage = nil

        guard SupabaseService.shared.isConfigured else {
            errorMessage = Localized.supabaseNotConfigured
            isLoading = false
            return
        }

        do {
            guard let query = SupabaseService.shared.from("notification_settings") else {
                errorMessage = Localized.supabaseNotConfigured
                isLoading = false
                return
            }

            let settings: NotificationSettingsDto? = try await query
                .select()
                .single()
                .execute()
                .value

            if let settings = settings {
                expiryAlertEnabled = settings.expiry_alert_enabled == 1
                expiryWarningDays = String(settings.expiry_warning_days)
                lowStockAlertEnabled = settings.low_stock_alert_enabled == 1
            }
        } catch {
            debugLog("NotificationSettingsView", "Error loading settings: \(error)")
            // Use defaults, no error to show
        }

        isLoading = false
    }

    @MainActor
    private func saveSettings() {
        guard let days = Int(expiryWarningDays), days >= 1, days <= 365 else {
            errorMessage = Localized.notificationInvalidDays
            return
        }

        isSaving = true
        errorMessage = nil

        Task {
            do {
                let settings = NotificationSettingsDto(
                    id: "global",
                    expiry_alert_enabled: expiryAlertEnabled ? 1 : 0,
                    expiry_warning_days: days,
                    expiry_dedup_days: 3,
                    expired_dedup_days: 7,
                    low_stock_alert_enabled: lowStockAlertEnabled ? 1 : 0,
                    low_stock_dedup_days: 7,
                    updated_at: Int64(Date().timeIntervalSince1970 * 1000),
                    updated_by: session.userId
                )

                guard let query = SupabaseService.shared.from("notification_settings") else {
                    errorMessage = Localized.supabaseNotConfigured
                    isSaving = false
                    return
                }

                try await query
                    .upsert(settings)
                    .execute()

                showSuccessAlert = true
            } catch {
                errorMessage = "\(Localized.error): \(error.localizedDescription)"
            }

            isSaving = false
        }
    }
}

// MARK: - DTO for iOS
struct NotificationSettingsDto: Codable {
    let id: String
    let expiry_alert_enabled: Int
    let expiry_warning_days: Int
    let expiry_dedup_days: Int
    let expired_dedup_days: Int
    let low_stock_alert_enabled: Int
    let low_stock_dedup_days: Int
    let updated_at: Int64
    let updated_by: String?

    init(
        id: String = "global",
        expiry_alert_enabled: Int = 1,
        expiry_warning_days: Int = 7,
        expiry_dedup_days: Int = 3,
        expired_dedup_days: Int = 7,
        low_stock_alert_enabled: Int = 1,
        low_stock_dedup_days: Int = 7,
        updated_at: Int64 = 0,
        updated_by: String? = nil
    ) {
        self.id = id
        self.expiry_alert_enabled = expiry_alert_enabled
        self.expiry_warning_days = expiry_warning_days
        self.expiry_dedup_days = expiry_dedup_days
        self.expired_dedup_days = expired_dedup_days
        self.low_stock_alert_enabled = low_stock_alert_enabled
        self.low_stock_dedup_days = low_stock_dedup_days
        self.updated_at = updated_at
        self.updated_by = updated_by
    }
}
