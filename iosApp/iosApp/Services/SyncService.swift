import Foundation
import shared

/// Service for synchronizing local data with Supabase
/// Mirrors the Android BaseSupabaseRepository sync functionality
class SyncService: ObservableObject {
    static let shared = SyncService()

    private let supabase = SupabaseClient.shared
    @Published private(set) var isSyncing = false
    @Published private(set) var lastSyncDate: Date?
    @Published private(set) var lastError: String?
    @Published private(set) var syncProgress: String = ""

    private let lastSyncKey = "medistock_last_sync"

    private init() {
        if let timestamp = UserDefaults.standard.object(forKey: lastSyncKey) as? Double {
            lastSyncDate = Date(timeIntervalSince1970: timestamp)
        }
    }

    // MARK: - Sync Status

    var canSync: Bool {
        supabase.isConfigured
    }

    // MARK: - Full Sync

    /// Perform a full sync of all data from Supabase to local SDK
    func performFullSync(sdk: MedistockSDK) async {
        guard canSync else {
            await MainActor.run {
                lastError = "Supabase n'est pas configuré"
            }
            return
        }

        await MainActor.run {
            isSyncing = true
            lastError = nil
            syncProgress = "Démarrage..."
        }

        do {
            // Sync all entities in order (respecting foreign key dependencies)
            await updateProgress("Synchronisation des sites...")
            try await syncSites(sdk: sdk)

            await updateProgress("Synchronisation des catégories...")
            try await syncCategories(sdk: sdk)

            await updateProgress("Synchronisation des conditionnements...")
            try await syncPackagingTypes(sdk: sdk)

            await updateProgress("Synchronisation des produits...")
            try await syncProducts(sdk: sdk)

            await updateProgress("Synchronisation des utilisateurs...")
            try await syncUsers(sdk: sdk)

            await updateProgress("Synchronisation des clients...")
            try await syncCustomers(sdk: sdk)

            await updateProgress("Synchronisation des achats...")
            try await syncPurchaseBatches(sdk: sdk)

            await updateProgress("Synchronisation des ventes...")
            try await syncSalesWithItems(sdk: sdk)

            await updateProgress("Synchronisation des mouvements...")
            try await syncStockMovements(sdk: sdk)

            await MainActor.run {
                isSyncing = false
                lastSyncDate = Date()
                syncProgress = "Terminé"
                UserDefaults.standard.set(lastSyncDate!.timeIntervalSince1970, forKey: lastSyncKey)
            }
        } catch {
            await MainActor.run {
                isSyncing = false
                lastError = error.localizedDescription
                syncProgress = "Erreur"
            }
        }
    }

    private func updateProgress(_ message: String) async {
        await MainActor.run {
            syncProgress = message
        }
    }

    // MARK: - Individual Sync Methods with proper upsert logic

    private func syncSites(sdk: MedistockSDK) async throws {
        let remoteSites: [RemoteSite] = try await supabase.fetchAll(from: "sites")
        for site in remoteSites {
            let localSite = Site(
                id: site.id,
                name: site.name,
                createdAt: site.createdAt,
                updatedAt: site.updatedAt,
                createdBy: site.createdBy,
                updatedBy: site.updatedBy
            )

            // Check if exists locally - use upsert logic
            let existing = try? await sdk.siteRepository.getById(id: site.id)
            if existing != nil {
                // Update existing record
                try? await sdk.siteRepository.update(site: localSite)
            } else {
                // Insert new record
                try? await sdk.siteRepository.insert(site: localSite)
            }
        }
    }

    private func syncCategories(sdk: MedistockSDK) async throws {
        let remoteCategories: [RemoteCategory] = try await supabase.fetchAll(from: "categories")
        for category in remoteCategories {
            let localCategory = Category(
                id: category.id,
                name: category.name,
                createdAt: category.createdAt,
                updatedAt: category.updatedAt,
                createdBy: category.createdBy,
                updatedBy: category.updatedBy
            )

            let existing = try? await sdk.categoryRepository.getById(id: category.id)
            if existing != nil {
                try? await sdk.categoryRepository.update(category: localCategory)
            } else {
                try? await sdk.categoryRepository.insert(category: localCategory)
            }
        }
    }

    private func syncPackagingTypes(sdk: MedistockSDK) async throws {
        let remoteTypes: [RemotePackagingType] = try await supabase.fetchAll(from: "packaging_types")
        for type in remoteTypes {
            let localType = PackagingType(
                id: type.id,
                name: type.name,
                level1Name: type.level1Name,
                level2Name: type.level2Name,
                level2Quantity: type.level2Quantity.map { KotlinInt(int: Int32($0)) },
                createdAt: type.createdAt,
                updatedAt: type.updatedAt,
                createdBy: type.createdBy,
                updatedBy: type.updatedBy
            )

            let existing = try? await sdk.packagingTypeRepository.getById(id: type.id)
            if existing != nil {
                try? await sdk.packagingTypeRepository.update(packagingType: localType)
            } else {
                try? await sdk.packagingTypeRepository.insert(packagingType: localType)
            }
        }
    }

