package com.medistock.shared.data.repository

import com.medistock.shared.db.MedistockDatabase
import com.medistock.shared.domain.model.Site
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class SiteRepository(private val database: MedistockDatabase) {

    private val queries = database.medistockQueries

    suspend fun getAll(): List<Site> = withContext(Dispatchers.Default) {
        queries.getAllSites().executeAsList().map { it.toModel() }
    }

    /**
     * Get only active sites (for dropdowns/pickers in operational screens).
     */
    suspend fun getActive(): List<Site> = withContext(Dispatchers.Default) {
        queries.getActiveSites().executeAsList().map { it.toModel() }
    }

    suspend fun getById(id: String): Site? = withContext(Dispatchers.Default) {
        queries.getSiteById(id).executeAsOneOrNull()?.toModel()
    }

    suspend fun insert(site: Site) = withContext(Dispatchers.Default) {
        queries.insertSite(
            id = site.id,
            name = site.name,
            is_active = if (site.isActive) 1L else 0L,
            created_at = site.createdAt,
            updated_at = site.updatedAt,
            created_by = site.createdBy,
            updated_by = site.updatedBy
        )
    }

    suspend fun update(site: Site) = withContext(Dispatchers.Default) {
        queries.updateSite(
            name = site.name,
            is_active = if (site.isActive) 1L else 0L,
            updated_at = site.updatedAt,
            updated_by = site.updatedBy,
            id = site.id
        )
    }

    suspend fun delete(id: String) = withContext(Dispatchers.Default) {
        queries.deleteSite(id)
    }

    /**
     * Deactivate a site (soft delete).
     */
    suspend fun deactivate(id: String, updatedBy: String) = withContext(Dispatchers.Default) {
        val now = Clock.System.now().toEpochMilliseconds()
        queries.deactivateSite(now, updatedBy, id)
    }

    /**
     * Reactivate a previously deactivated site.
     */
    suspend fun activate(id: String, updatedBy: String) = withContext(Dispatchers.Default) {
        val now = Clock.System.now().toEpochMilliseconds()
        queries.activateSite(now, updatedBy, id)
    }

    /**
     * Upsert (INSERT OR REPLACE) a site.
     * Use this for sync operations to handle both new and existing records.
     */
    suspend fun upsert(site: Site) = withContext(Dispatchers.Default) {
        queries.upsertSite(
            id = site.id,
            name = site.name,
            is_active = if (site.isActive) 1L else 0L,
            created_at = site.createdAt,
            updated_at = site.updatedAt,
            created_by = site.createdBy,
            updated_by = site.updatedBy
        )
    }

    fun observeAll(): Flow<List<Site>> {
        return queries.getAllSites()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toModel() } }
    }

    /**
     * Observe only active sites.
     */
    fun observeActive(): Flow<List<Site>> {
        return queries.getActiveSites()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toModel() } }
    }

    // Extension function to convert SQLDelight generated class to domain model
    private fun com.medistock.shared.db.Sites.toModel(): Site {
        return Site(
            id = id,
            name = name,
            isActive = is_active == 1L,
            createdAt = created_at,
            updatedAt = updated_at,
            createdBy = created_by,
            updatedBy = updated_by
        )
    }
}
