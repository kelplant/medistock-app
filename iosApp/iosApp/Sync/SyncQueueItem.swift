import Foundation
import shared

/// Represents a pending sync operation in the queue
/// Bridges between Swift and shared Kotlin SyncQueueItem
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
    let userId: String?
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
        userId: String?,
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

    // MARK: - Conversion from shared Kotlin model

    init(from kotlinItem: shared.SyncQueueItem) {
        self.id = kotlinItem.id
        self.entityType = EntityType(rawValue: kotlinItem.entityType) ?? .product
        self.entityId = kotlinItem.entityId
        self.operation = SyncOperation(from: kotlinItem.operation)
        self.payload = kotlinItem.payload.data(using: .utf8)
        self.localVersion = Int(kotlinItem.localVersion)
        self.remoteVersion = kotlinItem.remoteVersion.map { Int(truncating: $0) }
        self.lastKnownRemoteUpdatedAt = kotlinItem.lastKnownRemoteUpdatedAt?.int64Value
        self.status = SyncStatus(from: kotlinItem.status)
        self.retryCount = Int(kotlinItem.retryCount)
        self.lastError = kotlinItem.lastError
        self.lastAttemptAt = kotlinItem.lastAttemptAt.map { Date(timeIntervalSince1970: Double(truncating: $0) / 1000.0) }
        self.createdAt = Date(timeIntervalSince1970: Double(kotlinItem.createdAt) / 1000.0)
        self.userId = kotlinItem.userId
        self.siteId = kotlinItem.siteId
    }

    // MARK: - Conversion to shared Kotlin model

    func toKotlinModel() -> shared.SyncQueueItem {
        return shared.SyncQueueItem(
            id: id,
            entityType: entityType.rawValue,
            entityId: entityId,
            operation: operation.toKotlinOperation(),
            payload: payloadString ?? "{}",
            localVersion: Int64(localVersion),
            remoteVersion: remoteVersion.map { KotlinLong(value: Int64($0)) },
            lastKnownRemoteUpdatedAt: lastKnownRemoteUpdatedAt.map { KotlinLong(value: $0) },
            status: status.toKotlinStatus(),
            retryCount: Int32(retryCount),
            lastError: lastError,
            lastAttemptAt: lastAttemptAt.map { KotlinLong(value: Int64($0.timeIntervalSince1970 * 1000)) },
            createdAt: Int64(createdAt.timeIntervalSince1970 * 1000),
            userId: userId,
            siteId: siteId
        )
    }

    /// Get payload as string for Kotlin model
    var payloadString: String? {
        guard let data = payload else { return nil }
        return String(data: data, encoding: .utf8)
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
    /// Now uses the shared module's ConflictResolver for consistency
    var conflictStrategy: ConflictStrategy {
        SharedConflictResolver.getStrategy(for: self.rawValue)
    }
}

enum SyncOperation: String, Codable {
    case insert = "INSERT"
    case update = "UPDATE"
    case delete = "DELETE"

    init(from kotlinOp: shared.SyncOperation) {
        switch kotlinOp {
        case .insert: self = .insert
        case .update: self = .update
        case .delete_: self = .delete
        default: self = .insert
        }
    }

    func toKotlinOperation() -> shared.SyncOperation {
        switch self {
        case .insert: return .insert
        case .update: return .update
        case .delete: return .delete_
        }
    }
}

enum SyncStatus: String, Codable {
    case pending = "PENDING"
    case inProgress = "IN_PROGRESS"
    case synced = "SYNCED"
    case conflict = "CONFLICT"
    case failed = "FAILED"

    init(from kotlinStatus: shared.SyncStatus) {
        switch kotlinStatus {
        case .pending: self = .pending
        case .inProgress: self = .inProgress
        case .synced: self = .synced
        case .conflict: self = .conflict
        case .failed: self = .failed
        default: self = .pending
        }
    }

    func toKotlinStatus() -> shared.SyncStatus {
        switch self {
        case .pending: return .pending
        case .inProgress: return .inProgress
        case .synced: return .synced
        case .conflict: return .conflict
        case .failed: return .failed
        }
    }
}

enum ConflictStrategy: String, Codable {
    case serverWins = "SERVER_WINS"
    case clientWins = "CLIENT_WINS"
    case merge = "MERGE"
    case askUser = "ASK_USER"
    case keepBoth = "KEEP_BOTH"

    /// Convert from shared module's ConflictResolution
    init(from sharedResolution: ConflictResolution) {
        switch sharedResolution {
        case .remoteWins:
            self = .serverWins
        case .localWins:
            self = .clientWins
        case .merge:
            self = .merge
        case .askUser:
            self = .askUser
        case .keepBoth:
            self = .keepBoth
        default:
            self = .serverWins
        }
    }

    /// Convert to shared module's ConflictResolution
    var toSharedResolution: ConflictResolution {
        switch self {
        case .serverWins:
            return .remoteWins
        case .clientWins:
            return .localWins
        case .merge:
            return .merge
        case .askUser:
            return .askUser
        case .keepBoth:
            return .keepBoth
        }
    }
}

// MARK: - Shared Conflict Resolver Access

enum SharedConflictResolver {
    private static let resolver = ConflictResolver()

    static func getStrategy(for entityType: String) -> ConflictStrategy {
        let sharedStrategy = resolver.getStrategy(entityType: entityType)
        return ConflictStrategy(from: sharedStrategy)
    }

    static func detectConflict(lastKnownRemoteUpdatedAt: Int64?, remoteUpdatedAt: Int64?) -> Bool {
        let lastKnown: KotlinLong? = lastKnownRemoteUpdatedAt.map { KotlinLong(value: $0) }
        let remote: KotlinLong? = remoteUpdatedAt.map { KotlinLong(value: $0) }
        return resolver.detectConflict(lastKnownRemoteUpdatedAt: lastKnown, remoteUpdatedAt: remote)
    }
}

// MARK: - Sync Configuration
// Now delegates to shared module's RetryConfiguration

struct SyncConfiguration {
    // Get configuration from shared module
    private static let sharedConfig = RetryConfiguration.companion.DEFAULT

    static var maxRetries: Int {
        Int(sharedConfig.maxRetries)
    }

    static var batchSize: Int {
        Int(sharedConfig.batchSize)
    }

    static var syncInterval: TimeInterval {
        Double(sharedConfig.syncIntervalMs) / 1000.0
    }

    static func backoffDelay(for retryCount: Int) -> TimeInterval {
        Double(sharedConfig.getDelayMs(retryCount: Int32(retryCount))) / 1000.0
    }

    // Legacy backoff delays for reference (now computed from shared config)
    static var backoffDelays: [TimeInterval] {
        sharedConfig.backoffDelaysMs.map { Double(truncating: $0) / 1000.0 }
    }
}

// MARK: - Process Result

enum ProcessResult {
    case success
    case conflict(local: Data?, remote: Data?)
    case retry(error: Error)
    case skip(reason: String)
}