    private func syncProducts(sdk: MedistockSDK) async throws {
        let remoteProducts: [RemoteProduct] = try await supabase.fetchAll(from: "products")
        for product in remoteProducts {
            let localProduct = Product(
                id: product.id,
                name: product.name,
                unit: product.unit,
                unitVolume: product.unitVolume,
                packagingTypeId: product.packagingTypeId,
                selectedLevel: product.selectedLevel.map { KotlinInt(int: Int32($0)) },
                conversionFactor: product.conversionFactor.map { KotlinDouble(double: $0) },
                categoryId: product.categoryId,
                marginType: product.marginType,
                marginValue: product.marginValue.map { KotlinDouble(double: $0) },
                description: product.description_,
                siteId: product.siteId,
                minStock: product.minStock.map { KotlinDouble(double: $0) },
                maxStock: product.maxStock.map { KotlinDouble(double: $0) },
                createdAt: product.createdAt,
                updatedAt: product.updatedAt,
                createdBy: product.createdBy,
                updatedBy: product.updatedBy
            )

            let existing = try? await sdk.productRepository.getById(id: product.id)
            if existing != nil {
                try? await sdk.productRepository.update(product: localProduct)
            } else {
                try? await sdk.productRepository.insert(product: localProduct)
            }
        }
    }

    private func syncUsers(sdk: MedistockSDK) async throws {
        let remoteUsers: [RemoteUser] = try await supabase.fetchAll(from: "app_users")
        for user in remoteUsers {
            let localUser = User(
                id: user.id,
                username: user.username,
                password: user.password,
                fullName: user.fullName,
                isAdmin: user.isAdmin,
                isActive: user.isActive,
                createdAt: user.createdAt,
                updatedAt: user.updatedAt,
                createdBy: user.createdBy,
                updatedBy: user.updatedBy
            )

            let existing = try? await sdk.userRepository.getById(id: user.id)
            if existing != nil {
                try? await sdk.userRepository.update(user: localUser)
            } else {
                try? await sdk.userRepository.insert(user: localUser)
            }
        }
    }

    private func syncCustomers(sdk: MedistockSDK) async throws {
        let remoteCustomers: [RemoteCustomer] = try await supabase.fetchAll(from: "customers")
        for customer in remoteCustomers {
            let localCustomer = Customer(
                id: customer.id,
                name: customer.name,
                phone: customer.phone,
                email: customer.email,
                address: customer.address,
                notes: customer.notes,
                createdAt: customer.createdAt,
                updatedAt: customer.updatedAt,
                createdBy: customer.createdBy,
                updatedBy: customer.updatedBy
            )

            let existing = try? await sdk.customerRepository.getById(id: customer.id)
            if existing != nil {
                try? await sdk.customerRepository.update(customer: localCustomer)
            } else {
                try? await sdk.customerRepository.insert(customer: localCustomer)
            }
        }
    }

    private func syncPurchaseBatches(sdk: MedistockSDK) async throws {
        let remoteBatches: [RemotePurchaseBatch] = try await supabase.fetchAll(from: "purchase_batches")
        for batch in remoteBatches {
            let localBatch = PurchaseBatch(
                id: batch.id,
                productId: batch.productId,
                siteId: batch.siteId,
                batchNumber: batch.batchNumber,
                purchaseDate: batch.purchaseDate,
                initialQuantity: batch.initialQuantity,
                remainingQuantity: batch.remainingQuantity,
                purchasePrice: batch.purchasePrice,
                supplierName: batch.supplierName,
                expiryDate: batch.expiryDate.map { KotlinLong(longLong: $0) },
                isExhausted: batch.isExhausted,
                createdAt: batch.createdAt,
                updatedAt: batch.updatedAt,
                createdBy: batch.createdBy,
                updatedBy: batch.updatedBy
            )

            // PurchaseBatch doesn't have a full update method, check and update quantity if changed
            let existing = try? await sdk.purchaseBatchRepository.getById(id: batch.id)
            if let existing = existing {
                // Update quantity if changed
                if existing.remainingQuantity != batch.remainingQuantity || existing.isExhausted != batch.isExhausted {
                    try? await sdk.purchaseBatchRepository.updateQuantity(
                        id: batch.id,
                        remainingQuantity: batch.remainingQuantity,
                        isExhausted: batch.isExhausted,
                        updatedAt: batch.updatedAt,
                        updatedBy: batch.updatedBy
                    )
                }
            } else {
                try? await sdk.purchaseBatchRepository.insert(batch: localBatch)
            }
        }
    }

