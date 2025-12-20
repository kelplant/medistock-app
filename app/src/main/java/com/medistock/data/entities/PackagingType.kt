package com.medistock.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * PackagingType - Type de conditionnement administrable
 *
 * Exemples:
 * - 2 niveaux: "Boîte/Comprimés" (level1Name="Boîte", level2Name="Comprimés", defaultConversionFactor=30)
 * - 2 niveaux: "Flacon/ml" (level1Name="Flacon", level2Name="ml", defaultConversionFactor=100)
 * - 1 niveau: "Units" (level1Name="Units", level2Name=null)
 */
@Entity(tableName = "packaging_types")
data class PackagingType(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Nom du type de conditionnement (ex: "Boîte/Comprimés", "Flacon/ml", "Units")
    val name: String,

    // Nom de l'unité de niveau 1 (ex: "Boîte", "Flacon", "Units")
    val level1Name: String,

    // Nom de l'unité de niveau 2 (ex: "Comprimés", "ml", null pour 1 niveau uniquement)
    val level2Name: String?,

    // Facteur de conversion par défaut entre level1 et level2 (ex: 30 comprimés par boîte)
    // Null si 1 seul niveau
    val defaultConversionFactor: Double?,

    // Actif ou non (pour désactiver sans supprimer)
    val isActive: Boolean = true,

    // Ordre d'affichage dans les listes
    val displayOrder: Int = 0,

    // Audit
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val createdBy: String? = null,
    val updatedBy: String? = null
) {
    /**
     * Vérifie si ce type a 2 niveaux de conditionnement
     */
    fun hasTwoLevels(): Boolean = level2Name != null

    /**
     * Retourne le nom de l'unité pour le niveau spécifié
     */
    fun getLevelName(level: Int): String? {
        return when (level) {
            1 -> level1Name
            2 -> level2Name
            else -> null
        }
    }

    /**
     * Retourne le nom d'affichage complet
     */
    fun getDisplayName(): String {
        return if (hasTwoLevels()) {
            "$level1Name / $level2Name"
        } else {
            level1Name
        }
    }
}
