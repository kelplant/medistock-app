package com.medistock.shared.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AuditEntry(
    val id: String,
    val tableName: String,
    val recordId: String,
    val action: String,
    val oldValues: String? = null,
    val newValues: String? = null,
    val userId: String,
    val timestamp: Long
)
