package com.medistock.shared.data.dto

import com.medistock.shared.domain.model.Supplier
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for the suppliers table in Supabase.
 */
@Serializable
data class SupplierDto(
    val id: String,
    val name: String,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val notes: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("created_by") val createdBy: String,
    @SerialName("updated_by") val updatedBy: String,
    @SerialName("client_id") val clientId: String? = null
) {
    fun toModel(): Supplier = Supplier(
        id = id,
        name = name,
        phone = phone,
        email = email,
        address = address,
        notes = notes,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
        createdBy = createdBy,
        updatedBy = updatedBy
    )

    companion object {
        fun fromModel(supplier: Supplier, clientId: String? = null): SupplierDto = SupplierDto(
            id = supplier.id,
            name = supplier.name,
            phone = supplier.phone,
            email = supplier.email,
            address = supplier.address,
            notes = supplier.notes,
            isActive = supplier.isActive,
            createdAt = supplier.createdAt,
            updatedAt = supplier.updatedAt,
            createdBy = supplier.createdBy,
            updatedBy = supplier.updatedBy,
            clientId = clientId
        )
    }
}
