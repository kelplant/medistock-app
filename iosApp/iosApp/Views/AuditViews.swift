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
        ("all", "All"),
        ("products", "Products"),
        ("sales", "Sales"),
        ("purchase_batches", "Purchases"),
        ("sites", "Sites"),
        ("app_users", "Users")
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
                Picker("Filter by", selection: $selectedFilter) {
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
                        title: "No history",
                        message: "Change history will appear here."
                    )
                }
                .listRowSeparator(.hidden)
            } else {
                Section(header: Text("\(filteredEntries.count) entry(ies)")) {
                    ForEach(filteredEntries, id: \.id) { entry in
                        AuditEntryRowView(entry: entry)
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle("Audit History")
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
            errorMessage = "Error: \(error.localizedDescription)"
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
        case "insert", "create": return "Created"
        case "update": return "Updated"
        case "delete": return "Deleted"
        default: return entry.action
        }
    }

    var tableLabel: String {
        switch entry.tableName {
        case "products": return "Product"
        case "sites": return "Site"
        case "categories": return "Category"
        case "sales": return "Sale"
        case "sale_items": return "Sale item"
        case "purchase_batches": return "Purchase batch"
        case "app_users": return "User"
        case "customers": return "Customer"
        case "product_transfers": return "Transfer"
        case "stock_movements": return "Stock movement"
        case "inventories": return "Inventory"
        case "packaging_types": return "Packaging type"
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

            Text("By: \(entry.userId)")
                .font(.caption)
                .foregroundColor(.secondary)

            if let newValues = entry.newValues, !newValues.isEmpty {
                DisclosureGroup("Details") {
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
