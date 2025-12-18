package com.medistock.data.entities

import androidx.room.ColumnInfo
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
    @ColumnInfo(name = "userId") val userId: Long,
    @ColumnInfo(name = "module") val module: String, // e.g., "STOCK", "SALES", "PURCHASES", "INVENTORY", "ADMIN", "PRODUCTS", "SITES", "CATEGORIES"
    @ColumnInfo(name = "canView") val canView: Boolean = false,
    @ColumnInfo(name = "canCreate") val canCreate: Boolean = false,
    @ColumnInfo(name = "canEdit") val canEdit: Boolean = false,
    @ColumnInfo(name = "canDelete") val canDelete: Boolean = false,
    @ColumnInfo(name = "createdAt") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updatedAt") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "createdBy") val createdBy: String = "",
    @ColumnInfo(name = "updatedBy") val updatedBy: String = ""
)
