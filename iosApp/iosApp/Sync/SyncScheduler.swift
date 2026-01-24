import Foundation
import shared
import Combine
import BackgroundTasks

/// Orchestrates sync scheduling, network monitoring, and background sync
/// Mirrors Android's SyncScheduler
class SyncScheduler: ObservableObject {
    static let shared = SyncScheduler()

    // MARK: - Published State

    @Published private(set) var isRunning = false

    // MARK: - Dependencies

    private let statusManager = SyncStatusManager.shared
    private let syncManager = BidirectionalSyncManager.shared
    private let queueProcessor = SyncQueueProcessor.shared
    private let realtimeService = RealtimeSyncService.shared

    private var sdk: MedistockSDK?
    private var syncTimer: Timer?
    private var cancellables = Set<AnyCancellable>()

    private let syncInterval = SyncConfiguration.syncInterval

    private init() {
        setupNetworkCallbacks()
    }

    // MARK: - Public Interface

    /// Start the sync scheduler
    func start(sdk: MedistockSDK) {
        guard !isRunning else { return }

        self.sdk = sdk
        isRunning = true

        // Start realtime service
        realtimeService.start(sdk: sdk)

        // Schedule periodic sync
        schedulePeriodicSync()

        // Trigger immediate sync if online
        if statusManager.isOnline {
            triggerImmediate(reason: "App started")
        }

        debugLog("SyncScheduler", "Started")
    }

    /// Stop the sync scheduler
    func stop() {
        isRunning = false
        syncTimer?.invalidate()
        syncTimer = nil
        realtimeService.stop()
        queueProcessor.stopProcessing()

        debugLog("SyncScheduler", "Stopped")
    }

    /// Trigger an immediate sync
    func triggerImmediate(reason: String = "Manual trigger") {
        guard isRunning, let sdk = sdk else {
            debugLog("SyncScheduler", "Cannot trigger sync: not running or no SDK")
            return
        }

        guard statusManager.isOnline else {
            debugLog("SyncScheduler", "Cannot trigger sync: offline")
            return
        }

        debugLog("SyncScheduler", "Triggering immediate sync: \(reason)")

        Task {
            await syncManager.fullSync(sdk: sdk)
        }
    }

    /// Process only the local queue (push changes)
    func processQueue() {
        guard isRunning else { return }
        guard statusManager.isOnline else { return }

        queueProcessor.startProcessing()
    }

    // MARK: - Background Tasks (iOS)

    /// Register background task for periodic sync
    static func registerBackgroundTask() {
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: "com.medistock.sync",
            using: nil
        ) { task in
            SyncScheduler.shared.handleBackgroundTask(task as! BGProcessingTask)
        }
    }

    /// Schedule the next background sync
    func scheduleBackgroundSync() {
        let request = BGProcessingTaskRequest(identifier: "com.medistock.sync")
        request.requiresNetworkConnectivity = true
        request.requiresExternalPower = false
        request.earliestBeginDate = Date(timeIntervalSinceNow: syncInterval)

        do {
            try BGTaskScheduler.shared.submit(request)
            debugLog("SyncScheduler", "Background sync scheduled")
        } catch {
            debugLog("SyncScheduler", "Failed to schedule background sync: \(error)")
        }
    }

    private func handleBackgroundTask(_ task: BGProcessingTask) {
        // Set expiration handler
        task.expirationHandler = {
            self.queueProcessor.stopProcessing()
            task.setTaskCompleted(success: false)
        }

        guard let sdk = sdk else {
            task.setTaskCompleted(success: false)
            return
        }

        // Restore session if needed before syncing
        let keychain = KeychainService.shared
        guard keychain.hasAuthTokens && !keychain.areAuthTokensExpired else {
            debugLog("SyncScheduler", "No valid session for background sync, skipping")
            task.setTaskCompleted(success: false)
            scheduleBackgroundSync()
            return
        }

        Task {
            // Restore session on Supabase client
            await SupabaseService.shared.restoreSessionIfNeeded()
            await syncManager.fullSync(sdk: sdk)
            task.setTaskCompleted(success: true)
            scheduleBackgroundSync()
        }
    }

    // MARK: - Private Methods

    private func setupNetworkCallbacks() {
        statusManager.onNetworkAvailable = { [weak self] in
            debugLog("SyncScheduler", "Network available - triggering sync")
            self?.triggerImmediate(reason: "Network reconnected")

            // Restart realtime if needed
            if let sdk = self?.sdk {
                self?.realtimeService.start(sdk: sdk)
            }
        }

        statusManager.onNetworkLost = { [weak self] in
            debugLog("SyncScheduler", "Network lost - stopping realtime")
            self?.realtimeService.stop()
        }
    }

    private func schedulePeriodicSync() {
        syncTimer?.invalidate()

        syncTimer = Timer.scheduledTimer(withTimeInterval: syncInterval, repeats: true) { [weak self] _ in
            guard let self = self else { return }
            guard self.statusManager.isOnline else { return }
            guard self.statusManager.needsSync() else {
                debugLog("SyncScheduler", "No pending changes, skipping periodic sync")
                return
            }

            self.triggerImmediate(reason: "Periodic sync")
        }
    }
}
