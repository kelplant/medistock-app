import Foundation
import SwiftUI
import shared

// MARK: - Purchases List View
struct PurchasesListView: View {
    let sdk: MedistockSDK
    @ObservedObject var session: SessionManager
    let siteId: String?
    @ObservedObject private var syncStatus = SyncStatusManager.shared

    @State private var batches: [PurchaseBatch] = []
    @State private var products: [Product] = []
    @State private var sites: [Site] = []
    @State private var isLoading = true
    @State private var errorMessage: String?
    @State private var showAddSheet = false

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
            } else if batches.isEmpty {
                Section {
                    EmptyStateView(
                        icon: "shippingbox",
                        title: Localized.noPurchases,
                        message: Localized.strings.noPurchasesMessage
                    )
                }
                .listRowSeparator(.hidden)
            } else {
                Section(header: Text("\(batches.count) \(Localized.strings.batches)")) {
                    ForEach(batches, id: \.id) { batch in
                        PurchaseBatchRowView(
                            batch: batch,
                            productName: productName(for: batch.productId),
                            siteName: siteName(for: batch.siteId)
                        )
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle(Localized.purchases)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { showAddSheet = true }) {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $showAddSheet) {
            PurchaseEditorView(sdk: sdk, session: session, products: products, sites: sites, defaultSiteId: siteId) {
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
                // Sync purchase batches from Supabase using upsert (INSERT OR REPLACE)
                let remoteBatches: [PurchaseBatchDTO] = try await SupabaseService.shared.fetchAll(from: "purchase_batches")
                for dto in remoteBatches {
                    try? await sdk.purchaseBatchRepository.upsert(batch: dto.toEntity())
                }
            } catch {
                debugLog("PurchasesListView", "Failed to sync purchase batches from Supabase: \(error)")
            }
        }

        // Load from local database
        do {
            async let batchesResult = sdk.purchaseBatchRepository.getAll()
            async let productsResult = sdk.productRepository.getAll()
            async let sitesResult = sdk.siteRepository.getAll()

            batches = try await batchesResult
            products = try await productsResult
            sites = try await sitesResult

            // Filter by site if specified
            if let siteId = siteId {
                batches = batches.filter { $0.siteId == siteId }
            }
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

// MARK: - Purchase Batch Row View
struct PurchaseBatchRowView: View {
    let batch: PurchaseBatch
    let productName: String
    let siteName: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(productName)
                    .font(.headline)
                Spacer()
                if batch.isExhausted {
                    Text(Localized.exhausted)
                        .font(.caption)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 2)
                        .background(Color.red.opacity(0.2))
                        .foregroundColor(.red)
                        .cornerRadius(4)
                }
            }

            HStack {
                Text("\(Localized.remainingQty): \(String(format: "%.1f", batch.remainingQuantity))/\(String(format: "%.1f", batch.initialQuantity))")
                Spacer()
                Text("\(Localized.price): \(String(format: "%.2f", batch.purchasePrice)) EUR")
            }
            .font(.subheadline)

            HStack {
                Text("\(Localized.site): \(siteName)")
                if !batch.supplierName.isEmpty {
                    Text("- \(batch.supplierName)")
                }
            }
            .font(.caption)
            .foregroundColor(.secondary)

            HStack {
                Text("\(Localized.date): \(formatDate(batch.purchaseDate))")
                if let expiry = batch.expiryDate {
                    Text("- \(Localized.expiryDate): \(formatDate(expiry))")
                        .foregroundColor(isExpiringSoon(expiry) ? .orange : .secondary)
                }
            }
            .font(.caption)
            .foregroundColor(.secondary)

            if let batchNumber = batch.batchNumber, !batchNumber.isEmpty {
                Text("\(Localized.batchNumber): \(batchNumber)")
                    .font(.caption)
                    .foregroundColor(.secondary)
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

    private func isExpiringSoon<T: TimestampConvertible>(_ timestamp: T) -> Bool {
        let expiryDate = Date(timeIntervalSince1970: Double(timestamp.timestampValue) / 1000)
        let thirtyDaysFromNow = Calendar.current.date(byAdding: .day, value: 30, to: Date()) ?? Date()
        return expiryDate < thirtyDaysFromNow
    }
}

// MARK: - Purchase Editor View
struct PurchaseEditorView: View {
    let sdk: MedistockSDK
    @ObservedObject var session: SessionManager
    let products: [Product]
    let sites: [Site]
    let defaultSiteId: String?
    let onSave: () -> Void

    @ObservedObject private var syncStatus = SyncStatusManager.shared
    @Environment(\.dismiss) private var dismiss
    @State private var selectedProductId: String = ""
    @State private var selectedSiteId: String = ""
    @State private var quantityText: String = ""
    @State private var priceText: String = ""
    @State private var suppliers: [Supplier] = []
    @State private var selectedSupplierId: String = ""
    @State private var supplierName: String = ""
    @State private var batchNumber: String = ""
    @State private var expiryDate: Date = Calendar.current.date(byAdding: .year, value: 1, to: Date()) ?? Date()
    @State private var hasExpiryDate = false
    @State private var isSaving = false
    @State private var errorMessage: String?

    var filteredProducts: [Product] {
        if selectedSiteId.isEmpty {
            return products
        }
        return products.filter { $0.siteId == selectedSiteId }
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

                Section(header: Text(Localized.product)) {
                    if filteredProducts.isEmpty {
                        Text(Localized.noProducts)
                            .foregroundColor(.secondary)
                    } else {
                        Picker(Localized.product, selection: $selectedProductId) {
                            Text(Localized.select).tag("")
                            ForEach(filteredProducts, id: \.id) { product in
                                Text(product.name).tag(product.id)
                            }
                        }
                    }
                }

                Section(header: Text("\(Localized.quantity) & \(Localized.price)")) {
                    TextField(Localized.quantity, text: $quantityText)
                        .keyboardType(.decimalPad)
                    TextField(Localized.purchasePrice, text: $priceText)
                        .keyboardType(.decimalPad)
                }

                Section(header: Text(Localized.supplier)) {
                    Picker(Localized.selectSupplier, selection: $selectedSupplierId) {
                        Text(Localized.select).tag("")
                        ForEach(suppliers, id: \.id) { supplier in
                            Text(supplier.name).tag(supplier.id)
                        }
                    }
                    Text(Localized.orSelect)
                        .font(.caption)
                        .foregroundColor(.secondary)
                    TextField(Localized.supplierName, text: $supplierName)
                    TextField(Localized.batchNumber, text: $batchNumber)
                }

                Section(header: Text(Localized.expiryDate)) {
                    Toggle(Localized.expiryDate, isOn: $hasExpiryDate)
                    if hasExpiryDate {
                        DatePicker(Localized.date, selection: $expiryDate, displayedComponents: .date)
                    }
                }

                if let errorMessage {
                    Section {
                        Text(errorMessage)
                            .foregroundColor(.red)
                    }
                }
            }
            .navigationTitle(Localized.newPurchase)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(Localized.cancel) { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(Localized.save) {
                        savePurchase()
                    }
                    .disabled(!canSave || isSaving)
                }
            }
            .task {
                await loadSuppliers()
            }
            .onAppear {
                selectedSiteId = defaultSiteId ?? sites.first?.id ?? ""
            }
            .onChange(of: selectedSiteId) { _ in
                // Reset product selection when site changes
                if !filteredProducts.contains(where: { $0.id == selectedProductId }) {
                    selectedProductId = ""
                }
                // Reload suppliers for new site
                Task { await loadSuppliers() }
            }
            .onChange(of: selectedSupplierId) { newValue in
                // Auto-fill supplier name when selecting from picker
                if let supplier = suppliers.first(where: { $0.id == newValue }) {
                    supplierName = supplier.name
                }
            }
        }
    }

    @MainActor
    private func loadSuppliers() async {
        do {
            suppliers = try await sdk.supplierRepository.getActive()
        } catch {
            debugLog("PurchaseEditorView", "Failed to load suppliers: \(error)")
            suppliers = []
        }
    }

    private var canSave: Bool {
        !selectedProductId.isEmpty &&
        !selectedSiteId.isEmpty &&
        Double(quantityText.replacingOccurrences(of: ",", with: ".")) ?? 0 > 0 &&
        Double(priceText.replacingOccurrences(of: ",", with: ".")) ?? 0 > 0
    }

    private func savePurchase() {
        let quantity = Double(quantityText.replacingOccurrences(of: ",", with: ".")) ?? 0
        let price = Double(priceText.replacingOccurrences(of: ",", with: ".")) ?? 0

        isSaving = true
        errorMessage = nil

        Task {
            // Use PurchaseUseCase for business logic
            let input = PurchaseInput(
                productId: selectedProductId,
                siteId: selectedSiteId,
                quantity: quantity,
                purchasePrice: price,
                supplierName: supplierName,
                supplierId: selectedSupplierId.isEmpty ? nil : selectedSupplierId,
                batchNumber: batchNumber.isEmpty ? nil : batchNumber,
                expiryDate: hasExpiryDate ? KotlinLong(value: Int64(expiryDate.timeIntervalSince1970 * 1000)) : nil,
                userId: session.userId
            )

            do {
                let result = try await sdk.purchaseUseCase.execute(input: input)

                // Handle result
                if let success = result as? UseCaseResultSuccess<PurchaseResult>,
               let purchaseResult = success.data {
                let batch = purchaseResult.purchaseBatch
                let movement = purchaseResult.stockMovement

                // Show warnings if any (non-blocking)
                if !success.warnings.isEmpty {
                    for warning in success.warnings {
                        debugLog("PurchaseEditorView", "Warning: \(warning)")
                    }
                }

                // Sync to Supabase
                let batchDTO = PurchaseBatchDTO(from: batch)
                let movementDTO = StockMovementDTO(from: movement)

                var savedOnline = false
                if syncStatus.isOnline && SupabaseService.shared.isConfigured {
                    do {
                        try await SupabaseService.shared.upsert(into: "purchase_batches", record: batchDTO)
                        try await SupabaseService.shared.upsert(into: "stock_movements", record: movementDTO)
                        savedOnline = true
                    } catch {
                        debugLog("PurchaseEditorView", "Failed to save to Supabase: \(error)")
                    }
                }

                // Queue for sync if not saved online
                if !savedOnline {
                    SyncQueueHelper.shared.enqueueInsert(
                        entityType: .purchaseBatch,
                        entityId: batch.id,
                        entity: batchDTO,
                        userId: session.userId,
                        siteId: selectedSiteId
                    )
                    SyncQueueHelper.shared.enqueueInsert(
                        entityType: .stockMovement,
                        entityId: movement.id,
                        entity: movementDTO,
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

extension PurchaseBatch: @retroactive Identifiable {}
