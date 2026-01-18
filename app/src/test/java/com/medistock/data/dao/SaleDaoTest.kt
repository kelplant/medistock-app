package com.medistock.data.dao

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.medistock.data.db.AppDatabase
import com.medistock.data.entities.Sale
import com.medistock.data.entities.SaleItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SaleDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var saleDao: SaleDao
    private lateinit var saleItemDao: SaleItemDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        saleDao = database.saleDao()
        saleItemDao = database.saleItemDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertSale_insertsCorrectly() = runTest {
        // Given
        val sale = Sale(
            id = "sale-1",
            customerName = "John Doe",
            customerId = "customer-1",
            date = System.currentTimeMillis(),
            totalAmount = 150.0,
            siteId = "site-1"
        )

        // When
        saleDao.insert(sale)

        // Then
        val result = saleDao.getById("sale-1").first()
        assertNotNull(result)
        assertEquals("John Doe", result?.customerName)
        assertEquals(150.0, result?.totalAmount, 0.01)
    }

    @Test
    fun updateSale_updatesCorrectly() = runTest {
        // Given
        val sale = Sale(
            id = "sale-1",
            customerName = "John Doe",
            customerId = null,
            date = System.currentTimeMillis(),
            totalAmount = 150.0,
            siteId = "site-1"
        )
        saleDao.insert(sale)

        // When
        val updatedSale = sale.copy(totalAmount = 200.0)
        saleDao.update(updatedSale)

        // Then
        val result = saleDao.getById("sale-1").first()
        assertEquals(200.0, result?.totalAmount, 0.01)
    }

    @Test
    fun deleteSale_removesSale() = runTest {
        // Given
        val sale = Sale(
            id = "sale-1",
            customerName = "John Doe",
            customerId = null,
            date = System.currentTimeMillis(),
            totalAmount = 150.0,
            siteId = "site-1"
        )
        saleDao.insert(sale)

        // When
        saleDao.delete(sale)

        // Then
        val result = saleDao.getById("sale-1").first()
        assertNull(result)
    }

    @Test
    fun getAllForSite_filtersCorrectly() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val saleSite1_1 = Sale(
            id = "sale-1",
            customerName = "Customer 1",
            customerId = null,
            date = now - 10000,
            totalAmount = 100.0,
            siteId = "site-1"
        )
        val saleSite1_2 = Sale(
            id = "sale-2",
            customerName = "Customer 2",
            customerId = null,
            date = now,
            totalAmount = 200.0,
            siteId = "site-1"
        )
        val saleSite2 = Sale(
            id = "sale-3",
            customerName = "Customer 3",
            customerId = null,
            date = now,
            totalAmount = 300.0,
            siteId = "site-2"
        )

        saleDao.insert(saleSite1_1)
        saleDao.insert(saleSite1_2)
        saleDao.insert(saleSite2)

        // When
        val sales = saleDao.getAllForSite("site-1").first()

        // Then
        assertEquals(2, sales.size)
        // Should be ordered by date DESC (newest first)
        assertEquals("sale-2", sales[0].id)
        assertEquals("sale-1", sales[1].id)
    }

    @Test
    fun getSaleWithItems_returnsCompleteData() = runTest {
        // Given
        val sale = Sale(
            id = "sale-1",
            customerName = "John Doe",
            customerId = null,
            date = System.currentTimeMillis(),
            totalAmount = 150.0,
            siteId = "site-1"
        )
        saleDao.insert(sale)

        val saleItem1 = SaleItem(
            id = "item-1",
            saleId = "sale-1",
            productId = "product-1",
            productName = "Product 1",
            quantity = 5.0,
            unitPrice = 10.0,
            totalPrice = 50.0,
            siteId = "site-1"
        )
        val saleItem2 = SaleItem(
            id = "item-2",
            saleId = "sale-1",
            productId = "product-2",
            productName = "Product 2",
            quantity = 10.0,
            unitPrice = 10.0,
            totalPrice = 100.0,
            siteId = "site-1"
        )
        saleItemDao.insert(saleItem1)
        saleItemDao.insert(saleItem2)

        // When
        val saleWithItems = saleDao.getSaleWithItems("sale-1").first()

        // Then
        assertNotNull(saleWithItems)
        assertEquals("John Doe", saleWithItems?.sale?.customerName)
        assertEquals(2, saleWithItems?.items?.size)
        assertEquals(150.0, saleWithItems?.sale?.totalAmount, 0.01)
    }

    @Test
    fun getAllWithItemsForSite_returnsAllSalesWithItems() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val sale1 = Sale(
            id = "sale-1",
            customerName = "Customer 1",
            customerId = null,
            date = now - 10000,
            totalAmount = 100.0,
            siteId = "site-1"
        )
        val sale2 = Sale(
            id = "sale-2",
            customerName = "Customer 2",
            customerId = null,
            date = now,
            totalAmount = 200.0,
            siteId = "site-1"
        )
        val sale3 = Sale(
            id = "sale-3",
            customerName = "Customer 3",
            customerId = null,
            date = now,
            totalAmount = 300.0,
            siteId = "site-2"
        )

        saleDao.insert(sale1)
        saleDao.insert(sale2)
        saleDao.insert(sale3)

        val item1 = SaleItem(
            id = "item-1",
            saleId = "sale-1",
            productId = "product-1",
            productName = "Product 1",
            quantity = 10.0,
            unitPrice = 10.0,
            totalPrice = 100.0,
            siteId = "site-1"
        )
        val item2 = SaleItem(
            id = "item-2",
            saleId = "sale-2",
            productId = "product-2",
            productName = "Product 2",
            quantity = 20.0,
            unitPrice = 10.0,
            totalPrice = 200.0,
            siteId = "site-1"
        )
        saleItemDao.insert(item1)
        saleItemDao.insert(item2)

        // When
        val salesWithItems = saleDao.getAllWithItemsForSite("site-1").first()

        // Then
        assertEquals(2, salesWithItems.size)
        // Should be ordered by date DESC (newest first)
        assertEquals("sale-2", salesWithItems[0].sale.id)
        assertEquals("sale-1", salesWithItems[1].sale.id)
        assertEquals(1, salesWithItems[0].items.size)
        assertEquals(1, salesWithItems[1].items.size)
    }

    @Test
    fun deleteById_removesSale() = runTest {
        // Given
        val sale = Sale(
            id = "sale-1",
            customerName = "John Doe",
            customerId = null,
            date = System.currentTimeMillis(),
            totalAmount = 150.0,
            siteId = "site-1"
        )
        saleDao.insert(sale)

        // When
        saleDao.deleteById("sale-1")

        // Then
        val result = saleDao.getById("sale-1").first()
        assertNull(result)
    }

    @Test
    fun getSaleWithItems_returnsNullForNonexistentSale() = runTest {
        // When
        val result = saleDao.getSaleWithItems("nonexistent-id").first()

        // Then
        assertNull(result)
    }
}
