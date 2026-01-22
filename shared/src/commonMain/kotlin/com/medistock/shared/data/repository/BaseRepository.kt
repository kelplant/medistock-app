package com.medistock.shared.data.repository

import kotlinx.coroutines.flow.Flow

/**
 * Base repository interface for all entities
 */
interface BaseRepository<T, ID> {
    suspend fun getAll(): List<T>
    suspend fun getById(id: ID): T?
    suspend fun insert(item: T)
    suspend fun update(item: T)
    suspend fun delete(id: ID)
    fun observeAll(): Flow<List<T>>
}
