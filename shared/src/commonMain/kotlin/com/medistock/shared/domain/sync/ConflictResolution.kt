package com.medistock.shared.domain.sync

import kotlinx.serialization.Serializable

/**
 * Conflict resolution strategies for sync operations.
 */
enum class ConflictResolution {
    /** Client wins - keep local version */
    LOCAL_WINS,
    /** Server wins - keep remote version */
    REMOTE_WINS,
    /** Merge both versions */
    MERGE,
    /** Require user intervention */
    ASK_USER,
    /** Create copy, keep both versions */
    KEEP_BOTH
}

/**
 * Result of conflict resolution.
 */
@Serializable
data class ConflictResolutionResult(
    val resolution: ConflictResolution,
    val mergedPayload: String? = null,
    val message: String? = null
)

/**
 * Represents a conflict requiring user intervention.
 */
@Serializable
data class UserConflict(
    val queueItemId: String,
    val entityType: String,
    val entityId: String,
    val localPayload: String,
    val remotePayload: String?,
    val localUpdatedAt: Long,
    val remoteUpdatedAt: Long,
    val fieldDifferences: List<FieldDifference> = emptyList()
)

/**
 * Represents a difference in a specific field between local and remote versions.
 */
@Serializable
data class FieldDifference(
    val fieldName: String,
    val localValue: String?,
    val remoteValue: String?
)
