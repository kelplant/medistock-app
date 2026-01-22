package com.medistock.data.sync

import com.medistock.data.entities.*
import kotlinx.serialization.json.*

/**
 * @deprecated Utilisez [com.medistock.shared.domain.sync.ConflictResolver] du module partagé.
 * Cette classe est conservée temporairement pour compatibilité mais sera supprimée dans une version future.
 *
 * Résout les conflits de synchronisation selon le type d'entité.
 *
 * Stratégies par type d'entité:
 * - Product (référentiel): Server Wins - Le référentiel central prime
 * - Category, Site, PackagingType: Server Wins - Données de configuration
 * - Sale, SaleItem (transactions): Client Wins - Ventes offline sont valides
 * - StockMovement (transactions): Merge - Les deux mouvements sont valides
 * - PurchaseBatch: Server Wins avec alerte - Données sensibles
 * - Inventory: Last-Write-Wins avec alerte - Comptages indépendants
 * - Customer: Merge - Fusionner les infos clients
 * - User, UserPermission: Server Wins - Sécurité
 */
@Deprecated("Utilisez com.medistock.shared.domain.sync.ConflictResolver", ReplaceWith("com.medistock.shared.domain.sync.ConflictResolver"))
class ConflictResolver {

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            prettyPrint = false
        }
    }

    /**
     * Détermine la stratégie de résolution selon le type d'entité
     */
    fun getStrategy(entityType: String): ConflictResolution {
        return when (entityType) {
            // Référentiel - Server wins
            "Product", "Category", "Site", "PackagingType" -> ConflictResolution.REMOTE_WINS

            // Transactions de vente - Client wins (ventes offline valides)
            "Sale", "SaleItem", "SaleBatchAllocation" -> ConflictResolution.LOCAL_WINS

            // Mouvements de stock - Merge (les deux sont valides)
            "StockMovement" -> ConflictResolution.MERGE

            // Lots d'achat - Server wins (données sensibles)
            "PurchaseBatch" -> ConflictResolution.REMOTE_WINS

            // Inventaires - Demander à l'utilisateur
            "Inventory" -> ConflictResolution.ASK_USER

            // Clients - Merge
            "Customer" -> ConflictResolution.MERGE

            // Sécurité - Server wins
            "User", "UserPermission" -> ConflictResolution.REMOTE_WINS

            // Transferts - Merge
            "ProductTransfer" -> ConflictResolution.MERGE

            // Par défaut - Server wins
            else -> ConflictResolution.REMOTE_WINS
        }
    }

    /**
     * Détecte s'il y a un conflit entre la version locale et la version serveur
     */
    fun detectConflict(
        localItem: SyncQueueItem,
        remoteUpdatedAt: Long?,
        remoteVersion: Long?
    ): Boolean {
        // Pas de version remote connue = pas de conflit possible
        if (localItem.lastKnownRemoteUpdatedAt == null) return false
        if (remoteUpdatedAt == null) return false

        // Conflit si le serveur a été modifié depuis notre dernière sync
        return remoteUpdatedAt > localItem.lastKnownRemoteUpdatedAt
    }

    /**
     * Résout un conflit selon la stratégie de l'entité
     */
    fun resolve(
        entityType: String,
        localPayload: String,
        remotePayload: String?,
        localUpdatedAt: Long,
        remoteUpdatedAt: Long?
    ): ConflictResolutionResult {
        val strategy = getStrategy(entityType)

        return when (strategy) {
            ConflictResolution.LOCAL_WINS -> ConflictResolutionResult(
                resolution = ConflictResolution.LOCAL_WINS,
                mergedPayload = localPayload,
                message = "Version locale conservée (transaction offline valide)"
            )

            ConflictResolution.REMOTE_WINS -> ConflictResolutionResult(
                resolution = ConflictResolution.REMOTE_WINS,
                mergedPayload = remotePayload,
                message = "Version serveur conservée (référentiel central)"
            )

            ConflictResolution.MERGE -> {
                val merged = mergePayloads(entityType, localPayload, remotePayload)
                ConflictResolutionResult(
                    resolution = ConflictResolution.MERGE,
                    mergedPayload = merged,
                    message = "Versions fusionnées"
                )
            }

            ConflictResolution.ASK_USER -> ConflictResolutionResult(
                resolution = ConflictResolution.ASK_USER,
                message = "Conflit détecté - intervention utilisateur requise"
            )

            ConflictResolution.KEEP_BOTH -> ConflictResolutionResult(
                resolution = ConflictResolution.KEEP_BOTH,
                mergedPayload = localPayload, // Local sera créé comme copie
                message = "Les deux versions seront conservées"
            )
        }
    }

    /**
     * Fusionne deux payloads JSON
     * Stratégie: Prend les champs les plus récents de chaque version
     */
    private fun mergePayloads(entityType: String, localJson: String, remoteJson: String?): String {
        if (remoteJson == null) return localJson

        return try {
            val local = Json.parseToJsonElement(localJson).jsonObject
            val remote = Json.parseToJsonElement(remoteJson).jsonObject

            val merged = buildJsonObject {
                // Commencer avec tous les champs remote
                remote.forEach { (key, value) ->
                    put(key, value)
                }

                // Override avec les champs locaux modifiés (sauf timestamps système)
                val systemFields = setOf("createdAt", "updatedAt", "createdBy", "updatedBy", "id")
                local.forEach { (key, value) ->
                    if (key !in systemFields) {
                        // Pour les mouvements de stock, on additionne les quantités
                        if (entityType == "StockMovement" && key == "quantity") {
                            val localQty = value.jsonPrimitive.doubleOrNull ?: 0.0
                            val remoteQty = remote[key]?.jsonPrimitive?.doubleOrNull ?: 0.0
                            // Ne pas additionner, garder le local pour les transactions
                            put(key, JsonPrimitive(localQty))
                        } else {
                            put(key, value)
                        }
                    }
                }

                // Toujours prendre le updatedAt le plus récent
                val localUpdated = local["updatedAt"]?.jsonPrimitive?.longOrNull ?: 0L
                val remoteUpdated = remote["updatedAt"]?.jsonPrimitive?.longOrNull ?: 0L
                put("updatedAt", JsonPrimitive(maxOf(localUpdated, remoteUpdated)))
            }

            merged.toString()
        } catch (e: Exception) {
            // En cas d'erreur de parsing, retourner le local
            localJson
        }
    }

    /**
     * Vérifie si une entité peut être synchronisée sans risque
     * (pas de conflit potentiel)
     */
    fun canSyncSafely(
        localItem: SyncQueueItem,
        remoteUpdatedAt: Long?
    ): Boolean {
        // INSERT: toujours safe (nouvelle entité)
        if (localItem.operation == SyncOperation.INSERT) return true

        // DELETE: safe si pas modifié sur le serveur depuis
        if (localItem.operation == SyncOperation.DELETE) {
            return remoteUpdatedAt == null ||
                   localItem.lastKnownRemoteUpdatedAt == null ||
                   remoteUpdatedAt <= localItem.lastKnownRemoteUpdatedAt
        }

        // UPDATE: vérifier si pas de modification côté serveur
        return !detectConflict(localItem, remoteUpdatedAt, null)
    }
}

/**
 * Représente un conflit nécessitant une intervention utilisateur
 */
data class UserConflict(
    val queueItemId: String,
    val entityType: String,
    val entityId: String,
    val localPayload: String,
    val remotePayload: String,
    val localUpdatedAt: Long,
    val remoteUpdatedAt: Long,
    val fieldDifferences: List<FieldDifference>
)

/**
 * Différence sur un champ spécifique
 */
data class FieldDifference(
    val fieldName: String,
    val localValue: String?,
    val remoteValue: String?
)
