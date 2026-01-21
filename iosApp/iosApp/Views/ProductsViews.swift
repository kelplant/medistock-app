import Foundation
import SwiftUI
import shared

// MARK: - Products List View
struct ProductsListView: View {
    let sdk: MedistockSDK
    @ObservedObject var session: SessionManager
    @State private var products: [Product] = []
    @State private var sites: [Site] = []
    @State private var categories: [shared.Category] = []
    @State private var isLoading = true
    @State private var errorMessage: String?
    @State private var showAddSheet = false
    @State private var productToEdit: Product?
    @State private var searchText = ""

    var filteredProducts: [Product] {
        if searchText.isEmpty {
            return products
        }
        return products.filter { $0.name.localizedCaseInsensitiveContains(searchText) }
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
            } else if products.isEmpty {
                Section {
                    EmptyStateView(
                        icon: "cube.box",
                        title: "Aucun produit",
                        message: "Ajoutez votre premier produit pour commencer."
                    )
                }
                .listRowSeparator(.hidden)
            } else {
                Section(header: Text("\(filteredProducts.count) produit(s)")) {
                    ForEach(filteredProducts, id: \.id) { product in
                        ProductRowView(product: product, siteName: siteName(for: product.siteId), categoryName: categoryName(for: product.categoryId))
                            .contentShape(Rectangle())
                            .onTapGesture {
                                productToEdit = product
                            }
                    }
                    .onDelete(perform: deleteProducts)
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle("Produits")
        .searchable(text: $searchText, prompt: "Rechercher un produit")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { showAddSheet = true }) {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $showAddSheet) {
            ProductEditorView(sdk: sdk, session: session, product: nil, sites: sites, categories: categories) {
                Task { await loadData() }
            }
        }
        .sheet(item: $productToEdit) { product in
            ProductEditorView(sdk: sdk, session: session, product: product, sites: sites, categories: categories) {
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

        // Online-first: try Supabase first, then sync to local
        if SyncManager.shared.isOnline && SupabaseClient.shared.isConfigured {
            do {
                // Sync sites
                let remoteSites: [RemoteSite] = try await SupabaseClient.shared.fetchAll(from: "sites")
                for remoteSite in remoteSites {
                    let localSite = Site(
                        id: remoteSite.id, name: remoteSite.name,
                        createdAt: remoteSite.createdAt, updatedAt: remoteSite.updatedAt,
                        createdBy: remoteSite.createdBy, updatedBy: remoteSite.updatedBy
                    )
                    try? await sdk.siteRepository.upsert(site: localSite)
                }

                // Sync categories
                let remoteCategories: [RemoteCategory] = try await SupabaseClient.shared.fetchAll(from: "categories")
                for remoteCategory in remoteCategories {
                    let localCategory = shared.Category(
                        id: remoteCategory.id, name: remoteCategory.name,
                        createdAt: remoteCategory.createdAt, updatedAt: remoteCategory.updatedAt,
                        createdBy: remoteCategory.createdBy, updatedBy: remoteCategory.updatedBy
                    )
                    try? await sdk.categoryRepository.upsert(category: localCategory)
                }

                // Sync products
                let remoteProducts: [RemoteProduct] = try await SupabaseClient.shared.fetchAll(from: "products")
                for remoteProduct in remoteProducts {
                    let localProduct = Product(
                        id: remoteProduct.id, name: remoteProduct.name,
                        unit: remoteProduct.unit, unitVolume: remoteProduct.unitVolume ?? 1.0,
                        packagingTypeId: remoteProduct.packagingTypeId,
                        selectedLevel: remoteProduct.selectedLevel,
                        conversionFactor: remoteProduct.conversionFactor,
                        categoryId: remoteProduct.categoryId,
                        marginType: remoteProduct.marginType, marginValue: remoteProduct.marginValue,
                        description: remoteProduct.description,
                        siteId: "", minStock: 0, maxStock: 0,
                        createdAt: remoteProduct.createdAt, updatedAt: remoteProduct.updatedAt,
                        createdBy: remoteProduct.createdBy, updatedBy: remoteProduct.updatedBy
                    )
                    try? await sdk.productRepository.upsert(product: localProduct)
                }
            } catch {
                print("Failed to sync from Supabase: \(error)")
            }
        }

        // Load from local database
        do {
            async let sitesResult = sdk.siteRepository.getAll()
            async let categoriesResult = sdk.categoryRepository.getAll()
            async let productsResult = sdk.productRepository.getAll()

            sites = try await sitesResult
            categories = try await categoriesResult
            products = try await productsResult
        } catch {
            errorMessage = "Erreur: \(error.localizedDescription)"
        }
        isLoading = false
    }

    private func siteName(for siteId: String) -> String {
        sites.first { $0.id == siteId }?.name ?? "Inconnu"
    }

    private func categoryName(for categoryId: String?) -> String? {
        guard let categoryId = categoryId else { return nil }
        return categories.first { $0.id == categoryId }?.name
    }

    private func deleteProducts(at offsets: IndexSet) {
        let productsToDelete = offsets.map { filteredProducts[$0] }
        Task {
            for product in productsToDelete {
                do {
                    // Online-first: delete from Supabase first
                    if SyncManager.shared.isOnline && SupabaseClient.shared.isConfigured {
                        try await SupabaseClient.shared.delete(from: "products", id: product.id)
                    }
                    try await sdk.productRepository.delete(id: product.id)
                } catch {
                    await MainActor.run {
                        errorMessage = "Erreur lors de la suppression: \(error.localizedDescription)"
                    }
                }
            }
            await loadData()
        }
    }
}

// MARK: - Product Row View
struct ProductRowView: View {
    let product: Product
    let siteName: String
    let categoryName: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(product.name)
                .font(.headline)
            HStack {
                Text("Site: \(siteName)")
                if let categoryName = categoryName {
                    Text("• \(categoryName)")
                }
            }
            .font(.subheadline)
            .foregroundColor(.secondary)
            Text("Unité: \(product.unit) • Volume: \(String(format: "%.2f", product.unitVolume))")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Product Editor View
struct ProductEditorView: View {
    let sdk: MedistockSDK
    @ObservedObject var session: SessionManager
    let product: Product?
    let sites: [Site]
    let categories: [shared.Category]
    let onSave: () -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var name: String = ""
    @State private var unit: String = "unité"
    @State private var volumeText: String = "1"
    @State private var selectedSiteId: String = ""
    @State private var selectedCategoryId: String = ""
    @State private var description: String = ""
    @State private var isSaving = false
    @State private var errorMessage: String?

    var isEditing: Bool { product != nil }

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Informations")) {
                    TextField("Nom du produit", text: $name)
                    TextField("Unité (ex: comprimé, ml, boîte)", text: $unit)
                    TextField("Volume unitaire", text: $volumeText)
                        .keyboardType(.decimalPad)
                    TextField("Description (optionnel)", text: $description)
                }

                Section(header: Text("Site")) {
                    if sites.isEmpty {
                        Text("Ajoutez d'abord un site.")
                            .foregroundColor(.secondary)
                    } else {
                        Picker("Site", selection: $selectedSiteId) {
                            ForEach(sites, id: \.id) { site in
                                Text(site.name).tag(site.id)
                            }
                        }
                    }
                }

                Section(header: Text("Catégorie")) {
                    Picker("Catégorie", selection: $selectedCategoryId) {
                        Text("Aucune").tag("")
                        ForEach(categories, id: \.id) { category in
                            Text(category.name).tag(category.id)
                        }
                    }
                }

                if let errorMessage {
                    Section {
                        Text(errorMessage)
                            .foregroundColor(.red)
                    }
                }
            }
            .navigationTitle(isEditing ? "Modifier le produit" : "Nouveau produit")
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Annuler") { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(isEditing ? "Enregistrer" : "Ajouter") {
                        saveProduct()
                    }
                    .disabled(!canSave || isSaving)
                }
            }
            .onAppear {
                if let product = product {
                    name = product.name
                    unit = product.unit
                    volumeText = String(format: "%.2f", product.unitVolume)
                    selectedSiteId = product.siteId
                    selectedCategoryId = product.categoryId ?? ""
                    description = product.description_ ?? ""
                } else {
                    selectedSiteId = sites.first?.id ?? ""
                }
            }
        }
    }

    private var canSave: Bool {
        !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !selectedSiteId.isEmpty
    }

    private func saveProduct() {
        let trimmedName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedUnit = unit.trimmingCharacters(in: .whitespacesAndNewlines)
        let volume = Double(volumeText.replacingOccurrences(of: ",", with: ".")) ?? 1.0

        isSaving = true
        errorMessage = nil

        Task {
            do {
                var savedProduct: Product

                if let existingProduct = product {
                    savedProduct = Product(
                        id: existingProduct.id,
                        name: trimmedName,
                        unit: trimmedUnit.isEmpty ? "unité" : trimmedUnit,
                        unitVolume: volume,
                        packagingTypeId: existingProduct.packagingTypeId,
                        selectedLevel: existingProduct.selectedLevel,
                        conversionFactor: existingProduct.conversionFactor,
                        categoryId: selectedCategoryId.isEmpty ? nil : selectedCategoryId,
                        marginType: existingProduct.marginType,
                        marginValue: existingProduct.marginValue,
                        description: description.isEmpty ? nil : description,
                        siteId: selectedSiteId,
                        minStock: existingProduct.minStock,
                        maxStock: existingProduct.maxStock,
                        createdAt: existingProduct.createdAt,
                        updatedAt: Int64(Date().timeIntervalSince1970 * 1000),
                        createdBy: existingProduct.createdBy,
                        updatedBy: session.username
                    )
                } else {
                    savedProduct = sdk.createProduct(
                        name: trimmedName,
                        siteId: selectedSiteId,
                        unit: trimmedUnit.isEmpty ? "unité" : trimmedUnit,
                        unitVolume: volume,
                        categoryId: selectedCategoryId.isEmpty ? nil : selectedCategoryId,
                        userId: session.username
                    )
                }

                // Online-first: save to Supabase first
                if SyncManager.shared.isOnline && SupabaseClient.shared.isConfigured {
                    let remoteProduct = RemoteProduct(
                        id: savedProduct.id,
                        name: savedProduct.name,
                        description: savedProduct.description_,
                        unit: savedProduct.unit,
                        unitVolume: savedProduct.unitVolume,
                        packagingTypeId: savedProduct.packagingTypeId,
                        conversionFactor: savedProduct.conversionFactor,
                        categoryId: savedProduct.categoryId,
                        marginType: savedProduct.marginType,
                        marginValue: savedProduct.marginValue,
                        selectedLevel: savedProduct.selectedLevel,
                        createdAt: savedProduct.createdAt,
                        updatedAt: savedProduct.updatedAt,
                        createdBy: savedProduct.createdBy,
                        updatedBy: savedProduct.updatedBy
                    )
                    _ = try await SupabaseClient.shared.upsert(into: "products", record: remoteProduct)
                }

                // Then sync to local database
                if product != nil {
                    try await sdk.productRepository.update(product: savedProduct)
                } else {
                    try await sdk.productRepository.insert(product: savedProduct)
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

extension Product: @retroactive Identifiable {}
