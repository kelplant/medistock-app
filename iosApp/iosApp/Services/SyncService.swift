import Foundation
import shared

/// Service for synchronizing local data with Supabase
/// Mirrors the Android BaseSupabaseRepository sync functionality
class SyncService {
    static let shared = SyncService()

    private let supabase = SupabaseClient.shared
    @Published private(set) var isSyncing = false
    @Published private(set) var lastSyncDate: Date?
    @Published private(set) var lastError: String?

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
        }

        do {
            // Sync all entities in order (respecting foreign key dependencies)
            try await syncSites(sdk: sdk)
            try await syncCategories(sdk: sdk)
            try await syncPackagingTypes(sdk: sdk)
            try await syncProducts(sdk: sdk)
            try await syncUsers(sdk: sdk)
            try await syncCustomers(sdk: sdk)
            try await syncPurchaseBatches(sdk: sdk)
            try await syncSales(sdk: sdk)
            try await syncStock(sdk: sdk)

            await MainActor.run {
                isSyncing = false
                lastSyncDate = Date()
                UserDefaults.standard.set(lastSyncDate!.timeIntervalSince1970, forKey: lastSyncKey)
            }
        } catch {
            await MainActor.run {
                isSyncing = false
                lastError = error.localizedDescription
            }
        }
    }

    // MARK: - Individual Sync Methods

    private func syncSites(sdk: MedistockSDK) async throws {
        let remoteSites: [RemoteSite] = try await supabase.fetchAll(from: "sites")
        for site in remoteSites {
            let localSite = sdk.createSite(name: site.name, userId: site.createdBy)
            // Note: SDK would need upsert method - for now we insert
            try? await sdk.siteRepository.insert(site: Site(
                id: site.id,
                name: site.name,
                createdAt: site.createdAt,
                updatedAt: site.updatedAt,
                createdBy: site.createdBy,
                updatedBy: site.updatedBy
            ))
        }
    }

    private func syncCategories(sdk: MedistockSDK) async throws {
        let remoteCategories: [RemoteCategory] = try await supabase.fetchAll(from: "categories")
        for category in remoteCategories {
            try? await sdk.categoryRepository.insert(category: Category(
                id: category.id,
                name: category.name,
                createdAt: category.createdAt,
                updatedAt: category.updatedAt,
                createdBy: category.createdBy,
                updatedBy: category.updatedBy
            ))
        }
    }

    private func syncPackagingTypes(sdk: MedistockSDK) async throws {
        let remoteTypes: [RemotePackagingType] = try await supabase.fetchAll(from: "packaging_types")
        for type in remoteTypes {
            try? await sdk.packagingTypeRepository.insert(packagingType: PackagingType(
                id: type.id,
                name: type.name,
                level1Name: type.level1Name,
                level2Name: type.level2Name,
                level2Quantity: type.level2Quantity.map { KotlinInt(int: Int32($0)) },
                createdAt: type.createdAt,
                updatedAt: type.updatedAt,
                createdBy: type.createdBy,
                updatedBy: type.updatedBy
            ))
        }
    }

    private func syncProducts(sdk: MedistockSDK) async throws {
        let remoteProducts: [RemoteProduct] = try await supabase.fetchAll(from: "products")
        for product in remoteProducts {
            try? await sdk.productRepository.insert(product: Product(
                id: product.id,
                name: product.name,
                unit: product.unit,
                unitVolume: product.unitVolume,
                siteId: product.siteId,
                categoryId: product.categoryId,
                createdAt: product.createdAt,
                updatedAt: product.updatedAt,
                createdBy: product.createdBy,
                updatedBy: product.updatedBy
            ))
        }
    }

    private func syncUsers(sdk: MedistockSDK) async throws {
        let remoteUsers: [RemoteUser] = try await supabase.fetchAll(from: "app_users")
        for user in remoteUsers {
            try? await sdk.userRepository.insert(user: User(
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
            ))
        }
    }

    private func syncCustomers(sdk: MedistockSDK) async throws {
        let remoteCustomers: [RemoteCustomer] = try await supabase.fetchAll(from: "customers")
        for customer in remoteCustomers {
            try? await sdk.customerRepository.insert(customer: Customer(
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
            ))
        }
    }

    private func syncPurchaseBatches(sdk: MedistockSDK) async throws {
        let remoteBatches: [RemotePurchaseBatch] = try await supabase.fetchAll(from: "purchase_batches")
        for batch in remoteBatches {
            try? await sdk.purchaseBatchRepository.insert(batch: PurchaseBatch(
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
            ))
        }
    }

    private func syncSales(sdk: MedistockSDK) async throws {
        let remoteSales: [RemoteSale] = try await supabase.fetchAll(from: "sales")
        for sale in remoteSales {
            try? await sdk.saleRepository.insert(sale: Sale(
                id: sale.id,
                customerName: sale.customerName,
                customerId: sale.customerId,
                date: sale.date,
                totalAmount: sale.totalAmount,
                siteId: sale.siteId,
                createdAt: sale.createdAt,
                createdBy: sale.createdBy
            ))
        }
    }

    private func syncStock(sdk: MedistockSDK) async throws {
        let remoteStock: [RemoteStock] = try await supabase.fetchAll(from: "stock")
        for stock in remoteStock {
            try? await sdk.stockRepository.upsert(stock: Stock(
                id: stock.id,
                productId: stock.productId,
                siteId: stock.siteId,
                quantity: stock.quantity,
                updatedAt: stock.updatedAt
            ))
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

// MARK: - Remote Models (for Supabase JSON decoding)

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
    let siteId: String
    let categoryId: String?
    let createdAt: Int64
    let updatedAt: Int64
    let createdBy: String
    let updatedBy: String
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

struct RemoteStock: Codable, Identifiable {
    let id: String
    let productId: String
    let siteId: String
    let quantity: Double
    let updatedAt: Int64
}
