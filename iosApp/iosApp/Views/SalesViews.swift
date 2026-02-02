import Foundation
import SwiftUI
import shared

// MARK: - Sales List View
struct SalesListView: View {
    let sdk: MedistockSDK
    @ObservedObject var session: SessionManager
    let siteId: String?
    @ObservedObject private var syncStatus = SyncStatusManager.shared

    @State private var sales: [Sale] = []
    @State private var sites: [Site] = []
    @State private var isLoading = true
    @State private var errorMessage: String?
    @State private var showAddSheet = false
    @State private var selectedSale: Sale?

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
            } else if sales.isEmpty {
                Section {
                    EmptyStateView(
                        icon: "cart",
                        title: Localized.noSales,
                        message: Localized.strings.noSalesMessage
                    )
                }
                .listRowSeparator(.hidden)
            } else {
                Section(header: Text("\(sales.count) \(Localized.sales.lowercased())")) {
                    ForEach(sales, id: \.id) { sale in
                        SaleRowView(sale: sale, siteName: siteName(for: sale.siteId))
                            .contentShape(Rectangle())
                            .onTapGesture {
                                selectedSale = sale
                            }
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle(Localized.sales)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { showAddSheet = true }) {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $showAddSheet) {
            SaleEditorView(sdk: sdk, session: session, defaultSiteId: siteId) {
                Task { await loadData() }
            }
        }
        .sheet(item: $selectedSale) { sale in
            SaleDetailView(sdk: sdk, sale: sale)
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
                let remoteSales: [SaleDTO] = try await SupabaseService.shared.fetchAll(from: "sales")
                // Sync to local database using upsert (INSERT OR REPLACE)
                for dto in remoteSales {
                    try? await sdk.saleRepository.upsert(sale: dto.toEntity())
                }
            } catch {
                debugLog("SalesListView", "Failed to sync sales from Supabase: \(error)")
            }
        }

        // Load from local database
        do {
            async let salesResult = sdk.saleRepository.getAll()
            async let sitesResult = sdk.siteRepository.getAll()

            sales = try await salesResult
            sites = try await sitesResult

            if let siteId = siteId {
                sales = sales.filter { $0.siteId == siteId }
            }
        } catch {
            errorMessage = "Error: \(error.localizedDescription)"
        }
        isLoading = false
    }

    private func siteName(for siteId: String) -> String {
        sites.first { $0.id == siteId }?.name ?? Localized.unknownSite
    }
}

// MARK: - Sale Row View
struct SaleRowView: View {
    let sale: Sale
    let siteName: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(sale.customerName)
                    .font(.headline)
                Spacer()
                Text("\(String(format: "%.2f", sale.totalAmount)) EUR")
                    .font(.headline)
                    .foregroundColor(.green)
            }

            HStack {
                Text("\(Localized.site): \(siteName)")
                Spacer()
                Text(formatDate(sale.date))
            }
            .font(.caption)
            .foregroundColor(.secondary)
        }
        .padding(.vertical, 4)
    }

    private func formatDate<T: TimestampConvertible>(_ timestamp: T) -> String {
        let date = Date(timeIntervalSince1970: Double(timestamp.timestampValue) / 1000)
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .short
        return formatter.string(from: date)
    }
}

// MARK: - Sale Detail View
struct SaleDetailView: View {
    let sdk: MedistockSDK
    let sale: Sale

    @Environment(\.dismiss) private var dismiss
    @State private var saleWithItems: SaleWithItems?
    @State private var products: [Product] = []
    @State private var isLoading = true

