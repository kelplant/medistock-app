import Foundation
import SwiftUI
import shared

// MARK: - Categories List View
struct CategoriesListView: View {
    let sdk: MedistockSDK
    @ObservedObject var session: SessionManager
    @ObservedObject private var syncStatus = SyncStatusManager.shared
    @State private var categories: [shared.Category] = []
    @State private var isLoading = true
    @State private var errorMessage: String?
    @State private var showAddSheet = false
    @State private var categoryToEdit: shared.Category?

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
            } else if categories.isEmpty {
                Section {
                    EmptyStateView(
                        icon: "folder",
                        title: "Aucune catégorie",
                        message: "Ajoutez votre première catégorie pour organiser vos produits."
                    )
                }
                .listRowSeparator(.hidden)
            } else {
                Section(header: Text("\(categories.count) catégorie(s)")) {
                    ForEach(categories, id: \.id) { category in
                        CategoryRowView(category: category)
                            .contentShape(Rectangle())
                            .onTapGesture {
                                categoryToEdit = category
                            }
                    }
                    .onDelete(perform: deleteCategories)
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle("Catégories")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { showAddSheet = true }) {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $showAddSheet) {
            CategoryEditorView(sdk: sdk, session: session, category: nil) {
                Task { await loadCategories() }
            }
        }
        .sheet(item: $categoryToEdit) { category in
            CategoryEditorView(sdk: sdk, session: session, category: category) {
                Task { await loadCategories() }
            }
        }
        .refreshable {
            await BidirectionalSyncManager.shared.fullSync(sdk: sdk)
            await loadCategories()
        }
        .task {
            await loadCategories()
        }
    }

    @MainActor
    private func loadCategories() async {
        isLoading = true
        errorMessage = nil

        // Online-first: try Supabase first, then sync to local
        if syncStatus.isOnline && SupabaseService.shared.isConfigured {
            do {
                let remoteCategories: [CategoryDTO] = try await SupabaseService.shared.fetchAll(from: "categories")
                // Sync to local database using upsert (INSERT OR REPLACE)
                for dto in remoteCategories {
                    try? await sdk.categoryRepository.upsert(category: dto.toEntity())
                }
            } catch {
                debugLog("CategoriesListView", "Failed to fetch from Supabase: \(error)")
            }
        }

        // Always return from local database
        do {
            categories = try await sdk.categoryRepository.getAll()
        } catch {
            errorMessage = "Erreur: \(error.localizedDescription)"
            categories = []
        }
        isLoading = false
    }

    private func deleteCategories(at offsets: IndexSet) {
        let categoriesToDelete = offsets.map { categories[$0] }
        Task {
            for category in categoriesToDelete {
                do {
                    try await OnlineFirstHelper.shared.delete(
                        table: "categories",
                        entityType: .category,
                        entityId: category.id,
                        userId: session.userId
                    ) {
                        try await sdk.categoryRepository.delete(id: category.id)
                    }
                } catch {
                    await MainActor.run {
                        errorMessage = "Erreur lors de la suppression: \(error.localizedDescription)"
                    }
                }
            }
            await loadCategories()
        }
    }
}

// MARK: - Category Row View
struct CategoryRowView: View {
    let category: shared.Category

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(category.name)
                .font(.headline)
            Text("ID: \(category.id)")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Category Editor View
struct CategoryEditorView: View {
    let sdk: MedistockSDK
    @ObservedObject var session: SessionManager
    let category: shared.Category?
    let onSave: () -> Void

    @ObservedObject private var syncStatus = SyncStatusManager.shared
    @Environment(\.dismiss) private var dismiss
    @State private var name: String = ""
    @State private var isSaving = false
    @State private var errorMessage: String?

    var isEditing: Bool { category != nil }

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Informations")) {
                    TextField("Nom de la catégorie", text: $name)
                }

                if let errorMessage {
                    Section {
                        Text(errorMessage)
                            .foregroundColor(.red)
                    }
                }
            }
            .navigationTitle(isEditing ? "Modifier la catégorie" : "Nouvelle catégorie")
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Annuler") { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(isEditing ? "Enregistrer" : "Ajouter") {
                        saveCategory()
                    }
                    .disabled(name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isSaving)
                }
            }
            .onAppear {
                if let category = category {
                    name = category.name
                }
            }
        }
    }

    private func saveCategory() {
        let trimmedName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedName.isEmpty else { return }

        isSaving = true
        errorMessage = nil

        Task {
            do {
                let isNew = category == nil
                var savedCategory: shared.Category

                if let existingCategory = category {
                    savedCategory = shared.Category(
                        id: existingCategory.id,
                        name: trimmedName,
                        createdAt: existingCategory.createdAt,
                        updatedAt: Int64(Date().timeIntervalSince1970 * 1000),
                        createdBy: existingCategory.createdBy,
                        updatedBy: session.userId
                    )
                } else {
                    savedCategory = sdk.createCategory(name: trimmedName, userId: session.userId)
                }

                let dto = CategoryDTO(from: savedCategory)

                try await OnlineFirstHelper.shared.save(
                    table: "categories",
                    dto: dto,
                    entityType: .category,
                    entityId: savedCategory.id,
                    isNew: isNew,
                    userId: session.userId,
                    lastKnownRemoteUpdatedAt: category?.updatedAt
                ) {
                    if isNew {
                        try await sdk.categoryRepository.insert(category: savedCategory)
                    } else {
                        try await sdk.categoryRepository.update(category: savedCategory)
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

extension shared.Category: @retroactive Identifiable {}
