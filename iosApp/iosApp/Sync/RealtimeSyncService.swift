import Foundation
import Supabase
import shared
import Combine

/// Realtime sync service using Supabase Realtime
/// Filters own client's changes using client_id
/// Mirrors Android's RealtimeSyncService
class RealtimeSyncService: ObservableObject {
    static let shared = RealtimeSyncService()

    // MARK: - Published State

    @Published private(set) var isConnected = false
    @Published private(set) var lastEventAt: Date?

    // MARK: - Dependencies

    private let supabase = SupabaseService.shared
    private let statusManager = SyncStatusManager.shared
    private var sdk: MedistockSDK?

    private var channels: [RealtimeChannelV2] = []
    private var cancellables = Set<AnyCancellable>()

    private init() {}

    // MARK: - Public Interface

    /// Start listening to realtime changes
    func start(sdk: MedistockSDK) {
        guard supabase.isConfigured else {
            print("[RealtimeSyncService] Cannot start: Supabase not configured")
            return
        }

        guard let client = supabase.realtimeClient else {
            print("[RealtimeSyncService] Cannot start: No Supabase client")
            return
        }

        self.sdk = sdk

        // Subscribe to all relevant tables
        subscribeToTable("sites", sdk: sdk)
        subscribeToTable("categories", sdk: sdk)
        subscribeToTable("packaging_types", sdk: sdk)
        subscribeToTable("products", sdk: sdk)
        subscribeToTable("customers", sdk: sdk)
        subscribeToTable("purchase_batches", sdk: sdk)
        subscribeToTable("sales", sdk: sdk)
        subscribeToTable("sale_items", sdk: sdk)
        subscribeToTable("stock_movements", sdk: sdk)

        isConnected = true
        print("[RealtimeSyncService] Started listening to realtime changes")
    }

    /// Stop listening to realtime changes
    func stop() {
        Task {
            for channel in channels {
                await channel.unsubscribe()
            }
            channels.removeAll()
        }

        isConnected = false
        print("[RealtimeSyncService] Stopped listening to realtime changes")
    }

    // MARK: - Private Methods

    private func subscribeToTable(_ tableName: String, sdk: MedistockSDK) {
        guard let client = supabase.realtimeClient else { return }

        let channel = client.realtimeV2.channel(tableName)

        // Listen to all changes on the table
        let changes = channel.postgresChange(
            InsertAction.self,
            schema: "public",
            table: tableName
        )

        Task {
            for await action in changes {
                await handleChange(table: tableName, action: .insert, record: action.record, sdk: sdk)
            }
        }

        let updateChanges = channel.postgresChange(
            UpdateAction.self,
            schema: "public",
            table: tableName
        )

        Task {
            for await action in updateChanges {
                await handleChange(table: tableName, action: .update, record: action.record, sdk: sdk)
            }
        }

        let deleteChanges = channel.postgresChange(
            DeleteAction.self,
            schema: "public",
            table: tableName
        )

        Task {
            for await action in deleteChanges {
                await handleChange(table: tableName, action: .delete, record: action.oldRecord, sdk: sdk)
            }
        }

        Task {
            await channel.subscribe()
        }

        channels.append(channel)
    }

    private func handleChange(table: String, action: ChangeAction, record: [String: AnyJSON], sdk: MedistockSDK) async {
        // Filter own client's changes
        if let clientIdValue = record["client_id"],
           case .string(let clientId) = clientIdValue,
           clientId == SyncClientId.current {
            print("[RealtimeSyncService] Ignoring own change for \(table)")
            return
        }

        await MainActor.run {
            lastEventAt = Date()
        }

        print("[RealtimeSyncService] Received \(action) for \(table)")

        // Apply change to local database
        do {
            switch table {
            case "sites":
                try await handleSiteChange(action: action, record: record, sdk: sdk)
            case "categories":
                try await handleCategoryChange(action: action, record: record, sdk: sdk)
            case "packaging_types":
                try await handlePackagingTypeChange(action: action, record: record, sdk: sdk)
            case "products":
                try await handleProductChange(action: action, record: record, sdk: sdk)
            case "customers":
                try await handleCustomerChange(action: action, record: record, sdk: sdk)
            case "purchase_batches":
                try await handlePurchaseBatchChange(action: action, record: record, sdk: sdk)
            case "sales":
                try await handleSaleChange(action: action, record: record, sdk: sdk)
            case "stock_movements":
                try await handleStockMovementChange(action: action, record: record, sdk: sdk)
            default:
                print("[RealtimeSyncService] Unknown table: \(table)")
            }
        } catch {
            print("[RealtimeSyncService] Error handling change: \(error)")
        }
    }

    // MARK: - Entity-Specific Handlers

