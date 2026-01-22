import Foundation
import SwiftUI
import shared

// MARK: - Inventory List View
struct InventoryListView: View {
    let sdk: MedistockSDK
    @ObservedObject var session: SessionManager
    let siteId: String?

    @State private var inventories: [Inventory] = []
    @State private var sites: [Site] = []
    @State private var isLoading = true
    @State private var errorMessage: String?
    @State private var showAddSheet = false

    var body: some View {
        List {
            if let errorMessage {
                Section {
                    Text(errorMessage)
                        .foregroundColor(.red)
                }
            }

            if isLoading {
                Section {
                    HStack {
                        Spacer()
                        ProgressView()
                        Spacer()
                    }
                }
            } else if inventories.isEmpty {
                Section {
                    EmptyStateView(
                        icon: "list.clipboard",
                        title: "No inventories",
                        message: "Start an inventory to count your physical stock."
                    )
                }
                .listRowSeparator(.hidden)
            } else {
                // In progress
                let inProgress = inventories.filter { $0.status == "in_progress" }
                if !inProgress.isEmpty {
                    Section(header: Text("In progress (\(inProgress.count))")) {
                        ForEach(inProgress, id: \.id) { inventory in
                            InventoryRowView(
                                inventory: inventory,
                                siteName: siteName(for: inventory.siteId),
                                onComplete: { completeInventory(inventory) }
                            )
                        }
                    }
                }

                // Completed
                let completed = inventories.filter { $0.status == "completed" }
                if !completed.isEmpty {
                    Section(header: Text("Completed (\(completed.count))")) {
                        ForEach(completed, id: \.id) { inventory in
                            InventoryRowView(
                                inventory: inventory,
                                siteName: siteName(for: inventory.siteId),
                                onComplete: nil
                            )
                        }
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle("Inventories")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { showAddSheet = true }) {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $showAddSheet) {
            InventoryEditorView(sdk: sdk, session: session, sites: sites, defaultSiteId: siteId) {
                Task { await loadData() }
            }
        }
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
            async let inventoriesResult = sdk.inventoryRepository.getAll()
            async let sitesResult = sdk.siteRepository.getAll()

            inventories = try await inventoriesResult
            sites = try await sitesResult

            if let siteId = siteId {
                inventories = inventories.filter { $0.siteId == siteId }
            }
        } catch {
            errorMessage = "Error: \(error.localizedDescription)"
        }
        isLoading = false
    }

    private func siteName(for siteId: String) -> String {
        sites.first { $0.id == siteId }?.name ?? "Unknown site"
    }

    private func completeInventory(_ inventory: Inventory) {
        Task {
            do {
                let completedAt = KotlinLong(value: Int64(Date().timeIntervalSince1970 * 1000))
                try await sdk.inventoryRepository.updateStatus(
                    id: inventory.id,
                    status: "completed",
                    completedAt: completedAt
                )
                await loadData()
            } catch {
                await MainActor.run {
                    errorMessage = "Error: \(error.localizedDescription)"
                }
            }
        }
    }
}

// MARK: - Inventory Row View
struct InventoryRowView: View {
    let inventory: Inventory
    let siteName: String
    let onComplete: (() -> Void)?

    var statusColor: Color {
        inventory.status == "completed" ? .green : .orange
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text("Inventory - \(siteName)")
                    .font(.headline)
                Spacer()
                Text(inventory.status == "completed" ? "Completed" : "In progress")
                    .font(.caption)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 2)
                    .background(statusColor.opacity(0.2))
                    .foregroundColor(statusColor)
                    .cornerRadius(4)
            }

            HStack {
                Text("Started: \(formatDate(inventory.startedAt))")
                if let completed = inventory.completedAt {
                    Text("- Completed: \(formatDate(completed))")
                }
            }
            .font(.caption)
            .foregroundColor(.secondary)

            if let notes = inventory.notes, !notes.isEmpty {
                Text(notes)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            if let onComplete = onComplete, inventory.status == "in_progress" {
                Button(action: onComplete) {
                    Text("Complete inventory")
                        .font(.subheadline)
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .padding(.top, 4)
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

// MARK: - Inventory Editor View
struct InventoryEditorView: View {
    let sdk: MedistockSDK
    @ObservedObject var session: SessionManager
    let sites: [Site]
    let defaultSiteId: String?
    let onSave: () -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var selectedSiteId: String = ""
    @State private var notes: String = ""
    @State private var isSaving = false
    @State private var errorMessage: String?

    @ViewBuilder
    private var notesField: some View {
        if #available(iOS 16.0, *) {
            TextField("Notes", text: $notes, axis: .vertical)
                .lineLimit(3...6)
        } else {
            TextField("Notes", text: $notes)
        }
    }

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Site")) {
                    Picker("Site", selection: $selectedSiteId) {
                        Text("Select").tag("")
                        ForEach(sites, id: \.id) { site in
                            Text(site.name).tag(site.id)
                        }
                    }
                }

                Section(header: Text("Notes (optional)")) {
                    notesField
                }

                if let errorMessage {
                    Section {
                        Text(errorMessage)
                            .foregroundColor(.red)
                    }
                }
            }
            .navigationTitle("New Inventory")
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Start") {
                        saveInventory()
                    }
                    .disabled(selectedSiteId.isEmpty || isSaving)
                }
            }
            .onAppear {
                selectedSiteId = defaultSiteId ?? sites.first?.id ?? ""
            }
        }
    }

    private func saveInventory() {
        isSaving = true
        errorMessage = nil

        Task {
            do {
                let inventory = sdk.createInventory(
                    siteId: selectedSiteId,
                    notes: notes.isEmpty ? nil : notes,
                    userId: session.username
                )
                try await sdk.inventoryRepository.insert(inventory: inventory)

                await MainActor.run {
                    onSave()
                    dismiss()
                }
            } catch {
                await MainActor.run {
                    isSaving = false
                    errorMessage = "Error: \(error.localizedDescription)"
                }
            }
        }
    }
}

extension Inventory: @retroactive Identifiable {}
