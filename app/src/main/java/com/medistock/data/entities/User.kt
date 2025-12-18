package com.medistock.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val password: String, // In production, this should be hashed
    @ColumnInfo(name = "full_name") val fullName: String,
    @ColumnInfo(name = "is_admin") val isAdmin: Boolean = false,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "created_by") val createdBy: String = "",
    @ColumnInfo(name = "updated_by") val updatedBy: String = ""
)
