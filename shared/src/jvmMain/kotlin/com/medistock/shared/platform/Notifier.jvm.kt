package com.medistock.shared.platform

import com.medistock.shared.domain.notification.NotificationEvent

/**
 * JVM implementation of notification display (stub for testing).
 * On JVM/desktop, notifications are logged but not displayed.
 */
actual class Notifier {

    private val shownNotifications = mutableSetOf<String>()

    actual fun show(event: NotificationEvent) {
        shownNotifications.add(event.id)
        println("MediStock Notifier [JVM]: ${event.priority} - ${event.title}: ${event.message}")
    }

    actual fun cancel(eventId: String) {
        shownNotifications.remove(eventId)
        println("MediStock Notifier [JVM]: Cancelled notification $eventId")
    }

    actual fun cancelAll() {
        shownNotifications.clear()
        println("MediStock Notifier [JVM]: Cancelled all notifications")
    }

    actual suspend fun hasPermission(): Boolean {
        return true // Always permitted on JVM
    }

    actual suspend fun requestPermission(): Boolean {
        return true // Always granted on JVM
    }

    /**
     * Get IDs of currently shown notifications (for testing).
     */
    fun getShownNotificationIds(): Set<String> = shownNotifications.toSet()
}
