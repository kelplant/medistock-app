import Foundation
import SwiftUI
import shared

// MARK: - Packaging Types List View
struct PackagingTypesListView: View {
    let sdk: MedistockSDK
    @ObservedObject var session: SessionManager
    @ObservedObject private var syncStatus = SyncStatusManager.shared

    @State private var packagingTypes: [PackagingType] = []
    @State private var isLoading = true
    @State private var errorMessage: String?
    @State private var showAddSheet = false
    @State private var packagingTypeToEdit: PackagingType?

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
            } else if packagingTypes.isEmpty {
                Section {
                    EmptyStateView(
                        icon: "shippingbox",
                        title: "Aucun conditionnement",
                        message: "Ajoutez des types de conditionnement pour vos produits."
                    )
                }
                .listRowSeparator(.hidden)
            } else {
                Section(header: Text("\(packagingTypes.count) conditionnement(s)")) {
                    ForEach(packagingTypes, id: \.id) { packagingType in
                        PackagingTypeRowView(packagingType: packagingType)
                            .contentShape(Rectangle())
                            .onTapGesture {
                                packagingTypeToEdit = packagingType
                            }
                    }
                    .onDelete(perform: deletePackagingTypes)
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle("Conditionnements")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { showAddSheet = true }) {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $showAddSheet) {
            PackagingTypeEditorView(sdk: sdk, session: session, packagingType: nil) {
                Task { await loadPackagingTypes() }
            }
        }
        .sheet(item: $packagingTypeToEdit) { packagingType in
            PackagingTypeEditorView(sdk: sdk, session: session, packagingType: packagingType) {
                Task { await loadPackagingTypes() }
            }
        }
        .refreshable {
            await BidirectionalSyncManager.shared.fullSync(sdk: sdk)
            await loadPackagingTypes()
        }
        .task {
            await loadPackagingTypes()
        }
    }

    @MainActor
    private func loadPackagingTypes() async {
        isLoading = true
        errorMessage = nil

        // Online-first: try Supabase first, then sync to local
        if syncStatus.isOnline && SupabaseService.shared.isConfigured {
            do {
                let remoteTypes: [PackagingTypeDTO] = try await SupabaseService.shared.fetchAll(from: "packaging_types")
                // Sync to local database using upsert (INSERT OR REPLACE)
                for dto in remoteTypes {
                    try? await sdk.packagingTypeRepository.upsert(packagingType: dto.toEntity())
                }
            } catch {
                debugLog("PackagingTypesListView", "Failed to sync packaging types from Supabase: \(error)")
            }
        }

        // Load from local database
        do {
            packagingTypes = try await sdk.packagingTypeRepository.getAll()
        } catch {
            errorMessage = "Erreur: \(error.localizedDescription)"
            packagingTypes = []
        }
        isLoading = false
    }

    private func deletePackagingTypes(at offsets: IndexSet) {
        let typesToDelete = offsets.map { packagingTypes[$0] }
        Task {
            for packagingType in typesToDelete {
                do {
                    try await OnlineFirstHelper.shared.delete(
                        table: "packaging_types",
                        entityType: .packagingType,
                        entityId: packagingType.id,
                        userId: session.userId
                    ) {
                        try await sdk.packagingTypeRepository.delete(id: packagingType.id)
                    }
                } catch {
                    await MainActor.run {
                        errorMessage = "Erreur lors de la suppression: \(error.localizedDescription)"
                    }
                }
            }
            await loadPackagingTypes()
        }
    }
}

// MARK: - Packaging Type Row View
struct PackagingTypeRowView: View {
    let packagingType: PackagingType

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(packagingType.name)
                .font(.headline)

            Text("Niveau 1: \(packagingType.level1Name)")
                .font(.subheadline)
                .foregroundColor(.secondary)

            if let level2Name = packagingType.level2Name, !level2Name.isEmpty {
                HStack {
                    Text("Niveau 2: \(level2Name)")
                    if let qty = packagingType.level2Quantity {
                        Text("(\(qty) unités)")
                    }
                }
                .font(.caption)
                .foregroundColor(.secondary)
            }
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Packaging Type Editor View
struct PackagingTypeEditorView: View {
    let sdk: MedistockSDK
    @ObservedObject var session: SessionManager
    let packagingType: PackagingType?
    let onSave: () -> Void

