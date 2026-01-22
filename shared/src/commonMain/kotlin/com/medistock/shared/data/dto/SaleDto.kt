package com.medistock.shared.data.dto

import com.medistock.shared.domain.model.Sale
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for the sales table in Supabase.
 */
@Serializable
data class SaleDto(
    val id: String,
    @SerialName("customer_name") val customerName: String,
    @SerialName("customer_id") val customerId: String? = null,
    val date: Long,
    @SerialName("total_amount") val totalAmount: Double,
    @SerialName("site_id") val siteId: String,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("created_by") val createdBy: String,
    @SerialName("client_id") val clientId: String? = null
) {
    fun toModel(): Sale = Sale(
        id = id,
        customerName = customerName,
        customerId = customerId,
        date = date,
        totalAmount = totalAmount,
        siteId = siteId,
        createdAt = createdAt,
        createdBy = createdBy
    )

    companion object {
        fun fromModel(sale: Sale, clientId: String? = null): SaleDto = SaleDto(
            id = sale.id,
            customerName = sale.customerName,
            customerId = sale.customerId,
            date = sale.date,
            totalAmount = sale.totalAmount,
            siteId = sale.siteId,
            createdAt = sale.createdAt,
            createdBy = sale.createdBy,
            clientId = clientId
        )
    }
}
