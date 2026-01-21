package com.medistock.shared.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ProductPrice(
    val id: String,
    val productId: String,
    val siteId: String,
    val price: Double,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val createdBy: String = "",
    val updatedBy: String = ""
)
