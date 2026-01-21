package com.medistock.shared.data.repository

import com.medistock.shared.db.MedistockDatabase
import com.medistock.shared.domain.model.Sale
import com.medistock.shared.domain.model.SaleItem
import com.medistock.shared.domain.model.SaleWithItems
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SaleRepository(private val database: MedistockDatabase) {

    private val queries = database.medistockQueries

    suspend fun getAll(): List<Sale> = withContext(Dispatchers.Default) {
        queries.getAllSales().executeAsList().map { it.toModel() }
    }

    suspend fun getById(id: String): Sale? = withContext(Dispatchers.Default) {
        queries.getSaleById(id).executeAsOneOrNull()?.toModel()
    }

    suspend fun getBySite(siteId: String): List<Sale> = withContext(Dispatchers.Default) {
        queries.getSalesBySite(siteId).executeAsList().map { it.toModel() }
    }

    suspend fun getSaleWithItems(saleId: String): SaleWithItems? = withContext(Dispatchers.Default) {
        val sale = queries.getSaleById(saleId).executeAsOneOrNull()?.toModel() ?: return@withContext null
        val items = queries.getSaleItemsBySale(saleId).executeAsList().map { it.toModel() }
        SaleWithItems(sale, items)
    }

    suspend fun insert(sale: Sale) = withContext(Dispatchers.Default) {
        queries.insertSale(
            id = sale.id,
            customer_name = sale.customerName,
            customer_id = sale.customerId,
            date = sale.date,
            total_amount = sale.totalAmount,
            site_id = sale.siteId,
            created_at = sale.createdAt,
            created_by = sale.createdBy
        )
    }

    suspend fun insertSaleItem(item: SaleItem) = withContext(Dispatchers.Default) {
        queries.insertSaleItem(
            id = item.id,
            sale_id = item.saleId,
            product_id = item.productId,
            quantity = item.quantity,
            unit_price = item.unitPrice,
            total_price = item.totalPrice
        )
    }

    suspend fun insertSaleWithItems(sale: Sale, items: List<SaleItem>) = withContext(Dispatchers.Default) {
        database.transaction {
            queries.insertSale(
                id = sale.id,
                customer_name = sale.customerName,
                customer_id = sale.customerId,
                date = sale.date,
                total_amount = sale.totalAmount,
                site_id = sale.siteId,
                created_at = sale.createdAt,
                created_by = sale.createdBy
            )
            items.forEach { item ->
                queries.insertSaleItem(
                    id = item.id,
                    sale_id = item.saleId,
                    product_id = item.productId,
                    quantity = item.quantity,
                    unit_price = item.unitPrice,
                    total_price = item.totalPrice
                )
            }
        }
    }

    fun observeAll(): Flow<List<Sale>> {
        return queries.getAllSales()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toModel() } }
    }

    fun observeBySite(siteId: String): Flow<List<Sale>> {
        return queries.getSalesBySite(siteId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toModel() } }
    }

    private fun com.medistock.shared.db.Sales.toModel(): Sale {
        return Sale(
            id = id,
            customerName = customer_name,
            customerId = customer_id,
            date = date,
            totalAmount = total_amount,
            siteId = site_id,
            createdAt = created_at,
            createdBy = created_by
        )
    }

    private fun com.medistock.shared.db.Sale_items.toModel(): SaleItem {
        return SaleItem(
            id = id,
            saleId = sale_id,
            productId = product_id,
            quantity = quantity,
            unitPrice = unit_price,
            totalPrice = total_price
        )
    }
}
