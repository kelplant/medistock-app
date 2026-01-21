package com.medistock.shared.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PackagingType(
    val id: String,
    val name: String,
    val level1Name: String,
    val level2Name: String? = null,
    val level2Quantity: Int? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val createdBy: String = "",
    val updatedBy: String = ""
)
