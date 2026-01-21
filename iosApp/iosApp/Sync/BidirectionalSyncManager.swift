import Foundation
import shared
import Combine

/// Bidirectional sync manager for full data synchronization
/// Handles both syncLocalToRemote and syncRemoteToLocal
/// Mirrors Android's SyncManager
class BidirectionalSyncManager: ObservableObject {
    static let shared = BidirectionalSyncManager()

    // MARK: - Published State

    @Published private(set) var isSyncing = false
    @Published private(set) var progress: String = ""
    @Published private(set) var lastError: String?

    // MARK: - Dependencies

    private let supabase = SupabaseService.shared
    private let statusManager = SyncStatusManager.shared
    private let queueProcessor = SyncQueueProcessor.shared

    private init() {}

    // MARK: - Public Interface

    /// Perform full bidirectional sync
    /// 1. Push local changes (process queue)
    /// 2. Pull remote changes
    func fullSync(sdk: MedistockSDK) async {
        guard supabase.isConfigured else {
            await MainActor.run {
                lastError = "Supabase n'est pas configuré"
            }
            return
        }

        guard statusManager.isOnline else {
            await MainActor.run {
                lastError = "Mode hors ligne"
            }
            return
        }

        await MainActor.run {
            isSyncing = true
            lastError = nil
            progress = "Démarrage..."
        }

        // Step 1: Push local changes
        await updateProgress("Envoi des modifications locales...")
        queueProcessor.startProcessing()

        // Wait for queue processing to complete
        while queueProcessor.state.isProcessing {
            try? await Task.sleep(nanoseconds: 100_000_000) // 100ms
        }

        // Step 2: Pull remote changes
        await syncRemoteToLocal(sdk: sdk)

        await MainActor.run {
            isSyncing = false
            progress = "Terminé"
            statusManager.recordSyncSuccess()
        }
    }

    /// Push local changes to Supabase (process the queue)
    func syncLocalToRemote() async {
        await MainActor.run {
            isSyncing = true
            progress = "Envoi des modifications..."
        }

        queueProcessor.startProcessing()

        while queueProcessor.state.isProcessing {
            try? await Task.sleep(nanoseconds: 100_000_000)
        }

        await MainActor.run {
            isSyncing = false
        }
    }

    /// Pull remote changes to local database
    func syncRemoteToLocal(sdk: MedistockSDK) async {
        await MainActor.run {
            isSyncing = true
        }

        do {
            // Sync in dependency order
            await updateProgress("Récupération des sites...")
            try await syncSitesFromRemote(sdk: sdk)

            await updateProgress("Récupération des catégories...")
            try await syncCategoriesFromRemote(sdk: sdk)

            await updateProgress("Récupération des conditionnements...")
            try await syncPackagingTypesFromRemote(sdk: sdk)

            await updateProgress("Récupération des produits...")
            try await syncProductsFromRemote(sdk: sdk)

            await updateProgress("Récupération des utilisateurs...")
            try await syncUsersFromRemote(sdk: sdk)

            await updateProgress("Récupération des clients...")
            try await syncCustomersFromRemote(sdk: sdk)

            await updateProgress("Récupération des achats...")
            try await syncPurchaseBatchesFromRemote(sdk: sdk)

            await updateProgress("Récupération des ventes...")
            try await syncSalesFromRemote(sdk: sdk)

            await updateProgress("Récupération des mouvements...")
            try await syncStockMovementsFromRemote(sdk: sdk)

            await MainActor.run {
                isSyncing = false
                progress = "Récupération terminée"
            }

        } catch {
            await MainActor.run {
                isSyncing = false
                lastError = error.localizedDescription
            }
        }
    }

    // MARK: - Individual Sync Methods

    private func syncSitesFromRemote(sdk: MedistockSDK) async throws {
        let remoteSites: [SiteDTO] = try await supabase.fetchAll(from: "sites")

        for dto in remoteSites {
            // Skip if this change came from this client
            if dto.clientId == SyncClientId.current { continue }

            let entity = dto.toEntity()
            let existing = try? await sdk.siteRepository.getById(id: dto.id)

            if existing != nil {
                try? await sdk.siteRepository.update(site: entity)
            } else {
                try? await sdk.siteRepository.insert(site: entity)
            }
        }
    }

    private func syncCategoriesFromRemote(sdk: MedistockSDK) async throws {
        let remoteCategories: [CategoryDTO] = try await supabase.fetchAll(from: "categories")

        for dto in remoteCategories {
            if dto.clientId == SyncClientId.current { continue }

            let entity = dto.toEntity()
            let existing = try? await sdk.categoryRepository.getById(id: dto.id)

            if existing != nil {
                try? await sdk.categoryRepository.update(category: entity)
            } else {
                try? await sdk.categoryRepository.insert(category: entity)
            }
        }
    }

