package com.medistock.shared.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PackagingType(
    val id: String,
    val name: String,
    val level1Name: String,
    val level2Name: String? = null,
    val level2Quantity: Int? = null,
    val defaultConversionFactor: Double? = null,
    val isActive: Boolean = true,
    val displayOrder: Int = 0,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val createdBy: String = "",
    val updatedBy: String = ""
) {
    fun hasTwoLevels(): Boolean = level2Name != null

    fun getLevelName(level: Int): String? = when (level) {
        1 -> level1Name
        2 -> level2Name
        else -> null
    }

    fun getDisplayName(): String = if (hasTwoLevels()) {
        "$level1Name / $level2Name"
    } else {
        level1Name
    }
}
