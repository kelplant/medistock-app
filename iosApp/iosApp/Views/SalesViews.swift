import Foundation
import SwiftUI
import shared

// MARK: - Sales List View
struct SalesListView: View {
    let sdk: MedistockSDK
    @ObservedObject var session: SessionManager
    let siteId: String?

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
                        message: "Enregistrez votre première vente."
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
                Text("\(String(format: "%.2f", sale.totalAmount)) €")
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

    private func formatDate(_ timestamp: Int64) -> String {
        let date = Date(timeIntervalSince1970: Double(timestamp) / 1000)
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
                    LabeledContent("Client", value: sale.customerName)
                    LabeledContent("Total", value: String(format: "%.2f €", sale.totalAmount))
                    LabeledContent("Date", value: formatDate(sale.date))
                }

                if let items = saleWithItems?.items, !items.isEmpty {
                    Section(header: Text("Articles (\(items.count))")) {
                        ForEach(items, id: \.id) { item in
                            VStack(alignment: .leading, spacing: 4) {
                                Text(productName(for: item.productId))
                                    .font(.headline)
                                HStack {
                                    Text("Qté: \(String(format: "%.1f", item.quantity))")
                                    Spacer()
                                    Text("\(String(format: "%.2f", item.unitPrice)) € x \(String(format: "%.1f", item.quantity))")
                                    Text("= \(String(format: "%.2f", item.totalPrice)) €")
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
            .navigationTitle("Détail de la vente")
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

    private func formatDate(_ timestamp: Int64) -> String {
        let date = Date(timeIntervalSince1970: Double(timestamp) / 1000)
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
                        Text("Sélectionner").tag("")
                        ForEach(sites, id: \.id) { site in
                            Text(site.name).tag(site.id)
                        }
                    }
                }

                Section(header: Text("Client")) {
                    TextField("Nom du client", text: $customerName)
                    if !customers.isEmpty {
                        Picker("Ou sélectionner", selection: $selectedCustomerId) {
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
                        Text("Ajoutez des articles à la vente")
                            .foregroundColor(.secondary)
                    } else {
                        ForEach(saleItems.indices, id: \.self) { index in
                            let item = saleItems[index]
                            VStack(alignment: .leading, spacing: 4) {
                                Text(item.productName)
                                    .font(.headline)
                                HStack {
                                    Text("Qté: \(String(format: "%.1f", item.quantity))")
                                    Text("x \(String(format: "%.2f", item.unitPrice)) €")
                                    Spacer()
                                    Text("\(String(format: "%.2f", item.totalPrice)) €")
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
                        Text("\(String(format: "%.2f", totalAmount)) €")
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
            do {
                let sale = sdk.createSale(
                    customerName: customerName.trimmingCharacters(in: .whitespacesAndNewlines),
                    siteId: selectedSiteId,
                    totalAmount: totalAmount,
                    customerId: selectedCustomerId.isEmpty ? nil : selectedCustomerId,
                    userId: session.username
                )

                let items = saleItems.map { draft in
                    sdk.createSaleItem(
                        saleId: sale.id,
                        productId: draft.productId,
                        quantity: draft.quantity,
                        unitPrice: draft.unitPrice
                    )
                }

                try await sdk.saleRepository.insertSaleWithItems(sale: sale, items: items)

                // Create stock movements and update batches (FIFO)
                for draft in saleItems {
                    let movement = sdk.createStockMovement(
                        productId: draft.productId,
                        siteId: selectedSiteId,
                        quantity: -draft.quantity,
                        movementType: "SALE",
                        referenceId: sale.id,
                        notes: "Vente à \(customerName)",
                        userId: session.username
                    )
                    try await sdk.stockMovementRepository.insert(movement: movement)

                    // Deduct from batches (FIFO)
                    await deductFromBatches(productId: draft.productId, quantity: draft.quantity)
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

    private func deductFromBatches(productId: String, quantity: Double) async {
        var remainingToDeduct = quantity
        let productBatches = batches
            .filter { $0.productId == productId && $0.siteId == selectedSiteId && !$0.isExhausted }
            .sorted { $0.purchaseDate < $1.purchaseDate }

        for batch in productBatches {
            guard remainingToDeduct > 0 else { break }

            let deductAmount = min(batch.remainingQuantity, remainingToDeduct)
            let newRemaining = batch.remainingQuantity - deductAmount
            let isExhausted = newRemaining <= 0

            do {
                try await sdk.purchaseBatchRepository.updateQuantity(
                    id: batch.id,
                    remainingQuantity: newRemaining,
                    isExhausted: isExhausted,
                    updatedAt: Int64(Date().timeIntervalSince1970 * 1000),
                    updatedBy: session.username
                )
                remainingToDeduct -= deductAmount
            } catch {
                // Continue with next batch
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
                        Text("Sélectionner").tag("")
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
                        LabeledContent("Stock disponible", value: String(format: "%.1f", availableStock))
                    }
                }

                Section(header: Text("Quantité et Prix")) {
                    TextField("Quantité", text: $quantityText)
                        .keyboardType(.decimalPad)
                    TextField("Prix unitaire", text: $priceText)
                        .keyboardType(.decimalPad)
                }

                if let product = selectedProduct {
                    Section(header: Text("Résumé")) {
                        let qty = Double(quantityText.replacingOccurrences(of: ",", with: ".")) ?? 0
                        let price = Double(priceText.replacingOccurrences(of: ",", with: ".")) ?? 0
                        LabeledContent("Produit", value: product.name)
                        LabeledContent("Total", value: String(format: "%.2f €", qty * price))
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
              qty > 0, price > 0, qty <= availableStock else {
            return false
        }
        return true
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