    private func syncPackagingTypesFromRemote(sdk: MedistockSDK) async throws {
        let remoteTypes: [PackagingTypeDTO] = try await supabase.fetchAll(from: "packaging_types")

        for dto in remoteTypes {
            if dto.clientId == SyncClientId.current { continue }

            let entity = dto.toEntity()
            let existing = try? await sdk.packagingTypeRepository.getById(id: dto.id)

            if existing != nil {
                try? await sdk.packagingTypeRepository.update(packagingType: entity)
            } else {
                try? await sdk.packagingTypeRepository.insert(packagingType: entity)
            }
        }
    }

    private func syncProductsFromRemote(sdk: MedistockSDK) async throws {
        let remoteProducts: [ProductDTO] = try await supabase.fetchAll(from: "products")

        for dto in remoteProducts {
            if dto.clientId == SyncClientId.current { continue }

            let entity = dto.toEntity()
            let existing = try? await sdk.productRepository.getById(id: dto.id)

            if existing != nil {
                try? await sdk.productRepository.update(product: entity)
            } else {
                try? await sdk.productRepository.insert(product: entity)
            }
        }
    }

    private func syncUsersFromRemote(sdk: MedistockSDK) async throws {
        let remoteUsers: [UserDTO] = try await supabase.fetchAll(from: "app_users")

        for dto in remoteUsers {
            let entity = dto.toEntity()
            let existing = try? await sdk.userRepository.getById(id: dto.id)

            if existing != nil {
                try? await sdk.userRepository.update(user: entity)
            } else {
                try? await sdk.userRepository.insert(user: entity)
            }
        }
    }

    private func syncCustomersFromRemote(sdk: MedistockSDK) async throws {
        let remoteCustomers: [CustomerDTO] = try await supabase.fetchAll(from: "customers")

        for dto in remoteCustomers {
            if dto.clientId == SyncClientId.current { continue }

            let entity = dto.toEntity()
            let existing = try? await sdk.customerRepository.getById(id: dto.id)

            if existing != nil {
                try? await sdk.customerRepository.update(customer: entity)
            } else {
                try? await sdk.customerRepository.insert(customer: entity)
            }
        }
    }

    private func syncPurchaseBatchesFromRemote(sdk: MedistockSDK) async throws {
        let remoteBatches: [PurchaseBatchDTO] = try await supabase.fetchAll(from: "purchase_batches")

        for dto in remoteBatches {
            if dto.clientId == SyncClientId.current { continue }

            let entity = dto.toEntity()
            let existing = try? await sdk.purchaseBatchRepository.getById(id: dto.id)

            if existing != nil {
                try? await sdk.purchaseBatchRepository.updateQuantity(
                    id: dto.id,
                    remainingQuantity: dto.remainingQuantity,
                    isExhausted: dto.isExhausted,
                    updatedAt: dto.updatedAt,
                    updatedBy: dto.updatedBy
                )
            } else {
                try? await sdk.purchaseBatchRepository.insert(batch: entity)
            }
        }
    }

    private func syncSalesFromRemote(sdk: MedistockSDK) async throws {
        let remoteSales: [SaleDTO] = try await supabase.fetchAll(from: "sales")
        let remoteSaleItems: [SaleItemDTO] = try await supabase.fetchAll(from: "sale_items")

        let itemsBySale = Dictionary(grouping: remoteSaleItems) { $0.saleId }

        for saleDto in remoteSales {
            if saleDto.clientId == SyncClientId.current { continue }

            let saleEntity = saleDto.toEntity()
            let existing = try? await sdk.saleRepository.getById(id: saleDto.id)

            if existing == nil {
                let items = (itemsBySale[saleDto.id] ?? []).map { $0.toEntity() }
                try? await sdk.saleRepository.insertSaleWithItems(sale: saleEntity, items: items)
            }
        }
    }

    private func syncStockMovementsFromRemote(sdk: MedistockSDK) async throws {
        let remoteMovements: [StockMovementDTO] = try await supabase.fetchAll(from: "stock_movements")

        for dto in remoteMovements {
            if dto.clientId == SyncClientId.current { continue }

            let entity = dto.toEntity()
            // Stock movements are append-only
            try? await sdk.stockMovementRepository.insert(movement: entity)
        }
    }

    // MARK: - Helper

    private func updateProgress(_ message: String) async {
        await MainActor.run {
            progress = message
        }
    }
}
