package com.medistock.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,

    // Ancien système de conditionnement (deprecated, gardé pour compatibilité)
    val unit: String? = null,
    val unitVolume: Double? = null,

    // Nouveau système de conditionnement administrable
    val packagingTypeId: Long? = null,  // FK vers packaging_types
    val selectedLevel: Int? = null,      // 1 ou 2 - niveau d'unité sélectionné
    val conversionFactor: Double? = null, // Facteur de conversion spécifique au produit (peut override celui du type)

    val categoryId: Long?,  // Nullable to match queries
    val marginType: String?,  // Nullable
    val marginValue: Double?,  // Nullable
    val siteId: Long,
    val minStock: Double? = 0.0,
    val maxStock: Double? = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String = "",
    val updatedBy: String = ""
) {
    /**
     * Retourne vrai si le produit utilise le nouveau système de conditionnement
     */
    fun usesNewPackagingSystem(): Boolean = packagingTypeId != null

    /**
     * Retourne l'unité à utiliser (nouveau système ou ancien)
     */
    fun getEffectiveUnit(packagingType: PackagingType?): String {
        return if (usesNewPackagingSystem() && packagingType != null && selectedLevel != null) {
            packagingType.getLevelName(selectedLevel) ?: unit ?: "Units"
        } else {
            unit ?: "Units"
        }
    }

    /**
     * Retourne le facteur de conversion effectif
     */
    fun getEffectiveConversionFactor(packagingType: PackagingType?): Double {
        return when {
            conversionFactor != null -> conversionFactor
            packagingType?.defaultConversionFactor != null -> packagingType.defaultConversionFactor
            unitVolume != null -> unitVolume
            else -> 1.0
        }
    }
}