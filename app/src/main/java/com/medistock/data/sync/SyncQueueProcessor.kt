package com.medistock.data.sync

import android.content.Context
import com.medistock.MedistockApplication
import com.medistock.data.remote.SupabaseClientProvider
import com.medistock.shared.data.dto.ProductDto
import com.medistock.shared.data.dto.CategoryDto
import com.medistock.shared.data.dto.SiteDto
import com.medistock.shared.data.dto.CustomerDto
import com.medistock.shared.data.dto.PackagingTypeDto
import com.medistock.shared.data.repository.SyncQueueRepository
import com.medistock.data.remote.repository.*
import com.medistock.shared.domain.model.SyncOperation
import com.medistock.shared.domain.model.SyncQueueItem
import com.medistock.shared.domain.model.SyncStatus
import com.medistock.shared.domain.sync.ConflictResolution
import com.medistock.shared.domain.sync.ConflictResolver
import com.medistock.shared.domain.sync.RetryConfiguration
import com.medistock.shared.domain.sync.UserConflict
import com.medistock.util.NetworkStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Processeur de la queue de synchronisation.
 *
 * Responsabilités:
 * - Traiter les éléments en attente dans l'ordre FIFO
 * - Gérer les retries avec backoff exponentiel
 * - Détecter et résoudre les conflits
 * - Émettre des événements de progression
 */
