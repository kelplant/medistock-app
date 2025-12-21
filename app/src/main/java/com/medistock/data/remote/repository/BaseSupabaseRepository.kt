package com.medistock.data.remote.repository

import com.medistock.data.remote.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

/**
 * Repository de base avec les opérations CRUD communes pour toutes les tables
 *
 * @param T Type du DTO
 * @param tableName Nom de la table dans Supabase
 */
abstract class BaseSupabaseRepository<T : Any>(
    protected val tableName: String
) {
    protected val supabase = SupabaseClientProvider.client

    /**
     * Récupère tous les enregistrements
     */
    suspend inline fun <reified R> getAll(): List<R> {
        return supabase.from(tableName).select().decodeList()
    }

    /**
     * Récupère un enregistrement par ID
     */
    suspend inline fun <reified R> getById(id: Long): R? {
        return try {
            supabase.from(tableName)
                .select {
                    filter {
                        eq("id", id)
                    }
                }
                .decodeSingleOrNull()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Crée un nouvel enregistrement
     */
    suspend inline fun <reified R> create(item: R): R {
        return supabase.from(tableName).insert(item) {
            select()
        }.decodeSingle()
    }

    /**
     * Met à jour un enregistrement
     */
    suspend inline fun <reified R> update(id: Long, item: R): R {
        return supabase.from(tableName).update(item) {
            filter {
                eq("id", id)
            }
            select()
        }.decodeSingle()
    }

    /**
     * Supprime un enregistrement
     */
    suspend fun delete(id: Long) {
        supabase.from(tableName).delete {
            filter {
                eq("id", id)
            }
        }
    }

    /**
     * Récupère les enregistrements avec un filtre personnalisé
     */
    suspend inline fun <reified R> getWithFilter(
        noinline filterBlock: suspend io.github.jan.supabase.postgrest.query.PostgrestFilterBuilder.() -> Unit
    ): List<R> {
        return supabase.from(tableName).select {
            filter(filterBlock)
        }.decodeList()
    }

    /**
     * Compte le nombre d'enregistrements avec un filtre optionnel
     */
    suspend fun count(
        filterBlock: (suspend io.github.jan.supabase.postgrest.query.PostgrestFilterBuilder.() -> Unit)? = null
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
    suspend inline fun <reified R> exists(id: Long): Boolean {
        return getById<R>(id) != null
    }
}
