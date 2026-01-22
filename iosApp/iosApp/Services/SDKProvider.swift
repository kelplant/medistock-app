import Foundation
import shared

/// Provider for accessing the shared SDK from singleton services
/// Must be configured at app startup before using sync services
class SDKProvider {
    static let shared = SDKProvider()

    private(set) var sdk: MedistockSDK?

    private init() {}

    /// Configure the SDK provider with the app's SDK instance
    /// Call this in MedistockApp before any sync services are used
    func configure(sdk: MedistockSDK) {
        self.sdk = sdk
        debugLog("SDKProvider", "SDK configured")
    }

    /// Get the sync queue repository
    var syncQueueRepository: SyncQueueRepository {
        guard let sdk = sdk else {
            fatalError("SDKProvider not configured. Call SDKProvider.shared.configure(sdk:) first.")
        }
        return sdk.syncQueueRepository
    }

    /// Get the sync enqueue service
    var syncEnqueueService: SyncEnqueueService {
        guard let sdk = sdk else {
            fatalError("SDKProvider not configured. Call SDKProvider.shared.configure(sdk:) first.")
        }
        return sdk.syncEnqueueService
    }
}