class SyncQueueProcessor(
    private val context: Context
) {
    companion object {
        private const val TAG = "SyncQueueProcessor"
    }

    // Configuration de retry partagée (depuis le module shared)
    private val retryConfig = RetryConfiguration.DEFAULT

    private val sdk = MedistockApplication.sdk
    private val syncQueueRepository: SyncQueueRepository = sdk.syncQueueRepository
    private val conflictResolver = ConflictResolver()

    // Repositories Supabase
    private val productRepo by lazy { ProductSupabaseRepository() }
    private val categoryRepo by lazy { CategorySupabaseRepository() }
    private val siteRepo by lazy { SiteSupabaseRepository() }
    private val customerRepo by lazy { CustomerSupabaseRepository() }
    private val packagingTypeRepo by lazy { PackagingTypeSupabaseRepository() }

    // État de la synchronisation
    private val _syncState = MutableStateFlow(SyncProcessorState())
    val syncState: StateFlow<SyncProcessorState> = _syncState.asStateFlow()

    private val _events = MutableSharedFlow<SyncEvent>()
    val events: SharedFlow<SyncEvent> = _events.asSharedFlow()

    private var processingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    /**
     * Démarre le traitement de la queue
     */
    fun startProcessing() {
        if (processingJob?.isActive == true) {
            println("$TAG: Traitement déjà en cours")
            return
        }

        processingJob = scope.launch {
            processQueue()
        }
    }

    /**
     * Arrête le traitement de la queue
     */
    fun stopProcessing() {
        processingJob?.cancel()
        processingJob = null
        _syncState.value = _syncState.value.copy(isProcessing = false)
    }

    /**
     * Traite la queue de synchronisation
     */
    private suspend fun processQueue() {
        if (!canProcess()) {
            emitEvent(SyncEvent.CannotProcess("Pas de connexion ou Supabase non configuré"))
            return
        }

        _syncState.value = _syncState.value.copy(isProcessing = true)
        emitEvent(SyncEvent.ProcessingStarted)

        try {
            var hasMore = true
            var totalProcessed = 0
            var totalSuccess = 0
            var totalFailed = 0
            var totalConflicts = 0

            while (hasMore && isActive()) {
                val batch = syncQueueRepository.getPendingBatch(retryConfig.batchSize)

                if (batch.isEmpty()) {
                    hasMore = false
                    continue
                }

                for (item in batch) {
                    if (!isActive()) break

                    val result = processItem(item)
                    totalProcessed++

                    when (result) {
                        is ProcessResult.Success -> {
                            totalSuccess++
                            syncQueueRepository.deleteById(item.id)
                        }
                        is ProcessResult.Conflict -> {
                            totalConflicts++
                            syncQueueRepository.updateStatus(item.id, SyncStatus.CONFLICT)
                        }
                        is ProcessResult.Retry -> {
                            if (item.retryCount >= retryConfig.maxRetries) {
                                totalFailed++
                                syncQueueRepository.updateStatusWithRetry(
                                    item.id,
                                    SyncStatus.FAILED,
                                    System.currentTimeMillis(),
                                    result.error
                                )
                            } else {
                                syncQueueRepository.updateStatusWithRetry(
                                    item.id,
                                    SyncStatus.PENDING,
                                    System.currentTimeMillis(),
                                    result.error
                                )
                                // Attendre avec backoff avant de réessayer
                                delay(retryConfig.getDelayMs(item.retryCount))
                            }
                        }
                        is ProcessResult.Skip -> {
                            // Supprimer les éléments obsolètes
                            syncQueueRepository.deleteById(item.id)
                        }
                    }

                    _syncState.value = _syncState.value.copy(
                        processedCount = totalProcessed,
                        successCount = totalSuccess,
                        failedCount = totalFailed,
                        conflictCount = totalConflicts
                    )
                }
            }

            // Nettoyer les éléments synchronisés
            syncQueueRepository.deleteSynced()

            emitEvent(SyncEvent.ProcessingCompleted(
                processed = totalProcessed,
                success = totalSuccess,
                failed = totalFailed,
                conflicts = totalConflicts
            ))

        } catch (e: CancellationException) {
            emitEvent(SyncEvent.ProcessingCancelled)
            throw e
        } catch (e: Exception) {
            emitEvent(SyncEvent.ProcessingError(e.message ?: "Erreur inconnue"))
        } finally {
            _syncState.value = _syncState.value.copy(isProcessing = false)
        }
    }

    /**
     * Traite un élément individuel de la queue
     */
    private suspend fun processItem(item: SyncQueueItem): ProcessResult {
        return try {
            // Marquer comme en cours
            syncQueueRepository.updateStatus(item.id, SyncStatus.IN_PROGRESS)

            // Vérifier si l'opération est toujours pertinente
            if (isOperationObsolete(item)) {
                return ProcessResult.Skip("Opération obsolète")
            }

            // Récupérer la version actuelle sur le serveur
            val remoteData = fetchRemoteEntity(item.entityType, item.entityId)
            val remoteUpdatedAt = extractUpdatedAt(remoteData)

            // Détecter les conflits
            if (conflictResolver.detectConflict(item.lastKnownRemoteUpdatedAt, remoteUpdatedAt)) {
                val resolution = conflictResolver.resolve(
                    entityType = item.entityType,
                    localPayload = item.payload,
                    remotePayload = remoteData,
                    localUpdatedAt = item.createdAt,
                    remoteUpdatedAt = remoteUpdatedAt ?: 0L
                )

                when (resolution.resolution) {
                    ConflictResolution.LOCAL_WINS -> {
                        applyToRemote(item)
                    }
                    ConflictResolution.REMOTE_WINS -> {
                        if (remoteData != null) {
                            applyToLocal(item.entityType, item.entityId, remoteData)
                        }
                        return ProcessResult.Success
                    }
                    ConflictResolution.MERGE -> {
                        resolution.mergedPayload?.let { payload ->
                            applyMergedToRemote(item, payload)
                        }
                    }
                    ConflictResolution.ASK_USER -> {
                        emitEvent(SyncEvent.ConflictDetected(
                            conflictResolver.createUserConflict(
                                queueItemId = item.id,
                                entityType = item.entityType,
                                entityId = item.entityId,
                                localPayload = item.payload,
                                remotePayload = remoteData,
                                localUpdatedAt = item.createdAt,
                                remoteUpdatedAt = remoteUpdatedAt ?: 0L
                            )
                        ))
                        return ProcessResult.Conflict(resolution.message ?: "Conflit détecté")
                    }
                    ConflictResolution.KEEP_BOTH -> {
                        createCopyOnRemote(item)
                    }
                }
            } else {
                applyToRemote(item)
            }

            ProcessResult.Success
        } catch (e: Exception) {
            println("$TAG: Erreur traitement ${item.id}: ${e.message}")
            ProcessResult.Retry(e.message ?: "Erreur inconnue")
        }
    }

    /**
     * Applique une opération sur le serveur distant
     */
    private suspend fun applyToRemote(item: SyncQueueItem) {
        when (item.operation) {
            SyncOperation.INSERT -> insertToRemote(item)
            SyncOperation.UPDATE -> updateOnRemote(item)
            SyncOperation.DELETE -> deleteOnRemote(item)
        }
    }

    private suspend fun insertToRemote(item: SyncQueueItem) {
        when (item.entityType) {
            "Product" -> {
                val product = json.decodeFromString<ProductDto>(item.payload)
                productRepo.upsertProduct(product)
            }
            "Category" -> {
                val category = json.decodeFromString<CategoryDto>(item.payload)
                categoryRepo.upsertCategory(category)
            }
            "Site" -> {
                val site = json.decodeFromString<SiteDto>(item.payload)
                siteRepo.upsertSite(site)
            }
            "Customer" -> {
                val customer = json.decodeFromString<CustomerDto>(item.payload)
                customerRepo.upsertCustomer(customer)
            }
            "PackagingType" -> {
                val packagingType = json.decodeFromString<PackagingTypeDto>(item.payload)
                packagingTypeRepo.upsertPackagingType(packagingType)
            }
            else -> throw IllegalArgumentException("Type d'entité non supporté: ${item.entityType}")
        }
    }

    private suspend fun updateOnRemote(item: SyncQueueItem) {
        insertToRemote(item)
    }

    private suspend fun deleteOnRemote(item: SyncQueueItem) {
        when (item.entityType) {
            "Product" -> productRepo.deleteProduct(item.entityId)
            "Category" -> categoryRepo.deleteCategory(item.entityId)
            "Site" -> siteRepo.deleteSite(item.entityId)
            "Customer" -> customerRepo.deleteCustomer(item.entityId)
            "PackagingType" -> packagingTypeRepo.deletePackagingType(item.entityId)
            else -> throw IllegalArgumentException("Type d'entité non supporté: ${item.entityType}")
        }
    }

    private suspend fun applyMergedToRemote(item: SyncQueueItem, mergedPayload: String) {
        val mergedItem = item.copy(payload = mergedPayload)
        insertToRemote(mergedItem)
    }

    private suspend fun createCopyOnRemote(item: SyncQueueItem) {
        val newId = java.util.UUID.randomUUID().toString()
        val modifiedPayload = item.payload.replace(
            "\"id\":\"${item.entityId}\"",
            "\"id\":\"$newId\""
        )
        val copyItem = item.copy(entityId = newId, payload = modifiedPayload)
        insertToRemote(copyItem)
    }

    /**
     * Récupère l'entité depuis le serveur distant
     */
    private suspend fun fetchRemoteEntity(entityType: String, entityId: String): String? {
        return try {
            when (entityType) {
                "Product" -> productRepo.getProductById(entityId)?.let { json.encodeToString(ProductDto.serializer(), it) }
                "Category" -> categoryRepo.getCategoryById(entityId)?.let { json.encodeToString(CategoryDto.serializer(), it) }
                "Site" -> siteRepo.getSiteById(entityId)?.let { json.encodeToString(SiteDto.serializer(), it) }
                "Customer" -> customerRepo.getCustomerById(entityId)?.let { json.encodeToString(CustomerDto.serializer(), it) }
                "PackagingType" -> packagingTypeRepo.getPackagingTypeById(entityId)?.let { json.encodeToString(PackagingTypeDto.serializer(), it) }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Applique les données du serveur localement
     */
    private suspend fun applyToLocal(entityType: String, entityId: String, remoteData: String) {
        // Use upsert (INSERT OR REPLACE) to handle both new and existing records
        when (entityType) {
            "Product" -> {
                val dto = json.decodeFromString<ProductDto>(remoteData)
                sdk.productRepository.upsert(dto.toModel())
            }
            "Category" -> {
                val dto = json.decodeFromString<CategoryDto>(remoteData)
                sdk.categoryRepository.upsert(dto.toModel())
            }
            "Site" -> {
                val dto = json.decodeFromString<SiteDto>(remoteData)
                sdk.siteRepository.upsert(dto.toModel())
            }
            "Customer" -> {
                val dto = json.decodeFromString<CustomerDto>(remoteData)
                sdk.customerRepository.upsert(dto.toModel())
            }
            "PackagingType" -> {
                val dto = json.decodeFromString<PackagingTypeDto>(remoteData)
                sdk.packagingTypeRepository.upsert(dto.toModel())
            }
        }
    }

    /**
     * Extrait le timestamp updatedAt d'un payload JSON
     */
    private fun extractUpdatedAt(payload: String?): Long? {
        if (payload == null) return null
        return try {
            val jsonElement = Json.parseToJsonElement(payload)
            jsonElement.jsonObject["updated_at"]?.jsonPrimitive?.content?.toLongOrNull()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Vérifie si une opération est devenue obsolète
     */
    private suspend fun isOperationObsolete(item: SyncQueueItem): Boolean {
        val laterDelete = syncQueueRepository.getByEntity(item.entityType, item.entityId)
            .any { it.operation == SyncOperation.DELETE && it.createdAt > item.createdAt }

        return laterDelete && item.operation != SyncOperation.DELETE
    }

    private fun canProcess(): Boolean {
        return NetworkStatus.isOnline(context) && SupabaseClientProvider.isConfigured(context)
    }

    private fun isActive(): Boolean {
        return processingJob?.isActive == true
    }

    private suspend fun emitEvent(event: SyncEvent) {
        _events.emit(event)
    }

    /**
     * Résout manuellement un conflit
     */
    suspend fun resolveConflict(queueItemId: String, resolution: ConflictResolution, mergedPayload: String? = null) {
        val item = syncQueueRepository.getById(queueItemId) ?: return

        when (resolution) {
            ConflictResolution.LOCAL_WINS -> {
                applyToRemote(item)
                syncQueueRepository.deleteById(queueItemId)
            }
            ConflictResolution.REMOTE_WINS -> {
                syncQueueRepository.deleteById(queueItemId)
            }
            ConflictResolution.MERGE -> {
                if (mergedPayload != null) {
                    applyMergedToRemote(item, mergedPayload)
                }
                syncQueueRepository.deleteById(queueItemId)
            }
            else -> {
                syncQueueRepository.deleteById(queueItemId)
            }
        }
    }

    /**
     * Force le retraitement des éléments en échec
     */
    suspend fun retryFailed() {
        syncQueueRepository.updateAllStatus(SyncStatus.FAILED, SyncStatus.PENDING)
        startProcessing()
    }
}

/**
 * Résultat du traitement d'un élément
 */
sealed class ProcessResult {
    object Success : ProcessResult()
    data class Conflict(val reason: String) : ProcessResult()
    data class Retry(val error: String) : ProcessResult()
    data class Skip(val reason: String) : ProcessResult()
}

/**
 * État du processeur de sync
 */
data class SyncProcessorState(
    val isProcessing: Boolean = false,
    val processedCount: Int = 0,
    val successCount: Int = 0,
    val failedCount: Int = 0,
    val conflictCount: Int = 0
)

/**
 * Événements de synchronisation
 */
sealed class SyncEvent {
    object ProcessingStarted : SyncEvent()
    object ProcessingCancelled : SyncEvent()
    data class ProcessingCompleted(
        val processed: Int,
        val success: Int,
        val failed: Int,
        val conflicts: Int
    ) : SyncEvent()
    data class ProcessingError(val message: String) : SyncEvent()
    data class CannotProcess(val reason: String) : SyncEvent()
    data class ConflictDetected(val conflict: UserConflict) : SyncEvent()
    data class ItemSynced(val entityType: String, val entityId: String) : SyncEvent()
    data class ItemFailed(val entityType: String, val entityId: String, val error: String) : SyncEvent()
}
