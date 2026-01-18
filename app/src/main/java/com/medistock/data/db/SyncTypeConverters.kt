package com.medistock.data.db

import androidx.room.TypeConverter
import com.medistock.data.entities.SyncOperation
import com.medistock.data.entities.SyncStatus

/**
 * Convertisseurs Room pour les types enum de synchronisation.
 */
class SyncTypeConverters {

    @TypeConverter
    fun fromSyncStatus(status: SyncStatus): String {
        return status.name
    }

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus {
        return try {
            SyncStatus.valueOf(value)
        } catch (e: IllegalArgumentException) {
            SyncStatus.PENDING
        }
    }

    @TypeConverter
    fun fromSyncOperation(operation: SyncOperation): String {
        return operation.name
    }

    @TypeConverter
    fun toSyncOperation(value: String): SyncOperation {
        return try {
            SyncOperation.valueOf(value)
        } catch (e: IllegalArgumentException) {
            SyncOperation.UPDATE
        }
    }
}
