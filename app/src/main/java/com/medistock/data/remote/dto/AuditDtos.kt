package com.medistock.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * DTO pour la table audit_history
 */
@Serializable
data class AuditHistoryDto(
    val id: String = UUID.randomUUID().toString(),
    @SerialName("entity_type") val entityType: String,
    @SerialName("entity_id") val entityId: String,
    @SerialName("action_type") val actionType: String, // "INSERT", "UPDATE", "DELETE"
    @SerialName("field_name") val fieldName: String? = null,
    @SerialName("old_value") val oldValue: String? = null,
    @SerialName("new_value") val newValue: String? = null,
    @SerialName("changed_by") val changedBy: String,
    @SerialName("changed_at") val changedAt: Long = System.currentTimeMillis(),
    @SerialName("site_id") val siteId: String? = null,
    val description: String? = null
)

/**
 * DTO pour la vue current_stock (lecture seule)
 */
@Serializable
data class CurrentStockDto(
    @SerialName("product_id") val productId: String,
    @SerialName("product_name") val productName: String,
    @SerialName("site_id") val siteId: String,
    @SerialName("site_name") val siteName: String,
    @SerialName("current_stock") val currentStock: Double,
    @SerialName("min_stock") val minStock: Double?,
    @SerialName("max_stock") val maxStock: Double?,
    @SerialName("stock_status") val stockStatus: String // 'LOW', 'NORMAL', 'HIGH'
)
