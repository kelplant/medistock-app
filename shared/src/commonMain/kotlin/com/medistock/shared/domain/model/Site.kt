package com.medistock.shared.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Site(
    val id: String,
    val name: String,
    val isActive: Boolean = true,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val createdBy: String = "",
    val updatedBy: String = ""
)