    @ObservedObject private var syncStatus = SyncStatusManager.shared
    @Environment(\.dismiss) private var dismiss
    @State private var name: String = ""
    @State private var level1Name: String = ""
    @State private var level2Name: String = ""
    @State private var level2QuantityText: String = ""
    @State private var hasLevel2 = false
    @State private var isSaving = false
    @State private var errorMessage: String?

    var isEditing: Bool { packagingType != nil }

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Informations")) {
                    TextField("Nom du conditionnement", text: $name)
                    TextField("Nom niveau 1 (ex: Comprimé)", text: $level1Name)
                }

                Section(header: Text("Niveau 2 (optionnel)")) {
                    Toggle("Ajouter un niveau 2", isOn: $hasLevel2)

                    if hasLevel2 {
                        TextField("Nom niveau 2 (ex: Boîte)", text: $level2Name)
                        TextField("Quantité par niveau 2", text: $level2QuantityText)
                            .keyboardType(.numberPad)
                    }
                }

                if let errorMessage {
                    Section {
                        Text(errorMessage)
                            .foregroundColor(.red)
                    }
                }
            }
            .navigationTitle(isEditing ? "Modifier" : "Nouveau conditionnement")
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Annuler") { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(isEditing ? "Enregistrer" : "Ajouter") {
                        savePackagingType()
                    }
                    .disabled(!canSave || isSaving)
                }
            }
            .onAppear {
                if let packagingType = packagingType {
                    name = packagingType.name
                    level1Name = packagingType.level1Name
                    if let l2 = packagingType.level2Name {
                        hasLevel2 = true
                        level2Name = l2
                        if let qty = packagingType.level2Quantity {
                            level2QuantityText = "\(qty.int32Value)"
                        }
                    }
                }
            }
        }
    }

    private var canSave: Bool {
        !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
        !level1Name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    private func savePackagingType() {
        let trimmedName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedLevel1 = level1Name.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedLevel2 = level2Name.trimmingCharacters(in: .whitespacesAndNewlines)
        let level2Qty: KotlinInt? = hasLevel2 ? Int32(level2QuantityText).map { KotlinInt(int: $0) } : nil

        isSaving = true
        errorMessage = nil

        Task {
            do {
                let isNew = packagingType == nil
                var savedType: PackagingType

                if let existing = packagingType {
                    savedType = PackagingType(
                        id: existing.id,
                        name: trimmedName,
                        level1Name: trimmedLevel1,
                        level2Name: hasLevel2 && !trimmedLevel2.isEmpty ? trimmedLevel2 : nil,
                        level2Quantity: level2Qty,
                        defaultConversionFactor: existing.defaultConversionFactor,
                        isActive: existing.isActive,
                        displayOrder: existing.displayOrder,
                        createdAt: existing.createdAt,
                        updatedAt: Int64(Date().timeIntervalSince1970 * 1000),
                        createdBy: existing.createdBy,
                        updatedBy: session.userId
                    )
                } else {
                    savedType = sdk.createPackagingType(
                        name: trimmedName,
                        level1Name: trimmedLevel1,
                        level2Name: hasLevel2 && !trimmedLevel2.isEmpty ? trimmedLevel2 : nil,
                        level2Quantity: level2Qty,
                        defaultConversionFactor: nil,
                        isActive: true,
                        displayOrder: 0,
                        userId: session.userId
                    )
                }

                let dto = PackagingTypeDTO(from: savedType)

                try await OnlineFirstHelper.shared.save(
                    table: "packaging_types",
                    dto: dto,
                    entityType: .packagingType,
                    entityId: savedType.id,
                    isNew: isNew,
                    userId: session.userId,
                    lastKnownRemoteUpdatedAt: packagingType?.updatedAt
                ) {
                    if isNew {
                        try await sdk.packagingTypeRepository.insert(packagingType: savedType)
                    } else {
                        try await sdk.packagingTypeRepository.update(packagingType: savedType)
                    }
                }

                await MainActor.run {
                    onSave()
                    dismiss()
                }
            } catch {
                await MainActor.run {
                    isSaving = false
                    errorMessage = "Erreur: \(error.localizedDescription)"
                }
            }
        }
    }
}

extension PackagingType: @retroactive Identifiable {}
