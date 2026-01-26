import Foundation
import SwiftUI
import shared

// MARK: - Suppliers List View
struct SuppliersListView: View {
    let sdk: MedistockSDK
    @ObservedObject var session: SessionManager
    @ObservedObject private var syncStatus = SyncStatusManager.shared

    @State private var suppliers: [Supplier] = []
    @State private var isLoading = true
    @State private var errorMessage: String?
    @State private var showAddSheet = false
    @State private var supplierToEdit: Supplier?
    @State private var searchText = ""
    @State private var showCannotDeleteAlert = false
    @State private var cannotDeleteMessage = ""

    var filteredSuppliers: [Supplier] {
        if searchText.isEmpty {
            return suppliers
        }
        return suppliers.filter { $0.name.localizedCaseInsensitiveContains(searchText) }
    }

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
            } else if suppliers.isEmpty {
                Section {
                    EmptyStateView(
                        icon: "building.2",
                        title: Localized.noSuppliers,
                        message: Localized.strings.noSuppliersMessage
                    )
                }
                .listRowSeparator(.hidden)
            } else {
                Section(header: Text("\(filteredSuppliers.count) \(Localized.suppliers.lowercased())")) {
                    ForEach(filteredSuppliers, id: \.id) { supplier in
                        SupplierRowView(supplier: supplier)
                            .contentShape(Rectangle())
                            .onTapGesture {
                                supplierToEdit = supplier
                            }
                    }
                    .onDelete(perform: deleteSuppliers)
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle(Localized.suppliers)
        .searchable(text: $searchText, prompt: Localized.search)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { showAddSheet = true }) {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $showAddSheet) {
            SupplierEditorView(sdk: sdk, session: session, supplier: nil) {
                Task { await loadSuppliers() }
            }
        }
        .sheet(item: $supplierToEdit) { supplier in
            SupplierEditorView(sdk: sdk, session: session, supplier: supplier) {
                Task { await loadSuppliers() }
            }
        }
        .refreshable {
            await BidirectionalSyncManager.shared.fullSync(sdk: sdk)
            await loadSuppliers()
        }
        .task {
            await loadSuppliers()
        }
        .alert(Localized.strings.cannotDelete, isPresented: $showCannotDeleteAlert) {
            Button(Localized.strings.confirm) {
                // Deactivate will be handled in deleteSuppliers
            }
            Button(Localized.cancel, role: .cancel) {}
        } message: {
            Text(cannotDeleteMessage)
        }
    }

    @MainActor
    private func loadSuppliers() async {
        isLoading = true
        errorMessage = nil

        // Online-first: try Supabase first, then sync to local
        if syncStatus.isOnline && SupabaseService.shared.isConfigured {
            do {
                let remoteSuppliers: [SupplierDTO] = try await SupabaseService.shared.fetchAll(from: "suppliers")
                // Sync to local database using upsert (INSERT OR REPLACE)
                for dto in remoteSuppliers {
                    try? await sdk.supplierRepository.upsert(supplier: dto.toEntity())
                }
            } catch {
                debugLog("SuppliersListView", "Failed to sync suppliers from Supabase: \(error)")
            }
        }

        // Load from local database
        do {
            suppliers = try await sdk.supplierRepository.getAll()
        } catch {
            errorMessage = "Error: \(error.localizedDescription)"
            suppliers = []
        }
        isLoading = false
    }

    private func deleteSuppliers(at offsets: IndexSet) {
        let suppliersToDelete = offsets.map { filteredSuppliers[$0] }
        Task {
            for supplier in suppliersToDelete {
                // Check referential integrity before deleting
                let check = sdk.referentialIntegrityService.checkDeletion(entityType: .supplier, entityId: supplier.id)

                if let mustDeactivate = check as? DeletionCheck.MustDeactivate {
                    let count = mustDeactivate.usageDetails.totalUsageCount
                    let msg = Localized.strings.entityInUse
                        .replacingOccurrences(of: "{entity}", with: Localized.strings.supplier)
                        .replacingOccurrences(of: "{count}", with: "\(count)")

                    // Deactivate instead of delete
                    do {
                        try await sdk.supplierRepository.deactivate(id: supplier.id, updatedBy: session.userId)
                        await MainActor.run {
                            cannotDeleteMessage = "\(msg)\n\n\(Localized.strings.supplierDeactivated)"
                            showCannotDeleteAlert = true
                        }
                    } catch {
                        await MainActor.run {
                            errorMessage = "Error: \(error.localizedDescription)"
                        }
                    }
                } else {
                    // Safe to delete
                    do {
                        try await OnlineFirstHelper.shared.delete(
                            table: "suppliers",
                            entityType: .supplier,
                            entityId: supplier.id,
                            userId: session.userId
                        ) {
                            try await sdk.supplierRepository.delete(id: supplier.id)
                        }
                    } catch {
                        await MainActor.run {
                            errorMessage = "Error deleting: \(error.localizedDescription)"
                        }
                    }
                }
            }
            await loadSuppliers()
        }
    }
}

