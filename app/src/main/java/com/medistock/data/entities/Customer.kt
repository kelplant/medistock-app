package com.medistock.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String? = null,
    val address: String? = null,
    val notes: String? = null,
    val siteId: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = ""
)