    var body: some View {
        NavigationView {
            List {
                Section(header: Text(Localized.information)) {
                    LabeledContentCompat {
                        Text(Localized.customer)
                    } content: {
                        Text(sale.customerName)
                    }
                    LabeledContentCompat {
                        Text(Localized.total)
                    } content: {
                        Text(String(format: "%.2f EUR", sale.totalAmount))
                    }
                    LabeledContentCompat {
                        Text(Localized.date)
                    } content: {
                        Text(formatDate(sale.date))
                    }
                }

                if let items = saleWithItems?.items, !items.isEmpty {
                    Section(header: Text("\(Localized.items) (\(items.count))")) {
                        ForEach(items, id: \.id) { item in
                            VStack(alignment: .leading, spacing: 4) {
                                Text(productName(for: item.productId))
                                    .font(.headline)
                                HStack {
                                    Text("\(Localized.quantity): \(String(format: "%.1f", item.quantity))")
                                    Spacer()
                                    Text("\(String(format: "%.2f", item.unitPrice)) EUR x \(String(format: "%.1f", item.quantity))")
                                    Text("= \(String(format: "%.2f", item.totalPrice)) EUR")
                                        .fontWeight(.medium)
                                }
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                            }
                            .padding(.vertical, 2)
                        }
                    }
                }
            }
            .navigationTitle(Localized.saleDetails)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(Localized.close) { dismiss() }
                }
            }
            .task {
                await loadData()
            }
        }
    }

    @MainActor
    private func loadData() async {
        do {
            async let saleResult = sdk.saleRepository.getSaleWithItems(saleId: sale.id)
            async let productsResult = sdk.productRepository.getAll()

            saleWithItems = try await saleResult
            products = try await productsResult
        } catch {
            // Handle error
        }
        isLoading = false
    }

    private func productName(for productId: String) -> String {
        products.first { $0.id == productId }?.name ?? Localized.unknownProduct
    }

    private func formatDate<T: TimestampConvertible>(_ timestamp: T) -> String {
        let date = Date(timeIntervalSince1970: Double(timestamp.timestampValue) / 1000)
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .short
        return formatter.string(from: date)
    }
}

// MARK: - Sale Editor View
struct SaleEditorView: View {
    let sdk: MedistockSDK
    @ObservedObject var session: SessionManager
    let defaultSiteId: String?
    let onSave: () -> Void

    @ObservedObject private var syncStatus = SyncStatusManager.shared
    @Environment(\.dismiss) private var dismiss
    @State private var sites: [Site] = []
    @State private var products: [Product] = []
    @State private var customers: [Customer] = []
    @State private var batches: [PurchaseBatch] = []
    @State private var packagingTypes: [PackagingType] = []

    @State private var selectedSiteId: String = ""
    @State private var customerName: String = ""
    @State private var selectedCustomerId: String = ""
    @State private var saleItems: [SaleItemDraft] = []
    @State private var showAddItem = false
    @State private var isSaving = false
    @State private var errorMessage: String?

    var totalAmount: Double {
        saleItems.reduce(0) { $0 + $1.totalPrice }
    }

