package com.medistock.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "user_permissions",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("user_id")]
)
data class UserPermission(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "user_id") val userId: String,
    val module: String,
    @ColumnInfo(name = "can_view") val canView: Boolean = false,
    @ColumnInfo(name = "can_create") val canCreate: Boolean = false,
    @ColumnInfo(name = "can_edit") val canEdit: Boolean = false,
    @ColumnInfo(name = "can_delete") val canDelete: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "created_by") val createdBy: String = "",
    @ColumnInfo(name = "updated_by") val updatedBy: String = ""
)
