package com.medistock.shared.data.dto

import com.medistock.shared.domain.model.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for the app_users table in Supabase.
 */
@Serializable
data class UserDto(
    val id: String,
    val username: String,
    val password: String,
    @SerialName("full_name") val fullName: String,
    val language: String? = null,
    @SerialName("is_admin") val isAdmin: Boolean = false,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("created_by") val createdBy: String,
    @SerialName("updated_by") val updatedBy: String,
    @SerialName("client_id") val clientId: String? = null
) {
    fun toModel(): User = User(
        id = id,
        username = username,
        password = password,
        fullName = fullName,
        language = language,
        isAdmin = isAdmin,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
        createdBy = createdBy,
        updatedBy = updatedBy
    )

    companion object {
        fun fromModel(user: User, clientId: String? = null): UserDto = UserDto(
            id = user.id,
            username = user.username,
            password = user.password,
            fullName = user.fullName,
            language = user.language,
            isAdmin = user.isAdmin,
            isActive = user.isActive,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt,
            createdBy = user.createdBy,
            updatedBy = user.updatedBy,
            clientId = clientId
        )
    }
}
