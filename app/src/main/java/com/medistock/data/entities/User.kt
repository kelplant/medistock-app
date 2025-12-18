package com.medistock.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "username") val username: String,
    @ColumnInfo(name = "password") val password: String, // In production, this should be hashed
    @ColumnInfo(name = "fullName") val fullName: String,
    @ColumnInfo(name = "isAdmin") val isAdmin: Boolean = false,
    @ColumnInfo(name = "isActive") val isActive: Boolean = true,
    @ColumnInfo(name = "createdAt") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updatedAt") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "createdBy") val createdBy: String = "",
    @ColumnInfo(name = "updatedBy") val updatedBy: String = ""
)
