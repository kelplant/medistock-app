import Foundation
import shared

/// Helper for implementing online-first pattern in Views
/// Handles: Supabase fetch -> Local sync -> Local read -> Queue if offline
class OnlineFirstHelper {
    static let shared = OnlineFirstHelper()

    private let supabase = SupabaseService.shared
    private let syncStatus = SyncStatusManager.shared
    private let queueHelper = SyncQueueHelper.shared

    private init() {}

    // MARK: - Generic Fetch (Online-First Read)

    /// Fetch data with online-first pattern
    /// 1. If online: Fetch from Supabase, sync to local
    /// 2. Always return from local database
    func fetchAll<DTO: Decodable, Entity>(
        table: String,
        dtoType: DTO.Type,
        localFetch: () async throws -> [Entity],
        syncToLocal: (DTO) async throws -> Void
    ) async throws -> [Entity] {
        // Step 1: Try online fetch and sync
        if syncStatus.isOnline && supabase.isConfigured {
            do {
                let remoteDTOs: [DTO] = try await supabase.fetchAll(from: table)
                for dto in remoteDTOs {
                    try? await syncToLocal(dto)
                }
            } catch {
                print("[OnlineFirstHelper] Remote fetch failed for \(table): \(error)")
                // Continue to local fetch
            }
        }

        // Step 2: Always return from local
        return try await localFetch()
    }

    // MARK: - Generic Save (Online-First Write)

    /// Save data with online-first pattern
    /// 1. If online: Save to Supabase first
    /// 2. Save to local database
    /// 3. If offline: Queue for later sync
    func save<DTO: Encodable>(
        table: String,
        dto: DTO,
        entityType: EntityType,
        entityId: String,
        isNew: Bool,
        userId: String,
        siteId: String? = nil,
        localSave: () async throws -> Void,
        lastKnownRemoteUpdatedAt: Int64? = nil
    ) async throws {
        // Step 1: Try online save
        var savedOnline = false
        if syncStatus.isOnline && supabase.isConfigured {
            do {
                try await supabase.upsert(into: table, record: dto)
                savedOnline = true
            } catch {
                print("[OnlineFirstHelper] Remote save failed for \(table): \(error)")
                // Continue to local save and queue
            }
        }

        // Step 2: Save locally
        try await localSave()

        // Step 3: Queue if not saved online
        if !savedOnline {
            if isNew {
                queueHelper.enqueueInsert(
                    entityType: entityType,
                    entityId: entityId,
                    entity: dto,
                    userId: userId,
                    siteId: siteId
                )
            } else {
                queueHelper.enqueueUpdate(
                    entityType: entityType,
                    entityId: entityId,
                    entity: dto,
                    userId: userId,
                    siteId: siteId,
                    lastKnownRemoteUpdatedAt: lastKnownRemoteUpdatedAt
                )
            }
        }
    }

    /// Delete data with online-first pattern
    func delete(
        table: String,
        entityType: EntityType,
        entityId: String,
        userId: String,
        siteId: String? = nil,
        localDelete: () async throws -> Void
    ) async throws {
        // Step 1: Try online delete
        var deletedOnline = false
        if syncStatus.isOnline && supabase.isConfigured {
            do {
                try await supabase.delete(from: table, id: entityId)
                deletedOnline = true
            } catch {
                print("[OnlineFirstHelper] Remote delete failed for \(table): \(error)")
            }
        }

        // Step 2: Delete locally
        try await localDelete()

        // Step 3: Queue if not deleted online
        if !deletedOnline {
            queueHelper.enqueueDelete(
                entityType: entityType,
                entityId: entityId,
                userId: userId,
                siteId: siteId
            )
        }
    }
}

// MARK: - View Extension for Online-First Pattern

extension View {
    /// Add pull-to-refresh that triggers full sync
    func syncRefreshable(sdk: MedistockSDK, onRefresh: @escaping () async -> Void) -> some View {
        self.refreshable {
            await BidirectionalSyncManager.shared.fullSync(sdk: sdk)
            await onRefresh()
        }
    }
}
