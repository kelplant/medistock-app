package com.medistock.shared.data.repository

import com.medistock.shared.db.MedistockDatabase
import com.medistock.shared.domain.model.Site
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SiteRepository(private val database: MedistockDatabase) {

    private val queries = database.medistockQueries

    suspend fun getAll(): List<Site> = withContext(Dispatchers.Default) {
        queries.getAllSites().executeAsList().map { it.toModel() }
    }

    suspend fun getById(id: String): Site? = withContext(Dispatchers.Default) {
        queries.getSiteById(id).executeAsOneOrNull()?.toModel()
    }

    suspend fun insert(site: Site) = withContext(Dispatchers.Default) {
        queries.insertSite(
            id = site.id,
            name = site.name,
            created_at = site.createdAt,
            updated_at = site.updatedAt,
            created_by = site.createdBy,
            updated_by = site.updatedBy
        )
    }

    suspend fun update(site: Site) = withContext(Dispatchers.Default) {
        queries.updateSite(
            name = site.name,
            updated_at = site.updatedAt,
            updated_by = site.updatedBy,
            id = site.id
        )
    }

    suspend fun delete(id: String) = withContext(Dispatchers.Default) {
        queries.deleteSite(id)
    }

    fun observeAll(): Flow<List<Site>> {
        return queries.getAllSites()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toModel() } }
    }

    // Extension function to convert SQLDelight generated class to domain model
    private fun com.medistock.shared.db.Sites.toModel(): Site {
        return Site(
            id = id,
            name = name,
            createdAt = created_at,
            updatedAt = updated_at,
            createdBy = created_by,
            updatedBy = updated_by
        )
    }
}
