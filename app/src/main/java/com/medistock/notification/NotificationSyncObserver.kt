package com.medistock.notification

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleCoroutineScope
import com.medistock.MedistockApplication
import com.medistock.shared.domain.notification.NotificationEvent
import com.medistock.shared.platform.Notifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Observer for notification synchronization and display on Android.
 *
 * Responsibilities:
 * - Check for missed notifications at app startup
 * - Display notifications using the platform Notifier
 * - Mark notifications as displayed after showing them
 *
 * Note: Uses Activity's lifecycleScope to avoid memory leaks. The scope is
 * automatically cancelled when the Activity is destroyed.
 *
 * Future enhancements:
 * - Subscribe to Supabase Realtime for live updates
 * - Handle deep link navigation
 */
class NotificationSyncObserver(
    private val context: Context,
    private val scope: LifecycleCoroutineScope
) {

    private val sdk = MedistockApplication.sdk
    private val notifier = Notifier(context)

    /**
     * Check and display any notifications that were missed while the app was closed.
     * This should be called in onCreate of the main activity.
     */
    fun checkMissedNotifications() {
        if (!hasNotificationPermission()) {
            return
        }

        scope.launch {
            try {
                val undisplayedNotifications = withContext(Dispatchers.IO) {
                    sdk.notificationRepository.getUndisplayed()
                }

                for (event in undisplayedNotifications) {
                    showAndMarkDisplayed(event)
                }

                if (undisplayedNotifications.isNotEmpty()) {
                    Log.d(TAG, "Displayed ${undisplayedNotifications.size} missed notifications")
                }
            } catch (e: Exception) {
                Log.d(TAG, "Error checking missed notifications: ${e.message}")
            }
        }
    }

    /**
     * Display a notification and mark it as displayed in the repository.
     */
    private suspend fun showAndMarkDisplayed(event: NotificationEvent) {
        try {
            notifier.show(event)
            withContext(Dispatchers.IO) {
                sdk.notificationRepository.markAsDisplayed(event.id)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Error displaying notification ${event.id}: ${e.message}")
        }
    }

    /**
     * Dismiss a notification (user acknowledged it).
     */
    fun dismissNotification(notificationId: String) {
        scope.launch {
            try {
                notifier.cancel(notificationId)
                withContext(Dispatchers.IO) {
                    sdk.notificationRepository.markAsDismissed(notificationId)
                }
            } catch (e: Exception) {
                Log.d(TAG, "Error dismissing notification: ${e.message}")
            }
        }
    }

    /**
     * Dismiss all notifications.
     */
    fun dismissAllNotifications() {
        scope.launch {
            try {
                notifier.cancelAll()
                withContext(Dispatchers.IO) {
                    sdk.notificationRepository.dismissAll()
                }
            } catch (e: Exception) {
                Log.d(TAG, "Error dismissing all notifications: ${e.message}")
            }
        }
    }

    /**
     * Check if the app has permission to show notifications.
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    companion object {
        private const val TAG = "NotificationObserver"
        const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }
}
