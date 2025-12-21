package com.medistock.data.remote.repository

import com.medistock.data.remote.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.PostgrestFilterBuilder

/**
 * Repository de base avec les opérations CRUD communes pour toutes les tables
 *
 * @param T Type du DTO
 * @param tableName Nom de la table dans Supabase
 */
abstract class BaseSupabaseRepository<T : Any>(
    private val tableName: String
) {
    protected val supabase = SupabaseClientProvider.client

    /**
     * Récupère tous les enregistrements
     */
    suspend inline fun <reified T> getAll(): List<T> {
        return supabase.from(tableName).select().decodeList()
    }

    /**
     * Récupère un enregistrement par ID
     */
    suspend inline fun <reified T> getById(id: Long): T? {
        return try {
            supabase.from(tableName)
                .select {
                    filter { eq("id", id) }
                }
                .decodeSingleOrNull()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Crée un nouvel enregistrement
     */
    suspend inline fun <reified T> create(item: T): T {
        return supabase.from(tableName).insert(item) {
            select()
        }.decodeSingle()
    }

    /**
     * Met à jour un enregistrement
     */
    suspend inline fun <reified T> update(id: Long, item: T): T {
        return supabase.from(tableName).update(item) {
            filter { eq("id", id) }
            select()
        }.decodeSingle()
    }

    /**
     * Supprime un enregistrement
     */
    suspend fun delete(id: Long) {
        supabase.from(tableName).delete {
            filter { eq("id", id) }
        }
    }

    /**
     * Récupère les enregistrements avec un filtre personnalisé
     */
    suspend inline fun <reified T> getWithFilter(
        crossinline filterBlock: PostgrestFilterBuilder.() -> Unit
    ): List<T> {
        return supabase.from(tableName).select {
            filter(filterBlock)
        }.decodeList()
    }

    /**
     * Compte le nombre d'enregistrements avec un filtre optionnel
     */
    suspend fun count(
        filterBlock: (PostgrestFilterBuilder.() -> Unit)? = null
    ): Long {
        val result = if (filterBlock != null) {
            supabase.from(tableName).select(Columns.raw("count")) {
                filter(filterBlock)
            }
        } else {
            supabase.from(tableName).select(Columns.raw("count"))
        }
        // Parse count from response
        return 0L // TODO: Implement count parsing
    }

    /**
     * Vérifie si un enregistrement existe
     */
    suspend fun exists(id: Long): Boolean {
        return getById<T>(id) != null
    }
}
