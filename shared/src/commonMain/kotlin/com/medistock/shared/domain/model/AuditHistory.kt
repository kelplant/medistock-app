package com.medistock.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * Audit history entity that tracks all data changes in the system.
 * Provides detailed change tracking including old/new values and field-level changes.
 */
@Serializable
data class AuditHistory(
    val id: String,
    val entityType: String,
    val entityId: String,
    val actionType: String,
    val fieldName: String? = null,
    val oldValue: String? = null,
    val newValue: String? = null,
    val changedBy: String,
    val changedAt: Long,
    val siteId: String? = null,
    val description: String? = null
)
