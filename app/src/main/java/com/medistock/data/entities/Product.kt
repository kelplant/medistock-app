package com.medistock.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "products")
data class Product(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val unit: String, // Obligatoire - rempli automatiquement depuis PackagingType
    val unitVolume: Double, // Obligatoire - facteur de conversion

    // Nouveau système de conditionnement administrable
    val packagingTypeId: String? = null,  // FK vers packaging_types
    val selectedLevel: Int? = null,      // 1 ou 2 - niveau d'unité sélectionné
    val conversionFactor: Double? = null, // Facteur de conversion spécifique au produit (peut override celui du type)

    val categoryId: String?,  // Nullable to match queries
    val marginType: String?,  // Nullable
    val marginValue: Double?,  // Nullable
    val description: String? = null,
    val siteId: String,
    val minStock: Double? = 0.0,
    val maxStock: Double? = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String = "",
    val updatedBy: String = ""
)
