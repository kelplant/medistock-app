package com.medistock.shared.platform

import com.medistock.shared.domain.notification.NotificationEvent
import com.medistock.shared.domain.notification.NotificationPriority
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

/**
 * iOS implementation of system notification display.
 * Uses UNUserNotificationCenter for local notifications.
 */
@OptIn(ExperimentalForeignApi::class)
actual class Notifier {

    private val center: UNUserNotificationCenter = UNUserNotificationCenter.currentNotificationCenter()

    actual fun show(event: NotificationEvent) {
        val content = UNMutableNotificationContent().apply {
            setTitle(event.title)
            setBody(event.message)
            setSound(event.priority.toSound())

            // Add category for actions if needed
            event.deepLink?.let { link ->
                setUserInfo(mapOf("deepLink" to link))
            }

            // Set thread identifier for grouping
            event.siteId?.let { siteId ->
                setThreadIdentifier(siteId)
            }
        }

        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = event.id,
            content = content,
            trigger = null // Immediate delivery
        )

        center.addNotificationRequest(request) { error ->
            error?.let {
                println("MediStock Notifier: Error showing notification: ${it.localizedDescription}")
            }
        }
    }

    actual fun cancel(eventId: String) {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(eventId))
        center.removeDeliveredNotificationsWithIdentifiers(listOf(eventId))
    }

    actual fun cancelAll() {
        center.removeAllPendingNotificationRequests()
        center.removeAllDeliveredNotifications()
    }

    actual suspend fun hasPermission(): Boolean = suspendCancellableCoroutine { continuation ->
        center.getNotificationSettingsWithCompletionHandler { settings ->
            val authorized = settings?.authorizationStatus == UNAuthorizationStatusAuthorized
            continuation.resume(authorized)
        }
    }

    actual suspend fun requestPermission(): Boolean = suspendCancellableCoroutine { continuation ->
        val options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge

        center.requestAuthorizationWithOptions(options) { granted, error ->
            error?.let {
                println("MediStock Notifier: Authorization error: ${it.localizedDescription}")
            }
            continuation.resume(granted)
        }
    }

    private fun NotificationPriority.toSound(): UNNotificationSound? = when (this) {
        // Note: defaultCriticalSound requires Critical Alerts entitlement from Apple
        // Using defaultSound for all audible priorities instead
        NotificationPriority.CRITICAL, NotificationPriority.HIGH, NotificationPriority.MEDIUM ->
            UNNotificationSound.defaultSound
        NotificationPriority.LOW -> null
    }
}
