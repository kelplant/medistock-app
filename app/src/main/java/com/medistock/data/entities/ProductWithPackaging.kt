package com.medistock.data.entities

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Classe de relation pour joindre un Product avec son PackagingType
 */
data class ProductWithPackaging(
    @Embedded val product: Product,

    @Relation(
        parentColumn = "packagingTypeId",
        entityColumn = "id"
    )
    val packagingType: PackagingType?
) {
    /**
     * Retourne l'unité effective à afficher
     */
    fun getEffectiveUnit(): String {
        return product.getEffectiveUnit(packagingType)
    }

    /**
     * Retourne le facteur de conversion effectif
     */
    fun getEffectiveConversionFactor(): Double {
        return product.getEffectiveConversionFactor(packagingType)
    }

    /**
     * Retourne le nom du niveau sélectionné
     */
    fun getSelectedLevelName(): String? {
        return if (product.selectedLevel != null && packagingType != null) {
            packagingType.getLevelName(product.selectedLevel)
        } else {
            null
        }
    }

    /**
     * Retourne le nom complet du conditionnement (ex: "Boîte/Comprimés (utilise Comprimés)")
     */
    fun getPackagingDisplayName(): String {
        return if (packagingType != null && product.selectedLevel != null) {
            val typeName = packagingType.getDisplayName()
            val levelName = packagingType.getLevelName(product.selectedLevel)
            "$typeName (utilise $levelName)"
        } else {
            product.unit ?: "Units"
        }
    }
}
