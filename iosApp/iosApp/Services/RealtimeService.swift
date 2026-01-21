import Foundation
import Combine
import shared

/// Realtime connection status
enum RealtimeStatus: String {
    case connected
    case connecting
    case disconnected
}

/// Service for managing Realtime connection with Supabase
/// Mirrors Android RealtimeSyncService functionality
class RealtimeService: ObservableObject {
    static let shared = RealtimeService()

    @Published private(set) var status: RealtimeStatus = .disconnected
    @Published private(set) var lastError: String?

    private var pollingTimer: Timer?
    private var isRunning = false
    private let supabase = SupabaseClient.shared

    // Polling interval in seconds (Supabase realtime alternative via polling)
    private let pollingInterval: TimeInterval = 30

    private init() {}

    // MARK: - Public API

    /// Start realtime synchronization
    func start(sdk: MedistockSDK) {
        guard !isRunning else { return }
        guard supabase.isConfigured else {
            status = .disconnected
            return
        }

        isRunning = true
        status = .connecting

        // Initial sync
        Task {
            await performSync(sdk: sdk)
        }

        // Start polling for changes
        startPolling(sdk: sdk)
    }

    /// Stop realtime synchronization
    func stop() {
        isRunning = false
        stopPolling()
        status = .disconnected
    }

    /// Check if realtime is enabled
    var isEnabled: Bool {
        UserDefaults.standard.bool(forKey: "medistock_realtime_enabled")
    }

    /// Enable or disable realtime sync
    func setEnabled(_ enabled: Bool) {
        UserDefaults.standard.set(enabled, forKey: "medistock_realtime_enabled")
    }

    // MARK: - Polling Implementation

    private func startPolling(sdk: MedistockSDK) {
        stopPolling()

        DispatchQueue.main.async { [weak self] in
            self?.pollingTimer = Timer.scheduledTimer(withTimeInterval: self?.pollingInterval ?? 30, repeats: true) { [weak self] _ in
                Task { [weak self] in
                    await self?.performSync(sdk: sdk)
                }
            }
        }
    }

    private func stopPolling() {
        pollingTimer?.invalidate()
        pollingTimer = nil
    }

    private func performSync(sdk: MedistockSDK) async {
        guard supabase.isConfigured else {
            await MainActor.run {
                status = .disconnected
            }
            return
        }

        await MainActor.run {
            if status != .connected {
                status = .connecting
            }
        }

        // Perform sync
        await SyncService.shared.performFullSync(sdk: sdk)

        await MainActor.run {
            if SyncService.shared.lastError == nil {
                status = .connected
                lastError = nil
            } else {
                status = .disconnected
                lastError = SyncService.shared.lastError
            }
        }
    }

    // MARK: - Manual Sync

    /// Trigger a manual sync
    func syncNow(sdk: MedistockSDK) async {
        await performSync(sdk: sdk)
    }
}
