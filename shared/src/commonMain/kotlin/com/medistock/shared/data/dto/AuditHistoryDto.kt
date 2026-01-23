package com.medistock.shared.data.dto

import com.medistock.shared.domain.model.AuditHistory
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for the audit_history table in Supabase.
 * Uses snake_case for JSON serialization to match database column names.
 */
@Serializable
data class AuditHistoryDto(
    val id: String,
    @SerialName("entity_type") val entityType: String,
    @SerialName("entity_id") val entityId: String,
    @SerialName("action_type") val actionType: String,
    @SerialName("field_name") val fieldName: String? = null,
    @SerialName("old_value") val oldValue: String? = null,
    @SerialName("new_value") val newValue: String? = null,
    @SerialName("changed_by") val changedBy: String,
    @SerialName("changed_at") val changedAt: Long,
    @SerialName("site_id") val siteId: String? = null,
    val description: String? = null,
    @SerialName("client_id") val clientId: String? = null
) {
    /**
     * Convert this DTO to a domain model.
     */
    fun toModel(): AuditHistory = AuditHistory(
        id = id,
        entityType = entityType,
        entityId = entityId,
        actionType = actionType,
        fieldName = fieldName,
        oldValue = oldValue,
        newValue = newValue,
        changedBy = changedBy,
        changedAt = changedAt,
        siteId = siteId,
        description = description
    )

    companion object {
        /**
         * Create a DTO from a domain model.
         * @param auditHistory The domain model to convert
         * @param clientId Optional client ID for realtime filtering
         */
        fun fromModel(auditHistory: AuditHistory, clientId: String? = null): AuditHistoryDto = AuditHistoryDto(
            id = auditHistory.id,
            entityType = auditHistory.entityType,
            entityId = auditHistory.entityId,
            actionType = auditHistory.actionType,
            fieldName = auditHistory.fieldName,
            oldValue = auditHistory.oldValue,
            newValue = auditHistory.newValue,
            changedBy = auditHistory.changedBy,
            changedAt = auditHistory.changedAt,
            siteId = auditHistory.siteId,
            description = auditHistory.description,
            clientId = clientId
        )
    }
}
