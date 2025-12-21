package com.medistock.data.remote.repository

import com.medistock.data.remote.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from

/**
 * Repository de base avec les opérations CRUD communes pour toutes les tables
 *
 * @param tableName Nom de la table dans Supabase
 */
abstract class BaseSupabaseRepository(
    val tableName: String
) {
    val supabase = SupabaseClientProvider.client

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
    suspend inline fun <reified R : Any> create(item: R): R {
        return supabase.from(tableName).insert(item) {
            select()
        }.decodeSingle()
    }

    /**
     * Met à jour un enregistrement
     */
    suspend inline fun <reified R : Any> update(id: Long, item: R): R {
        return supabase.from(tableName).update(item) {
            select()
            filter {
                eq("id", id)
            }
        }.decodeSingle()
    }

    /**
     * Upsert (Insert or Update) - Crée si n'existe pas, met à jour sinon
     * Utilise la clé primaire 'id' pour détecter les conflits
     */
    suspend inline fun <reified R : Any> upsert(item: R): R {
        return supabase.from(tableName).upsert(item) {
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
}
