package com.medistock.data.remote.repository

import com.medistock.data.remote.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

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
    suspend inline fun <reified R> getById(id: String): R? {
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
        val payload = withClientId(item)
        return supabase.from(tableName).insert(payload) {
            select()
        }.decodeSingle()
    }

    /**
     * Met à jour un enregistrement
     */
    suspend inline fun <reified R : Any> update(id: String, item: R): R {
        val payload = withClientId(item)
        return supabase.from(tableName).update(payload) {
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
        println("🔄 Upsert to $tableName: $item")
        val payload = withClientId(item)
        println("🔄 Payload with client_id: $payload")
        val result = supabase.from(tableName).upsert(payload) {
            select()
        }.decodeSingle<R>()
        println("✅ Upsert result from $tableName: $result")
        return result
    }

    /**
     * Supprime un enregistrement
     */
    suspend fun delete(id: String) {
        supabase.from(tableName).delete {
            filter {
                eq("id", id)
            }
        }
    }

    @PublishedApi
    internal inline fun <reified R : Any> withClientId(item: R): Any {
        val currentClientId = SupabaseClientProvider.getClientId() ?: return item
        val element = runCatching { Json.encodeToJsonElement(item) }.getOrNull()
        val baseObject = element?.jsonObject ?: return item

        return JsonObject(
            buildMap {
                baseObject.forEach { (key, value) -> put(key, value) }
                put("client_id", Json.encodeToJsonElement(currentClientId))
            }
        )
    }
}