// MARK: - Supplier Row View
struct SupplierRowView: View {
    let supplier: Supplier

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(supplier.name)
                .font(.headline)

            HStack {
                if let phone = supplier.phone, !phone.isEmpty {
                    Label(phone, systemImage: "phone")
                }
                if let email = supplier.email, !email.isEmpty {
                    Label(email, systemImage: "envelope")
                }
            }
            .font(.caption)
            .foregroundColor(.secondary)

            if let address = supplier.address, !address.isEmpty {
                Label(address, systemImage: "location")
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .lineLimit(1)
            }
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Supplier Editor View
struct SupplierEditorView: View {
    let sdk: MedistockSDK
    @ObservedObject var session: SessionManager
    let supplier: Supplier?
    let onSave: () -> Void

    @ObservedObject private var syncStatus = SyncStatusManager.shared
    @Environment(\.dismiss) private var dismiss
    @State private var name: String = ""
    @State private var phone: String = ""
    @State private var email: String = ""
    @State private var address: String = ""
    @State private var notes: String = ""
    @State private var isSaving = false
    @State private var errorMessage: String?

    var isEditing: Bool { supplier != nil }

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text(Localized.information)) {
                    TextField(Localized.supplierName, text: $name)
                    TextField(Localized.phone, text: $phone)
                        .keyboardType(.phonePad)
                    TextField(Localized.email, text: $email)
                        .keyboardType(.emailAddress)
                        .textInputAutocapitalization(.never)
                }

                Section(header: Text(Localized.address)) {
                    TextField(Localized.address, text: $address)
                }

                Section(header: Text(Localized.notes)) {
                    TextEditor(text: $notes)
                        .frame(minHeight: 80)
                }

                if let errorMessage {
                    Section {
                        Text(errorMessage)
                            .foregroundColor(.red)
                    }
                }
            }
            .navigationTitle(isEditing ? Localized.editSupplier : Localized.addSupplier)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(Localized.cancel) { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(isEditing ? Localized.save : Localized.add) {
                        saveSupplier()
                    }
                    .disabled(name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isSaving)
                }
            }
            .onAppear {
                if let supplier = supplier {
                    name = supplier.name
                    phone = supplier.phone ?? ""
                    email = supplier.email ?? ""
                    address = supplier.address ?? ""
                    notes = supplier.notes ?? ""
                }
            }
        }
    }

    private func saveSupplier() {
        let trimmedName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedName.isEmpty else { return }

        isSaving = true
        errorMessage = nil

        Task {
            do {
                let isNew = supplier == nil
                var savedSupplier: Supplier

                if let existingSupplier = supplier {
                    savedSupplier = Supplier(
                        id: existingSupplier.id,
                        name: trimmedName,
                        phone: phone.isEmpty ? nil : phone,
                        email: email.isEmpty ? nil : email,
                        address: address.isEmpty ? nil : address,
                        notes: notes.isEmpty ? nil : notes,
                        isActive: existingSupplier.isActive,
                        createdAt: existingSupplier.createdAt,
                        updatedAt: Int64(Date().timeIntervalSince1970 * 1000),
                        createdBy: existingSupplier.createdBy,
                        updatedBy: session.userId
                    )
                } else {
                    savedSupplier = sdk.createSupplier(
                        name: trimmedName,
                        phone: phone.isEmpty ? nil : phone,
                        email: email.isEmpty ? nil : email,
                        address: address.isEmpty ? nil : address,
                        notes: notes.isEmpty ? nil : notes,
                        userId: session.userId
                    )
                }

                let dto = SupplierDTO(from: savedSupplier)

                try await OnlineFirstHelper.shared.save(
                    table: "suppliers",
                    dto: dto,
                    entityType: .supplier,
                    entityId: savedSupplier.id,
                    isNew: isNew,
                    userId: session.userId,
                    lastKnownRemoteUpdatedAt: supplier?.updatedAt
                ) {
                    if isNew {
                        try await sdk.supplierRepository.insert(supplier: savedSupplier)
                    } else {
                        try await sdk.supplierRepository.update(supplier: savedSupplier)
                    }
                }

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

extension Supplier: @retroactive Identifiable {}
