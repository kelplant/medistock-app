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
                        title: "Aucune vente",
                        message: "Enregistrez votre premiere vente."
                    )
                }
                .listRowSeparator(.hidden)
            } else {
                Section(header: Text("\(sales.count) vente(s)")) {
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
        .navigationTitle("Ventes")
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
            errorMessage = "Erreur: \(error.localizedDescription)"
        }
        isLoading = false
    }

    private func siteName(for siteId: String) -> String {
        sites.first { $0.id == siteId }?.name ?? "Site inconnu"
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
                Text("Site: \(siteName)")
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
                Section(header: Text("Informations")) {
                    LabeledContentCompat {
                        Text("Client")
                    } content: {
                        Text(sale.customerName)
                    }
                    LabeledContentCompat {
                        Text("Total")
                    } content: {
                        Text(String(format: "%.2f EUR", sale.totalAmount))
                    }
                    LabeledContentCompat {
                        Text("Date")
                    } content: {
                        Text(formatDate(sale.date))
                    }
                }

                if let items = saleWithItems?.items, !items.isEmpty {
                    Section(header: Text("Articles (\(items.count))")) {
                        ForEach(items, id: \.id) { item in
                            VStack(alignment: .leading, spacing: 4) {
                                Text(productName(for: item.productId))
                                    .font(.headline)
                                HStack {
                                    Text("Qte: \(String(format: "%.1f", item.quantity))")
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
            .navigationTitle("Detail de la vente")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Fermer") { dismiss() }
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
        products.first { $0.id == productId }?.name ?? "Produit inconnu"
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
                Section(header: Text("Site")) {
                    Picker("Site", selection: $selectedSiteId) {
                        Text("Selectionner").tag("")
                        ForEach(sites, id: \.id) { site in
                            Text(site.name).tag(site.id)
                        }
                    }
                }

                Section(header: Text("Client")) {
                    TextField("Nom du client", text: $customerName)
                    if !customers.isEmpty {
                        Picker("Ou selectionner", selection: $selectedCustomerId) {
                            Text("Client occasionnel").tag("")
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
                    Text("Articles")
                    Spacer()
                    Button(action: { showAddItem = true }) {
                        Image(systemName: "plus.circle")
                    }
                    .disabled(selectedSiteId.isEmpty)
                }) {
                    if saleItems.isEmpty {
                        Text("Ajoutez des articles a la vente")
                            .foregroundColor(.secondary)
                    } else {
                        ForEach(saleItems.indices, id: \.self) { index in
                            let item = saleItems[index]
                            VStack(alignment: .leading, spacing: 4) {
                                Text(item.productName)
                                    .font(.headline)
                                HStack {
                                    Text("Qte: \(String(format: "%.1f", item.quantity))")
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
                        Text("Total")
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
            .navigationTitle("Nouvelle vente")
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Annuler") { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Enregistrer") {
                        saveSale()
                    }
                    .disabled(!canSave || isSaving)
                }
            }
            .sheet(isPresented: $showAddItem) {
                SaleItemEditorView(products: filteredProducts, batches: batches.filter { $0.siteId == selectedSiteId }) { item in
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

            sites = try await sitesResult
            products = try await productsResult
            customers = try await customersResult
            batches = try await batchesResult

            selectedSiteId = defaultSiteId ?? sites.first?.id ?? ""
        } catch {
            errorMessage = "Erreur lors du chargement"
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
                    unitPrice: draft.unitPrice
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
                    errorMessage = "Erreur: \(error.localizedDescription)"
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
    var totalPrice: Double { quantity * unitPrice }
}

// MARK: - Sale Item Editor View
struct SaleItemEditorView: View {
    let products: [Product]
    let batches: [PurchaseBatch]
    let onAdd: (SaleItemDraft) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var selectedProductId: String = ""
    @State private var quantityText: String = ""
    @State private var priceText: String = ""

    var selectedProduct: Product? {
        products.first { $0.id == selectedProductId }
    }

    var availableStock: Double {
        batches
            .filter { $0.productId == selectedProductId && !$0.isExhausted }
            .reduce(0) { $0 + $1.remainingQuantity }
    }

    var suggestedPrice: Double {
        guard let batch = batches.first(where: { $0.productId == selectedProductId && !$0.isExhausted }) else {
            return 0
        }
        // Simple margin of 30%
        return batch.purchasePrice * 1.3
    }

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Produit")) {
                    Picker("Produit", selection: $selectedProductId) {
                        Text("Selectionner").tag("")
                        ForEach(products, id: \.id) { product in
                            Text(product.name).tag(product.id)
                        }
                    }
                    .onChange(of: selectedProductId) { _ in
                        if suggestedPrice > 0 {
                            priceText = String(format: "%.2f", suggestedPrice)
                        }
                    }

                    if !selectedProductId.isEmpty {
                        LabeledContentCompat {
                            Text("Stock disponible")
                        } content: {
                            Text(String(format: "%.1f", availableStock))
                        }
                    }
                }

                Section(header: Text("Quantite et Prix")) {
                    TextField("Quantite", text: $quantityText)
                        .keyboardType(.decimalPad)
                    TextField("Prix unitaire", text: $priceText)
                        .keyboardType(.decimalPad)
                }

                if let product = selectedProduct {
                    Section(header: Text("Resume")) {
                        let qty = Double(quantityText.replacingOccurrences(of: ",", with: ".")) ?? 0
                        let price = Double(priceText.replacingOccurrences(of: ",", with: ".")) ?? 0
                        LabeledContentCompat {
                            Text("Produit")
                        } content: {
                            Text(product.name)
                        }
                        LabeledContentCompat {
                            Text("Total")
                        } content: {
                            Text(String(format: "%.2f EUR", qty * price))
                        }
                    }
                }
            }
            .navigationTitle("Ajouter un article")
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Annuler") { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Ajouter") {
                        addItem()
                    }
                    .disabled(!canAdd)
                }
            }
        }
    }

    private var canAdd: Bool {
        guard !selectedProductId.isEmpty,
              let qty = Double(quantityText.replacingOccurrences(of: ",", with: ".")),
              let price = Double(priceText.replacingOccurrences(of: ",", with: ".")),
              qty > 0, price > 0 else {
            return false
        }
        // Note: We allow sales even with insufficient stock (negative stock allowed per business rules)
        // The UseCase will generate a warning but not block the sale
        return true
    }

    private var hasInsufficientStock: Bool {
        guard let qty = Double(quantityText.replacingOccurrences(of: ",", with: ".")) else {
            return false
        }
        return qty > availableStock
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
            unitPrice: price
        )
        onAdd(draft)
        dismiss()
    }
}

extension Sale: @retroactive Identifiable {}
extension SaleItem: @retroactive Identifiable {}
