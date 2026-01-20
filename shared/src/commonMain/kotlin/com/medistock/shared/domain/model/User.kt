package com.medistock.shared.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val username: String,
    val password: String,
    val fullName: String,
    val isAdmin: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val createdBy: String = "",
    val updatedBy: String = ""
)

@Serializable
data class UserPermission(
    val id: String,
    val userId: String,
    val siteId: String,
    val canSell: Boolean = false,
    val canPurchase: Boolean = false,
    val canManageStock: Boolean = false,
    val canViewReports: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)
