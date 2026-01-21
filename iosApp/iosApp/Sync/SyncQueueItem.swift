import Foundation

/// Represents a pending sync operation in the queue
/// Mirrors Android's SyncQueueItem entity
struct SyncQueueItem: Codable, Identifiable {
    let id: String
    let entityType: EntityType
    let entityId: String
    let operation: SyncOperation
    let payload: Data?
    let localVersion: Int
    let remoteVersion: Int?
    let lastKnownRemoteUpdatedAt: Int64?
    var status: SyncStatus
    var retryCount: Int
    var lastError: String?
    var lastAttemptAt: Date?
    let createdAt: Date
    let userId: String
    let siteId: String?

    init(
        id: String = UUID().uuidString,
        entityType: EntityType,
        entityId: String,
        operation: SyncOperation,
        payload: Data?,
        localVersion: Int = 1,
        remoteVersion: Int? = nil,
        lastKnownRemoteUpdatedAt: Int64? = nil,
        status: SyncStatus = .pending,
        retryCount: Int = 0,
        lastError: String? = nil,
        lastAttemptAt: Date? = nil,
        createdAt: Date = Date(),
        userId: String,
        siteId: String? = nil
    ) {
        self.id = id
        self.entityType = entityType
        self.entityId = entityId
        self.operation = operation
        self.payload = payload
        self.localVersion = localVersion
        self.remoteVersion = remoteVersion
        self.lastKnownRemoteUpdatedAt = lastKnownRemoteUpdatedAt
        self.status = status
        self.retryCount = retryCount
        self.lastError = lastError
        self.lastAttemptAt = lastAttemptAt
        self.createdAt = createdAt
        self.userId = userId
        self.siteId = siteId
    }
}

// MARK: - Enums

enum EntityType: String, Codable, CaseIterable {
    case site = "Site"
    case category = "Category"
    case packagingType = "PackagingType"
    case product = "Product"
    case customer = "Customer"
    case user = "User"
    case userPermission = "UserPermission"
    case purchaseBatch = "PurchaseBatch"
    case sale = "Sale"
    case saleItem = "SaleItem"
    case stockMovement = "StockMovement"
    case transfer = "Transfer"
    case inventoryCount = "InventoryCount"
    case inventoryItem = "InventoryItem"

    /// Supabase table name
    var tableName: String {
        switch self {
        case .site: return "sites"
        case .category: return "categories"
        case .packagingType: return "packaging_types"
        case .product: return "products"
        case .customer: return "customers"
        case .user: return "app_users"
        case .userPermission: return "user_permissions"
        case .purchaseBatch: return "purchase_batches"
        case .sale: return "sales"
        case .saleItem: return "sale_items"
        case .stockMovement: return "stock_movements"
        case .transfer: return "transfers"
        case .inventoryCount: return "inventory_counts"
        case .inventoryItem: return "inventory_items"
        }
    }

    /// Conflict resolution strategy for this entity type
    var conflictStrategy: ConflictStrategy {
        switch self {
        case .product, .category, .site, .packagingType, .purchaseBatch, .user, .userPermission:
            return .serverWins
        case .sale, .saleItem:
            return .clientWins
        case .stockMovement, .customer, .transfer:
            return .merge
        case .inventoryCount, .inventoryItem:
            return .askUser
        }
    }
}

enum SyncOperation: String, Codable {
    case insert = "INSERT"
    case update = "UPDATE"
    case delete = "DELETE"
}

enum SyncStatus: String, Codable {
    case pending = "PENDING"
    case inProgress = "IN_PROGRESS"
    case synced = "SYNCED"
    case conflict = "CONFLICT"
    case failed = "FAILED"
}

enum ConflictStrategy: String, Codable {
    case serverWins = "SERVER_WINS"
    case clientWins = "CLIENT_WINS"
    case merge = "MERGE"
    case askUser = "ASK_USER"
    case keepBoth = "KEEP_BOTH"
}

// MARK: - Sync Configuration

struct SyncConfiguration {
    static let maxRetries = 5
    static let backoffDelays: [TimeInterval] = [1, 2, 4, 8, 16] // seconds
    static let batchSize = 10
    static let syncInterval: TimeInterval = 30 // seconds

    static func backoffDelay(for retryCount: Int) -> TimeInterval {
        let index = min(retryCount, backoffDelays.count - 1)
        return backoffDelays[index]
    }
}

// MARK: - Process Result

enum ProcessResult {
    case success
    case conflict(local: Data?, remote: Data?)
    case retry(error: Error)
    case skip(reason: String)
}
