import Foundation
import SwiftUI
import shared

// MARK: - Transfers List View
struct TransfersListView: View {
    let sdk: MedistockSDK
    @ObservedObject var session: SessionManager

    @State private var transfers: [ProductTransfer] = []
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
            } else if transfers.isEmpty {
                Section {
                    EmptyStateView(
                        icon: "arrow.left.arrow.right",
                        title: Localized.noTransfers,
                        message: Localized.strings.noTransfersMessage
                    )
                }
                .listRowSeparator(.hidden)
            } else {
                // Pending transfers
                let pending = transfers.filter { $0.status == "pending" }
                if !pending.isEmpty {
                    Section(header: Text("\(Localized.pending) (\(pending.count))")) {
                        ForEach(pending, id: \.id) { transfer in
                            TransferRowView(
                                transfer: transfer,
                                productName: productName(for: transfer.productId),
                                fromSiteName: siteName(for: transfer.fromSiteId),
                                toSiteName: siteName(for: transfer.toSiteId),
                                onComplete: { completeTransfer(transfer) }
                            )
                        }
                    }
                }

                // Completed transfers
                let completed = transfers.filter { $0.status == "completed" }
                if !completed.isEmpty {
                    Section(header: Text("\(Localized.completed) (\(completed.count))")) {
                        ForEach(completed, id: \.id) { transfer in
                            TransferRowView(
                                transfer: transfer,
                                productName: productName(for: transfer.productId),
                                fromSiteName: siteName(for: transfer.fromSiteId),
                                toSiteName: siteName(for: transfer.toSiteId),
                                onComplete: nil
                            )
                        }
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle(Localized.transfers)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { showAddSheet = true }) {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $showAddSheet) {
            TransferEditorView(sdk: sdk, session: session, products: products, sites: sites) {
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
        do {
            async let transfersResult = sdk.productTransferRepository.getAll()
            async let productsResult = sdk.productRepository.getAll()
            async let sitesResult = sdk.siteRepository.getAll()

            transfers = try await transfersResult
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

    private func completeTransfer(_ transfer: ProductTransfer) {
        Task {
            do {
                // Update transfer status
                try await sdk.productTransferRepository.updateStatus(
                    id: transfer.id,
                    status: "completed",
                    updatedAt: Int64(Date().timeIntervalSince1970 * 1000),
                    updatedBy: session.username
                )

                // Create stock movements
                let movementOut = sdk.createStockMovement(
                    productId: transfer.productId,
                    siteId: transfer.fromSiteId,
                    quantity: -transfer.quantity,
                    movementType: "TRANSFER_OUT",
                    purchasePriceAtMovement: 0.0,
                    sellingPriceAtMovement: 0.0,
                    referenceId: transfer.id,
                    notes: "Transfer to \(siteName(for: transfer.toSiteId))",
                    userId: session.username
                )
                try await sdk.stockMovementRepository.insert(movement: movementOut)

                let movementIn = sdk.createStockMovement(
                    productId: transfer.productId,
                    siteId: transfer.toSiteId,
                    quantity: transfer.quantity,
                    movementType: "TRANSFER_IN",
                    purchasePriceAtMovement: 0.0,
                    sellingPriceAtMovement: 0.0,
                    referenceId: transfer.id,
                    notes: "Transfer from \(siteName(for: transfer.fromSiteId))",
                    userId: session.username
                )
                try await sdk.stockMovementRepository.insert(movement: movementIn)

                await loadData()
            } catch {
                await MainActor.run {
                    errorMessage = "Error: \(error.localizedDescription)"
                }
            }
        }
    }
}

// MARK: - Transfer Row View
struct TransferRowView: View {
    let transfer: ProductTransfer
    let productName: String
    let fromSiteName: String
    let toSiteName: String
    let onComplete: (() -> Void)?

    var statusColor: Color {
        transfer.status == "completed" ? .green : .orange
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(productName)
                    .font(.headline)
                Spacer()
                Text("\(String(format: "%.1f", transfer.quantity))")
                    .font(.headline)
            }

            HStack {
                Image(systemName: "arrow.right")
                Text("\(fromSiteName) → \(toSiteName)")
            }
            .font(.subheadline)
            .foregroundColor(.secondary)

            HStack {
                Text(transfer.status == "completed" ? Localized.completed : Localized.pending)
                    .font(.caption)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 2)
                    .background(statusColor.opacity(0.2))
                    .foregroundColor(statusColor)
                    .cornerRadius(4)

                Spacer()

                Text(formatDate(transfer.createdAt))
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            if let notes = transfer.notes, !notes.isEmpty {
                Text(notes)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            if let onComplete = onComplete, transfer.status == "pending" {
                Button(action: onComplete) {
                    Text(Localized.completeTransfer)
                        .font(.subheadline)
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .padding(.top, 4)
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

// MARK: - Transfer Editor View
struct TransferEditorView: View {
    let sdk: MedistockSDK
    @ObservedObject var session: SessionManager
    let products: [Product]
    let sites: [Site]
    let onSave: () -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var selectedProductId: String = ""
    @State private var fromSiteId: String = ""
    @State private var toSiteId: String = ""
    @State private var quantityText: String = ""
    @State private var notes: String = ""
    @State private var batches: [PurchaseBatch] = []
    @State private var selectedBatchId: String = ""
    @State private var isSaving = false
    @State private var errorMessage: String?

    var filteredProducts: [Product] {
        // Products are global, stock is per-site -- show all products
        return products
    }

    var availableStock: Double {
        batches
            .filter { $0.productId == selectedProductId && $0.siteId == fromSiteId && !$0.isExhausted }
            .reduce(0) { $0 + $1.remainingQuantity }
    }

    // Batches sorted with smart suggestion: expiring soon first, then FIFO
    var availableBatchesSorted: [PurchaseBatch] {
        let productBatches = batches.filter { $0.productId == selectedProductId && $0.siteId == fromSiteId && !$0.isExhausted && $0.remainingQuantity > 0 }
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

    var hasInsufficientStock: Bool {
        guard let qty = Double(quantityText.replacingOccurrences(of: ",", with: ".")) else {
            return false
        }
        return qty > availableStock
    }

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text(Localized.sites)) {
                    Picker(Localized.sourceSite, selection: $fromSiteId) {
                        Text(Localized.select).tag("")
                        ForEach(sites, id: \.id) { site in
                            Text(site.name).tag(site.id)
                        }
                    }

                    Picker(Localized.destinationSite, selection: $toSiteId) {
                        Text(Localized.select).tag("")
                        ForEach(sites.filter { $0.id != fromSiteId }, id: \.id) { site in
                            Text(site.name).tag(site.id)
                        }
                    }
                }

                Section(header: Text(Localized.product)) {
                    if filteredProducts.isEmpty {
                        Text(Localized.strings.selectSourceSiteFirst)
                            .foregroundColor(.secondary)
                    } else {
                        Picker(Localized.product, selection: $selectedProductId) {
                            Text(Localized.select).tag("")
                            ForEach(filteredProducts, id: \.id) { product in
                                Text(product.name).tag(product.id)
                            }
                        }

                        if !selectedProductId.isEmpty {
                            LabeledContentCompat {
                                Text(Localized.availableStock)
                            } content: {
                                Text(String(format: "%.1f", availableStock))
                            }

                            // Batch picker
                            if !availableBatchesSorted.isEmpty {
                                Picker("Lot", selection: $selectedBatchId) {
                                    ForEach(availableBatchesSorted, id: \.id) { batch in
                                        Text("Lot: \(batch.batchNumber ?? "N/A") (reste: \(String(format: "%.1f", batch.remainingQuantity)))").tag(batch.id)
                                    }
                                }

                                if selectedBatchExpiresSoon {
                                    Text("Lot proche de l'expiration (expire le \(selectedBatchExpiryDateFormatted))")
                                        .font(.caption)
                                        .foregroundColor(.orange)
                                }
                            }
                        }
                    }
                }

                Section(header: Text(Localized.quantity)) {
                    TextField(Localized.quantityToTransfer, text: $quantityText)
                        .keyboardType(.decimalPad)

                    // Non-blocking stock warning
                    if hasInsufficientStock {
                        Text("Stock insuffisant (\(String(format: "%.1f", availableStock)) disponible)")
                            .font(.caption)
                            .foregroundColor(.orange)
                    }
                }

                Section(header: Text(Localized.notes)) {
                    TextField(Localized.notes, text: $notes)
                }

                if let errorMessage {
                    Section {
                        Text(errorMessage)
                            .foregroundColor(.red)
                    }
                }
            }
            .navigationTitle(Localized.newTransfer)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(Localized.cancel) { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(Localized.create) {
                        saveTransfer()
                    }
                    .disabled(!canSave || isSaving)
                }
            }
            .task {
                await loadBatches()
            }
            .onChange(of: fromSiteId) { _ in
                selectedProductId = ""
                selectedBatchId = ""
            }
            .onChange(of: selectedProductId) { _ in
                selectedBatchId = availableBatchesSorted.first?.id ?? ""
            }
        }
    }

    private var canSave: Bool {
        guard !selectedProductId.isEmpty,
              !fromSiteId.isEmpty,
              !toSiteId.isEmpty,
              fromSiteId != toSiteId,
              let qty = Double(quantityText.replacingOccurrences(of: ",", with: ".")),
              qty > 0 else {
            return false
        }
        // Note: We allow transfers even with insufficient stock (negative stock allowed per business rules)
        return true
    }

    @MainActor
    private func loadBatches() async {
        do {
            batches = try await sdk.purchaseBatchRepository.getAll()
        } catch {
            // Ignore
        }
    }

    private func saveTransfer() {
        let quantity = Double(quantityText.replacingOccurrences(of: ",", with: ".")) ?? 0

        isSaving = true
        errorMessage = nil

        Task {
            // Use TransferUseCase for business logic (handles FIFO batch transfer, stock movements, etc.)
            let input = TransferInput(
                productId: selectedProductId,
                fromSiteId: fromSiteId,
                toSiteId: toSiteId,
                quantity: quantity,
                notes: notes.isEmpty ? nil : notes,
                userId: session.userId,
                preferredBatchId: selectedBatchId.isEmpty ? nil : selectedBatchId
            )

            do {
                let result = try await sdk.transferUseCase.execute(input: input)

                // Handle result
                if let success = result as? UseCaseResultSuccess<TransferResult>,
                   let transferResult = success.data {

                    // Show warnings if any (e.g., insufficient stock)
                    if !success.warnings.isEmpty {
                        for warning in success.warnings {
                            if let stockWarning = warning as? BusinessWarning.InsufficientStock {
                                debugLog("TransferEditorView", "Warning: Insufficient stock for transfer: requested \(stockWarning.requested), available \(stockWarning.available)")
                            }
                        }
                    }

                    // Sync to Supabase
                    let syncStatus = SyncStatusManager.shared
                    if syncStatus.isOnline && SupabaseService.shared.isConfigured {
                        do {
                            let transferDTO = ProductTransferDTO(from: transferResult.transfer)
                            try await SupabaseService.shared.upsert(into: "product_transfers", record: transferDTO)
                        } catch {
                            debugLog("TransferEditorView", "Failed to save to Supabase: \(error)")
                        }
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

extension ProductTransfer: @retroactive Identifiable {}
