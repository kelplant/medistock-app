import Foundation
import SwiftUI
import shared

// MARK: - Audit History List View
struct AuditHistoryListView: View {
    let sdk: MedistockSDK

    @State private var auditEntries: [AuditEntry] = []
    @State private var isLoading = true
    @State private var errorMessage: String?
    @State private var selectedFilter: String = "all"

    let tableFilters = [
        ("all", "Tout"),
        ("products", "Produits"),
        ("sales", "Ventes"),
        ("purchase_batches", "Achats"),
        ("sites", "Sites"),
        ("app_users", "Utilisateurs")
    ]

    var filteredEntries: [AuditEntry] {
        if selectedFilter == "all" {
            return auditEntries
        }
        return auditEntries.filter { $0.tableName == selectedFilter }
    }

    var body: some View {
        List {
            if let errorMessage {
                Section {
                    Text(errorMessage)
                        .foregroundColor(.red)
                }
            }

            // Filter
            Section {
                Picker("Filtrer par", selection: $selectedFilter) {
                    ForEach(tableFilters, id: \.0) { filter in
                        Text(filter.1).tag(filter.0)
                    }
                }
                .pickerStyle(.menu)
            }

            if isLoading {
                Section {
                    HStack {
                        Spacer()
                        ProgressView()
                        Spacer()
                    }
                }
            } else if auditEntries.isEmpty {
                Section {
                    EmptyStateView(
                        icon: "clock.arrow.circlepath",
                        title: "Aucun historique",
                        message: "L'historique des modifications apparaîtra ici."
                    )
                }
                .listRowSeparator(.hidden)
            } else {
                Section(header: Text("\(filteredEntries.count) entrée(s)")) {
                    ForEach(filteredEntries, id: \.id) { entry in
                        AuditEntryRowView(entry: entry)
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle("Historique des audits")
        .refreshable {
            await loadData()
        }
        .task {
            await loadData()
        }
    }

    @MainActor
    private func loadData() async {
        isLoading = true
        errorMessage = nil
        do {
            auditEntries = try await sdk.auditRepository.getAll(limit: 200)
        } catch {
            errorMessage = "Erreur: \(error.localizedDescription)"
        }
        isLoading = false
    }
}

// MARK: - Audit Entry Row View
struct AuditEntryRowView: View {
    let entry: AuditEntry

    var actionColor: Color {
        switch entry.action.lowercased() {
        case "insert", "create": return .green
        case "update": return .orange
        case "delete": return .red
        default: return .gray
        }
    }

    var actionLabel: String {
        switch entry.action.lowercased() {
        case "insert", "create": return "Création"
        case "update": return "Modification"
        case "delete": return "Suppression"
        default: return entry.action
        }
    }

    var tableLabel: String {
        switch entry.tableName {
        case "products": return "Produit"
        case "sites": return "Site"
        case "categories": return "Catégorie"
        case "sales": return "Vente"
        case "sale_items": return "Article de vente"
        case "purchase_batches": return "Lot d'achat"
        case "app_users": return "Utilisateur"
        case "customers": return "Client"
        case "product_transfers": return "Transfert"
        case "stock_movements": return "Mouvement de stock"
        case "inventories": return "Inventaire"
        case "packaging_types": return "Conditionnement"
        default: return entry.tableName
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(actionLabel)
                    .font(.caption)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 2)
                    .background(actionColor.opacity(0.2))
                    .foregroundColor(actionColor)
                    .cornerRadius(4)

                Text(tableLabel)
                    .font(.headline)

                Spacer()

                Text(formatDate(entry.timestamp))
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            Text("ID: \(entry.recordId)")
                .font(.caption)
                .foregroundColor(.secondary)
                .lineLimit(1)

            Text("Par: \(entry.userId)")
                .font(.caption)
                .foregroundColor(.secondary)

            if let newValues = entry.newValues, !newValues.isEmpty {
                DisclosureGroup("Détails") {
                    Text(newValues)
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
                .font(.caption)
            }
        }
        .padding(.vertical, 4)
    }

    private func formatDate<T: TimestampConvertible>(_ timestamp: T) -> String {
        let date = Date(timeIntervalSince1970: Double(timestamp.timestampValue) / 1000)
        let formatter = DateFormatter()
        formatter.dateStyle = .short
        formatter.timeStyle = .short
        return formatter.string(from: date)
    }
}

extension AuditEntry: @retroactive Identifiable {}
