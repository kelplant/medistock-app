package com.medistock.shared.data.dto

import com.medistock.shared.domain.model.Customer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for the customers table in Supabase.
 */
@Serializable
data class CustomerDto(
    val id: String,
    val name: String,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val notes: String? = null,
    @SerialName("site_id") val siteId: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("created_by") val createdBy: String,
    @SerialName("updated_by") val updatedBy: String,
    @SerialName("client_id") val clientId: String? = null
) {
    fun toModel(): Customer = Customer(
        id = id,
        name = name,
        phone = phone,
        email = email,
        address = address,
        notes = notes,
        siteId = siteId,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
        createdBy = createdBy,
        updatedBy = updatedBy
    )

    companion object {
        fun fromModel(customer: Customer, clientId: String? = null): CustomerDto = CustomerDto(
            id = customer.id,
            name = customer.name,
            phone = customer.phone,
            email = customer.email,
            address = customer.address,
            notes = customer.notes,
            siteId = customer.siteId,
            isActive = customer.isActive,
            createdAt = customer.createdAt,
            updatedAt = customer.updatedAt,
            createdBy = customer.createdBy,
            updatedBy = customer.updatedBy,
            clientId = clientId
        )
    }
}
