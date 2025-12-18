package com.medistock.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_permissions",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class UserPermission(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long,
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
