package com.medistock.data.dao

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.medistock.data.db.AppDatabase
import com.medistock.data.entities.PurchaseBatch
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
class PurchaseBatchDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var purchaseBatchDao: PurchaseBatchDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        purchaseBatchDao = database.purchaseBatchDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertBatch_insertsCorrectly() = runTest {
        // Given
        val batch = PurchaseBatch(
            id = "batch-1",
            productId = "product-1",
            siteId = "site-1",
            batchNumber = "BATCH001",
            purchaseDate = System.currentTimeMillis(),
            initialQuantity = 100.0,
            remainingQuantity = 100.0,
            purchasePrice = 10.0,
            supplierName = "Supplier A",
            expiryDate = null,
            isExhausted = false
        )

        // When
        purchaseBatchDao.insert(batch)

        // Then
        val result = purchaseBatchDao.getById("batch-1").first()
        assertNotNull(result)
        assertEquals("BATCH001", result!!.batchNumber)
        assertEquals(100.0, result.remainingQuantity, 0.01)
    }

    @Test
    fun updateBatch_updatesCorrectly() = runTest {
        // Given
        val batch = PurchaseBatch(
            id = "batch-1",
            productId = "product-1",
            siteId = "site-1",
            batchNumber = "BATCH001",
            purchaseDate = System.currentTimeMillis(),
            initialQuantity = 100.0,
            remainingQuantity = 100.0,
            purchasePrice = 10.0
        )
        purchaseBatchDao.insert(batch)

        // When
        val updatedBatch = batch.copy(remainingQuantity = 50.0)
        purchaseBatchDao.update(updatedBatch)

        // Then
        val result = purchaseBatchDao.getById("batch-1").first()
        assertNotNull(result)
        assertEquals(50.0, result!!.remainingQuantity, 0.01)
    }

    @Test
    fun getBatchesForProduct_returnsCorrectBatches() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val batch1 = PurchaseBatch(
            id = "batch-1",
            productId = "product-1",
            siteId = "site-1",
            purchaseDate = now - 10000,
            initialQuantity = 100.0,
            remainingQuantity = 100.0,
            purchasePrice = 10.0
        )
        val batch2 = PurchaseBatch(
            id = "batch-2",
            productId = "product-1",
            siteId = "site-1",
            purchaseDate = now,
            initialQuantity = 50.0,
            remainingQuantity = 50.0,
            purchasePrice = 12.0
        )
        val batch3 = PurchaseBatch(
            id = "batch-3",
            productId = "product-2",
            siteId = "site-1",
            purchaseDate = now,
            initialQuantity = 75.0,
            remainingQuantity = 75.0,
            purchasePrice = 8.0
        )

        purchaseBatchDao.insert(batch1)
        purchaseBatchDao.insert(batch2)
        purchaseBatchDao.insert(batch3)

        // When
        val batches = purchaseBatchDao.getBatchesForProduct("product-1", "site-1").first()

        // Then
        assertEquals(2, batches.size)
        // Should be ordered by purchaseDate ASC (oldest first)
        assertEquals("batch-1", batches[0].id)
        assertEquals("batch-2", batches[1].id)
    }

    @Test
    fun getAvailableBatchesFIFO_returnsOnlyAvailableBatches() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val availableBatch1 = PurchaseBatch(
            id = "batch-1",
            productId = "product-1",
            siteId = "site-1",
            purchaseDate = now - 20000,
            initialQuantity = 100.0,
            remainingQuantity = 50.0,
            purchasePrice = 10.0,
            isExhausted = false
        )
        val availableBatch2 = PurchaseBatch(
            id = "batch-2",
            productId = "product-1",
            siteId = "site-1",
            purchaseDate = now - 10000,
            initialQuantity = 100.0,
            remainingQuantity = 100.0,
            purchasePrice = 11.0,
            isExhausted = false
        )
        val exhaustedBatch = PurchaseBatch(
            id = "batch-3",
            productId = "product-1",
            siteId = "site-1",
            purchaseDate = now - 30000,
            initialQuantity = 100.0,
            remainingQuantity = 0.0,
            purchasePrice = 9.0,
            isExhausted = true
        )

        purchaseBatchDao.insert(availableBatch1)
        purchaseBatchDao.insert(availableBatch2)
        purchaseBatchDao.insert(exhaustedBatch)

        // When
        val batches = purchaseBatchDao.getAvailableBatchesFIFO("product-1", "site-1").first()

        // Then
        assertEquals(2, batches.size)
        // Should be ordered by purchaseDate ASC (FIFO - oldest first)
        assertEquals("batch-1", batches[0].id)
        assertEquals("batch-2", batches[1].id)
    }

    @Test
    fun getAvailableBatchesFIFOSync_returnsNonFlowVersion() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val batch1 = PurchaseBatch(
            id = "batch-1",
            productId = "product-1",
            siteId = "site-1",
            purchaseDate = now - 10000,
            initialQuantity = 100.0,
            remainingQuantity = 50.0,
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
            purchasePrice = 11.0,
            isExhausted = false
        )

        purchaseBatchDao.insert(batch1)
        purchaseBatchDao.insert(batch2)

        // When
        val batches = purchaseBatchDao.getAvailableBatchesFIFOSync("product-1", "site-1")

        // Then
        assertEquals(2, batches.size)
        assertEquals("batch-1", batches[0].id)
        assertEquals("batch-2", batches[1].id)
    }

    @Test
    fun getBatchesExpiringSoon_returnsExpiringBatches() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val daysInMillis = 24 * 60 * 60 * 1000L

        val expiringBatch = PurchaseBatch(
            id = "batch-1",
            productId = "product-1",
            siteId = "site-1",
            purchaseDate = now - 10000,
            initialQuantity = 100.0,
            remainingQuantity = 50.0,
            purchasePrice = 10.0,
            expiryDate = now + 5 * daysInMillis, // Expires in 5 days
            isExhausted = false
        )
        val notExpiringBatch = PurchaseBatch(
            id = "batch-2",
            productId = "product-1",
            siteId = "site-1",
            purchaseDate = now,
            initialQuantity = 100.0,
            remainingQuantity = 100.0,
            purchasePrice = 11.0,
            expiryDate = now + 60 * daysInMillis, // Expires in 60 days
            isExhausted = false
        )

        purchaseBatchDao.insert(expiringBatch)
        purchaseBatchDao.insert(notExpiringBatch)

        // When - Get batches expiring within 30 days
        val thresholdDate = now + 30 * daysInMillis
        val batches = purchaseBatchDao.getBatchesExpiringSoon("product-1", "site-1", thresholdDate).first()

        // Then
        assertEquals(1, batches.size)
        assertEquals("batch-1", batches[0].id)
    }

    @Test
    fun getAveragePurchasePrice_calculatesCorrectly() = runTest {
        // Given
        val batch1 = PurchaseBatch(
            id = "batch-1",
            productId = "product-1",
            siteId = "site-1",
            purchaseDate = System.currentTimeMillis(),
            initialQuantity = 100.0,
            remainingQuantity = 50.0,
            purchasePrice = 10.0,
            isExhausted = false
        )
        val batch2 = PurchaseBatch(
            id = "batch-2",
            productId = "product-1",
            siteId = "site-1",
            purchaseDate = System.currentTimeMillis(),
            initialQuantity = 100.0,
            remainingQuantity = 100.0,
            purchasePrice = 20.0,
            isExhausted = false
        )

        purchaseBatchDao.insert(batch1)
        purchaseBatchDao.insert(batch2)

        // When
        val avgPrice = purchaseBatchDao.getAveragePurchasePrice("product-1", "site-1")

        // Then
        assertEquals(15.0, avgPrice!!, 0.01) // (10 + 20) / 2 = 15
    }

    @Test
    fun getTotalRemainingQuantity_sumsCorrectly() = runTest {
        // Given
        val batch1 = PurchaseBatch(
            id = "batch-1",
            productId = "product-1",
            siteId = "site-1",
            purchaseDate = System.currentTimeMillis(),
            initialQuantity = 100.0,
            remainingQuantity = 50.0,
            purchasePrice = 10.0,
            isExhausted = false
        )
        val batch2 = PurchaseBatch(
            id = "batch-2",
            productId = "product-1",
            siteId = "site-1",
            purchaseDate = System.currentTimeMillis(),
            initialQuantity = 100.0,
            remainingQuantity = 75.0,
            purchasePrice = 11.0,
            isExhausted = false
        )
        val exhaustedBatch = PurchaseBatch(
            id = "batch-3",
            productId = "product-1",
            siteId = "site-1",
            purchaseDate = System.currentTimeMillis(),
            initialQuantity = 100.0,
            remainingQuantity = 0.0,
            purchasePrice = 9.0,
            isExhausted = true
        )

        purchaseBatchDao.insert(batch1)
        purchaseBatchDao.insert(batch2)
        purchaseBatchDao.insert(exhaustedBatch)

        // When
        val totalQuantity = purchaseBatchDao.getTotalRemainingQuantity("product-1", "site-1")

        // Then
        assertEquals(125.0, totalQuantity!!, 0.01) // 50 + 75 = 125
    }

    @Test
    fun updateRemainingQuantity_updatesCorrectly() = runTest {
        // Given
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

        // When
        val updatedAt = System.currentTimeMillis()
        purchaseBatchDao.updateRemainingQuantity("batch-1", 30.0, updatedAt)

        // Then
        val result = purchaseBatchDao.getById("batch-1").first()
        assertNotNull(result)
        assertEquals(30.0, result!!.remainingQuantity, 0.01)
        assertFalse(result.isExhausted)
    }

    @Test
    fun updateRemainingQuantity_setsExhaustedWhenZero() = runTest {
        // Given
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

        // When
        val updatedAt = System.currentTimeMillis()
        purchaseBatchDao.updateRemainingQuantity("batch-1", 0.0, updatedAt)

        // Then
        val result = purchaseBatchDao.getById("batch-1").first()
        assertNotNull(result)
        assertEquals(0.0, result!!.remainingQuantity, 0.01)
        assertTrue(result.isExhausted)
    }

    @Test
    fun deleteById_removesBatch() = runTest {
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

        // When
        purchaseBatchDao.deleteById("batch-1")

        // Then
        val result = purchaseBatchDao.getById("batch-1").first()
        assertNull(result)
    }
}
