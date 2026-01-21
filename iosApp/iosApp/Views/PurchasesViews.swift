import Foundation
import SwiftUI
import shared

// MARK: - Purchases List View
struct PurchasesListView: View {
    let sdk: MedistockSDK
    @ObservedObject var session: SessionManager
    let siteId: String?

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
                        title: "Aucun achat",
                        message: "Enregistrez votre premier achat pour alimenter votre stock."
                    )
                }
                .listRowSeparator(.hidden)
            } else {
                Section(header: Text("\(batches.count) lot(s)")) {
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
        .navigationTitle("Achats")
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
            errorMessage = "Erreur: \(error.localizedDescription)"
        }
        isLoading = false
    }

    private func productName(for productId: String) -> String {
        products.first { $0.id == productId }?.name ?? "Produit inconnu"
    }

    private func siteName(for siteId: String) -> String {
        sites.first { $0.id == siteId }?.name ?? "Site inconnu"
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
                    Text("Épuisé")
                        .font(.caption)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 2)
                        .background(Color.red.opacity(0.2))
                        .foregroundColor(.red)
                        .cornerRadius(4)
                }
            }

            HStack {
                Text("Qté restante: \(String(format: "%.1f", batch.remainingQuantity))/\(String(format: "%.1f", batch.initialQuantity))")
                Spacer()
                Text("Prix: \(String(format: "%.2f", batch.purchasePrice)) €")
            }
            .font(.subheadline)

            HStack {
                Text("Site: \(siteName)")
                if !batch.supplierName.isEmpty {
                    Text("• \(batch.supplierName)")
                }
            }
            .font(.caption)
            .foregroundColor(.secondary)

            HStack {
                Text("Date: \(formatDate(batch.purchaseDate))")
                if let expiry = batch.expiryDate {
                    Text("• Exp: \(formatDate(expiry.int64Value))")
                        .foregroundColor(isExpiringSoon(expiry.int64Value) ? .orange : .secondary)
                }
            }
            .font(.caption)
            .foregroundColor(.secondary)

            if let batchNumber = batch.batchNumber, !batchNumber.isEmpty {
                Text("Lot: \(batchNumber)")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .padding(.vertical, 4)
    }

    private func formatDate(_ timestamp: Int64) -> String {
        let date = Date(timeIntervalSince1970: Double(timestamp) / 1000)
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

// MARK: - Purchase Editor View
struct PurchaseEditorView: View {
    let sdk: MedistockSDK
    @ObservedObject var session: SessionManager
    let products: [Product]
    let sites: [Site]
    let defaultSiteId: String?
    let onSave: () -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var selectedProductId: String = ""
    @State private var selectedSiteId: String = ""
    @State private var quantityText: String = ""
    @State private var priceText: String = ""
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
                Section(header: Text("Site")) {
                    Picker("Site", selection: $selectedSiteId) {
                        Text("Sélectionner").tag("")
                        ForEach(sites, id: \.id) { site in
                            Text(site.name).tag(site.id)
                        }
                    }
                }

                Section(header: Text("Produit")) {
                    if filteredProducts.isEmpty {
                        Text("Aucun produit disponible pour ce site")
                            .foregroundColor(.secondary)
                    } else {
                        Picker("Produit", selection: $selectedProductId) {
                            Text("Sélectionner").tag("")
                            ForEach(filteredProducts, id: \.id) { product in
                                Text(product.name).tag(product.id)
                            }
                        }
                    }
                }

                Section(header: Text("Quantité et Prix")) {
                    TextField("Quantité", text: $quantityText)
                        .keyboardType(.decimalPad)
                    TextField("Prix d'achat unitaire", text: $priceText)
                        .keyboardType(.decimalPad)
                }

                Section(header: Text("Fournisseur")) {
                    TextField("Nom du fournisseur", text: $supplierName)
                    TextField("Numéro de lot (optionnel)", text: $batchNumber)
                }

                Section(header: Text("Date d'expiration")) {
                    Toggle("Date d'expiration", isOn: $hasExpiryDate)
                    if hasExpiryDate {
                        DatePicker("Date", selection: $expiryDate, displayedComponents: .date)
                    }
                }

                if let errorMessage {
                    Section {
                        Text(errorMessage)
                            .foregroundColor(.red)
                    }
                }
            }
            .navigationTitle("Nouvel achat")
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Annuler") { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Enregistrer") {
                        savePurchase()
                    }
                    .disabled(!canSave || isSaving)
                }
            }
            .onAppear {
                selectedSiteId = defaultSiteId ?? sites.first?.id ?? ""
            }
            .onChange(of: selectedSiteId) { _ in
                // Reset product selection when site changes
                if !filteredProducts.contains(where: { $0.id == selectedProductId }) {
                    selectedProductId = ""
                }
            }
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
            do {
                let batch = sdk.createPurchaseBatch(
                    productId: selectedProductId,
                    siteId: selectedSiteId,
                    quantity: quantity,
                    purchasePrice: price,
                    supplierName: supplierName,
                    batchNumber: batchNumber.isEmpty ? nil : batchNumber,
                    expiryDate: hasExpiryDate ? KotlinLong(value: Int64(expiryDate.timeIntervalSince1970 * 1000)) : nil,
                    userId: session.username
                )
                try await sdk.purchaseBatchRepository.insert(batch: batch)

                // Create stock movement
                let movement = sdk.createStockMovement(
                    productId: selectedProductId,
                    siteId: selectedSiteId,
                    quantity: quantity,
                    movementType: "PURCHASE",
                    referenceId: batch.id,
                    notes: "Achat - Lot: \(batchNumber.isEmpty ? batch.id : batchNumber)",
                    userId: session.username
                )
                try await sdk.stockMovementRepository.insert(movement: movement)

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

extension PurchaseBatch: @retroactive Identifiable {}
