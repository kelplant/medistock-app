package com.medistock.shared.data.dto

import com.medistock.shared.domain.notification.NotificationEvent
import com.medistock.shared.domain.notification.NotificationPriority
import com.medistock.shared.domain.notification.NotificationType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for the notification_events table in Supabase.
 * Uses snake_case for JSON serialization to match database column names.
 */
@Serializable
data class NotificationEventDto(
    val id: String,
    val type: String,
    val priority: String,
    val title: String,
    val message: String,
    @SerialName("reference_id") val referenceId: String? = null,
    @SerialName("reference_type") val referenceType: String? = null,
    @SerialName("site_id") val siteId: String? = null,
    @SerialName("deep_link") val deepLink: String? = null,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("expires_at") val expiresAt: Long? = null,
    @SerialName("is_active") val isActive: Int = 1
) {
    /**
     * Convert this DTO to a domain model.
     */
    fun toModel(): NotificationEvent = NotificationEvent(
        id = id,
        type = NotificationType.entries.find { it.name == type } ?: NotificationType.SYNC_PENDING,
        priority = NotificationPriority.fromString(priority),
        title = title,
        message = message,
        referenceId = referenceId,
        referenceType = referenceType,
        siteId = siteId,
        deepLink = deepLink,
        createdAt = createdAt,
        isDisplayed = false,
        isDismissed = false
    )

    companion object {
        /**
         * Create a DTO from a domain model.
         * Note: isDisplayed and isDismissed are local-only fields.
         */
        fun fromModel(event: NotificationEvent): NotificationEventDto = NotificationEventDto(
            id = event.id,
            type = event.type.name,
            priority = event.priority.name,
            title = event.title,
            message = event.message,
            referenceId = event.referenceId,
            referenceType = event.referenceType,
            siteId = event.siteId,
            deepLink = event.deepLink,
            createdAt = event.createdAt,
            expiresAt = null,
            isActive = 1
        )
    }
}

/**
 * DTO for the notification_dismissals table in Supabase.
 * Tracks which user has dismissed which notification.
 */
@Serializable
data class NotificationDismissalDto(
    @SerialName("notification_id") val notificationId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("dismissed_at") val dismissedAt: Long
)

/**
 * DTO for the notification_preferences table in Supabase.
 * User-specific notification settings.
 */
@Serializable
data class NotificationPreferencesDto(
    @SerialName("user_id") val userId: String,
    @SerialName("expiry_alert_enabled") val expiryAlertEnabled: Int = 1,
    @SerialName("expiry_days_threshold") val expiryDaysThreshold: Int = 30,
    @SerialName("low_stock_alert_enabled") val lowStockAlertEnabled: Int = 1,
    @SerialName("quiet_hours_start") val quietHoursStart: String? = null,
    @SerialName("quiet_hours_end") val quietHoursEnd: String? = null,
    @SerialName("sound_enabled") val soundEnabled: Int = 1,
    @SerialName("updated_at") val updatedAt: Long = 0
)

/**
 * DTO for the notification_settings table in Supabase.
 * Global admin-configurable notification settings.
 * These settings control how the pg_cron function generates notifications.
 *
 * Note: Notification message texts are not configurable here - they are
 * handled via the localization system (LocalizationManager / Localized).
 */
@Serializable
data class NotificationSettingsDto(
    val id: String = "global",

    // Expiry alerts
    @SerialName("expiry_alert_enabled") val expiryAlertEnabled: Int = 1,
    @SerialName("expiry_warning_days") val expiryWarningDays: Int = 7,
    @SerialName("expiry_dedup_days") val expiryDedupDays: Int = 3,
    @SerialName("expired_dedup_days") val expiredDedupDays: Int = 7,

    // Low stock alerts
    @SerialName("low_stock_alert_enabled") val lowStockAlertEnabled: Int = 1,
    @SerialName("low_stock_dedup_days") val lowStockDedupDays: Int = 7,

    // Metadata
    @SerialName("updated_at") val updatedAt: Long = 0,
    @SerialName("updated_by") val updatedBy: String? = null
)
