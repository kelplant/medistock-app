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
    private let orchestrator = SyncOrchestrator()

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
            progress = orchestrator.getCompletionMessage(direction: .bidirectional)
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
                progress = orchestrator.getCompletionMessage(direction: .remoteToLocal)
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

            // Use upsert (INSERT OR REPLACE) to handle both new and existing records
            try? await sdk.siteRepository.upsert(site: dto.toEntity())
        }
    }

    private func syncCategoriesFromRemote(sdk: MedistockSDK) async throws {
        let remoteCategories: [CategoryDTO] = try await supabase.fetchAll(from: "categories")

        for dto in remoteCategories {
            if dto.clientId == SyncClientId.current { continue }

            // Use upsert (INSERT OR REPLACE) to handle both new and existing records
            try? await sdk.categoryRepository.upsert(category: dto.toEntity())
        }
    }

    private func syncPackagingTypesFromRemote(sdk: MedistockSDK) async throws {
        let remoteTypes: [PackagingTypeDTO] = try await supabase.fetchAll(from: "packaging_types")

        for dto in remoteTypes {
            if dto.clientId == SyncClientId.current { continue }

            // Use upsert (INSERT OR REPLACE) to handle both new and existing records
            try? await sdk.packagingTypeRepository.upsert(packagingType: dto.toEntity())
        }
    }

    private func syncProductsFromRemote(sdk: MedistockSDK) async throws {
        let remoteProducts: [ProductDTO] = try await supabase.fetchAll(from: "products")

        for dto in remoteProducts {
            if dto.clientId == SyncClientId.current { continue }

            // Use upsert (INSERT OR REPLACE) to handle both new and existing records
            try? await sdk.productRepository.upsert(product: dto.toEntity())
        }
    }

    private func syncUsersFromRemote(sdk: MedistockSDK) async throws {
        let remoteUsers: [UserDTO] = try await supabase.fetchAll(from: "app_users")

        for dto in remoteUsers {
            // Use upsert (INSERT OR REPLACE) to handle both new and existing records
            try? await sdk.userRepository.upsert(user: dto.toEntity())
        }

        // Remove local system admin if real users were synced
        if !remoteUsers.isEmpty {
            let removed = try await sdk.defaultAdminService.removeLocalAdminIfRemoteUsersExist()
            if removed.boolValue {
                debugLog("BidirectionalSyncManager", "Local system admin removed after syncing \(remoteUsers.count) remote users")
            }
        }
    }

    private func syncCustomersFromRemote(sdk: MedistockSDK) async throws {
        let remoteCustomers: [CustomerDTO] = try await supabase.fetchAll(from: "customers")

        for dto in remoteCustomers {
            if dto.clientId == SyncClientId.current { continue }

            // Use upsert (INSERT OR REPLACE) to handle both new and existing records
            try? await sdk.customerRepository.upsert(customer: dto.toEntity())
        }
    }

    private func syncPurchaseBatchesFromRemote(sdk: MedistockSDK) async throws {
        let remoteBatches: [PurchaseBatchDTO] = try await supabase.fetchAll(from: "purchase_batches")

        for dto in remoteBatches {
            if dto.clientId == SyncClientId.current { continue }

            // Use upsert (INSERT OR REPLACE) to handle both new and existing records
            try? await sdk.purchaseBatchRepository.upsert(batch: dto.toEntity())
        }
    }

    private func syncSalesFromRemote(sdk: MedistockSDK) async throws {
        let remoteSales: [SaleDTO] = try await supabase.fetchAll(from: "sales")
        let remoteSaleItems: [SaleItemDTO] = try await supabase.fetchAll(from: "sale_items")

        for saleDto in remoteSales {
            if saleDto.clientId == SyncClientId.current { continue }

            // Use upsert (INSERT OR REPLACE) to handle both new and existing records
            try? await sdk.saleRepository.upsert(sale: saleDto.toEntity())
        }

        // Upsert sale items separately
        for itemDto in remoteSaleItems {
            if itemDto.clientId == SyncClientId.current { continue }

            try? await sdk.saleRepository.upsertSaleItem(item: itemDto.toEntity())
        }
    }

    private func syncStockMovementsFromRemote(sdk: MedistockSDK) async throws {
        let remoteMovements: [StockMovementDTO] = try await supabase.fetchAll(from: "stock_movements")

        for dto in remoteMovements {
            if dto.clientId == SyncClientId.current { continue }

            // Use upsert (INSERT OR REPLACE) to handle both new and existing records
            try? await sdk.stockMovementRepository.upsert(movement: dto.toEntity())
        }
    }

    // MARK: - Helper

    private func updateProgress(_ message: String) async {
        await MainActor.run {
            progress = message
        }
    }
}
