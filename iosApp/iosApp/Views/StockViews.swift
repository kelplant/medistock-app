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
                        title: "Aucun stock",
                        message: "Effectuez des achats pour voir votre stock."
                    )
                }
                .listRowSeparator(.hidden)
            } else {
                // Summary
                Section(header: Text("Résumé")) {
                    HStack {
                        VStack {
                            Text("\(stockItems.count)")
                                .font(.title)
                                .fontWeight(.bold)
                            Text("Produits")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        Spacer()
                        VStack {
                            Text("\(stockItems.filter { $0.totalStock <= 0 }.count)")
                                .font(.title)
                                .fontWeight(.bold)
                                .foregroundColor(.red)
                            Text("Ruptures")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        Spacer()
                        VStack {
                            Text("\(stockItems.filter { $0.totalStock > 0 && $0.totalStock < 10 }.count)")
                                .font(.title)
                                .fontWeight(.bold)
                                .foregroundColor(.orange)
                            Text("Stock bas")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                    .padding(.vertical, 8)
                }

                // Stock list
                Section(header: Text("Stock par produit")) {
                    ForEach(filteredItems, id: \.productId) { item in
                        StockItemRowView(item: item)
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle("Stock")
        .searchable(text: $searchText, prompt: "Rechercher un produit")
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
            let allStock = try await sdk.stockRepository.getAllCurrentStock()
            let batches = try await sdk.purchaseBatchRepository.getAll()

            var items: [StockItem] = []

            for product in products {
                // Filter by site if specified
                if let siteId = siteId, product.siteId != siteId {
                    continue
                }

                let stock = allStock.first { $0.productId == product.id && $0.siteId == product.siteId }
                let siteName = sites.first { $0.id == product.siteId }?.name ?? "Inconnu"

                // Get batches for expiry info
                let productBatches = batches.filter { $0.productId == product.id && !$0.isExhausted }
                let nearestExpiry = productBatches.compactMap { $0.expiryDate?.timestampValue }.min()

                items.append(StockItem(
                    productId: product.id,
                    productName: product.name,
                    siteId: product.siteId,
                    siteName: siteName,
                    totalStock: stock?.totalStock ?? 0,
                    unit: product.unit,
                    nearestExpiryDate: nearestExpiry,
                    batchCount: productBatches.count
                ))
            }

            // Sort: out of stock first, then low stock, then by name
            stockItems = items.sorted { a, b in
                if a.totalStock <= 0 && b.totalStock > 0 { return true }
                if a.totalStock > 0 && b.totalStock <= 0 { return false }
                if a.totalStock < 10 && b.totalStock >= 10 { return true }
                if a.totalStock >= 10 && b.totalStock < 10 { return false }
                return a.productName < b.productName
            }
        } catch {
            errorMessage = "Erreur: \(error.localizedDescription)"
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
    let unit: String
    let nearestExpiryDate: Int64?
    let batchCount: Int
}

// MARK: - Stock Item Row View
struct StockItemRowView: View {
    let item: StockItem

    var stockColor: Color {
        if item.totalStock <= 0 { return .red }
        if item.totalStock < 10 { return .orange }
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
                Text("Site: \(item.siteName)")
                Spacer()
                Text("\(item.batchCount) lot(s)")
            }
            .font(.caption)
            .foregroundColor(.secondary)

            if let expiry = item.nearestExpiryDate {
                HStack {
                    Image(systemName: "calendar.badge.exclamationmark")
                    Text("Exp. proche: \(formatDate(expiry))")
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

    @State private var movements: [StockMovement] = []
    @State private var products: [Product] = []
    @State private var sites: [Site] = []
    @State private var isLoading = true
    @State private var errorMessage: String?

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
                        title: "Aucun mouvement",
                        message: "Les mouvements de stock apparaîtront ici."
                    )
                }
                .listRowSeparator(.hidden)
            } else {
                Section(header: Text("\(movements.count) mouvement(s)")) {
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
        .navigationTitle("Mouvements de stock")
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
            async let movementsResult = sdk.stockMovementRepository.getAll()
            async let productsResult = sdk.productRepository.getAll()
            async let sitesResult = sdk.siteRepository.getAll()

            movements = try await movementsResult
            products = try await productsResult
            sites = try await sitesResult
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

// MARK: - Stock Movement Row View
struct StockMovementRowView: View {
    let movement: StockMovement
    let productName: String
    let siteName: String

    var movementTypeLabel: String {
        switch movement.movementType {
        case "PURCHASE": return "Achat"
        case "SALE": return "Vente"
        case "TRANSFER_IN": return "Transfert entrant"
        case "TRANSFER_OUT": return "Transfert sortant"
        case "ADJUSTMENT": return "Ajustement"
        case "INVENTORY": return "Inventaire"
        default: return movement.movementType
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

            Text("Site: \(siteName)")
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
