package com.medistock.shared.data.repository

import com.medistock.shared.db.MedistockDatabase
import com.medistock.shared.domain.model.Customer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CustomerRepository(private val database: MedistockDatabase) {

    private val queries = database.medistockQueries

    suspend fun getAll(): List<Customer> = withContext(Dispatchers.Default) {
        queries.getAllCustomers().executeAsList().map { it.toModel() }
    }

    suspend fun getById(id: String): Customer? = withContext(Dispatchers.Default) {
        queries.getCustomerById(id).executeAsOneOrNull()?.toModel()
    }

    suspend fun insert(customer: Customer) = withContext(Dispatchers.Default) {
        queries.insertCustomer(
            id = customer.id,
            name = customer.name,
            phone = customer.phone,
            email = customer.email,
            address = customer.address,
            notes = customer.notes,
            site_id = customer.siteId,
            created_at = customer.createdAt,
            updated_at = customer.updatedAt,
            created_by = customer.createdBy,
            updated_by = customer.updatedBy
        )
    }

    suspend fun update(customer: Customer) = withContext(Dispatchers.Default) {
        queries.updateCustomer(
            name = customer.name,
            phone = customer.phone,
            email = customer.email,
            address = customer.address,
            notes = customer.notes,
            site_id = customer.siteId,
            updated_at = customer.updatedAt,
            updated_by = customer.updatedBy,
            id = customer.id
        )
    }

    suspend fun getBySite(siteId: String): List<Customer> = withContext(Dispatchers.Default) {
        queries.getCustomersBySite(siteId).executeAsList().map { it.toModel() }
    }

    suspend fun search(query: String): List<Customer> = withContext(Dispatchers.Default) {
        queries.searchCustomers(query, query).executeAsList().map { it.toModel() }
    }

    suspend fun delete(id: String) = withContext(Dispatchers.Default) {
        queries.deleteCustomer(id)
    }

    /**
     * Upsert (INSERT OR REPLACE) a customer.
     * Use this for sync operations to handle both new and existing records.
     */
    suspend fun upsert(customer: Customer) = withContext(Dispatchers.Default) {
        queries.upsertCustomer(
            id = customer.id,
            name = customer.name,
            phone = customer.phone,
            email = customer.email,
            address = customer.address,
            notes = customer.notes,
            site_id = customer.siteId,
            created_at = customer.createdAt,
            updated_at = customer.updatedAt,
            created_by = customer.createdBy,
            updated_by = customer.updatedBy
        )
    }

    fun observeAll(): Flow<List<Customer>> {
        return queries.getAllCustomers()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toModel() } }
    }

    private fun com.medistock.shared.db.Customers.toModel(): Customer {
        return Customer(
            id = id,
            name = name,
            phone = phone,
            email = email,
            address = address,
            notes = notes,
            siteId = site_id,
            createdAt = created_at,
            updatedAt = updated_at,
            createdBy = created_by,
            updatedBy = updated_by
        )
    }
}
