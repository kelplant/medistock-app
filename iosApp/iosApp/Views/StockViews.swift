import Foundation
import SwiftUI
import shared

// MARK: - Stock List View
struct StockListView: View {
    let sdk: MedistockSDK
    let siteId: String?

    @State private var stockItems: [StockItem] = []
    @State private var isLoading = true
    @State private var errorMessage: String?
    @State private var searchText = ""

    var filteredItems: [StockItem] {
        if searchText.isEmpty {
            return stockItems
        }
        return stockItems.filter { $0.productName.localizedCaseInsensitiveContains(searchText) }
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
            } else if stockItems.isEmpty {
                Section {
                    EmptyStateView(
                        icon: "chart.bar",
                        title: Localized.noStock,
                        message: Localized.strings.noStockMessage
                    )
                }
                .listRowSeparator(.hidden)
            } else {
                // Summary
                Section(header: Text(Localized.summary)) {
                    HStack {
                        VStack {
                            Text("\(stockItems.count)")
                                .font(.title)
                                .fontWeight(.bold)
                            Text(Localized.products)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        Spacer()
                        VStack {
                            Text("\(stockItems.filter { $0.totalStock <= 0 }.count)")
                                .font(.title)
                                .fontWeight(.bold)
                                .foregroundColor(.red)
                            Text(Localized.outOfStock)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        Spacer()
                        VStack {
                            Text("\(stockItems.filter { $0.totalStock > 0 && $0.totalStock <= $0.minStock }.count)")
                                .font(.title)
                                .fontWeight(.bold)
                                .foregroundColor(.orange)
                            Text(Localized.lowStock)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                    .padding(.vertical, 8)
                }

                // Stock list
                Section(header: Text(Localized.stockByProduct)) {
                    ForEach(filteredItems, id: \.productId) { item in
                        StockItemRowView(item: item)
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle(Localized.stock)
        .searchable(text: $searchText, prompt: Localized.search)
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
            let products = try await sdk.productRepository.getAll()
            let sites = try await sdk.siteRepository.getAll()
            let packagingTypes = try await sdk.packagingTypeRepository.getAll()
            let allStock = try await sdk.stockRepository.getAllCurrentStock()
            let batches = try await sdk.purchaseBatchRepository.getAll()

            var items: [StockItem] = []

            for product in products {
                // Filter by site if specified
                if let siteId = siteId, product.siteId != siteId {
                    continue
                }

                let stock = allStock.first { $0.productId == product.id && $0.siteId == product.siteId }
                let siteName = sites.first { $0.id == product.siteId }?.name ?? "Unknown"

                // Derive unit from packaging type
                let packagingType = packagingTypes.first { $0.id == product.packagingTypeId }
                let selectedLevel = Int(product.selectedLevel)
                let unit: String
                if let pt = packagingType {
                    if selectedLevel == 2, let level2Name = pt.level2Name {
                        unit = level2Name
                    } else {
                        unit = pt.level1Name
                    }
                } else {
                    unit = "unit"
                }

                // Get batches for expiry info
                let productBatches = batches.filter { $0.productId == product.id && !$0.isExhausted }
                let nearestExpiry = productBatches.compactMap { $0.expiryDate?.timestampValue }.min()

                items.append(StockItem(
                    productId: product.id,
                    productName: product.name,
                    siteId: product.siteId,
                    siteName: siteName,
                    totalStock: stock?.totalStock ?? 0,
                    minStock: product.minStock?.doubleValue ?? 0,
                    unit: unit,
                    nearestExpiryDate: nearestExpiry,
                    batchCount: productBatches.count
                ))
            }

            // Filter out items with zero stock - only show products actually present
            // Then sort by low stock first, then by name
            stockItems = items
                .filter { $0.totalStock > 0 }
                .sorted { a, b in
                    // Sort by low stock first (items at or below min_stock threshold)
                    let aIsLow = a.totalStock <= a.minStock
                    let bIsLow = b.totalStock <= b.minStock
                    if aIsLow && !bIsLow { return true }
                    if !aIsLow && bIsLow { return false }
                    return a.productName < b.productName
                }
        } catch {
            errorMessage = "Error: \(error.localizedDescription)"
        }
        isLoading = false
    }
}

// MARK: - Stock Item Model
struct StockItem {
    let productId: String
    let productName: String
    let siteId: String
    let siteName: String
    let totalStock: Double
    let minStock: Double
    let unit: String
    let nearestExpiryDate: Int64?
    let batchCount: Int
}

// MARK: - Stock Item Row View
struct StockItemRowView: View {
    let item: StockItem

    var stockColor: Color {
        if item.totalStock <= 0 { return .red }
        if item.totalStock <= item.minStock { return .orange }
        return .green
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(item.productName)
                    .font(.headline)
                Spacer()
                Text("\(String(format: "%.1f", item.totalStock)) \(item.unit)")
                    .font(.headline)
                    .foregroundColor(stockColor)
            }

            HStack {
                Text("\(Localized.site): \(item.siteName)")
                Spacer()
                Text("\(item.batchCount) \(Localized.lots)")
            }
            .font(.caption)
            .foregroundColor(.secondary)

            if let expiry = item.nearestExpiryDate {
                HStack {
                    Image(systemName: "calendar.badge.exclamationmark")
                    Text("\(Localized.nearestExpiry): \(formatDate(expiry))")
                }
                .font(.caption)
                .foregroundColor(isExpiringSoon(expiry) ? .orange : .secondary)
            }
        }
        .padding(.vertical, 4)
    }

    private func formatDate<T: TimestampConvertible>(_ timestamp: T) -> String {
        let date = Date(timeIntervalSince1970: Double(timestamp.timestampValue) / 1000)
        let formatter = DateFormatter()
        formatter.dateStyle = .short
        return formatter.string(from: date)
    }

    private func isExpiringSoon(_ timestamp: Int64) -> Bool {
        let expiryDate = Date(timeIntervalSince1970: Double(timestamp) / 1000)
        let thirtyDaysFromNow = Calendar.current.date(byAdding: .day, value: 30, to: Date()) ?? Date()
        return expiryDate < thirtyDaysFromNow
    }
}

// MARK: - Stock Movements List View
struct StockMovementsListView: View {
    let sdk: MedistockSDK
    let siteId: String?

    @State private var movements: [StockMovement] = []
    @State private var products: [Product] = []
    @State private var sites: [Site] = []
    @State private var isLoading = true
    @State private var errorMessage: String?
    @State private var showingCreation = false

    init(sdk: MedistockSDK, siteId: String? = nil) {
        self.sdk = sdk
        self.siteId = siteId
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
            } else if movements.isEmpty {
                Section {
                    EmptyStateView(
                        icon: "arrow.up.arrow.down",
                        title: Localized.noMovements,
                        message: Localized.strings.noMovementsMessage
                    )
                }
                .listRowSeparator(.hidden)
            } else {
                Section(header: Text("\(movements.count) \(Localized.stockMovements.lowercased())")) {
                    ForEach(movements, id: \.id) { movement in
                        StockMovementRowView(
                            movement: movement,
                            productName: productName(for: movement.productId),
                            siteName: siteName(for: movement.siteId)
                        )
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle(Localized.stockMovements)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button {
                    showingCreation = true
                } label: {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $showingCreation) {
            StockMovementCreationView(sdk: sdk, siteId: siteId)
        }
        .refreshable {
            await loadData()
        }
        .task {
            await loadData()
        }
        .onChange(of: showingCreation) { isShowing in
            if !isShowing {
                // Refresh after creation
                Task {
                    await loadData()
                }
            }
        }
    }

    @MainActor
    private func loadData() async {
        isLoading = true
        errorMessage = nil
        do {
            async let movementsResult = sdk.stockMovementRepository.getAll()
            async let productsResult = sdk.productRepository.getAll()
            async let sitesResult = sdk.siteRepository.getAll()

            movements = try await movementsResult
            products = try await productsResult
            sites = try await sitesResult
        } catch {
            errorMessage = "Error: \(error.localizedDescription)"
        }
        isLoading = false
    }

    private func productName(for productId: String) -> String {
        products.first { $0.id == productId }?.name ?? Localized.unknownProduct
    }

    private func siteName(for siteId: String) -> String {
        sites.first { $0.id == siteId }?.name ?? Localized.unknownSite
    }
}

// MARK: - Stock Movement Row View
struct StockMovementRowView: View {
    let movement: StockMovement
    let productName: String
    let siteName: String

    var movementTypeLabel: String {
        let type = movement.movementType ?? movement.type
        switch type {
        case "PURCHASE": return Localized.purchase
        case "SALE": return Localized.sale
        case "TRANSFER_IN": return Localized.strings.transferIn
        case "TRANSFER_OUT": return Localized.strings.transferOut
        case "ADJUSTMENT": return Localized.stockAdjustment
        case "INVENTORY": return Localized.inventory
        default: return type
        }
    }

    var movementColor: Color {
        movement.quantity >= 0 ? .green : .red
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(productName)
                    .font(.headline)
                Spacer()
                Text("\(movement.quantity >= 0 ? "+" : "")\(String(format: "%.1f", movement.quantity))")
                    .font(.headline)
                    .foregroundColor(movementColor)
            }

            HStack {
                Text(movementTypeLabel)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 2)
                    .background(movementColor.opacity(0.2))
                    .cornerRadius(4)
                Spacer()
                Text(formatDate(movement.createdAt))
            }
            .font(.caption)

            Text("\(Localized.site): \(siteName)")
                .font(.caption)
                .foregroundColor(.secondary)

            if let notes = movement.notes, !notes.isEmpty {
                Text(notes)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .lineLimit(2)
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

extension StockMovement: @retroactive Identifiable {}
extension CurrentStock: @retroactive Identifiable {
    public var id: String { "\(productId)-\(siteId)" }
}

// MARK: - Stock Movement Creation View
struct StockMovementCreationView: View {
    let sdk: MedistockSDK
    let siteId: String?

    @Environment(\.dismiss) private var dismiss

    @State private var products: [Product] = []
    @State private var packagingTypes: [PackagingType] = []
    @State private var selectedProduct: Product?
    @State private var movementType: MovementTypeOption = .stockIn
    @State private var quantity: String = ""
    @State private var notes: String = ""
    @State private var isLoading = true
    @State private var isSaving = false
    @State private var errorMessage: String?
    @State private var showingWarning = false
    @State private var warningMessage: String = ""

    /// Derives the unit name from the product's packaging type and selected level
    func derivedUnit(for product: Product) -> String {
        guard let packagingType = packagingTypes.first(where: { $0.id == product.packagingTypeId }) else {
            return "unit"
        }
        let selectedLevel = Int(product.selectedLevel)
        if selectedLevel == 2, let level2Name = packagingType.level2Name {
            return level2Name
        }
        return packagingType.level1Name
    }

    enum MovementTypeOption: CaseIterable {
        case stockIn
        case stockOut

        var displayName: String {
            switch self {
            case .stockIn: return Localized.stockIn
            case .stockOut: return Localized.stockOut
            }
        }

        var movementTypeValue: String {
            switch self {
            case .stockIn: return "MANUAL_IN"
            case .stockOut: return "MANUAL_OUT"
            }
        }

        var icon: String {
            switch self {
            case .stockIn: return "arrow.down.circle.fill"
            case .stockOut: return "arrow.up.circle.fill"
            }
        }

        var color: Color {
            switch self {
            case .stockIn: return .green
            case .stockOut: return .red
            }
        }
    }

    var body: some View {
        NavigationView {
            Form {
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
                } else {
                    // Product selection
                    Section(header: Text(Localized.product)) {
                        Picker(Localized.selectProduct, selection: $selectedProduct) {
                            Text(Localized.chooseProduct).tag(nil as Product?)
                            ForEach(products, id: \.id) { product in
                                Text("\(product.name) (\(derivedUnit(for: product)))")
                                    .tag(product as Product?)
                            }
                        }
                        .pickerStyle(.menu)
                    }

                    // Movement type
                    Section(header: Text(Localized.movementType)) {
                        Picker(Localized.movementType, selection: $movementType) {
                            ForEach(MovementTypeOption.allCases, id: \.self) { type in
                                Label(type.displayName, systemImage: type.icon)
                                    .tag(type)
                            }
                        }
                        .pickerStyle(.segmented)
                    }

                    // Quantity
                    Section(header: Text(Localized.quantity)) {
                        HStack {
                            TextField(Localized.quantity, text: $quantity)
                                .keyboardType(.decimalPad)

                            if let product = selectedProduct {
                                Text(derivedUnit(for: product))
                                    .foregroundColor(.secondary)
                            }
                        }
                    }

                    // Notes (optional)
                    Section(header: Text(Localized.notes)) {
                        TextEditor(text: $notes)
                            .frame(minHeight: 60)
                            .overlay(
                                Group {
                                    if notes.isEmpty {
                                        Text(Localized.addNote)
                                            .foregroundColor(.secondary)
                                            .padding(.horizontal, 4)
                                            .padding(.vertical, 8)
                                    }
                                },
                                alignment: .topLeading
                            )
                    }

                    // Preview
                    if let product = selectedProduct, let qty = Double(quantity.replacingOccurrences(of: ",", with: ".")), qty > 0 {
                        Section(header: Text(Localized.preview)) {
                            HStack {
                                Image(systemName: movementType.icon)
                                    .foregroundColor(movementType.color)
                                Text(product.name)
                                Spacer()
                                Text("\(movementType == .stockIn ? "+" : "-")\(String(format: "%.1f", qty)) \(derivedUnit(for: product))")
                                    .foregroundColor(movementType.color)
                                    .fontWeight(.semibold)
                            }
                        }
                    }
                }
            }
            .navigationTitle(Localized.stockMovements)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(Localized.cancel) {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(Localized.save) {
                        Task {
                            await saveMovement()
                        }
                    }
                    .disabled(!canSave || isSaving)
                }
            }
            .task {
                await loadProducts()
            }
            .alert(Localized.warning, isPresented: $showingWarning) {
                Button(Localized.`continue`, role: .destructive) {
                    Task {
                        await performSave()
                    }
                }
                Button(Localized.cancel, role: .cancel) {}
            } message: {
                Text(warningMessage)
            }
            .overlay {
                if isSaving {
                    Color.black.opacity(0.3)
                        .ignoresSafeArea()
                    ProgressView(Localized.saving)
                        .padding()
                        .background(Color(.systemBackground))
                        .cornerRadius(10)
                }
            }
        }
    }

    private var canSave: Bool {
        guard let _ = selectedProduct,
              let qty = Double(quantity.replacingOccurrences(of: ",", with: ".")),
              qty > 0 else {
            return false
        }
        return true
    }

    @MainActor
    private func loadProducts() async {
        isLoading = true
        errorMessage = nil
        do {
            async let allProductsResult = sdk.productRepository.getAll()
            async let packagingTypesResult = sdk.packagingTypeRepository.getAll()

            let allProducts = try await allProductsResult
            packagingTypes = try await packagingTypesResult

            // Filter by site if needed
            if let siteId = siteId {
                products = allProducts.filter { $0.siteId == siteId }
            } else {
                products = allProducts
            }
        } catch {
            errorMessage = "Error: \(error.localizedDescription)"
        }
        isLoading = false
    }

    @MainActor
    private func saveMovement() async {
        guard let product = selectedProduct,
              let qty = Double(quantity.replacingOccurrences(of: ",", with: ".")),
              qty > 0 else {
            return
        }

        // For stock out, check if there's enough stock (warning only, not blocking)
        if movementType == .stockOut {
            do {
                let allStock = try await sdk.stockRepository.getAllCurrentStock()
                let currentStock = allStock.first { $0.productId == product.id && $0.siteId == product.siteId }
                let stockLevel = currentStock?.totalStock ?? 0

                if stockLevel < qty {
                    let unit = derivedUnit(for: product)
                    warningMessage = "Current stock (\(String(format: "%.1f", stockLevel)) \(unit)) is less than the requested quantity (\(String(format: "%.1f", qty)) \(unit)).\n\nStock will become negative. Do you want to continue?"
                    showingWarning = true
                    return
                }
            } catch {
                // If we can't check stock, proceed anyway
                debugLog("StockMovementCreation", "Could not check stock: \(error)")
            }
        }

        await performSave()
    }

    @MainActor
    private func performSave() async {
        guard let product = selectedProduct,
              let qty = Double(quantity.replacingOccurrences(of: ",", with: ".")),
              qty > 0 else {
            return
        }

        isSaving = true
        errorMessage = nil

        do {
            let userId = SessionManager.shared.userId.isEmpty ? "ios" : SessionManager.shared.userId

            // Calculate quantity with sign based on movement type
            // unitVolume conversion like Android
            let quantityInBaseUnit = qty * product.unitVolume
            let finalQuantity = movementType == .stockIn ? quantityInBaseUnit : -quantityInBaseUnit

            // Create movement using SDK
            let movement = sdk.createStockMovement(
                productId: product.id,
                siteId: product.siteId,
                quantity: finalQuantity,
                movementType: movementType.movementTypeValue,
                purchasePriceAtMovement: 0.0,
                sellingPriceAtMovement: 0.0,
                referenceId: nil,
                notes: notes.isEmpty ? nil : notes,
                userId: userId
            )

            // Save to database
            try await sdk.stockMovementRepository.insert(movement: movement)

            // Enqueue for sync if online
            if SyncStatusManager.shared.isOnline && SupabaseService.shared.isConfigured {
                SyncQueueHelper.shared.enqueueStockMovementInsert(
                    movement,
                    userId: userId,
                    siteId: product.siteId
                )
            }

            dismiss()
        } catch {
            errorMessage = "Error saving: \(error.localizedDescription)"
        }

        isSaving = false
    }
}
