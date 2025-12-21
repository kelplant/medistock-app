package com.medistock.data.remote.repository


import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.PostgrestFilterBuilder

import com.medistock.data.remote.dto.*

/**
 * Repository pour les sites
 */
class SiteSupabaseRepository : BaseSupabaseRepository("sites") {

    suspend fun getAllSites(): List<SiteDto> = getAll()

    suspend fun getSiteById(id: Long): SiteDto? = getById(id)

    suspend fun createSite(site: SiteDto): SiteDto = create(site)

    suspend fun updateSite(id: Long, site: SiteDto): SiteDto = update(id, site)

    suspend fun deleteSite(id: Long) = delete(id)

    /**
     * Recherche de sites par nom
     */
    suspend fun searchByName(name: String): List<SiteDto> {
        return getWithFilter {
            ilike("name", "%$name%")
        }
    }
}

/**
 * Repository pour les catégories
 */
class CategorySupabaseRepository : BaseSupabaseRepository("categories") {

    suspend fun getAllCategories(): List<CategoryDto> = getAll()

    suspend fun getCategoryById(id: Long): CategoryDto? = getById(id)

    suspend fun createCategory(category: CategoryDto): CategoryDto = create(category)

    suspend fun updateCategory(id: Long, category: CategoryDto): CategoryDto = update(id, category)

    suspend fun deleteCategory(id: Long) = delete(id)

    /**
     * Recherche de catégories par nom
     */
    suspend fun searchByName(name: String): List<CategoryDto> {
        return getWithFilter {
            ilike("name", "%$name%")
        }
    }
}

/**
 * Repository pour les types de conditionnement
 */
class PackagingTypeSupabaseRepository : BaseSupabaseRepository("packaging_types") {

    suspend fun getAllPackagingTypes(): List<PackagingTypeDto> = getAll()

    suspend fun getPackagingTypeById(id: Long): PackagingTypeDto? = getById(id)

    suspend fun createPackagingType(packagingType: PackagingTypeDto): PackagingTypeDto = create(packagingType)

    suspend fun updatePackagingType(id: Long, packagingType: PackagingTypeDto): PackagingTypeDto = update(id, packagingType)

    suspend fun deletePackagingType(id: Long) = delete(id)

    /**
     * Récupère uniquement les types actifs
     */
    suspend fun getActivePackagingTypes(): List<PackagingTypeDto> {
        return getWithFilter {
            eq("is_active", true)
        }
    }

    /**
     * Récupère les types triés par ordre d'affichage
     */
    suspend fun getPackagingTypesByOrder(): List<PackagingTypeDto> {
        return getWithFilter {
            order("display_order")
        }
    }
}

/**
 * Repository pour les clients
 */
class CustomerSupabaseRepository : BaseSupabaseRepository("customers") {

    suspend fun getAllCustomers(): List<CustomerDto> = getAll()

    suspend fun getCustomerById(id: Long): CustomerDto? = getById(id)

    suspend fun createCustomer(customer: CustomerDto): CustomerDto = create(customer)

    suspend fun updateCustomer(id: Long, customer: CustomerDto): CustomerDto = update(id, customer)

    suspend fun deleteCustomer(id: Long) = delete(id)

    /**
     * Récupère les clients d'un site spécifique
     */
    suspend fun getCustomersBySite(siteId: Long): List<CustomerDto> {
        return getWithFilter {
            eq("site_id", siteId)
        }
    }

    /**
     * Recherche de clients par nom
     */
    suspend fun searchByName(name: String): List<CustomerDto> {
        return getWithFilter {
            ilike("name", "%$name%")
        }
    }

    /**
     * Recherche de clients par numéro de téléphone
     */
    suspend fun searchByPhone(phone: String): List<CustomerDto> {
        return getWithFilter {
            ilike("phone", "%$phone%")
        }
    }
}
