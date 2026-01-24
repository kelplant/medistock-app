import Foundation
import SwiftUI
import shared

// MARK: - Notification Center View
/// Main view for displaying all undismissed notifications.
/// Users can view, dismiss individual notifications, or dismiss all at once.
struct NotificationCenterView: View {
    let sdk: MedistockSDK
    @ObservedObject private var notificationObserver = NotificationObserver.shared
    @State private var notifications: [NotificationEvent] = []
    @State private var isLoading = true
    @State private var showErrorAlert = false
    @State private var errorMessage = ""

    var body: some View {
        Group {
            if isLoading {
                ProgressView(Localized.loading)
            } else if notifications.isEmpty {
                EmptyNotificationView()
            } else {
                NotificationListView(
                    notifications: notifications,
                    onDismiss: dismissNotification,
                    onTap: handleNotificationTap
                )
            }
        }
        .navigationTitle(Localized.notifications)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                if !notifications.isEmpty {
                    Button(Localized.dismissAll) {
                        Task {
                            await dismissAllNotifications()
                        }
                    }
                }
            }
        }
        .task {
            await loadNotifications()
        }
        .refreshable {
            await loadNotifications()
        }
        .alert(Localized.error, isPresented: $showErrorAlert) {
            Button(Localized.ok, role: .cancel) {}
        } message: {
            Text(errorMessage)
        }
    }

    @MainActor
    private func loadNotifications() async {
        isLoading = true
        do {
            notifications = try await sdk.notificationRepository.getUndismissed()
        } catch {
            debugLog("NotificationCenterView", "Error loading notifications: \(error)")
            errorMessage = Localized.strings.unableToLoadNotifications
            showErrorAlert = true
            // Keep existing notifications on error
        }
        isLoading = false
    }

    @MainActor
    private func dismissNotification(_ event: NotificationEvent) {
        Task {
            await notificationObserver.dismissNotification(notificationId: event.id)
            await loadNotifications()
        }
    }

    @MainActor
    private func dismissAllNotifications() async {
        await notificationObserver.dismissAllNotifications()
        await loadNotifications()
    }

    @MainActor
    private func handleNotificationTap(_ event: NotificationEvent) {
        guard let deepLink = event.deepLink else { return }

        // Parse the deep link: medistock://stock/{productId}
        if deepLink.hasPrefix("medistock://stock/") {
            let productId = String(deepLink.dropFirst("medistock://stock/".count))

            // Validate UUID format for security
            guard UUID(uuidString: productId) != nil else {
                debugLog("NotificationCenterView", "Invalid product ID in deep link: \(productId)")
                return
            }

            // Navigate to stock view with highlighted product
            // Note: Deep navigation requires custom NavigationPath or coordinator pattern
            // For now, we log and user can use the Stock menu
            debugLog("NotificationCenterView", "Navigate to product: \(productId)")
        }
    }
}

// MARK: - Empty Notification View
struct EmptyNotificationView: View {
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "bell.slash")
                .font(.system(size: 64))
                .foregroundColor(.secondary.opacity(0.5))

            Text(Localized.noNotifications)
                .font(.title2)
                .foregroundColor(.secondary)

            Text(Localized.strings.allNotificationsDismissed)
                .font(.subheadline)
                .foregroundColor(.secondary.opacity(0.7))
        }
        .padding()
        .accessibilityIdentifier("empty-notifications-view")
    }
}

// MARK: - Notification List View
struct NotificationListView: View {
    let notifications: [NotificationEvent]
    let onDismiss: (NotificationEvent) -> Void
    let onTap: (NotificationEvent) -> Void

    var body: some View {
        List {
            Section(header: Text("\(notifications.count) \(Localized.notifications.lowercased())")) {
                ForEach(notifications, id: \.id) { notification in
                    NotificationRowView(
                        notification: notification,
                        onDismiss: { onDismiss(notification) },
                        onTap: { onTap(notification) }
                    )
                }
            }
        }
        .listStyle(.insetGrouped)
        .accessibilityIdentifier("notifications-list")
    }
}

// MARK: - Notification Row View
struct NotificationRowView: View {
    let notification: NotificationEvent
    let onDismiss: () -> Void
    let onTap: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            // Priority indicator
            RoundedRectangle(cornerRadius: 2)
                .fill(priorityColor)
                .frame(width: 4)

            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(notification.title)
                        .font(.headline)
                        .lineLimit(1)

                    Spacer()

                    Text(priorityLabel)
                        .font(.caption2)
                        .fontWeight(.semibold)
                        .foregroundColor(.white)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(priorityColor)
                        .cornerRadius(4)
                }

                Text(notification.message)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .lineLimit(2)

                Text(formatRelativeTime(notification.createdAt))
                    .font(.caption)
                    .foregroundColor(.secondary.opacity(0.7))
            }

            Button(action: onDismiss) {
                Image(systemName: "xmark.circle.fill")
                    .foregroundColor(.secondary)
                    .font(.title3)
            }
            .buttonStyle(.plain)
            .accessibilityIdentifier("dismiss-notification-\(notification.id)")
        }
        .padding(.vertical, 4)
        .contentShape(Rectangle())
        .onTapGesture(perform: onTap)
        .accessibilityIdentifier("notification-row-\(notification.id)")
    }

    private var priorityColor: Color {
        switch notification.priority {
        case .critical: return .red
        case .high: return .orange
        case .medium: return .blue
        case .low: return .gray
        default: return .gray
        }
    }

    private var priorityLabel: String {
        switch notification.priority {
        case .critical: return Localized.critical.uppercased()
        case .high: return Localized.urgent.uppercased()
        case .medium: return Localized.info.uppercased()
        case .low: return Localized.low.uppercased()
        default: return Localized.info.uppercased()
        }
    }

    private func formatRelativeTime(_ timestamp: Int64) -> String {
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        let diff = now - timestamp

        let minute: Int64 = 60 * 1000
        let hour: Int64 = 60 * minute
        let day: Int64 = 24 * hour

        if diff < minute {
            return Localized.justNow
        } else if diff < hour {
            let minutes = diff / minute
            return Localized.format(Localized.minutesAgo, "count", Int(minutes))
        } else if diff < day {
            let hours = diff / hour
            return Localized.format(Localized.hoursAgo, "count", Int(hours))
        } else {
            let days = diff / day
            return Localized.format(Localized.daysAgo, "count", Int(days))
        }
    }
}

// MARK: - Notification Badge View
/// Badge showing the count of pending notifications.
/// To be displayed in the toolbar or as an overlay on a button.
struct NotificationBadgeView: View {
    @ObservedObject private var notificationObserver = NotificationObserver.shared

    var body: some View {
        ZStack(alignment: .topTrailing) {
            Image(systemName: "bell.fill")
                .font(.title3)
                .foregroundColor(.accentColor)

            if notificationObserver.pendingCount > 0 {
                Text(badgeText)
                    .font(.caption2)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
                    .padding(.horizontal, 4)
                    .padding(.vertical, 1)
                    .background(Color.red)
                    .clipShape(Capsule())
                    .offset(x: 8, y: -6)
            }
        }
        .accessibilityIdentifier("notification-badge")
    }

    private var badgeText: String {
        let count = notificationObserver.pendingCount
        if count > 99 {
            return "99+"
        }
        return "\(count)"
    }
}
