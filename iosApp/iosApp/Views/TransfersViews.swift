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
                        title: "No transfers",
                        message: "Create a transfer to move products between sites."
                    )
                }
                .listRowSeparator(.hidden)
            } else {
                // Pending transfers
                let pending = transfers.filter { $0.status == "pending" }
                if !pending.isEmpty {
                    Section(header: Text("Pending (\(pending.count))")) {
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
                    Section(header: Text("Completed (\(completed.count))")) {
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
        .navigationTitle("Transfers")
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
        products.first { $0.id == productId }?.name ?? "Unknown product"
    }

    private func siteName(for siteId: String) -> String {
        sites.first { $0.id == siteId }?.name ?? "Unknown site"
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
                Text(transfer.status == "completed" ? "Completed" : "Pending")
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
                    Text("Complete transfer")
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
    @State private var isSaving = false
    @State private var errorMessage: String?

    var filteredProducts: [Product] {
        if fromSiteId.isEmpty {
            return products
        }
        return products.filter { $0.siteId == fromSiteId }
    }

    var availableStock: Double {
        batches
            .filter { $0.productId == selectedProductId && $0.siteId == fromSiteId && !$0.isExhausted }
            .reduce(0) { $0 + $1.remainingQuantity }
    }

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Sites")) {
                    Picker("Source site", selection: $fromSiteId) {
                        Text("Select").tag("")
                        ForEach(sites, id: \.id) { site in
                            Text(site.name).tag(site.id)
                        }
                    }

                    Picker("Destination site", selection: $toSiteId) {
                        Text("Select").tag("")
                        ForEach(sites.filter { $0.id != fromSiteId }, id: \.id) { site in
                            Text(site.name).tag(site.id)
                        }
                    }
                }

                Section(header: Text("Product")) {
                    if filteredProducts.isEmpty {
                        Text("Select a source site first")
                            .foregroundColor(.secondary)
                    } else {
                        Picker("Product", selection: $selectedProductId) {
                            Text("Select").tag("")
                            ForEach(filteredProducts, id: \.id) { product in
                                Text(product.name).tag(product.id)
                            }
                        }

                        if !selectedProductId.isEmpty {
                            LabeledContentCompat {
                                Text("Available stock")
                            } content: {
                                Text(String(format: "%.1f", availableStock))
                            }
                        }
                    }
                }

                Section(header: Text("Quantity")) {
                    TextField("Quantity to transfer", text: $quantityText)
                        .keyboardType(.decimalPad)
                }

                Section(header: Text("Notes (optional)")) {
                    TextField("Notes", text: $notes)
                }

                if let errorMessage {
                    Section {
                        Text(errorMessage)
                            .foregroundColor(.red)
                    }
                }
            }
            .navigationTitle("New Transfer")
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Create") {
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
                userId: session.userId
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
