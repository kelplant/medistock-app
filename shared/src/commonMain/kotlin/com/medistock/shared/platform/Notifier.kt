package com.medistock.shared.platform

import com.medistock.shared.domain.notification.NotificationEvent

/**
 * Platform-specific notification display service.
 * Each platform provides its own implementation for showing system notifications.
 *
 * Usage:
 * - Android: Instantiate with Context in the app module
 * - iOS: Instantiate without parameters in the app module
 */
expect class Notifier {
    /**
     * Display a notification to the user.
     * @param event The notification event to display
     */
    fun show(event: NotificationEvent)

    /**
     * Cancel a previously shown notification.
     * @param eventId The ID of the notification to cancel
     */
    fun cancel(eventId: String)

    /**
     * Cancel all notifications.
     */
    fun cancelAll()

    /**
     * Check if the app has permission to display notifications.
     * @return true if notifications are permitted
     */
    suspend fun hasPermission(): Boolean

    /**
     * Request permission to display notifications.
     *
     * IMPORTANT: Platform behavior differs significantly:
     * - iOS: Triggers the UNUserNotificationCenter authorization dialog and returns the result.
     * - Android: Cannot trigger the system dialog from this method (requires Activity context).
     *   Returns true if already granted, false otherwise. On Android 13+, use
     *   ActivityResultContracts.RequestPermission with Manifest.permission.POST_NOTIFICATIONS
     *   at the Activity level instead.
     * - JVM: Always returns true (stub for testing).
     *
     * @return true if permission was granted (or already granted on Android)
     */
    suspend fun requestPermission(): Boolean
}