    var filteredProducts: [Product] {
        products.filter { $0.siteId == selectedSiteId }
    }

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text(Localized.site)) {
                    Picker(Localized.site, selection: $selectedSiteId) {
                        Text(Localized.select).tag("")
                        ForEach(sites, id: \.id) { site in
                            Text(site.name).tag(site.id)
                        }
                    }
                }

                Section(header: Text(Localized.customer)) {
                    TextField(Localized.customerName, text: $customerName)
                    if !customers.isEmpty {
                        Picker(Localized.orSelect, selection: $selectedCustomerId) {
                            Text(Localized.walkInCustomer).tag("")
                            ForEach(customers, id: \.id) { customer in
                                Text(customer.name).tag(customer.id)
                            }
                        }
                        .onChange(of: selectedCustomerId) { newValue in
                            if let customer = customers.first(where: { $0.id == newValue }) {
                                customerName = customer.name
                            }
                        }
                    }
                }

                Section(header: HStack {
                    Text(Localized.items)
                    Spacer()
                    Button(action: { showAddItem = true }) {
                        Image(systemName: "plus.circle")
                    }
                    .disabled(selectedSiteId.isEmpty)
                }) {
                    if saleItems.isEmpty {
                        Text(Localized.noSaleItems)
                            .foregroundColor(.secondary)
                    } else {
                        ForEach(saleItems.indices, id: \.self) { index in
                            let item = saleItems[index]
                            VStack(alignment: .leading, spacing: 4) {
                                Text(item.productName)
                                    .font(.headline)
                                HStack {
                                    Text("\(Localized.quantity): \(String(format: "%.1f", item.quantity))")
                                    Text("x \(String(format: "%.2f", item.unitPrice)) EUR")
                                    Spacer()
                                    Text("\(String(format: "%.2f", item.totalPrice)) EUR")
                                        .fontWeight(.medium)
                                }
                                .font(.subheadline)
                            }
                        }
                        .onDelete(perform: deleteItems)
                    }
                }

                Section {
                    HStack {
                        Text(Localized.total)
                            .font(.headline)
                        Spacer()
                        Text("\(String(format: "%.2f", totalAmount)) EUR")
                            .font(.title2)
                            .fontWeight(.bold)
                            .foregroundColor(.green)
                    }
                }

                if let errorMessage {
                    Section {
                        Text(errorMessage)
                            .foregroundColor(.red)
                    }
                }
            }
            .navigationTitle(Localized.newSale)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(Localized.cancel) { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(Localized.save) {
                        saveSale()
                    }
                    .disabled(!canSave || isSaving)
                }
            }
            .sheet(isPresented: $showAddItem) {
                SaleItemEditorView(products: filteredProducts, batches: batches.filter { $0.siteId == selectedSiteId }, packagingTypes: packagingTypes) { item in
                    saleItems.append(item)
                }
            }
            .task {
                await loadData()
            }
            .onChange(of: selectedSiteId) { _ in
                saleItems.removeAll()
            }
        }
    }

    private var canSave: Bool {
        !selectedSiteId.isEmpty &&
        !customerName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
        !saleItems.isEmpty
    }

    @MainActor
    private func loadData() async {
        do {
            async let sitesResult = sdk.siteRepository.getAll()
            async let productsResult = sdk.productRepository.getAll()
            async let customersResult = sdk.customerRepository.getAll()
            async let batchesResult = sdk.purchaseBatchRepository.getAll()
            async let packagingTypesResult = sdk.packagingTypeRepository.getAll()

            sites = try await sitesResult
            products = try await productsResult
            customers = try await customersResult
            batches = try await batchesResult
            packagingTypes = try await packagingTypesResult

            selectedSiteId = defaultSiteId ?? sites.first?.id ?? ""
        } catch {
            errorMessage = Localized.error
        }
    }

    private func deleteItems(at offsets: IndexSet) {
        saleItems.remove(atOffsets: offsets)
    }

    private func saveSale() {
        isSaving = true
        errorMessage = nil

        Task {
            // Convert sale items to UseCase input format
            let itemInputs = saleItems.map { draft in
                SaleItemInput(
                    productId: draft.productId,
                    quantity: draft.quantity,
                    unitPrice: draft.unitPrice,
                    selectedLevel: draft.selectedLevel,
                    conversionFactor: draft.conversionFactor.map { KotlinDouble(double: $0) },
                    batchId: draft.batchId
                )
            }

            // Use SaleUseCase for business logic (handles FIFO allocation, stock movements, etc.)
            let input = SaleInput(
                siteId: selectedSiteId,
                customerName: customerName.trimmingCharacters(in: .whitespacesAndNewlines),
                customerId: selectedCustomerId.isEmpty ? nil : selectedCustomerId,
                items: itemInputs,
                userId: session.userId
            )

            do {
                let result = try await sdk.saleUseCase.execute(input: input)

                // Handle result
                if let success = result as? UseCaseResultSuccess<SaleResult>,
               let saleResult = success.data {
                let sale = saleResult.sale

                // Show warnings if any (e.g., insufficient stock - still allowed per business rules)
                if !success.warnings.isEmpty {
                    for warning in success.warnings {
                        if let stockWarning = warning as? BusinessWarning.InsufficientStock {
                            debugLog("SaleEditorView", "Warning: Insufficient stock for product \(stockWarning.productId): requested \(stockWarning.requested), available \(stockWarning.available)")
                        }
                    }
                }

                // Sync to Supabase
                var savedOnline = false
                if syncStatus.isOnline && SupabaseService.shared.isConfigured {
                    do {
                        let saleDTO = SaleDTO(from: sale)
                        try await SupabaseService.shared.upsert(into: "sales", record: saleDTO)

                        for processedItem in saleResult.items {
                            let itemDTO = SaleItemDTO(from: processedItem.saleItem)
                            try await SupabaseService.shared.upsert(into: "sale_items", record: itemDTO)
                        }
                        savedOnline = true
                    } catch {
                        debugLog("SaleEditorView", "Failed to save to Supabase: \(error)")
                    }
                }

                // Queue for sync if not saved online
                if !savedOnline {
                    let saleDTO = SaleDTO(from: sale)
                    SyncQueueHelper.shared.enqueueInsert(
                        entityType: .sale,
                        entityId: sale.id,
                        entity: saleDTO,
                        userId: session.userId,
                        siteId: selectedSiteId
                    )
                }

                await MainActor.run {
                    onSave()
                    dismiss()
                }

            } else if let error = result as? UseCaseResultError {
                await MainActor.run {
                    isSaving = false
                    errorMessage = error.error.message
                }
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

// MARK: - Sale Item Draft
struct SaleItemDraft {
    let productId: String
    let productName: String
    let quantity: Double
    let unitPrice: Double
    let selectedLevel: Int32
    let conversionFactor: Double?
    let batchId: String?
    var totalPrice: Double { quantity * unitPrice }
    var baseQuantity: Double? {
        if selectedLevel == 2, let cf = conversionFactor {
            return quantity * cf
        }
        return nil
    }
}

// MARK: - Sale Item Editor View
struct SaleItemEditorView: View {
    let products: [Product]
    let batches: [PurchaseBatch]
    let packagingTypes: [PackagingType]
    let onAdd: (SaleItemDraft) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var selectedProductId: String = ""
    @State private var selectedLevel: Int32 = 1
    @State private var selectedBatchId: String = ""
    @State private var quantityText: String = ""
    @State private var priceText: String = ""

    var selectedProduct: Product? {
        products.first { $0.id == selectedProductId }
    }

    var selectedPackagingType: PackagingType? {
        guard let product = selectedProduct else { return nil }
        return packagingTypes.first { $0.id == product.packagingTypeId }
    }

    var productHasTwoLevels: Bool {
        selectedPackagingType?.level2Name != nil
    }

    var currentLevelName: String {
        guard let pt = selectedPackagingType else { return Localized.unit }
        if selectedLevel == 2, let l2 = pt.level2Name {
            return l2
        }
        return pt.level1Name
    }

    var productConversionFactor: Double? {
        selectedProduct?.conversionFactor?.doubleValue
    }

    // Batches sorted with smart suggestion: expiring soon first, then FIFO
    var availableBatchesSorted: [PurchaseBatch] {
        let productBatches = batches.filter { $0.productId == selectedProductId && !$0.isExhausted && $0.remainingQuantity > 0 }
        let now = Date()
        let thirtyDaysFromNow = Calendar.current.date(byAdding: .day, value: 30, to: now)!
        let thresholdMs = Int64(thirtyDaysFromNow.timeIntervalSince1970 * 1000)

        let expiringSoon = productBatches
            .filter { batch in
                guard let expiryDate = batch.expiryDate?.int64Value else { return false }
                return expiryDate < thresholdMs
            }
            .sorted { a, b in
                (a.expiryDate?.int64Value ?? 0) < (b.expiryDate?.int64Value ?? 0)
            }

        let rest = productBatches
            .filter { batch in
                if let expiryDate = batch.expiryDate?.int64Value {
                    return expiryDate >= thresholdMs
                }
                return true
            }
            .sorted { $0.purchaseDate < $1.purchaseDate }

        return expiringSoon + rest
    }

    var selectedBatch: PurchaseBatch? {
        batches.first { $0.id == selectedBatchId }
    }

    var selectedBatchExpiresSoon: Bool {
        guard let batch = selectedBatch,
              let expiryDate = batch.expiryDate?.int64Value else { return false }
        let now = Date()
        let thirtyDaysFromNow = Calendar.current.date(byAdding: .day, value: 30, to: now)!
        let thresholdMs = Int64(thirtyDaysFromNow.timeIntervalSince1970 * 1000)
        return expiryDate < thresholdMs
    }

    var selectedBatchExpiryDateFormatted: String {
        guard let batch = selectedBatch,
              let expiryMs = batch.expiryDate?.int64Value else { return "" }
        let date = Date(timeIntervalSince1970: Double(expiryMs) / 1000)
        let formatter = DateFormatter()
        formatter.dateFormat = "dd/MM/yyyy"
        return formatter.string(from: date)
    }

    // Stock is always in level 1 units
    var availableStockBase: Double {
        batches
            .filter { $0.productId == selectedProductId && !$0.isExhausted }
            .reduce(0) { $0 + $1.remainingQuantity }
    }

    // Available stock displayed in the selected level's unit
    var availableStockDisplay: Double {
        if selectedLevel == 2, let cf = productConversionFactor, cf > 0 {
            return availableStockBase / cf
        }
        return availableStockBase
    }

    // Purchase price per base unit (level 1) from the selected batch, or oldest non-exhausted
    var purchasePricePerBaseUnit: Double {
        if !selectedBatchId.isEmpty, let batch = selectedBatch {
            return batch.purchasePrice
        }
        guard let batch = batches.first(where: { $0.productId == selectedProductId && !$0.isExhausted }) else {
            return 0
        }
        return batch.purchasePrice
    }

    // Purchase price adjusted for the selected level
    var purchasePriceForLevel: Double {
        if selectedLevel == 2, let cf = productConversionFactor {
            return purchasePricePerBaseUnit * cf
        }
        return purchasePricePerBaseUnit
    }

    // Suggested selling price based on product's margin settings
    var suggestedPrice: Double {
        let cost = purchasePriceForLevel
        guard cost > 0 else { return 0 }

        guard let product = selectedProduct else { return cost }

        let marginType = product.marginType ?? "PERCENTAGE"
        let marginValue = product.marginValue?.doubleValue ?? 0.0

        switch marginType.lowercased() {
        case "fixed":
            // Fixed margin is per base unit; scale by conversionFactor for level 2
            if selectedLevel == 2, let cf = productConversionFactor {
                return cost + marginValue * cf
            }
            return cost + marginValue
        case "percentage":
            if marginValue > 0 {
                return cost * (1.0 + marginValue / 100.0)
            }
            return cost
        default:
            return cost
        }
    }

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text(Localized.product)) {
                    Picker(Localized.product, selection: $selectedProductId) {
                        Text(Localized.select).tag("")
                        ForEach(products, id: \.id) { product in
                            Text(product.name).tag(product.id)
                        }
                    }
                    .onChange(of: selectedProductId) { _ in
                        // Reset level to product's default
                        if let product = selectedProduct {
                            selectedLevel = product.selectedLevel
                        } else {
                            selectedLevel = 1
                        }
                        // Auto-select first batch from smart-sorted list
                        selectedBatchId = availableBatchesSorted.first?.id ?? ""
                        updateSuggestedPrice()
                    }

                    if !selectedProductId.isEmpty {
                        // Level picker (only shown for products with two packaging levels)
                        if productHasTwoLevels, let pt = selectedPackagingType {
                            Picker(Localized.unit, selection: $selectedLevel) {
                                Text(pt.level1Name).tag(Int32(1))
                                if let l2 = pt.level2Name {
                                    Text(l2).tag(Int32(2))
                                }
                            }
                            .pickerStyle(.segmented)
                            .onChange(of: selectedLevel) { _ in
                                updateSuggestedPrice()
                            }

                            if selectedLevel == 2, let cf = productConversionFactor {
                                LabeledContentCompat {
                                    Text("1 \(pt.level2Name ?? "") =")
                                } content: {
                                    Text("\(String(format: "%.0f", cf)) \(pt.level1Name)")
                                }
                                .font(.caption)
                                .foregroundColor(.secondary)
                            }
                        }

                        LabeledContentCompat {
                            Text(Localized.availableStock)
                        } content: {
                            Text("\(String(format: "%.1f", availableStockDisplay)) \(currentLevelName)")
                        }

                        // Batch picker
                        if !availableBatchesSorted.isEmpty {
                            Picker("Lot", selection: $selectedBatchId) {
                                ForEach(availableBatchesSorted, id: \.id) { batch in
                                    Text("Lot: \(batch.batchNumber ?? "N/A") - \(String(format: "%.0f", batch.purchasePrice)) FCFA (reste: \(String(format: "%.1f", batch.remainingQuantity)))").tag(batch.id)
                                }
                            }
                            .onChange(of: selectedBatchId) { _ in
                                updateSuggestedPrice()
                            }

                            if selectedBatchExpiresSoon {
                                Text("Lot proche de l'expiration (expire le \(selectedBatchExpiryDateFormatted))")
                                    .font(.caption)
                                    .foregroundColor(.orange)
                            }
                        }

                        // Show purchase price info
                        if purchasePriceForLevel > 0 {
                            LabeledContentCompat {
                                Text(Localized.purchasePrice)
                            } content: {
                                Text("\(String(format: "%.2f", purchasePriceForLevel)) EUR/\(currentLevelName)")
                            }
                            .foregroundColor(.secondary)

                            if let product = selectedProduct {
                                let marginType = product.marginType ?? "PERCENTAGE"
                                let marginValue = product.marginValue?.doubleValue ?? 0.0
                                if marginValue > 0 {
                                    LabeledContentCompat {
                                        Text("Marge")
                                    } content: {
                                        if marginType.lowercased() == "fixed" {
                                            Text("+\(String(format: "%.2f", marginValue)) EUR")
                                        } else {
                                            Text("+\(String(format: "%.0f", marginValue))%")
                                        }
                                    }
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                }
                            }
                        }
                    }
                }

                Section(header: Text("\(Localized.quantity) & \(Localized.price)")) {
                    HStack {
                        TextField(Localized.quantity, text: $quantityText)
                            .keyboardType(.decimalPad)
                        Text(currentLevelName)
                            .foregroundColor(.secondary)
                    }

                    // Level 2 validation: quantity must not exceed conversionFactor
                    if selectedLevel == 2, let cf = productConversionFactor,
                       let qty = Double(quantityText.replacingOccurrences(of: ",", with: ".")),
                       qty > cf {
                        Text("Quantite max: \(String(format: "%.0f", cf)) \(selectedPackagingType?.level2Name ?? "")")
                            .font(.caption)
                            .foregroundColor(.red)
                    }

                    // Non-blocking stock warning
                    if hasInsufficientStock {
                        Text("Stock insuffisant (disponible: \(String(format: "%.1f", availableStockDisplay)) \(currentLevelName))")
                            .font(.caption)
                            .foregroundColor(.orange)
                    }

                    HStack {
                        TextField(Localized.unitPrice, text: $priceText)
                            .keyboardType(.decimalPad)
                        Text("EUR/\(currentLevelName)")
                            .foregroundColor(.secondary)
                    }
                }

                if let product = selectedProduct {
                    Section(header: Text(Localized.summary)) {
                        let qty = Double(quantityText.replacingOccurrences(of: ",", with: ".")) ?? 0
                        let price = Double(priceText.replacingOccurrences(of: ",", with: ".")) ?? 0
                        LabeledContentCompat {
                            Text(Localized.product)
                        } content: {
                            Text(product.name)
                        }
                        LabeledContentCompat {
                            Text(Localized.total)
                        } content: {
                            Text(String(format: "%.2f EUR", qty * price))
                        }
                        // Show base quantity equivalent for level 2 sales
                        if selectedLevel == 2, let cf = productConversionFactor, let pt = selectedPackagingType {
                            LabeledContentCompat {
                                Text("= \(String(format: "%.0f", qty * cf)) \(pt.level1Name)")
                            } content: {
                                Text("")
                            }
                            .font(.caption)
                            .foregroundColor(.secondary)
                        }
                    }
                }
            }
            .navigationTitle(Localized.addItem)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(Localized.cancel) { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(Localized.add) {
                        addItem()
                    }
                    .disabled(!canAdd)
                }
            }
        }
    }

    private func updateSuggestedPrice() {
        if suggestedPrice > 0 {
            priceText = String(format: "%.2f", suggestedPrice)
        }
    }

    private var canAdd: Bool {
        guard !selectedProductId.isEmpty,
              let qty = Double(quantityText.replacingOccurrences(of: ",", with: ".")),
              let price = Double(priceText.replacingOccurrences(of: ",", with: ".")),
              qty > 0, price > 0 else {
            return false
        }
        // Level 2 validation: quantity must not exceed conversionFactor
        if selectedLevel == 2, let cf = productConversionFactor, qty > cf {
            return false
        }
        return true
    }

    private var hasInsufficientStock: Bool {
        guard let qty = Double(quantityText.replacingOccurrences(of: ",", with: ".")) else {
            return false
        }
        return qty > availableStockDisplay
    }

    private func addItem() {
        guard let product = selectedProduct,
              let qty = Double(quantityText.replacingOccurrences(of: ",", with: ".")),
              let price = Double(priceText.replacingOccurrences(of: ",", with: ".")) else {
            return
        }

        let draft = SaleItemDraft(
            productId: product.id,
            productName: product.name,
            quantity: qty,
            unitPrice: price,
            selectedLevel: selectedLevel,
            conversionFactor: productConversionFactor,
            batchId: selectedBatchId.isEmpty ? nil : selectedBatchId
        )
        onAdd(draft)
        dismiss()
    }
}

extension Sale: @retroactive Identifiable {}
extension SaleItem: @retroactive Identifiable {}
