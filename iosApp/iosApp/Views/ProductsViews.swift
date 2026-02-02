import Foundation
import SwiftUI
import shared

// MARK: - Products List View
struct ProductsListView: View {
    let sdk: MedistockSDK
    @ObservedObject var session: SessionManager
    @ObservedObject private var syncStatus = SyncStatusManager.shared
    @State private var products: [Product] = []
    @State private var sites: [Site] = []
    @State private var categories: [shared.Category] = []
    @State private var packagingTypes: [PackagingType] = []
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
                        title: Localized.noProducts,
                        message: Localized.strings.noProductsMessage
                    )
                }
                .listRowSeparator(.hidden)
            } else {
                Section(header: Text("\(filteredProducts.count) \(Localized.products.lowercased())")) {
                    ForEach(filteredProducts, id: \.id) { product in
                        ProductRowView(product: product, siteName: siteName(for: product.siteId), categoryName: categoryName(for: product.categoryId), packagingType: packagingType(for: product.packagingTypeId))
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
        .navigationTitle(Localized.products)
        .searchable(text: $searchText, prompt: Localized.search)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { showAddSheet = true }) {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $showAddSheet) {
            ProductEditorView(sdk: sdk, session: session, product: nil, sites: sites, categories: categories, packagingTypes: packagingTypes) {
                Task { await loadData() }
            }
        }
        .sheet(item: $productToEdit) { product in
            ProductEditorView(sdk: sdk, session: session, product: product, sites: sites, categories: categories, packagingTypes: packagingTypes) {
                Task { await loadData() }
            }
        }
        .refreshable {
            await BidirectionalSyncManager.shared.fullSync(sdk: sdk)
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
        if syncStatus.isOnline && SupabaseService.shared.isConfigured {
            do {
                // Sync sites (upsert handles both new and existing records)
                let remoteSites: [SiteDTO] = try await SupabaseService.shared.fetchAll(from: "sites")
                for dto in remoteSites {
                    try? await sdk.siteRepository.upsert(site: dto.toEntity())
                }

                // Sync categories
                let remoteCategories: [CategoryDTO] = try await SupabaseService.shared.fetchAll(from: "categories")
                for dto in remoteCategories {
                    try? await sdk.categoryRepository.upsert(category: dto.toEntity())
                }

                // Sync packaging types
                let remotePackagingTypes: [PackagingTypeDTO] = try await SupabaseService.shared.fetchAll(from: "packaging_types")
                for dto in remotePackagingTypes {
                    try? await sdk.packagingTypeRepository.upsert(packagingType: dto.toEntity())
                }

                // Sync products
                let remoteProducts: [ProductDTO] = try await SupabaseService.shared.fetchAll(from: "products")
                for dto in remoteProducts {
                    try? await sdk.productRepository.upsert(product: dto.toEntity())
                }
            } catch {
                debugLog("ProductsListView", "Failed to sync from Supabase: \(error)")
            }
        }

        // Load from local database
        do {
            async let sitesResult = sdk.siteRepository.getAll()
            async let categoriesResult = sdk.categoryRepository.getAll()
            async let packagingTypesResult = sdk.packagingTypeRepository.getAll()
            async let productsResult = sdk.productRepository.getAll()

            sites = try await sitesResult
            categories = try await categoriesResult
            packagingTypes = try await packagingTypesResult
            products = try await productsResult
        } catch {
            errorMessage = "Error: \(error.localizedDescription)"
        }
        isLoading = false
    }

    private func siteName(for siteId: String) -> String {
        sites.first { $0.id == siteId }?.name ?? "Unknown"
    }

    private func categoryName(for categoryId: String?) -> String? {
        guard let categoryId = categoryId else { return nil }
        return categories.first { $0.id == categoryId }?.name
    }

    private func packagingType(for packagingTypeId: String?) -> PackagingType? {
        guard let packagingTypeId = packagingTypeId else { return nil }
        return packagingTypes.first { $0.id == packagingTypeId }
    }

    private func deleteProducts(at offsets: IndexSet) {
        let productsToDelete = offsets.map { filteredProducts[$0] }
        Task {
            for product in productsToDelete {
                do {
                    try await OnlineFirstHelper.shared.delete(
                        table: "products",
                        entityType: .product,
                        entityId: product.id,
                        userId: session.userId
                    ) {
                        try await sdk.productRepository.delete(id: product.id)
                    }
                } catch {
                    await MainActor.run {
                        errorMessage = "Error deleting: \(error.localizedDescription)"
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
    let packagingType: PackagingType?

    /// Derives the unit name from the packaging type based on selectedLevel
    var derivedUnit: String {
        guard let packagingType = packagingType else { return "unit" }
        let selectedLevel = Int(product.selectedLevel)
        if selectedLevel == 2, let level2Name = packagingType.level2Name {
            return level2Name
        }
        return packagingType.level1Name
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(product.name)
                .font(.headline)
            HStack {
                Text("\(Localized.site): \(siteName)")
                if let categoryName = categoryName {
                    Text("• \(categoryName)")
                }
            }
            .font(.subheadline)
            .foregroundColor(.secondary)
            Text("\(Localized.unit): \(derivedUnit) • \(String(format: "%.2f", product.unitVolume))")
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
    let packagingTypes: [PackagingType]
    let onSave: () -> Void

    @ObservedObject private var syncStatus = SyncStatusManager.shared
    @Environment(\.dismiss) private var dismiss
    @State private var name: String = ""
    @State private var volumeText: String = "1"
    @State private var selectedSiteId: String = ""
    @State private var selectedCategoryId: String = ""
    @State private var selectedPackagingTypeId: String = ""
    @State private var selectedLevel: Int = 1
    @State private var description: String = ""
    @State private var minStockText: String = ""
    @State private var isSaving = false
    @State private var errorMessage: String?

    var isEditing: Bool { product != nil }

    /// Returns the currently selected packaging type
    var selectedPackagingType: PackagingType? {
        packagingTypes.first { $0.id == selectedPackagingTypeId }
    }

    /// Derives the unit name from the selected packaging type and level
    var derivedUnit: String {
        guard let packagingType = selectedPackagingType else { return "unit" }
        if selectedLevel == 2, let level2Name = packagingType.level2Name {
            return level2Name
        }
        return packagingType.level1Name
    }

    /// Check if level 2 is available for the selected packaging type
    var hasLevel2: Bool {
        selectedPackagingType?.level2Name != nil
    }

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text(Localized.information)) {
                    TextField(Localized.productName, text: $name)
                    TextField(Localized.strings.unitVolume, text: $volumeText)
                        .keyboardType(.decimalPad)
                    TextField(Localized.description_, text: $description)
                }

                Section(header: Text(Localized.strings.packagingType)) {
                    Picker(Localized.strings.packagingType, selection: $selectedPackagingTypeId) {
                        Text(Localized.strings.none).tag("")
                        ForEach(packagingTypes, id: \.id) { packagingType in
                            Text(packagingType.name).tag(packagingType.id)
                        }
                    }
                    .onChange(of: selectedPackagingTypeId) { _ in
                        // Reset to level 1 when packaging type changes
                        selectedLevel = 1
                    }

                    if selectedPackagingType != nil {
                        if hasLevel2 {
                            Picker(Localized.unit, selection: $selectedLevel) {
                                if let pt = selectedPackagingType {
                                    Text(pt.level1Name).tag(1)
                                    if let level2Name = pt.level2Name {
                                        Text(level2Name).tag(2)
                                    }
                                }
                            }
                            .pickerStyle(.segmented)
                        }

                        HStack {
                            Text(Localized.unit)
                                .foregroundColor(.secondary)
                            Spacer()
                            Text(derivedUnit)
                                .fontWeight(.medium)
                        }
                    }
                }

                Section(header: Text(Localized.site)) {
                    if sites.isEmpty {
                        Text(Localized.strings.addSiteFirst)
                            .foregroundColor(.secondary)
                    } else {
                        Picker(Localized.site, selection: $selectedSiteId) {
                            ForEach(sites, id: \.id) { site in
                                Text(site.name).tag(site.id)
                            }
                        }
                    }
                }

                Section(header: Text(Localized.category)) {
                    Picker(Localized.category, selection: $selectedCategoryId) {
                        Text(Localized.strings.none).tag("")
                        ForEach(categories, id: \.id) { category in
                            Text(category.name).tag(category.id)
                        }
                    }
                }

                Section(header: Text(Localized.strings.stockAlerts)) {
                    TextField(Localized.minStock, text: $minStockText)
                        .keyboardType(.decimalPad)
                    Text(Localized.strings.stockAlertDescription)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }

                if let errorMessage {
                    Section {
                        Text(errorMessage)
                            .foregroundColor(.red)
                    }
                }
            }
            .navigationTitle(isEditing ? Localized.editProduct : Localized.addProduct)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(Localized.cancel) { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(isEditing ? Localized.save : Localized.add) {
                        saveProduct()
                    }
                    .disabled(!canSave || isSaving)
                }
            }
            .onAppear {
                if let product = product {
                    name = product.name
                    volumeText = String(format: "%.2f", product.unitVolume)
                    selectedSiteId = product.siteId
                    selectedCategoryId = product.categoryId ?? ""
                    selectedPackagingTypeId = product.packagingTypeId ?? ""
                    selectedLevel = Int(product.selectedLevel)
                    description = product.description_ ?? ""
                    let minStock = product.minStock?.doubleValue ?? 0.0
                    minStockText = minStock > 0 ? String(format: "%.0f", minStock) : ""
                } else {
                    selectedSiteId = sites.first?.id ?? ""
                    selectedPackagingTypeId = packagingTypes.first?.id ?? ""
                }
            }
        }
    }

    private var canSave: Bool {
        !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !selectedSiteId.isEmpty
    }

    private func saveProduct() {
        let trimmedName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        let volume = Double(volumeText.replacingOccurrences(of: ",", with: ".")) ?? 1.0
        let minStock = Double(minStockText.replacingOccurrences(of: ",", with: ".")) ?? 0.0

        isSaving = true
        errorMessage = nil

        Task {
            do {
                let isNew = product == nil
                var savedProduct: Product

                if let existingProduct = product {
                    savedProduct = Product(
                        id: existingProduct.id,
                        name: trimmedName,
                        unitVolume: volume,
                        packagingTypeId: selectedPackagingTypeId,
                        selectedLevel: Int32(selectedLevel),
                        conversionFactor: existingProduct.conversionFactor,
                        categoryId: selectedCategoryId.isEmpty ? nil : selectedCategoryId,
                        marginType: existingProduct.marginType,
                        marginValue: existingProduct.marginValue,
                        description: description.isEmpty ? nil : description,
                        siteId: selectedSiteId,
                        minStock: KotlinDouble(value: minStock),
                        maxStock: existingProduct.maxStock,
                        isActive: existingProduct.isActive,
                        createdAt: existingProduct.createdAt,
                        updatedAt: Int64(Date().timeIntervalSince1970 * 1000),
                        createdBy: existingProduct.createdBy,
                        updatedBy: session.userId
                    )
                } else {
                    let newProduct = sdk.createProduct(
                        name: trimmedName,
                        siteId: selectedSiteId,
                        packagingTypeId: selectedPackagingTypeId,
                        unitVolume: volume,
                        selectedLevel: Int32(selectedLevel),
                        conversionFactor: nil,
                        categoryId: selectedCategoryId.isEmpty ? nil : selectedCategoryId,
                        userId: session.userId
                    )
                    // Set minStock, packagingTypeId and selectedLevel on the new product
                    savedProduct = Product(
                        id: newProduct.id,
                        name: newProduct.name,
                        unitVolume: newProduct.unitVolume,
                        packagingTypeId: selectedPackagingTypeId,
                        selectedLevel: Int32(selectedLevel),
                        conversionFactor: newProduct.conversionFactor,
                        categoryId: newProduct.categoryId,
                        marginType: newProduct.marginType,
                        marginValue: newProduct.marginValue,
                        description: description.isEmpty ? nil : description,
                        siteId: newProduct.siteId,
                        minStock: KotlinDouble(value: minStock),
                        maxStock: newProduct.maxStock,
                        isActive: newProduct.isActive,
                        createdAt: newProduct.createdAt,
                        updatedAt: newProduct.updatedAt,
                        createdBy: newProduct.createdBy,
                        updatedBy: newProduct.updatedBy
                    )
                }

                let dto = ProductDTO(from: savedProduct)

                try await OnlineFirstHelper.shared.save(
                    table: "products",
                    dto: dto,
                    entityType: .product,
                    entityId: savedProduct.id,
                    isNew: isNew,
                    userId: session.userId,
                    lastKnownRemoteUpdatedAt: product?.updatedAt
                ) {
                    if isNew {
                        try await sdk.productRepository.insert(product: savedProduct)
                    } else {
                        try await sdk.productRepository.update(product: savedProduct)
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

extension Product: @retroactive Identifiable {}
