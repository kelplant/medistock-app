package com.medistock.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * DTO pour la table sites
 */
@Serializable
data class SiteDto(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("updated_at") val updatedAt: Long = System.currentTimeMillis(),
    @SerialName("created_by") val createdBy: String = "",
    @SerialName("updated_by") val updatedBy: String = ""
)

/**
 * DTO pour la table categories
 */
@Serializable
data class CategoryDto(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("updated_at") val updatedAt: Long = System.currentTimeMillis(),
    @SerialName("created_by") val createdBy: String = "",
    @SerialName("updated_by") val updatedBy: String = ""
)

/**
 * DTO pour la table packaging_types
 */
@Serializable
data class PackagingTypeDto(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    @SerialName("level1_name") val level1Name: String,
    @SerialName("level2_name") val level2Name: String? = null,
    @SerialName("default_conversion_factor") val defaultConversionFactor: Double? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("display_order") val displayOrder: Int = 0,
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("updated_at") val updatedAt: Long = System.currentTimeMillis(),
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("updated_by") val updatedBy: String? = null
)

/**
 * DTO pour la table app_users
 */
@Serializable
data class AppUserDto(
    val id: String = UUID.randomUUID().toString(),
    val username: String,
    val password: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("is_admin") val isAdmin: Boolean = false,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("updated_at") val updatedAt: Long = System.currentTimeMillis(),
    @SerialName("created_by") val createdBy: String = "",
    @SerialName("updated_by") val updatedBy: String = ""
)

/**
 * DTO pour la table user_permissions
 */
@Serializable
data class UserPermissionDto(
    val id: String = UUID.randomUUID().toString(),
    @SerialName("user_id") val userId: String,
    val module: String,
    @SerialName("can_view") val canView: Boolean = false,
    @SerialName("can_create") val canCreate: Boolean = false,
    @SerialName("can_edit") val canEdit: Boolean = false,
    @SerialName("can_delete") val canDelete: Boolean = false,
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("updated_at") val updatedAt: Long = System.currentTimeMillis(),
    @SerialName("created_by") val createdBy: String = "",
    @SerialName("updated_by") val updatedBy: String = ""
)

/**
 * DTO pour la table customers
 */
@Serializable
data class CustomerDto(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String? = null,
    val address: String? = null,
    val notes: String? = null,
    @SerialName("site_id") val siteId: String,
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("updated_at") val updatedAt: Long = System.currentTimeMillis(),
    @SerialName("created_by") val createdBy: String = "",
    @SerialName("updated_by") val updatedBy: String = ""
)
