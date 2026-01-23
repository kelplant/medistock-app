package com.medistock.shared.data.dto

import com.medistock.shared.domain.model.ProductPrice
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for the product_prices table in Supabase.
 * Uses snake_case for JSON serialization to match database column names.
 */
@Serializable
data class ProductPriceDto(
    val id: String,
    @SerialName("product_id") val productId: String,
    @SerialName("effective_date") val effectiveDate: Long,
    @SerialName("purchase_price") val purchasePrice: Double,
    @SerialName("selling_price") val sellingPrice: Double,
    val source: String,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("created_by") val createdBy: String,
    @SerialName("updated_by") val updatedBy: String,
    @SerialName("client_id") val clientId: String? = null
) {
    /**
     * Convert this DTO to a domain model.
     */
    fun toModel(): ProductPrice = ProductPrice(
        id = id,
        productId = productId,
        effectiveDate = effectiveDate,
        purchasePrice = purchasePrice,
        sellingPrice = sellingPrice,
        source = source,
        createdAt = createdAt,
        updatedAt = updatedAt,
        createdBy = createdBy,
        updatedBy = updatedBy
    )

    companion object {
        /**
         * Create a DTO from a domain model.
         * @param productPrice The domain model to convert
         * @param clientId Optional client ID for realtime filtering
         */
        fun fromModel(productPrice: ProductPrice, clientId: String? = null): ProductPriceDto = ProductPriceDto(
            id = productPrice.id,
            productId = productPrice.productId,
            effectiveDate = productPrice.effectiveDate,
            purchasePrice = productPrice.purchasePrice,
            sellingPrice = productPrice.sellingPrice,
            source = productPrice.source,
            createdAt = productPrice.createdAt,
            updatedAt = productPrice.updatedAt,
            createdBy = productPrice.createdBy,
            updatedBy = productPrice.updatedBy,
            clientId = clientId
        )
    }
}
