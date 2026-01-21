package com.medistock.shared.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Inventory(
    val id: String,
    val siteId: String,
    val status: String = "in_progress",
    val startedAt: Long,
    val completedAt: Long? = null,
    val notes: String? = null,
    val createdBy: String = ""
)
