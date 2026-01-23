import Foundation
import UserNotifications
import shared

/// Observer for notification synchronization and display on iOS.
///
/// Responsibilities:
/// - Check for missed notifications at app startup
/// - Display notifications using UNUserNotificationCenter
/// - Mark notifications as displayed after showing them
/// - Handle notification permission requests
///
/// Future enhancements:
/// - Subscribe to Supabase Realtime for live updates
/// - Handle deep link navigation
@MainActor
class NotificationObserver: ObservableObject {

    static let shared = NotificationObserver()

    @Published private(set) var hasPermission = false
    @Published private(set) var pendingCount: Int64 = 0

    private let center = UNUserNotificationCenter.current()
    private var notifier: Notifier?

    private init() {
        // Initialize notifier
        notifier = Notifier()

        // Check initial permission status
        Task {
            await checkPermissionStatus()
        }
    }

    /// Check and update the current permission status.
    func checkPermissionStatus() async {
        let settings = await center.notificationSettings()
        hasPermission = settings.authorizationStatus == .authorized
    }

    /// Request notification permission from the user.
    /// - Returns: true if permission was granted
    @discardableResult
    func requestPermission() async -> Bool {
        do {
            let options: UNAuthorizationOptions = [.alert, .sound, .badge]
            let granted = try await center.requestAuthorization(options: options)
            hasPermission = granted
            return granted
        } catch {
            debugLog("NotificationObserver", "Permission request error: \(error)")
            hasPermission = false
            return false
        }
    }

    /// Check and display any notifications that were missed while the app was closed.
    /// This should be called in .task of the main view.
    func checkMissedNotifications() async {
        guard hasPermission else {
            debugLog("NotificationObserver", "Skipping missed notifications check - no permission")
            return
        }

        guard let sdk = SDKProvider.shared.sdk else {
            debugLog("NotificationObserver", "SDK not available")
            return
        }

        do {
            let undisplayedNotifications = try await sdk.notificationRepository.getUndisplayed()

            for event in undisplayedNotifications {
                await showAndMarkDisplayed(event: event, sdk: sdk)
            }

            if !undisplayedNotifications.isEmpty {
                debugLog("NotificationObserver", "Displayed \(undisplayedNotifications.count) missed notifications")
            }

            // Update pending count
            let count = try await sdk.notificationRepository.countUndismissed()
            pendingCount = count.int64Value
        } catch {
            debugLog("NotificationObserver", "Error checking missed notifications: \(error)")
        }
    }

    /// Display a notification and mark it as displayed in the repository.
    private func showAndMarkDisplayed(event: NotificationEvent, sdk: MedistockSDK) async {
        guard let notifier = notifier else { return }

        do {
            notifier.show(event: event)
            try await sdk.notificationRepository.markAsDisplayed(id: event.id)
        } catch {
            debugLog("NotificationObserver", "Error displaying notification \(event.id): \(error)")
        }
    }

    /// Dismiss a notification (user acknowledged it).
    func dismissNotification(notificationId: String) async {
        guard let sdk = SDKProvider.shared.sdk else { return }

        notifier?.cancel(eventId: notificationId)

        do {
            try await sdk.notificationRepository.markAsDismissed(id: notificationId)
            let count = try await sdk.notificationRepository.countUndismissed()
            pendingCount = count.int64Value
        } catch {
            debugLog("NotificationObserver", "Error dismissing notification: \(error)")
        }
    }

    /// Dismiss all notifications.
    func dismissAllNotifications() async {
        guard let sdk = SDKProvider.shared.sdk else { return }

        notifier?.cancelAll()

        do {
            try await sdk.notificationRepository.dismissAll()
            pendingCount = 0
        } catch {
            debugLog("NotificationObserver", "Error dismissing all notifications: \(error)")
        }
    }

    /// Refresh the pending notification count.
    func refreshPendingCount() async {
        guard let sdk = SDKProvider.shared.sdk else { return }

        do {
            let count = try await sdk.notificationRepository.countUndismissed()
            pendingCount = count.int64Value
        } catch {
            debugLog("NotificationObserver", "Error refreshing pending count: \(error)")
        }
    }
}
