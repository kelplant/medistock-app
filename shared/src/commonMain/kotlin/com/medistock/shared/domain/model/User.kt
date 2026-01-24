package com.medistock.shared.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val username: String,
    val password: String,
    val fullName: String,
    val language: String? = null,
    val isAdmin: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val createdBy: String = "",
    val updatedBy: String = ""
)

/**
 * Module-based permission model.
 * Each permission grants CRUD access to a specific module for a user.
 */
@Serializable
data class UserPermission(
    val id: String,
    val userId: String,
    val module: String,
    val canView: Boolean = false,
    val canCreate: Boolean = false,
    val canEdit: Boolean = false,
    val canDelete: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val createdBy: String = "",
    val updatedBy: String = ""
)
