package com.medistock.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Représente une opération en attente de synchronisation avec le serveur.
 * Cette table persiste les changements effectués en mode offline pour les rejouer
 * lors du retour en mode connecté.
 *
 * Stratégie:
 * - Chaque modification locale crée une entrée dans cette queue
 * - Au retour en ligne, les entrées sont traitées dans l'ordre (FIFO)
 * - En cas de conflit, la stratégie appropriée est appliquée selon l'entité
 * - L'entrée est supprimée après sync réussie ou marquée en conflit
 */
@Entity(
    tableName = "sync_queue",
    indices = [
        Index(value = ["status"]),
        Index(value = ["entityType", "entityId"]),
        Index(value = ["createdAt"])
    ]
)
data class SyncQueueItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    /** Type d'entité concernée (Product, Sale, StockMovement, etc.) */
    val entityType: String,

    /** ID de l'entité concernée */
    val entityId: String,

    /** Type d'opération: INSERT, UPDATE, DELETE */
    val operation: SyncOperation,

    /** Données JSON de l'entité au moment de l'opération */
    val payload: String,

    /** Version de l'entité locale au moment de l'opération (pour détection de conflits) */
    val localVersion: Long,

    /** Version de l'entité sur le serveur au moment de la dernière sync (null si nouvelle) */
    val remoteVersion: Long? = null,

    /** Timestamp de la dernière modification connue sur le serveur */
    val lastKnownRemoteUpdatedAt: Long? = null,

    /** Statut de l'élément dans la queue */
    val status: SyncStatus = SyncStatus.PENDING,

    /** Nombre de tentatives de sync échouées */
    val retryCount: Int = 0,

    /** Message d'erreur de la dernière tentative (si échec) */
    val lastError: String? = null,

    /** Timestamp de la dernière tentative de sync */
    val lastAttemptAt: Long? = null,

    /** Timestamp de création de l'entrée */
    val createdAt: Long = System.currentTimeMillis(),

    /** ID de l'utilisateur ayant effectué l'opération */
    val userId: String? = null,

    /** ID du site concerné */
    val siteId: String? = null
)

/**
 * Types d'opérations de synchronisation
 */
enum class SyncOperation {
    INSERT,
    UPDATE,
    DELETE
}

/**
 * Statuts possibles d'un élément dans la queue de sync
 */
enum class SyncStatus {
    /** En attente de synchronisation */
    PENDING,

    /** En cours de synchronisation */
    IN_PROGRESS,

    /** Synchronisation réussie (sera supprimé) */
    SYNCED,

    /** Conflit détecté - nécessite résolution */
    CONFLICT,

    /** Échec après max retries - nécessite intervention */
    FAILED
}

/**
 * Résultat de la résolution d'un conflit
 */
data class ConflictResolutionResult(
    val resolution: ConflictResolution,
    val mergedPayload: String? = null,
    val message: String? = null
)

/**
 * Stratégies de résolution de conflits
 */
enum class ConflictResolution {
    /** Garder la version locale (client wins) */
    LOCAL_WINS,

    /** Garder la version serveur (server wins) */
    REMOTE_WINS,

    /** Fusionner les deux versions */
    MERGE,

    /** Demander à l'utilisateur */
    ASK_USER,

    /** Conserver les deux (créer une copie) */
    KEEP_BOTH
}
