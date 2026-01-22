package com.medistock.shared.domain.model

/**
 * Represents a pending synchronization operation.
 * Each local modification creates an entry in this queue for later sync with the server.
 */
data class SyncQueueItem(
    val id: String,
    val entityType: String,
    val entityId: String,
    val operation: SyncOperation,
    val payload: String,
    val localVersion: Long = 1,
    val remoteVersion: Long? = null,
    val lastKnownRemoteUpdatedAt: Long? = null,
    val status: SyncStatus = SyncStatus.PENDING,
    val retryCount: Int = 0,
    val lastError: String? = null,
    val lastAttemptAt: Long? = null,
    val createdAt: Long = 0,
    val userId: String? = null,
    val siteId: String? = null
)

/**
 * Types of synchronization operations
 */
enum class SyncOperation {
    INSERT,
    UPDATE,
    DELETE;

    companion object {
        fun fromString(value: String): SyncOperation {
            return when (value.uppercase()) {
                "INSERT" -> INSERT
                "UPDATE" -> UPDATE
                "DELETE" -> DELETE
                else -> INSERT
            }
        }
    }
}

/**
 * Possible statuses for a sync queue item
 */
enum class SyncStatus {
    /** Waiting to be synchronized */
    PENDING,

    /** Currently being synchronized */
    IN_PROGRESS,

    /** Successfully synchronized (will be deleted) */
    SYNCED,

    /** Conflict detected - requires resolution */
    CONFLICT,

    /** Failed after max retries - requires intervention */
    FAILED;

    companion object {
        fun fromString(value: String): SyncStatus {
            return when (value.lowercase()) {
                "pending" -> PENDING
                "in_progress" -> IN_PROGRESS
                "synced" -> SYNCED
                "conflict" -> CONFLICT
                "failed" -> FAILED
                else -> PENDING
            }
        }
    }
}
