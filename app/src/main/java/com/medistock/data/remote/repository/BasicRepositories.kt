package com.medistock.data.remote.repository

import com.medistock.data.remote.dto.*
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

class SiteSupabaseRepository : BaseSupabaseRepository("sites") {
    suspend fun getAllSites(): List<SiteDto> = getAll()
    suspend fun getSiteById(id: Long): SiteDto? = getById(id)
    suspend fun createSite(site: SiteDto): SiteDto = create(site)
    suspend fun updateSite(id: Long, site: SiteDto): SiteDto = update(id, site)
    suspend fun upsertSite(site: SiteDto): SiteDto = upsert(site)
    suspend fun deleteSite(id: Long) = delete(id)

    suspend fun searchByName(name: String): List<SiteDto> {
        return supabase.from(tableName).select {
            filter { ilike("name", "%$name%") }
        }.decodeList()
    }
}

class CategorySupabaseRepository : BaseSupabaseRepository("categories") {
    suspend fun getAllCategories(): List<CategoryDto> = getAll()
    suspend fun getCategoryById(id: Long): CategoryDto? = getById(id)
    suspend fun createCategory(category: CategoryDto): CategoryDto = create(category)
    suspend fun updateCategory(id: Long, category: CategoryDto): CategoryDto = update(id, category)
    suspend fun upsertCategory(category: CategoryDto): CategoryDto = upsert(category)
    suspend fun deleteCategory(id: Long) = delete(id)

    suspend fun searchByName(name: String): List<CategoryDto> {
        return supabase.from(tableName).select {
            filter { ilike("name", "%$name%") }
        }.decodeList()
    }
}

class PackagingTypeSupabaseRepository : BaseSupabaseRepository("packaging_types") {
    suspend fun getAllPackagingTypes(): List<PackagingTypeDto> = getAll()
    suspend fun getPackagingTypeById(id: Long): PackagingTypeDto? = getById(id)
    suspend fun createPackagingType(packagingType: PackagingTypeDto): PackagingTypeDto = create(packagingType)
    suspend fun updatePackagingType(id: Long, packagingType: PackagingTypeDto): PackagingTypeDto = update(id, packagingType)
    suspend fun upsertPackagingType(packagingType: PackagingTypeDto): PackagingTypeDto = upsert(packagingType)
    suspend fun deletePackagingType(id: Long) = delete(id)

    suspend fun getActivePackagingTypes(): List<PackagingTypeDto> {
        return supabase.from(tableName).select {
            filter { eq("is_active", true) }
        }.decodeList()
    }

    suspend fun getPackagingTypesByOrder(): List<PackagingTypeDto> {
        return supabase.from(tableName).select {
            order(column = "display_order", order = Order.ASCENDING)
        }.decodeList()
    }
}

class CustomerSupabaseRepository : BaseSupabaseRepository("customers") {
    suspend fun getAllCustomers(): List<CustomerDto> = getAll()
    suspend fun getCustomerById(id: Long): CustomerDto? = getById(id)
    suspend fun createCustomer(customer: CustomerDto): CustomerDto = create(customer)
    suspend fun updateCustomer(id: Long, customer: CustomerDto): CustomerDto = update(id, customer)
    suspend fun upsertCustomer(customer: CustomerDto): CustomerDto = upsert(customer)
    suspend fun deleteCustomer(id: Long) = delete(id)

    suspend fun getCustomersBySite(siteId: Long): List<CustomerDto> {
        return supabase.from(tableName).select {
            filter { eq("site_id", siteId) }
        }.decodeList()
    }

    suspend fun searchByName(name: String): List<CustomerDto> {
        return supabase.from(tableName).select {
            filter { ilike("name", "%$name%") }
        }.decodeList()
    }

    suspend fun searchByPhone(phone: String): List<CustomerDto> {
        return supabase.from(tableName).select {
            filter { ilike("phone", "%$phone%") }
        }.decodeList()
    }
}
