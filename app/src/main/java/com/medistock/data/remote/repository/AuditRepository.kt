package com.medistock.data.remote.repository

import com.medistock.shared.data.dto.AuditHistoryDto
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

/**
 * Repository pour l'historique d'audit
 */
class AuditHistorySupabaseRepository : BaseSupabaseRepository("audit_history") {

    suspend fun getAllAuditHistory(): List<AuditHistoryDto> = getAll()

    suspend fun getAuditHistoryById(id: String): AuditHistoryDto? = getById(id)

    suspend fun createAuditEntry(audit: AuditHistoryDto): AuditHistoryDto = create(audit)

    /**
     * Récupère l'historique d'une entité spécifique
     */
    suspend fun getAuditHistoryByEntity(entityType: String, entityId: String): List<AuditHistoryDto> {
        return supabase.from(tableName).select {
            filter {
                eq("entity_type", entityType)
                eq("entity_id", entityId)
            }
            order(column = "changed_at", order = Order.DESCENDING)
        }.decodeList()
    }

    /**
     * Récupère l'historique des actions d'un utilisateur
     */
    suspend fun getAuditHistoryByUser(username: String): List<AuditHistoryDto> {
        return supabase.from(tableName).select {
            filter {
                eq("changed_by", username)
            }
            order(column = "changed_at", order = Order.DESCENDING)
        }.decodeList()
    }

    /**
     * Récupère l'historique d'un site
     */
    suspend fun getAuditHistoryBySite(siteId: String): List<AuditHistoryDto> {
        return supabase.from(tableName).select {
            filter {
                eq("site_id", siteId)
            }
            order(column = "changed_at", order = Order.DESCENDING)
        }.decodeList()
    }

    /**
     * Récupère l'historique par type d'action
     */
    suspend fun getAuditHistoryByAction(actionType: String): List<AuditHistoryDto> {
        return supabase.from(tableName).select {
            filter {
                eq("action_type", actionType)
            }
            order(column = "changed_at", order = Order.DESCENDING)
        }.decodeList()
    }

    /**
     * Récupère l'historique sur une période
     */
    suspend fun getAuditHistoryByDateRange(
        startDate: Long,
        endDate: Long,
        entityType: String? = null
    ): List<AuditHistoryDto> {
        return supabase.from(tableName).select {
            filter {
                gte("changed_at", startDate)
                lte("changed_at", endDate)
                if (entityType != null) {
                    eq("entity_type", entityType)
                }
            }
            order(column = "changed_at", order = Order.DESCENDING)
        }.decodeList()
    }

    /**
     * Récupère l'historique des modifications d'un champ spécifique
     */
    suspend fun getAuditHistoryByField(
        entityType: String,
        entityId: String,
        fieldName: String
    ): List<AuditHistoryDto> {
        return supabase.from(tableName).select {
            filter {
                eq("entity_type", entityType)
                eq("entity_id", entityId)
                eq("field_name", fieldName)
            }
            order(column = "changed_at", order = Order.DESCENDING)
        }.decodeList()
    }

    /**
     * Récupère les dernières modifications (toutes entités confondues)
     */
    suspend fun getRecentAuditHistory(limit: Int = 50): List<AuditHistoryDto> {
        return supabase.from(tableName).select {
            order(column = "changed_at", order = Order.DESCENDING)
            limit(limit.toLong())
        }.decodeList()
    }

    /**
     * Purge l'historique d'audit plus ancien qu'une certaine date
     * Utile pour nettoyer les anciennes données
     */
    suspend fun purgeOldAuditHistory(beforeDate: Long) {
        supabase.from(tableName).delete {
            filter {
                lt("changed_at", beforeDate)
            }
        }
    }
}
