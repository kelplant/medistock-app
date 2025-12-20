package com.medistock.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Audit history entity that tracks all data changes and entries in the system
 */
@Entity(tableName = "audit_history")
data class AuditHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    // Entity information
    @ColumnInfo(name = "entity_type") val entityType: String, // e.g., "Product", "Sale", "User"
    @ColumnInfo(name = "entity_id") val entityId: Long, // ID of the affected entity

    // Action information
    @ColumnInfo(name = "action_type") val actionType: String, // "INSERT", "UPDATE", "DELETE"

    // Change tracking
    @ColumnInfo(name = "field_name") val fieldName: String?, // Field that changed (null for INSERT/DELETE)
    @ColumnInfo(name = "old_value") val oldValue: String?, // Previous value (null for INSERT)
    @ColumnInfo(name = "new_value") val newValue: String?, // New value (null for DELETE)

    // User and timestamp
    @ColumnInfo(name = "changed_by") val changedBy: String, // Username who made the change
    @ColumnInfo(name = "changed_at") val changedAt: Long = System.currentTimeMillis(),

    // Additional context
    @ColumnInfo(name = "site_id") val siteId: Long? = null, // Site context if applicable
    @ColumnInfo(name = "description") val description: String? = null // Optional description
)
