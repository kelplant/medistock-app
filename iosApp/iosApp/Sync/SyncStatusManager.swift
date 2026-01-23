import Foundation
import Combine
import Network
import shared

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
        pendingCount == 0 && conflictCount == 0 && !isSyncing
    }

    var hasIssues: Bool {
        conflictCount > 0 || (!lastSyncInfo.success && lastSyncInfo.hasEverSynced)
    }

    /// Indicator color with same priority order as shared Kotlin model:
    /// hasIssues > !isOnline > isSyncing > pendingCount > synced
    var indicatorColor: IndicatorColor {
        if hasIssues {
            return .error
        }
        if !isOnline {
            return .offline
        }
        if isSyncing {
            return .syncing
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

    /// Refresh counts from the shared repository
    func refreshFromRepository() {
        Task {
            await store.refreshCounts()
        }
    }
}

// MARK: - Supporting Types
// These Swift types mirror the shared Kotlin models in:
// shared/src/commonMain/kotlin/com/medistock/shared/domain/sync/SyncStatusModel.kt
// Swift types are used for SwiftUI compatibility and Codable conformance.

/// Sync mode enum - mirrors shared.SyncMode
enum SyncMode: String, Codable {
    case automatic = "AUTOMATIC"
    case manual = "MANUAL"
    case offlineForced = "OFFLINE_FORCED"

    /// Convert from shared Kotlin SyncMode
    init(from kotlinMode: shared.SyncMode) {
        switch kotlinMode {
        case .automatic: self = .automatic
        case .manual: self = .manual
        case .offlineForced: self = .offlineForced
        default: self = .automatic
        }
    }

    /// Convert to shared Kotlin SyncMode
    func toKotlin() -> shared.SyncMode {
        switch self {
        case .automatic: return .automatic
        case .manual: return .manual
        case .offlineForced: return .offlineForced
        }
    }
}

/// Last sync info - mirrors shared.LastSyncInfo
/// Uses Date? instead of Long? for Swift convenience
struct LastSyncInfo {
    var timestamp: Date?
    var success: Bool = true
    var error: String?

    var hasEverSynced: Bool { timestamp != nil }

    /// Convert from shared Kotlin LastSyncInfo
    init(from kotlinInfo: shared.LastSyncInfo) {
        if let ts = kotlinInfo.timestamp?.int64Value {
            self.timestamp = Date(timeIntervalSince1970: TimeInterval(ts) / 1000.0)
        }
        self.success = kotlinInfo.success
        self.error = kotlinInfo.error
    }

    init(timestamp: Date? = nil, success: Bool = true, error: String? = nil) {
        self.timestamp = timestamp
        self.success = success
        self.error = error
    }

    /// Convert to shared Kotlin LastSyncInfo
    func toKotlin() -> shared.LastSyncInfo {
        let ts: KotlinLong? = timestamp.map { KotlinLong(value: Int64($0.timeIntervalSince1970 * 1000)) }
        return shared.LastSyncInfo(timestamp: ts, success: success, error: error)
    }
}

/// Global sync status - mirrors shared.GlobalSyncStatus
struct GlobalSyncStatus {
    let pendingCount: Int
    let conflictCount: Int
    let isOnline: Bool
    let syncMode: SyncMode
    let lastSyncInfo: LastSyncInfo
    let isSyncing: Bool

    var isFullySynced: Bool {
        pendingCount == 0 && conflictCount == 0 && !isSyncing
    }

    var hasIssues: Bool {
        conflictCount > 0 || (!lastSyncInfo.success && lastSyncInfo.hasEverSynced)
    }

    /// Convert from shared Kotlin GlobalSyncStatus
    init(from kotlinStatus: shared.GlobalSyncStatus) {
        self.pendingCount = Int(kotlinStatus.pendingCount)
        self.conflictCount = Int(kotlinStatus.conflictCount)
        self.isOnline = kotlinStatus.isOnline
        self.syncMode = SyncMode(from: kotlinStatus.syncMode)
        self.lastSyncInfo = LastSyncInfo(from: kotlinStatus.lastSyncInfo)
        self.isSyncing = kotlinStatus.isSyncing
    }

    init(pendingCount: Int, conflictCount: Int, isOnline: Bool, syncMode: SyncMode, lastSyncInfo: LastSyncInfo, isSyncing: Bool) {
        self.pendingCount = pendingCount
        self.conflictCount = conflictCount
        self.isOnline = isOnline
        self.syncMode = syncMode
        self.lastSyncInfo = lastSyncInfo
        self.isSyncing = isSyncing
    }

    /// Convert to shared Kotlin GlobalSyncStatus
    func toKotlin() -> shared.GlobalSyncStatus {
        shared.GlobalSyncStatus(
            pendingCount: Int32(pendingCount),
            conflictCount: Int32(conflictCount),
            isOnline: isOnline,
            syncMode: syncMode.toKotlin(),
            lastSyncInfo: lastSyncInfo.toKotlin(),
            isSyncing: isSyncing
        )
    }
}

/// Indicator color enum - mirrors shared.SyncIndicatorColor
enum IndicatorColor {
    case synced     // Green - fully synchronized
    case pending    // Yellow - modifications waiting
    case syncing    // Blue - sync in progress
    case offline    // Gray - no network
    case error      // Red - conflicts or failures

    /// Convert from shared Kotlin SyncIndicatorColor
    init(from kotlinColor: shared.SyncIndicatorColor) {
        switch kotlinColor {
        case .synced: self = .synced
        case .pending: self = .pending
        case .syncing: self = .syncing
        case .offline: self = .offline
        case .error: self = .error
        default: self = .synced
        }
    }

    /// Convert to shared Kotlin SyncIndicatorColor
    func toKotlin() -> shared.SyncIndicatorColor {
        switch self {
        case .synced: return .synced
        case .pending: return .pending
        case .syncing: return .syncing
        case .offline: return .offline
        case .error: return .error
        }
    }
}
