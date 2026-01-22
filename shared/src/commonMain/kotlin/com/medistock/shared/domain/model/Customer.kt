package com.medistock.shared.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Customer(
    val id: String,
    val name: String,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val notes: String? = null,
    val siteId: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val createdBy: String = "",
    val updatedBy: String = ""
)
