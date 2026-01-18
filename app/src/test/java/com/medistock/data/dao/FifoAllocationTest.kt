package com.medistock.data.dao

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.medistock.data.db.AppDatabase
import com.medistock.data.entities.PurchaseBatch
import com.medistock.data.entities.SaleBatchAllocation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for FIFO (First In First Out) batch allocation logic.
 * Tests the critical business logic for pharmaceutical inventory management.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class FifoAllocationTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var purchaseBatchDao: PurchaseBatchDao
    private lateinit var saleBatchAllocationDao: SaleBatchAllocationDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        purchaseBatchDao = database.purchaseBatchDao()
        saleBatchAllocationDao = database.saleBatchAllocationDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun fifoAllocation_singleBatch_allocatesCorrectly() = runTest {
        // Given - One batch with 100 units
        val batch = PurchaseBatch(
            id = "batch-1",
            productId = "product-1",
            siteId = "site-1",
            purchaseDate = System.currentTimeMillis(),
            initialQuantity = 100.0,
            remainingQuantity = 100.0,
            purchasePrice = 10.0,
            isExhausted = false
        )
        purchaseBatchDao.insert(batch)

        // When - Allocate 30 units for a sale
        val allocation = SaleBatchAllocation(
            id = "alloc-1",
            saleItemId = "sale-item-1",
            batchId = "batch-1",
            quantityAllocated = 30.0,
            purchasePriceAtAllocation = 10.0
        )
        saleBatchAllocationDao.insert(allocation)

        // Update batch remaining quantity
        purchaseBatchDao.updateRemainingQuantity("batch-1", 70.0, System.currentTimeMillis())

        // Then
        val updatedBatch = purchaseBatchDao.getById("batch-1").first()
        assertEquals(70.0, updatedBatch?.remainingQuantity, 0.01)
        assertFalse(updatedBatch?.isExhausted ?: true)
    }

    @Test
    fun fifoAllocation_multipleBatches_allocatesOldestFirst() = runTest {
        // Given - Three batches with different purchase dates
        val now = System.currentTimeMillis()
        val oldestBatch = PurchaseBatch(
            id = "batch-old",
            productId = "product-1",
            siteId = "site-1",
            purchaseDate = now - 30000, // Oldest
            initialQuantity = 50.0,
            remainingQuantity = 50.0,
            purchasePrice = 8.0,
            isExhausted = false
        )
        val middleBatch = PurchaseBatch(
            id = "batch-middle",
            productId = "product-1",
            siteId = "site-1",
            purchaseDate = now - 20000,
            initialQuantity = 100.0,
            remainingQuantity = 100.0,
            purchasePrice = 10.0,
            isExhausted = false
        )
        val newestBatch = PurchaseBatch(
            id = "batch-new",
            productId = "product-1",
            siteId = "site-1",
            purchaseDate = now, // Newest
            initialQuantity = 200.0,
            remainingQuantity = 200.0,
            purchasePrice = 12.0,
            isExhausted = false
        )

        purchaseBatchDao.insert(oldestBatch)
        purchaseBatchDao.insert(middleBatch)
        purchaseBatchDao.insert(newestBatch)

        // When - Get available batches for FIFO
        val availableBatches = purchaseBatchDao.getAvailableBatchesFIFO("product-1", "site-1").first()

        // Then - Should be ordered oldest first
        assertEquals(3, availableBatches.size)
        assertEquals("batch-old", availableBatches[0].id)
        assertEquals("batch-middle", availableBatches[1].id)
        assertEquals("batch-new", availableBatches[2].id)
    }

    @Test
    fun fifoAllocation_exhaustBatch_allocatesMultipleBatches() = runTest {
        // Given - Two batches
        val now = System.currentTimeMillis()
        val batch1 = PurchaseBatch(
            id = "batch-1",
            productId = "product-1",
            siteId = "site-1",
            purchaseDate = now - 10000,
            initialQuantity = 30.0,
            remainingQuantity = 30.0,
            purchasePrice = 10.0,
            isExhausted = false
        )
        val batch2 = PurchaseBatch(
            id = "batch-2",
            productId = "product-1",
            siteId = "site-1",
            purchaseDate = now,
            initialQuantity = 100.0,
            remainingQuantity = 100.0,
            purchasePrice = 12.0,
            isExhausted = false
        )

        purchaseBatchDao.insert(batch1)
        purchaseBatchDao.insert(batch2)

        // When - Allocate 80 units (more than batch1 has)
        // Step 1: Allocate all 30 from batch-1
        val allocation1 = SaleBatchAllocation(
            id = "alloc-1",
            saleItemId = "sale-item-1",
            batchId = "batch-1",
            quantityAllocated = 30.0,
            purchasePriceAtAllocation = 10.0
        )
        saleBatchAllocationDao.insert(allocation1)
        purchaseBatchDao.updateRemainingQuantity("batch-1", 0.0, System.currentTimeMillis())

        // Step 2: Allocate remaining 50 from batch-2
        val allocation2 = SaleBatchAllocation(
            id = "alloc-2",
            saleItemId = "sale-item-1",
            batchId = "batch-2",
            quantityAllocated = 50.0,
            purchasePriceAtAllocation = 12.0
        )
        saleBatchAllocationDao.insert(allocation2)
        purchaseBatchDao.updateRemainingQuantity("batch-2", 50.0, System.currentTimeMillis())

        // Then
        val updatedBatch1 = purchaseBatchDao.getById("batch-1").first()
        assertEquals(0.0, updatedBatch1?.remainingQuantity, 0.01)
        assertTrue(updatedBatch1?.isExhausted ?: false)

        val updatedBatch2 = purchaseBatchDao.getById("batch-2").first()
        assertEquals(50.0, updatedBatch2?.remainingQuantity, 0.01)
        assertFalse(updatedBatch2?.isExhausted ?: true)

        val allocations = saleBatchAllocationDao.getAllocationsForSaleItem("sale-item-1").first()
        assertEquals(2, allocations.size)
    }

    @Test
    fun fifoAllocation_excludesExhaustedBatches() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val exhaustedBatch = PurchaseBatch(
            id = "batch-exhausted",
            productId = "product-1",
            siteId = "site-1",
            purchaseDate = now - 30000,
            initialQuantity = 100.0,
            remainingQuantity = 0.0,
            purchasePrice = 10.0,
            isExhausted = true
        )
        val availableBatch = PurchaseBatch(
            id = "batch-available",
            productId = "product-1",
            siteId = "site-1",
            purchaseDate = now,
            initialQuantity = 100.0,
            remainingQuantity = 100.0,
            purchasePrice = 12.0,
            isExhausted = false
        )

        purchaseBatchDao.insert(exhaustedBatch)
        purchaseBatchDao.insert(availableBatch)

        // When
        val availableBatches = purchaseBatchDao.getAvailableBatchesFIFO("product-1", "site-1").first()

        // Then - Should only return available batch
        assertEquals(1, availableBatches.size)
        assertEquals("batch-available", availableBatches[0].id)
    }

    @Test
    fun fifoAllocation_calculateTotalAllocated() = runTest {
        // Given
        val batch = PurchaseBatch(
            id = "batch-1",
            productId = "product-1",
            siteId = "site-1",
            purchaseDate = System.currentTimeMillis(),
            initialQuantity = 100.0,
            remainingQuantity = 100.0,
            purchasePrice = 10.0
        )
        purchaseBatchDao.insert(batch)

        val allocation1 = SaleBatchAllocation(
            id = "alloc-1",
            saleItemId = "sale-item-1",
            batchId = "batch-1",
            quantityAllocated = 20.0,
            purchasePriceAtAllocation = 10.0
        )
        val allocation2 = SaleBatchAllocation(
            id = "alloc-2",
            saleItemId = "sale-item-2",
            batchId = "batch-1",
            quantityAllocated = 30.0,
            purchasePriceAtAllocation = 10.0
        )

        saleBatchAllocationDao.insert(allocation1)
        saleBatchAllocationDao.insert(allocation2)

        // When
        val totalAllocated = saleBatchAllocationDao.getTotalAllocatedForBatch("batch-1")

        // Then
        assertEquals(50.0, totalAllocated!!, 0.01)
    }

    @Test
    fun fifoAllocation_getTotalRemainingQuantity() = runTest {
        // Given
        val batch1 = PurchaseBatch(
            id = "batch-1",
            productId = "product-1",
            siteId = "site-1",
            purchaseDate = System.currentTimeMillis(),
            initialQuantity = 100.0,
            remainingQuantity = 75.0,
            purchasePrice = 10.0,
            isExhausted = false
        )
        val batch2 = PurchaseBatch(
            id = "batch-2",
            productId = "product-1",
            siteId = "site-1",
            purchaseDate = System.currentTimeMillis(),
            initialQuantity = 50.0,
            remainingQuantity = 50.0,
            purchasePrice = 12.0,
            isExhausted = false
        )
        val exhaustedBatch = PurchaseBatch(
            id = "batch-3",
            productId = "product-1",
            siteId = "site-1",
            purchaseDate = System.currentTimeMillis(),
            initialQuantity = 100.0,
            remainingQuantity = 0.0,
            purchasePrice = 8.0,
            isExhausted = true
        )

        purchaseBatchDao.insert(batch1)
        purchaseBatchDao.insert(batch2)
        purchaseBatchDao.insert(exhaustedBatch)

        // When
        val totalRemaining = purchaseBatchDao.getTotalRemainingQuantity("product-1", "site-1")

        // Then - Should sum only non-exhausted batches
        assertEquals(125.0, totalRemaining!!, 0.01)
    }

    @Test
    fun fifoAllocation_deleteAllForSaleItem() = runTest {
        // Given
        val allocation1 = SaleBatchAllocation(
            id = "alloc-1",
            saleItemId = "sale-item-1",
            batchId = "batch-1",
            quantityAllocated = 20.0,
            purchasePriceAtAllocation = 10.0
        )
        val allocation2 = SaleBatchAllocation(
            id = "alloc-2",
            saleItemId = "sale-item-1",
            batchId = "batch-2",
            quantityAllocated = 30.0,
            purchasePriceAtAllocation = 12.0
        )
        val otherAllocation = SaleBatchAllocation(
            id = "alloc-3",
            saleItemId = "sale-item-2",
            batchId = "batch-1",
            quantityAllocated = 10.0,
            purchasePriceAtAllocation = 10.0
        )

        saleBatchAllocationDao.insert(allocation1)
        saleBatchAllocationDao.insert(allocation2)
        saleBatchAllocationDao.insert(otherAllocation)

        // When
        saleBatchAllocationDao.deleteAllForSaleItem("sale-item-1")

        // Then
        val remainingForItem1 = saleBatchAllocationDao.getAllocationsForSaleItem("sale-item-1").first()
        val remainingForItem2 = saleBatchAllocationDao.getAllocationsForSaleItem("sale-item-2").first()

        assertEquals(0, remainingForItem1.size)
        assertEquals(1, remainingForItem2.size)
    }
}
