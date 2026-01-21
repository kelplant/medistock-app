import Foundation
import Combine
import Network

/// Manages and exposes sync state to UI
/// Mirrors Android's SyncStatusManager
class SyncStatusManager: ObservableObject {
    static let shared = SyncStatusManager()

    // MARK: - Published State

    @Published private(set) var pendingCount: Int = 0
    @Published private(set) var conflictCount: Int = 0
    @Published private(set) var isOnline: Bool = true
    @Published private(set) var syncMode: SyncMode = .automatic
    @Published private(set) var lastSyncInfo: LastSyncInfo = LastSyncInfo()
    @Published private(set) var isSyncing: Bool = false

    // MARK: - Computed Properties

    var globalStatus: GlobalSyncStatus {
        GlobalSyncStatus(
            pendingCount: pendingCount,
            conflictCount: conflictCount,
            isOnline: isOnline,
            syncMode: syncMode,
            lastSyncInfo: lastSyncInfo,
            isSyncing: isSyncing
        )
    }

    var isFullySynced: Bool {
        pendingCount == 0 && conflictCount == 0
    }

    var hasIssues: Bool {
        conflictCount > 0 || !lastSyncInfo.success
    }

    var indicatorColor: IndicatorColor {
        if isSyncing {
            return .syncing
        }
        if !isOnline {
            return .offline
        }
        if conflictCount > 0 || !lastSyncInfo.success {
            return .error
        }
        if pendingCount > 0 {
            return .pending
        }
        return .synced
    }

    var statusSummary: String {
        if isSyncing {
            return "Synchronisation en cours..."
        }
        if !isOnline {
            return "Mode hors ligne"
        }
        if conflictCount > 0 {
            return "\(conflictCount) conflit(s) à résoudre"
        }
        if pendingCount > 0 {
            return "\(pendingCount) modification(s) en attente"
        }
        if let lastSync = lastSyncInfo.timestamp {
            let formatter = RelativeDateTimeFormatter()
            formatter.locale = Locale(identifier: "fr_FR")
            return "Synchronisé \(formatter.localizedString(for: lastSync, relativeTo: Date()))"
        }
        return "Synchronisé"
    }

    // MARK: - Private

    private let monitor = NWPathMonitor()
    private let monitorQueue = DispatchQueue(label: "com.medistock.networkmonitor")
    private var cancellables = Set<AnyCancellable>()
    private let store = SyncQueueStore.shared

    private let lastSyncKey = "medistock_last_sync"
    private let lastSyncSuccessKey = "medistock_last_sync_success"
    private let syncModeKey = "medistock_sync_mode"

    // MARK: - Callbacks for external components

    var onNetworkAvailable: (() -> Void)?
    var onNetworkLost: (() -> Void)?

    private init() {
        loadPersistedState()
        setupNetworkMonitoring()
        observeStore()
    }

    // MARK: - Public Methods

    func updateConnectionStatus(_ online: Bool) {
        let wasOffline = !isOnline
        DispatchQueue.main.async {
            self.isOnline = online
        }

        if wasOffline && online {
            onNetworkAvailable?()
        } else if !online && !wasOffline {
            onNetworkLost?()
        }
    }

    func setSyncMode(_ mode: SyncMode) {
        DispatchQueue.main.async {
            self.syncMode = mode
        }
        UserDefaults.standard.set(mode.rawValue, forKey: syncModeKey)
    }

    func recordSyncSuccess() {
        let now = Date()
        DispatchQueue.main.async {
            self.lastSyncInfo = LastSyncInfo(timestamp: now, success: true)
        }
        UserDefaults.standard.set(now.timeIntervalSince1970, forKey: lastSyncKey)
        UserDefaults.standard.set(true, forKey: lastSyncSuccessKey)
    }

    func recordSyncFailure(error: String) {
        let now = Date()
        DispatchQueue.main.async {
            self.lastSyncInfo = LastSyncInfo(timestamp: now, success: false, error: error)
        }
        UserDefaults.standard.set(now.timeIntervalSince1970, forKey: lastSyncKey)
        UserDefaults.standard.set(false, forKey: lastSyncSuccessKey)
    }

    func setSyncing(_ syncing: Bool) {
        DispatchQueue.main.async {
            self.isSyncing = syncing
        }
    }

    func needsSync() -> Bool {
        pendingCount > 0
    }

    func hasConflicts() -> Bool {
        conflictCount > 0
    }

    // MARK: - Private Methods

    private func loadPersistedState() {
        if let timestamp = UserDefaults.standard.object(forKey: lastSyncKey) as? Double {
            let date = Date(timeIntervalSince1970: timestamp)
            let success = UserDefaults.standard.bool(forKey: lastSyncSuccessKey)
            lastSyncInfo = LastSyncInfo(timestamp: date, success: success)
        }

        if let modeRaw = UserDefaults.standard.string(forKey: syncModeKey),
           let mode = SyncMode(rawValue: modeRaw) {
            syncMode = mode
        }
    }

    private func setupNetworkMonitoring() {
        monitor.pathUpdateHandler = { [weak self] path in
            let online = path.status == .satisfied
            self?.updateConnectionStatus(online)
        }
        monitor.start(queue: monitorQueue)
    }

    private func observeStore() {
        store.$pendingCount
            .receive(on: DispatchQueue.main)
            .assign(to: &$pendingCount)

        store.$conflictCount
            .receive(on: DispatchQueue.main)
            .assign(to: &$conflictCount)
    }
}

// MARK: - Supporting Types

enum SyncMode: String, Codable {
    case automatic = "AUTOMATIC"
    case manual = "MANUAL"
    case offlineForced = "OFFLINE_FORCED"
}

struct LastSyncInfo {
    var timestamp: Date?
    var success: Bool = true
    var error: String?
}

struct GlobalSyncStatus {
    let pendingCount: Int
    let conflictCount: Int
    let isOnline: Bool
    let syncMode: SyncMode
    let lastSyncInfo: LastSyncInfo
    let isSyncing: Bool
}

enum IndicatorColor {
    case synced     // Green - fully synchronized
    case pending    // Yellow - modifications waiting
    case syncing    // Blue - sync in progress
    case offline    // Gray - no network
    case error      // Red - conflicts or failures
}
