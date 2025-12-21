package com.medistock.data.remote.repository


import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.PostgrestFilterBuilder

import com.medistock.data.remote.dto.AuditHistoryDto

/**
 * Repository pour l'historique d'audit
 */
class AuditHistorySupabaseRepository : BaseSupabaseRepository("audit_history") {

    suspend fun getAllAuditHistory(): List<AuditHistoryDto> = getAll()

    suspend fun getAuditHistoryById(id: Long): AuditHistoryDto? = getById(id)

    suspend fun createAuditEntry(audit: AuditHistoryDto): AuditHistoryDto = create(audit)

    /**
     * Récupère l'historique d'une entité spécifique
     */
    suspend fun getAuditHistoryByEntity(entityType: String, entityId: Long): List<AuditHistoryDto> {
        return getWithFilter {
            eq("entity_type", entityType)
            eq("entity_id", entityId)
            order("changed_at", ascending = false)
        }
    }

    /**
     * Récupère l'historique des actions d'un utilisateur
     */
    suspend fun getAuditHistoryByUser(username: String): List<AuditHistoryDto> {
        return getWithFilter {
            eq("changed_by", username)
            order("changed_at", ascending = false)
        }
    }

    /**
     * Récupère l'historique d'un site
     */
    suspend fun getAuditHistoryBySite(siteId: Long): List<AuditHistoryDto> {
        return getWithFilter {
            eq("site_id", siteId)
            order("changed_at", ascending = false)
        }
    }

    /**
     * Récupère l'historique par type d'action
     */
    suspend fun getAuditHistoryByAction(actionType: String): List<AuditHistoryDto> {
        return getWithFilter {
            eq("action_type", actionType)
            order("changed_at", ascending = false)
        }
    }

    /**
     * Récupère l'historique sur une période
     */
    suspend fun getAuditHistoryByDateRange(
        startDate: Long,
        endDate: Long,
        entityType: String? = null
    ): List<AuditHistoryDto> {
        return getWithFilter {
            gte("changed_at", startDate)
            lte("changed_at", endDate)
            if (entityType != null) {
                eq("entity_type", entityType)
            }
            order("changed_at", ascending = false)
        }
    }

    /**
     * Récupère l'historique des modifications d'un champ spécifique
     */
    suspend fun getAuditHistoryByField(
        entityType: String,
        entityId: Long,
        fieldName: String
    ): List<AuditHistoryDto> {
        return getWithFilter {
            eq("entity_type", entityType)
            eq("entity_id", entityId)
            eq("field_name", fieldName)
            order("changed_at", ascending = false)
        }
    }

    /**
     * Récupère les dernières modifications (toutes entités confondues)
     */
    suspend fun getRecentAuditHistory(limit: Int = 50): List<AuditHistoryDto> {
        return getWithFilter {
            order("changed_at", ascending = false)
            limit(limit.toLong())
        }
    }

    /**
     * Purge l'historique d'audit plus ancien qu'une certaine date
     * Utile pour nettoyer les anciennes données
     */
    suspend fun purgeOldAuditHistory(beforeDate: Long) {
        supabase.from("audit_history").delete {
            filter {
                lt("changed_at", beforeDate)
            }
        }
    }
}
