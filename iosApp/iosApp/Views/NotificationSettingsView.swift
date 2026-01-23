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

    // Message templates
    @State private var templateExpiredTitle = "Produit expiré"
    @State private var templateExpiredMessage = "{{product_name}} a expiré"
    @State private var templateExpiringTitle = "Expiration proche"
    @State private var templateExpiringMessage = "{{product_name}} expire dans {{days_until}} jour(s)"
    @State private var templateLowStockTitle = "Stock faible"
    @State private var templateLowStockMessage = "{{product_name}}: {{current_stock}}/{{min_stock}}"

    var body: some View {
        Form {
            // Expiry Alerts Section
            Section(header: Text("Expiry Alerts")) {
                Toggle("Enable expiry alerts", isOn: $expiryAlertEnabled)

                HStack {
                    Text("Warning days before expiry")
                    Spacer()
                    TextField("7", text: $expiryWarningDays)
                        .keyboardType(.numberPad)
                        .multilineTextAlignment(.trailing)
                        .frame(width: 60)
                }

                Text("Products expiring within this many days will trigger a warning notification.")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            // Low Stock Alerts Section
            Section(header: Text("Low Stock Alerts")) {
                Toggle("Enable low stock alerts", isOn: $lowStockAlertEnabled)

                Text("Stock thresholds are configured per product in the product settings.")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            // Message Templates Section
            Section(header: Text("Message Templates")) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Available variables:")
                        .font(.caption)
                        .fontWeight(.medium)
                    Text("{{product_name}}, {{days_until}}, {{current_stock}}, {{min_stock}}")
                        .font(.caption)
                        .foregroundColor(.blue)
                }
                .padding(.vertical, 4)
            }

            // Expired Product Template
            Section(header: Text("Expired Product")) {
                TextField("Title", text: $templateExpiredTitle)
                TextField("Message", text: $templateExpiredMessage)
            }

            // Expiring Soon Template
            Section(header: Text("Expiring Soon")) {
                TextField("Title", text: $templateExpiringTitle)
                TextField("Message", text: $templateExpiringMessage)
            }

            // Low Stock Template
            Section(header: Text("Low Stock")) {
                TextField("Title", text: $templateLowStockTitle)
                TextField("Message", text: $templateLowStockMessage)
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
                        Text("Save Settings")
                            .fontWeight(.semibold)
                        Spacer()
                    }
                }
                .disabled(!canSave || isSaving)
            }
        }
        .navigationTitle("Notification Settings")
        .task {
            await loadSettings()
        }
        .alert("Success", isPresented: $showSuccessAlert) {
            Button("OK", role: .cancel) {}
        } message: {
            Text("Settings saved successfully.")
        }
    }

    private var canSave: Bool {
        guard let days = Int(expiryWarningDays), days >= 1, days <= 365 else { return false }
        // Validate template lengths
        let maxLength = 500
        let templates = [
            templateExpiredTitle, templateExpiredMessage,
            templateExpiringTitle, templateExpiringMessage,
            templateLowStockTitle, templateLowStockMessage
        ]
        guard templates.allSatisfy({ $0.count <= maxLength }) else { return false }
        return SupabaseService.shared.isConfigured
    }

    @MainActor
    private func loadSettings() async {
        isLoading = true
        errorMessage = nil

        guard SupabaseService.shared.isConfigured else {
            errorMessage = "Supabase not configured. Settings are stored on the server."
            isLoading = false
            return
        }

        do {
            let settings: NotificationSettingsDto? = try await SupabaseService.shared.client
                .from("notification_settings")
                .select()
                .single()
                .execute()
                .value

            if let settings = settings {
                expiryAlertEnabled = settings.expiry_alert_enabled == 1
                expiryWarningDays = String(settings.expiry_warning_days)
                lowStockAlertEnabled = settings.low_stock_alert_enabled == 1
                templateExpiredTitle = settings.template_expired_title.isEmpty ? "Produit expiré" : settings.template_expired_title
                templateExpiredMessage = settings.template_expired_message.isEmpty ? "{{product_name}} a expiré" : settings.template_expired_message
                templateExpiringTitle = settings.template_expiring_title
                templateExpiringMessage = settings.template_expiring_message
                templateLowStockTitle = settings.template_low_stock_title
                templateLowStockMessage = settings.template_low_stock_message
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
            errorMessage = "Please enter a valid number of warning days (1-365)"
            return
        }

        // Validate template lengths
        let maxLength = 500
        let templates = [
            templateExpiredTitle, templateExpiredMessage,
            templateExpiringTitle, templateExpiringMessage,
            templateLowStockTitle, templateLowStockMessage
        ]
        if !templates.allSatisfy({ $0.count <= maxLength }) {
            errorMessage = "Template messages must be less than \(maxLength) characters"
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
                    template_expired_title: templateExpiredTitle.isEmpty ? "Produit expiré" : templateExpiredTitle,
                    template_expired_message: templateExpiredMessage.isEmpty ? "{{product_name}} a expiré" : templateExpiredMessage,
                    template_expiring_title: templateExpiringTitle.isEmpty ? "Expiration proche" : templateExpiringTitle,
                    template_expiring_message: templateExpiringMessage.isEmpty ? "{{product_name}} expire dans {{days_until}} jour(s)" : templateExpiringMessage,
                    template_low_stock_title: templateLowStockTitle.isEmpty ? "Stock faible" : templateLowStockTitle,
                    template_low_stock_message: templateLowStockMessage.isEmpty ? "{{product_name}}: {{current_stock}}/{{min_stock}}" : templateLowStockMessage,
                    updated_at: Int64(Date().timeIntervalSince1970 * 1000),
                    updated_by: session.userId
                )

                try await SupabaseService.shared.client
                    .from("notification_settings")
                    .upsert(settings)
                    .execute()

                showSuccessAlert = true
            } catch {
                errorMessage = "Error saving settings: \(error.localizedDescription)"
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
    let template_expired_title: String
    let template_expired_message: String
    let template_expiring_title: String
    let template_expiring_message: String
    let template_low_stock_title: String
    let template_low_stock_message: String
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
        template_expired_title: String = "Produit expiré",
        template_expired_message: String = "{{product_name}} a expiré",
        template_expiring_title: String = "Expiration proche",
        template_expiring_message: String = "{{product_name}} expire dans {{days_until}} jour(s)",
        template_low_stock_title: String = "Stock faible",
        template_low_stock_message: String = "{{product_name}}: {{current_stock}}/{{min_stock}}",
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
        self.template_expired_title = template_expired_title
        self.template_expired_message = template_expired_message
        self.template_expiring_title = template_expiring_title
        self.template_expiring_message = template_expiring_message
        self.template_low_stock_title = template_low_stock_title
        self.template_low_stock_message = template_low_stock_message
        self.updated_at = updated_at
        self.updated_by = updated_by
    }
}
