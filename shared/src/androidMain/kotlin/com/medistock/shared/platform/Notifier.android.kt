package com.medistock.shared.platform

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.medistock.shared.domain.notification.NotificationEvent
import com.medistock.shared.domain.notification.NotificationPriority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Android implementation of system notification display.
 * Uses NotificationCompat for backward compatibility.
 *
 * @param context Application context for accessing system services
 */
actual class Notifier(private val context: Context) {

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private var permissionCallback: ((Boolean) -> Unit)? = null

    init {
        createNotificationChannels()
    }

    actual fun show(event: NotificationEvent) {
        if (!hasNotificationPermission()) {
            return
        }

        val channelId = event.priority.toChannelId()

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(event.title)
            .setContentText(event.message)
            .setPriority(event.priority.toAndroidPriority())
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)

        event.deepLink?.let { deepLink ->
            // Security: Only allow medistock:// scheme to prevent intent injection
            val uri = Uri.parse(deepLink)
            if (uri.scheme == DEEP_LINK_SCHEME) {
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    setPackage(context.packageName) // Restrict to our app
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    event.id.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.setContentIntent(pendingIntent)
            }
        }

        try {
            NotificationManagerCompat.from(context).notify(event.id.hashCode(), builder.build())
        } catch (e: SecurityException) {
            // Permission denied - ignore silently
        }
    }

    actual fun cancel(eventId: String) {
        notificationManager.cancel(eventId.hashCode())
    }

    actual fun cancelAll() {
        notificationManager.cancelAll()
    }

    actual suspend fun hasPermission(): Boolean {
        return hasNotificationPermission()
    }

    actual suspend fun requestPermission(): Boolean = suspendCancellableCoroutine { continuation ->
        if (hasNotificationPermission()) {
            continuation.resume(true)
        } else {
            // On Android 13+, permission must be requested via Activity
            // This returns false - the app must handle permission request at Activity level
            continuation.resume(false)
        }
    }

    /**
     * Called from Activity when permission result is received.
     * Use this if you need to handle async permission results.
     */
    fun onPermissionResult(granted: Boolean) {
        permissionCallback?.invoke(granted)
        permissionCallback = null
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_CRITICAL,
                    "Alertes critiques",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Produits expirés et alertes urgentes"
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_HIGH,
                    "Alertes importantes",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Produits expirant bientôt"
                },
                NotificationChannel(
                    CHANNEL_MEDIUM,
                    "Informations",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Stock faible et autres informations"
                },
                NotificationChannel(
                    CHANNEL_LOW,
                    "Notifications silencieuses",
                    NotificationManager.IMPORTANCE_MIN
                ).apply {
                    description = "Notifications de synchronisation"
                }
            )

            channels.forEach { channel ->
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    private fun NotificationPriority.toChannelId(): String = when (this) {
        NotificationPriority.CRITICAL -> CHANNEL_CRITICAL
        NotificationPriority.HIGH -> CHANNEL_HIGH
        NotificationPriority.MEDIUM -> CHANNEL_MEDIUM
        NotificationPriority.LOW -> CHANNEL_LOW
    }

    private fun NotificationPriority.toAndroidPriority(): Int = when (this) {
        NotificationPriority.CRITICAL -> NotificationCompat.PRIORITY_HIGH
        NotificationPriority.HIGH -> NotificationCompat.PRIORITY_DEFAULT
        NotificationPriority.MEDIUM -> NotificationCompat.PRIORITY_LOW
        NotificationPriority.LOW -> NotificationCompat.PRIORITY_MIN
    }

    companion object {
        const val CHANNEL_CRITICAL = "medistock_critical"
        const val CHANNEL_HIGH = "medistock_high"
        const val CHANNEL_MEDIUM = "medistock_medium"
        const val CHANNEL_LOW = "medistock_low"
        private const val DEEP_LINK_SCHEME = "medistock"
    }
}