    private func handleSiteChange(action: ChangeAction, record: [String: AnyJSON], sdk: MedistockSDK) async throws {
        guard let dto = try? decodeRecord(SiteDTO.self, from: record) else { return }
        let entity = dto.toEntity()

        switch action {
        case .insert, .update:
            let existing = try? await sdk.siteRepository.getById(id: dto.id)
            if existing != nil {
                try await sdk.siteRepository.update(site: entity)
            } else {
                try await sdk.siteRepository.insert(site: entity)
            }
        case .delete:
            // Sites typically aren't deleted, but handle it
            break
        }
    }

    private func handleCategoryChange(action: ChangeAction, record: [String: AnyJSON], sdk: MedistockSDK) async throws {
        guard let dto = try? decodeRecord(CategoryDTO.self, from: record) else { return }
        let entity = dto.toEntity()

        switch action {
        case .insert, .update:
            let existing = try? await sdk.categoryRepository.getById(id: dto.id)
            if existing != nil {
                try await sdk.categoryRepository.update(category: entity)
            } else {
                try await sdk.categoryRepository.insert(category: entity)
            }
        case .delete:
            break
        }
    }

    private func handlePackagingTypeChange(action: ChangeAction, record: [String: AnyJSON], sdk: MedistockSDK) async throws {
        guard let dto = try? decodeRecord(PackagingTypeDTO.self, from: record) else { return }
        let entity = dto.toEntity()

        switch action {
        case .insert, .update:
            let existing = try? await sdk.packagingTypeRepository.getById(id: dto.id)
            if existing != nil {
                try await sdk.packagingTypeRepository.update(packagingType: entity)
            } else {
                try await sdk.packagingTypeRepository.insert(packagingType: entity)
            }
        case .delete:
            break
        }
    }

    private func handleProductChange(action: ChangeAction, record: [String: AnyJSON], sdk: MedistockSDK) async throws {
        guard let dto = try? decodeRecord(ProductDTO.self, from: record) else { return }
        let entity = dto.toEntity()

        switch action {
        case .insert, .update:
            let existing = try? await sdk.productRepository.getById(id: dto.id)
            if existing != nil {
                try await sdk.productRepository.update(product: entity)
            } else {
                try await sdk.productRepository.insert(product: entity)
            }
        case .delete:
            try await sdk.productRepository.delete(id: dto.id)
        }
    }

    private func handleCustomerChange(action: ChangeAction, record: [String: AnyJSON], sdk: MedistockSDK) async throws {
        guard let dto = try? decodeRecord(CustomerDTO.self, from: record) else { return }
        let entity = dto.toEntity()

        switch action {
        case .insert, .update:
            let existing = try? await sdk.customerRepository.getById(id: dto.id)
            if existing != nil {
                try await sdk.customerRepository.update(customer: entity)
            } else {
                try await sdk.customerRepository.insert(customer: entity)
            }
        case .delete:
            break
        }
    }

    private func handlePurchaseBatchChange(action: ChangeAction, record: [String: AnyJSON], sdk: MedistockSDK) async throws {
        guard let dto = try? decodeRecord(PurchaseBatchDTO.self, from: record) else { return }
        let entity = dto.toEntity()

        switch action {
        case .insert:
            try await sdk.purchaseBatchRepository.insert(batch: entity)
        case .update:
            try await sdk.purchaseBatchRepository.updateQuantity(
                id: dto.id,
                remainingQuantity: dto.remainingQuantity,
                isExhausted: dto.isExhausted,
                updatedAt: dto.updatedAt,
                updatedBy: dto.updatedBy
            )
        case .delete:
            break
        }
    }

    private func handleSaleChange(action: ChangeAction, record: [String: AnyJSON], sdk: MedistockSDK) async throws {
        guard let dto = try? decodeRecord(SaleDTO.self, from: record) else { return }
        let entity = dto.toEntity()

        switch action {
        case .insert:
            // Sales need items too - will be handled by sale_items changes
            try await sdk.saleRepository.insertSaleWithItems(sale: entity, items: [])
        case .update, .delete:
            break
        }
    }

    private func handleStockMovementChange(action: ChangeAction, record: [String: AnyJSON], sdk: MedistockSDK) async throws {
        guard let dto = try? decodeRecord(StockMovementDTO.self, from: record) else { return }
        let entity = dto.toEntity()

        switch action {
        case .insert:
            try await sdk.stockMovementRepository.insert(movement: entity)
        case .update, .delete:
            // Stock movements are append-only
            break
        }
    }

    // MARK: - Helpers

    private func decodeRecord<T: Decodable>(_ type: T.Type, from record: [String: AnyJSON]) throws -> T {
        let data = try JSONEncoder().encode(record)
        return try JSONDecoder().decode(T.self, from: data)
    }
}

// MARK: - Supporting Types

private enum ChangeAction {
    case insert
    case update
    case delete
}