    /// Sync sales AND their items together
    private func syncSalesWithItems(sdk: MedistockSDK) async throws {
        // Fetch sales
        let remoteSales: [RemoteSale] = try await supabase.fetchAll(from: "sales")

        // Fetch all sale items
        let remoteItems: [RemoteSaleItem] = try await supabase.fetchAll(from: "sale_items")

        // Group items by sale_id
        let itemsBySale = Dictionary(grouping: remoteItems) { $0.saleId }

        for sale in remoteSales {
            let localSale = Sale(
                id: sale.id,
                customerName: sale.customerName,
                customerId: sale.customerId,
                date: sale.date,
                totalAmount: sale.totalAmount,
                siteId: sale.siteId,
                createdAt: sale.createdAt,
                createdBy: sale.createdBy
            )

            let saleItems = itemsBySale[sale.id] ?? []
            let localItems = saleItems.map { item in
                SaleItem(
                    id: item.id,
                    saleId: item.saleId,
                    productId: item.productId,
                    quantity: item.quantity,
                    unitPrice: item.unitPrice,
                    totalPrice: item.totalPrice
                )
            }

            // Check if sale exists locally
            let existing = try? await sdk.saleRepository.getById(id: sale.id)
            if existing == nil {
                // Insert new sale with items
                try? await sdk.saleRepository.insertSaleWithItems(sale: localSale, items: localItems)
            }
            // Note: Sales are typically immutable after creation, so we don't update
        }
    }

    private func syncStockMovements(sdk: MedistockSDK) async throws {
        let remoteMovements: [RemoteStockMovement] = try await supabase.fetchAll(from: "stock_movements")
        for movement in remoteMovements {
            let localMovement = StockMovement(
                id: movement.id,
                productId: movement.productId,
                siteId: movement.siteId,
                quantity: movement.quantity,
                movementType: movement.movementType,
                referenceId: movement.referenceId,
                notes: movement.notes,
                createdAt: movement.createdAt,
                createdBy: movement.createdBy
            )

            // Stock movements are append-only, just try to insert
            // If it fails due to duplicate ID, that's fine - it already exists
            try? await sdk.stockMovementRepository.insert(movement: localMovement)
        }
    }

    // MARK: - Push to Supabase

    /// Push local changes to Supabase
    func pushToSupabase<T: Codable>(table: String, records: [T]) async throws {
        guard canSync else {
            throw SupabaseError.notConfigured
        }

        for record in records {
            _ = try await supabase.upsert(into: table, record: record)
        }
    }
}

// MARK: - Remote Models (for Supabase JSON encoding/decoding)

struct RemoteUser: Codable, Identifiable {
    let id: String
    let username: String
    let password: String
    let fullName: String
    let isAdmin: Bool
    let isActive: Bool
    let createdAt: Int64
    let updatedAt: Int64
    let createdBy: String
    let updatedBy: String
}

struct RemoteSite: Codable, Identifiable {
    let id: String
    let name: String
    let createdAt: Int64
    let updatedAt: Int64
    let createdBy: String
    let updatedBy: String
}

struct RemoteCategory: Codable, Identifiable {
    let id: String
    let name: String
    let createdAt: Int64
    let updatedAt: Int64
    let createdBy: String
    let updatedBy: String
}

struct RemotePackagingType: Codable, Identifiable {
    let id: String
    let name: String
    let level1Name: String
    let level2Name: String?
    let level2Quantity: Int?
    let createdAt: Int64
    let updatedAt: Int64
    let createdBy: String
    let updatedBy: String
}

struct RemoteProduct: Codable, Identifiable {
    let id: String
    let name: String
    let unit: String
    let unitVolume: Double
    let packagingTypeId: String?
    let selectedLevel: Int?
    let conversionFactor: Double?
    let categoryId: String?
    let marginType: String?
    let marginValue: Double?
    let description_: String?
    let siteId: String
    let minStock: Double?
    let maxStock: Double?
    let createdAt: Int64
    let updatedAt: Int64
    let createdBy: String
    let updatedBy: String

    enum CodingKeys: String, CodingKey {
        case id, name, unit, unitVolume, packagingTypeId, selectedLevel
        case conversionFactor, categoryId, marginType, marginValue
        case description_ = "description"
        case siteId, minStock, maxStock, createdAt, updatedAt, createdBy, updatedBy
    }
}

struct RemoteCustomer: Codable, Identifiable {
    let id: String
    let name: String
    let phone: String?
    let email: String?
    let address: String?
    let notes: String?
    let createdAt: Int64
    let updatedAt: Int64
    let createdBy: String
    let updatedBy: String
}

struct RemotePurchaseBatch: Codable, Identifiable {
    let id: String
    let productId: String
    let siteId: String
    let batchNumber: String?
    let purchaseDate: Int64
    let initialQuantity: Double
    let remainingQuantity: Double
    let purchasePrice: Double
    let supplierName: String
    let expiryDate: Int64?
    let isExhausted: Bool
    let createdAt: Int64
    let updatedAt: Int64
    let createdBy: String
    let updatedBy: String
}

struct RemoteSale: Codable, Identifiable {
    let id: String
    let customerName: String
    let customerId: String?
    let date: Int64
    let totalAmount: Double
    let siteId: String
    let createdAt: Int64
    let createdBy: String
}

struct RemoteSaleItem: Codable, Identifiable {
    let id: String
    let saleId: String
    let productId: String
    let quantity: Double
    let unitPrice: Double
    let totalPrice: Double
}

struct RemoteStockMovement: Codable, Identifiable {
    let id: String
    let productId: String
    let siteId: String
    let quantity: Double
    let movementType: String
    let referenceId: String?
    let notes: String?
    let createdAt: Int64
    let createdBy: String
}
