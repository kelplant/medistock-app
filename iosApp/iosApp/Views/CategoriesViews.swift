import Foundation
import SwiftUI
import shared

// MARK: - Categories List View
struct CategoriesListView: View {
    let sdk: MedistockSDK
    @ObservedObject var session: SessionManager
    @State private var categories: [Category] = []
    @State private var isLoading = true
    @State private var errorMessage: String?
    @State private var showAddSheet = false
    @State private var categoryToEdit: Category?

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
                    try await sdk.categoryRepository.delete(id: category.id)
                } catch {
                    await MainActor.run {
                        errorMessage = "Erreur lors de la suppression"
                    }
                }
            }
            await loadCategories()
        }
    }
}

// MARK: - Category Row View
struct CategoryRowView: View {
    let category: Category

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
    let category: Category?
    let onSave: () -> Void

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
                if let existingCategory = category {
                    let updated = Category(
                        id: existingCategory.id,
                        name: trimmedName,
                        createdAt: existingCategory.createdAt,
                        updatedAt: Int64(Date().timeIntervalSince1970 * 1000),
                        createdBy: existingCategory.createdBy,
                        updatedBy: session.username
                    )
                    try await sdk.categoryRepository.update(category: updated)
                } else {
                    let newCategory = sdk.createCategory(name: trimmedName, userId: session.username)
                    try await sdk.categoryRepository.insert(category: newCategory)
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

extension Category: Identifiable {}
