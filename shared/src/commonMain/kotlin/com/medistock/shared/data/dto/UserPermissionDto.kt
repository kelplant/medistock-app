package com.medistock.shared.data.dto

import com.medistock.shared.domain.model.UserPermission
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for the user_permissions table in Supabase.
 */
@Serializable
data class UserPermissionDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val module: String,
    @SerialName("can_view") val canView: Boolean = false,
    @SerialName("can_create") val canCreate: Boolean = false,
    @SerialName("can_edit") val canEdit: Boolean = false,
    @SerialName("can_delete") val canDelete: Boolean = false,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("created_by") val createdBy: String,
    @SerialName("updated_by") val updatedBy: String,
    @SerialName("client_id") val clientId: String? = null
) {
    fun toModel(): UserPermission = UserPermission(
        id = id,
        userId = userId,
        module = module,
        canView = canView,
        canCreate = canCreate,
        canEdit = canEdit,
        canDelete = canDelete,
        createdAt = createdAt,
        updatedAt = updatedAt,
        createdBy = createdBy,
        updatedBy = updatedBy
    )

    companion object {
        fun fromModel(permission: UserPermission, clientId: String? = null): UserPermissionDto = UserPermissionDto(
            id = permission.id,
            userId = permission.userId,
            module = permission.module,
            canView = permission.canView,
            canCreate = permission.canCreate,
            canEdit = permission.canEdit,
            canDelete = permission.canDelete,
            createdAt = permission.createdAt,
            updatedAt = permission.updatedAt,
            createdBy = permission.createdBy,
            updatedBy = permission.updatedBy,
            clientId = clientId
        )
    }
}
