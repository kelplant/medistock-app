package com.medistock.shared.data.repository

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.medistock.shared.db.MedistockDatabase
import com.medistock.shared.domain.model.CurrentStock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Unit tests for StockRepository.
 * Tests cover read operations, write operations (insert, upsert, update, delete),
 * stock delta calculations, and multi-product/multi-site scenarios.
 */
@OptIn(ExperimentalUuidApi::class)
class StockRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var database: MedistockDatabase
    private lateinit var repository: StockRepository

    @BeforeTest
    fun setUp() {
        // Create in-memory SQLite database for testing
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        MedistockDatabase.Schema.create(driver)
        database = MedistockDatabase(driver)
        repository = StockRepository(database)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    // ===== Helper Functions =====

    /**
     * Create test site for stock operations.
     */
    private fun createTestSite(
        id: String = "site-1",
        name: String = "Main Warehouse"
    ) {
        database.medistockQueries.insertSite(
            id = id,
            name = name,
            is_active = 1,
            created_at = 0,
            updated_at = 0,
            created_by = "test",
            updated_by = "test"
        )
    }

    /**
     * Create test category for products.
     */
    private fun createTestCategory(
        id: String = "category-1",
        name: String = "Medical Supplies"
    ) {
        database.medistockQueries.insertCategory(
            id = id,
            name = name,
            is_active = 1,
            created_at = 0,
            updated_at = 0,
            created_by = "test",
            updated_by = "test"
        )
    }

    /**
     * Create test packaging type for products.
     */
    private fun createTestPackagingType(
        id: String = "packaging-1",
        name: String = "Box",
        level1Name: String = "Unit",
        level2Name: String? = "Box",
        level2Quantity: Long? = 10
    ) {
        database.medistockQueries.insertPackagingType(
            id = id,
            name = name,
            level1_name = level1Name,
            level2_name = level2Name,
            level2_quantity = level2Quantity,
            default_conversion_factor = null,
            is_active = 1,
            display_order = 0,
            created_at = 0,
            updated_at = 0,
            created_by = "test",
            updated_by = "test"
        )
    }

    /**
     * Create test product for stock operations.
     */
    private fun createTestProduct(
        id: String = "product-1",
        name: String = "Aspirin",
        siteId: String = "site-1",
        categoryId: String = "category-1",
        packagingTypeId: String = "packaging-1",
        minStock: Double = 10.0,
        maxStock: Double = 100.0
    ) {
        database.medistockQueries.insertProduct(
            id = id,
            name = name,
            unit_volume = 1.0,
            packaging_type_id = packagingTypeId,
            selected_level = 1,
            conversion_factor = null,
            category_id = categoryId,
            margin_type = null,
            margin_value = null,
            description = null,
            site_id = siteId,
            min_stock = minStock,
            max_stock = maxStock,
            is_active = 1,
            created_at = 0,
            updated_at = 0,
            created_by = "test",
            updated_by = "test"
        )
    }

    /**
     * Setup basic test data (site, category, packaging, product).
     */
    private fun setupBasicTestData(
        siteId: String = "site-1",
        productId: String = "product-1",
        categoryId: String = "category-1",
        packagingTypeId: String = "packaging-1"
    ) {
        createTestSite(id = siteId)
        createTestCategory(id = categoryId)
        createTestPackagingType(id = packagingTypeId)
        createTestProduct(
            id = productId,
            siteId = siteId,
            categoryId = categoryId,
            packagingTypeId = packagingTypeId
        )
    }

    /**
     * Generate a unique ID for test data.
     */
    private fun generateId(): String = Uuid.random().toString()

    // ===== Read Operations - Empty Database =====

    @Test
    fun `should_returnZero_when_getStockQuantityWithNoStock`() = runTest {
        // Arrange
        setupBasicTestData()

        // Act
        val quantity = repository.getStockQuantity("product-1", "site-1")

        // Assert
        assertEquals(0.0, quantity)
    }

    @Test
    fun `should_returnZero_when_getStockQuantityWithNonExistentProduct`() = runTest {
        // Arrange
        setupBasicTestData()

        // Act
        val quantity = repository.getStockQuantity("non-existent", "site-1")

        // Assert
        assertEquals(0.0, quantity)
    }

    @Test
    fun `should_returnZero_when_getStockQuantityWithNonExistentSite`() = runTest {
        // Arrange
        setupBasicTestData()

        // Act
        val quantity = repository.getStockQuantity("product-1", "non-existent")

        // Assert
        assertEquals(0.0, quantity)
    }

    @Test
    fun `should_returnEmptyList_when_getCurrentStockForSiteWithNoStock`() = runTest {
        // Arrange
        setupBasicTestData()

        // Act
        val stock = repository.getCurrentStockForSite("site-1")

        // Assert
        assertTrue(stock.isEmpty())
    }

    @Test
    fun `should_returnEmptyList_when_getAllCurrentStockWithNoData`() = runTest {
        // Arrange
        setupBasicTestData()

        // Act
        val stock = repository.getAllCurrentStock()

        // Assert
        assertTrue(stock.isEmpty())
    }

    @Test
    fun `should_returnFalse_when_hasStockEntryWithNoStock`() = runTest {
        // Arrange
        setupBasicTestData()

        // Act
        val hasStock = repository.hasStockEntry("product-1", "site-1")

        // Assert
        assertFalse(hasStock)
    }

    // ===== Insert and Read Operations =====

    @Test
    fun `should_insertStock_when_validDataProvided`() = runTest {
        // Arrange
        setupBasicTestData()

        // Act
        repository.insertStock("product-1", "site-1", 50.0)
        val quantity = repository.getStockQuantity("product-1", "site-1")

        // Assert
        assertEquals(50.0, quantity)
    }

    @Test
    fun `should_insertStockWithZeroQuantity_when_requested`() = runTest {
        // Arrange
        setupBasicTestData()

        // Act
        repository.insertStock("product-1", "site-1", 0.0)
        val quantity = repository.getStockQuantity("product-1", "site-1")

        // Assert
        assertEquals(0.0, quantity)
    }

    @Test
    fun `should_insertStockWithNegativeQuantity_when_allowed`() = runTest {
        // Arrange
        setupBasicTestData()

        // Act
        repository.insertStock("product-1", "site-1", -10.0)
        val quantity = repository.getStockQuantity("product-1", "site-1")

        // Assert
        assertEquals(-10.0, quantity)
    }

    @Test
    fun `should_insertStockWithMovementReference_when_provided`() = runTest {
        // Arrange
        setupBasicTestData()
        val movementId = "movement-1"

        // Act
        repository.insertStock("product-1", "site-1", 50.0, movementId)

        // Assert
        // Verify insertion succeeded by checking quantity
        val quantity = repository.getStockQuantity("product-1", "site-1")
        assertEquals(50.0, quantity)
    }

    @Test
    fun `should_returnTrue_when_hasStockEntryAfterInsert`() = runTest {
        // Arrange
        setupBasicTestData()

        // Act
        repository.insertStock("product-1", "site-1", 50.0)
        val hasStock = repository.hasStockEntry("product-1", "site-1")

        // Assert
        assertTrue(hasStock)
    }

    @Test
    fun `should_returnCurrentStock_when_getCurrentStockForSiteAfterInsert`() = runTest {
        // Arrange
        setupBasicTestData()

        // Act
        repository.insertStock("product-1", "site-1", 50.0)
        val stockList = repository.getCurrentStockForSite("site-1")

        // Assert
        assertEquals(1, stockList.size)
        val stock = stockList.first()
        assertEquals("product-1", stock.productId)
        assertEquals("site-1", stock.siteId)
        assertEquals(50.0, stock.quantityOnHand)
        assertEquals("Aspirin", stock.productName)
        assertEquals("Main Warehouse", stock.siteName)
        assertEquals("Medical Supplies", stock.categoryName)
        assertEquals(10.0, stock.minStock)
        assertEquals(100.0, stock.maxStock)
    }

    @Test
    fun `should_returnCurrentStock_when_getAllCurrentStockAfterInsert`() = runTest {
        // Arrange
        setupBasicTestData()

        // Act
        repository.insertStock("product-1", "site-1", 50.0)
        val stockList = repository.getAllCurrentStock()

        // Assert
        assertEquals(1, stockList.size)
        val stock = stockList.first()
        assertEquals("product-1", stock.productId)
        assertEquals("site-1", stock.siteId)
        assertEquals(50.0, stock.quantityOnHand)
    }

    // ===== Upsert Operations =====

    @Test
    fun `should_insertNewStock_when_upsertWithNonExistentEntry`() = runTest {
        // Arrange
        setupBasicTestData()

        // Act
        repository.upsertStock("product-1", "site-1", 75.0)
        val quantity = repository.getStockQuantity("product-1", "site-1")

        // Assert
        assertEquals(75.0, quantity)
    }

    @Test
    fun `should_updateStock_when_upsertWithExistingEntry`() = runTest {
        // Arrange
        setupBasicTestData()
        repository.insertStock("product-1", "site-1", 50.0)

        // Act
        repository.upsertStock("product-1", "site-1", 100.0)
        val quantity = repository.getStockQuantity("product-1", "site-1")

        // Assert
        assertEquals(100.0, quantity)
    }

    @Test
    fun `should_replaceQuantity_when_upsertMultipleTimes`() = runTest {
        // Arrange
        setupBasicTestData()

        // Act
        repository.upsertStock("product-1", "site-1", 10.0)
        repository.upsertStock("product-1", "site-1", 20.0)
        repository.upsertStock("product-1", "site-1", 30.0)
        val quantity = repository.getStockQuantity("product-1", "site-1")

        // Assert
        assertEquals(30.0, quantity)
    }

    // ===== Update Delta Operations =====

    @Test
    fun `should_addDelta_when_updateStockDeltaWithPositiveValue`() = runTest {
        // Arrange
        setupBasicTestData()
        repository.insertStock("product-1", "site-1", 50.0)

        // Act
        val success = repository.updateStockDelta("product-1", "site-1", 25.0)
        val quantity = repository.getStockQuantity("product-1", "site-1")

        // Assert
        assertTrue(success)
        assertEquals(75.0, quantity)
    }

    @Test
    fun `should_subtractDelta_when_updateStockDeltaWithNegativeValue`() = runTest {
        // Arrange
        setupBasicTestData()
        repository.insertStock("product-1", "site-1", 50.0)

        // Act
        val success = repository.updateStockDelta("product-1", "site-1", -15.0)
        val quantity = repository.getStockQuantity("product-1", "site-1")

        // Assert
        assertTrue(success)
        assertEquals(35.0, quantity)
    }

    @Test
    fun `should_allowNegativeStock_when_updateStockDeltaExceedsQuantity`() = runTest {
        // Arrange
        setupBasicTestData()
        repository.insertStock("product-1", "site-1", 10.0)

        // Act
        val success = repository.updateStockDelta("product-1", "site-1", -20.0)
        val quantity = repository.getStockQuantity("product-1", "site-1")

        // Assert
        assertTrue(success)
        assertEquals(-10.0, quantity)
    }

    @Test
    fun `should_createEntryWithDelta_when_updateStockDeltaWithNoExistingEntry`() = runTest {
        // Arrange
        setupBasicTestData()

        // Act
        val success = repository.updateStockDelta("product-1", "site-1", 30.0)
        val quantity = repository.getStockQuantity("product-1", "site-1")

        // Assert
        assertTrue(success)
        assertEquals(30.0, quantity)
    }

    @Test
    fun `should_applyMultipleDeltas_when_calledSequentially`() = runTest {
        // Arrange
        setupBasicTestData()
        repository.insertStock("product-1", "site-1", 100.0)

        // Act
        repository.updateStockDelta("product-1", "site-1", 20.0)  // +20 = 120
        repository.updateStockDelta("product-1", "site-1", -30.0) // -30 = 90
        repository.updateStockDelta("product-1", "site-1", 10.0)  // +10 = 100
        val quantity = repository.getStockQuantity("product-1", "site-1")

        // Assert
        assertEquals(100.0, quantity)
    }

    @Test
    fun `should_handleZeroDelta_when_updateStockDelta`() = runTest {
        // Arrange
        setupBasicTestData()
        repository.insertStock("product-1", "site-1", 50.0)

        // Act
        val success = repository.updateStockDelta("product-1", "site-1", 0.0)
        val quantity = repository.getStockQuantity("product-1", "site-1")

        // Assert
        assertTrue(success)
        assertEquals(50.0, quantity)
    }

    // ===== Set Quantity Operations =====

    @Test
    fun `should_setAbsoluteQuantity_when_setStockQuantityWithExistingEntry`() = runTest {
        // Arrange
        setupBasicTestData()
        repository.insertStock("product-1", "site-1", 50.0)

        // Act
        val success = repository.setStockQuantity("product-1", "site-1", 200.0)
        val quantity = repository.getStockQuantity("product-1", "site-1")

        // Assert
        assertTrue(success)
        assertEquals(200.0, quantity)
    }

    @Test
    fun `should_returnFalse_when_setStockQuantityWithNoExistingEntry`() = runTest {
        // Arrange
        setupBasicTestData()

        // Act
        val success = repository.setStockQuantity("product-1", "site-1", 100.0)
        val quantity = repository.getStockQuantity("product-1", "site-1")

        // Assert
        assertFalse(success)
        assertEquals(0.0, quantity)
    }

    @Test
    fun `should_setZeroQuantity_when_setStockQuantityToZero`() = runTest {
        // Arrange
        setupBasicTestData()
        repository.insertStock("product-1", "site-1", 50.0)

        // Act
        val success = repository.setStockQuantity("product-1", "site-1", 0.0)
        val quantity = repository.getStockQuantity("product-1", "site-1")

        // Assert
        assertTrue(success)
        assertEquals(0.0, quantity)
    }

    @Test
    fun `should_setNegativeQuantity_when_inventoryAdjustment`() = runTest {
        // Arrange
        setupBasicTestData()
        repository.insertStock("product-1", "site-1", 50.0)

        // Act
        val success = repository.setStockQuantity("product-1", "site-1", -5.0)
        val quantity = repository.getStockQuantity("product-1", "site-1")

        // Assert
        assertTrue(success)
        assertEquals(-5.0, quantity)
    }

    // ===== Delete Operations =====

    @Test
    fun `should_deleteStock_when_entryExists`() = runTest {
        // Arrange
        setupBasicTestData()
        repository.insertStock("product-1", "site-1", 50.0)

        // Act
        repository.deleteStock("product-1", "site-1")
        val quantity = repository.getStockQuantity("product-1", "site-1")
        val hasStock = repository.hasStockEntry("product-1", "site-1")

        // Assert
        assertEquals(0.0, quantity)
        assertFalse(hasStock)
    }

    @Test
    fun `should_doNothing_when_deleteStockWithNoExistingEntry`() = runTest {
        // Arrange
        setupBasicTestData()

        // Act
        repository.deleteStock("product-1", "site-1")
        val quantity = repository.getStockQuantity("product-1", "site-1")

        // Assert
        assertEquals(0.0, quantity)
    }

    @Test
    fun `should_notAffectOtherStock_when_deleteOneEntry`() = runTest {
        // Arrange
        setupBasicTestData()
        createTestProduct(id = "product-2", name = "Ibuprofen")
        repository.insertStock("product-1", "site-1", 50.0)
        repository.insertStock("product-2", "site-1", 100.0)

        // Act
        repository.deleteStock("product-1", "site-1")
        val quantity1 = repository.getStockQuantity("product-1", "site-1")
        val quantity2 = repository.getStockQuantity("product-2", "site-1")

        // Assert
        assertEquals(0.0, quantity1)
        assertEquals(100.0, quantity2)
    }

    // ===== Ensure Stock Entry Operations =====

    @Test
    fun `should_createEntryWithZero_when_ensureStockEntryWithNoExistingEntry`() = runTest {
        // Arrange
        setupBasicTestData()

        // Act
        repository.ensureStockEntry("product-1", "site-1")
        val quantity = repository.getStockQuantity("product-1", "site-1")
        val hasStock = repository.hasStockEntry("product-1", "site-1")

        // Assert
        assertTrue(hasStock)
        assertEquals(0.0, quantity)
    }

    @Test
    fun `should_notModifyQuantity_when_ensureStockEntryWithExistingEntry`() = runTest {
        // Arrange
        setupBasicTestData()
        repository.insertStock("product-1", "site-1", 75.0)

        // Act
        repository.ensureStockEntry("product-1", "site-1")
        val quantity = repository.getStockQuantity("product-1", "site-1")

        // Assert
        assertEquals(75.0, quantity)
    }

    @Test
    fun `should_createMultipleEntries_when_ensureStockEntryCalledMultipleTimes`() = runTest {
        // Arrange
        setupBasicTestData()

        // Act
        repository.ensureStockEntry("product-1", "site-1")
        repository.ensureStockEntry("product-1", "site-1")
        repository.ensureStockEntry("product-1", "site-1")
        val quantity = repository.getStockQuantity("product-1", "site-1")

        // Assert
        assertEquals(0.0, quantity)
    }

    // ===== Multiple Products and Sites =====

    @Test
    fun `should_trackStockSeparately_when_multipleProductsAtSameSite`() = runTest {
        // Arrange
        createTestSite(id = "site-1")
        createTestCategory()
        createTestPackagingType()
        createTestProduct(id = "product-1", name = "Aspirin")
        createTestProduct(id = "product-2", name = "Ibuprofen")

        // Act
        repository.insertStock("product-1", "site-1", 50.0)
        repository.insertStock("product-2", "site-1", 100.0)

        val quantity1 = repository.getStockQuantity("product-1", "site-1")
        val quantity2 = repository.getStockQuantity("product-2", "site-1")

        // Assert
        assertEquals(50.0, quantity1)
        assertEquals(100.0, quantity2)
    }

    @Test
    fun `should_trackStockSeparately_when_sameProductAtMultipleSites`() = runTest {
        // Arrange
        createTestSite(id = "site-1", name = "Main Warehouse")
        createTestSite(id = "site-2", name = "Branch Store")
        createTestCategory()
        createTestPackagingType()
        createTestProduct(id = "product-1", siteId = "site-1")
        createTestProduct(id = "product-2", name = "Aspirin Copy", siteId = "site-2")

        // Act
        repository.insertStock("product-1", "site-1", 50.0)
        repository.insertStock("product-2", "site-2", 75.0)

        val quantity1 = repository.getStockQuantity("product-1", "site-1")
        val quantity2 = repository.getStockQuantity("product-2", "site-2")

        // Assert
        assertEquals(50.0, quantity1)
        assertEquals(75.0, quantity2)
    }

    @Test
    fun `should_returnAllStock_when_getCurrentStockForSiteWithMultipleProducts`() = runTest {
        // Arrange
        createTestSite(id = "site-1")
        createTestCategory()
        createTestPackagingType()
        createTestProduct(id = "product-1", name = "Aspirin")
        createTestProduct(id = "product-2", name = "Ibuprofen")
        createTestProduct(id = "product-3", name = "Paracetamol")

        repository.insertStock("product-1", "site-1", 50.0)
        repository.insertStock("product-2", "site-1", 100.0)
        repository.insertStock("product-3", "site-1", 75.0)

        // Act
        val stockList = repository.getCurrentStockForSite("site-1")

        // Assert
        assertEquals(3, stockList.size)
        assertTrue(stockList.any { it.productId == "product-1" && it.quantityOnHand == 50.0 })
        assertTrue(stockList.any { it.productId == "product-2" && it.quantityOnHand == 100.0 })
        assertTrue(stockList.any { it.productId == "product-3" && it.quantityOnHand == 75.0 })
    }

    @Test
    fun `should_returnAllStock_when_getAllCurrentStockWithMultipleSites`() = runTest {
        // Arrange
        createTestSite(id = "site-1", name = "Main Warehouse")
        createTestSite(id = "site-2", name = "Branch Store")
        createTestCategory()
        createTestPackagingType()
        createTestProduct(id = "product-1", siteId = "site-1")
        createTestProduct(id = "product-2", siteId = "site-2")

        repository.insertStock("product-1", "site-1", 50.0)
        repository.insertStock("product-2", "site-2", 100.0)

        // Act
        val stockList = repository.getAllCurrentStock()

        // Assert
        assertEquals(2, stockList.size)
        assertTrue(stockList.any { it.siteId == "site-1" && it.quantityOnHand == 50.0 })
        assertTrue(stockList.any { it.siteId == "site-2" && it.quantityOnHand == 100.0 })
    }

    @Test
    fun `should_filterBySite_when_getCurrentStockForSiteWithMultipleSites`() = runTest {
        // Arrange
        createTestSite(id = "site-1", name = "Main Warehouse")
        createTestSite(id = "site-2", name = "Branch Store")
        createTestCategory()
        createTestPackagingType()
        createTestProduct(id = "product-1", siteId = "site-1")
        createTestProduct(id = "product-2", siteId = "site-2")
        createTestProduct(id = "product-3", name = "Aspirin Copy", siteId = "site-1")

        repository.insertStock("product-1", "site-1", 50.0)
        repository.insertStock("product-2", "site-2", 100.0)
        repository.insertStock("product-3", "site-1", 75.0)

        // Act
        val site1Stock = repository.getCurrentStockForSite("site-1")
        val site2Stock = repository.getCurrentStockForSite("site-2")

        // Assert
        assertEquals(2, site1Stock.size)
        assertEquals(1, site2Stock.size)
        assertTrue(site1Stock.all { it.siteId == "site-1" })
        assertTrue(site2Stock.all { it.siteId == "site-2" })
    }

    // ===== Flow Observer Tests =====

    @Test
    fun `should_emitStockList_when_observeCurrentStockForSite`() = runTest {
        // Arrange
        setupBasicTestData()
        repository.insertStock("product-1", "site-1", 50.0)

        // Act
        val stockList = repository.observeCurrentStockForSite("site-1").first()

        // Assert
        assertEquals(1, stockList.size)
        assertEquals("product-1", stockList.first().productId)
        assertEquals(50.0, stockList.first().quantityOnHand)
    }

    @Test
    fun `should_emitEmptyList_when_observeCurrentStockForSiteWithNoData`() = runTest {
        // Arrange
        setupBasicTestData()

        // Act
        val stockList = repository.observeCurrentStockForSite("site-1").first()

        // Assert
        assertTrue(stockList.isEmpty())
    }

    @Test
    fun `should_emitAllStock_when_observeAllCurrentStock`() = runTest {
        // Arrange
        createTestSite(id = "site-1")
        createTestSite(id = "site-2")
        createTestCategory()
        createTestPackagingType()
        createTestProduct(id = "product-1", siteId = "site-1")
        createTestProduct(id = "product-2", siteId = "site-2")

        repository.insertStock("product-1", "site-1", 50.0)
        repository.insertStock("product-2", "site-2", 100.0)

        // Act
        val stockList = repository.observeAllCurrentStock().first()

        // Assert
        assertEquals(2, stockList.size)
        assertTrue(stockList.any { it.productId == "product-1" })
        assertTrue(stockList.any { it.productId == "product-2" })
    }

    @Test
    fun `should_emitUpdates_when_observeCurrentStockAndDataChanges`() = runTest {
        // Arrange
        setupBasicTestData()
        repository.insertStock("product-1", "site-1", 50.0)

        // Act
        val initial = repository.observeCurrentStockForSite("site-1").first()
        repository.updateStockDelta("product-1", "site-1", 25.0)
        val updated = repository.observeCurrentStockForSite("site-1").first()

        // Assert
        assertEquals(50.0, initial.first().quantityOnHand)
        assertEquals(75.0, updated.first().quantityOnHand)
    }

    // ===== Edge Cases =====

    @Test
    fun `should_handleVeryLargeQuantity_when_insertingStock`() = runTest {
        // Arrange
        setupBasicTestData()
        val largeQuantity = 999999999.99

        // Act
        repository.insertStock("product-1", "site-1", largeQuantity)
        val quantity = repository.getStockQuantity("product-1", "site-1")

        // Assert
        assertEquals(largeQuantity, quantity)
    }

    @Test
    fun `should_handleVerySmallQuantity_when_insertingStock`() = runTest {
        // Arrange
        setupBasicTestData()
        val smallQuantity = 0.01

        // Act
        repository.insertStock("product-1", "site-1", smallQuantity)
        val quantity = repository.getStockQuantity("product-1", "site-1")

        // Assert
        assertEquals(smallQuantity, quantity)
    }

    @Test
    fun `should_handleDecimalPrecision_when_updatingStockDelta`() = runTest {
        // Arrange
        setupBasicTestData()
        repository.insertStock("product-1", "site-1", 10.5)

        // Act
        repository.updateStockDelta("product-1", "site-1", 0.3)
        val quantity = repository.getStockQuantity("product-1", "site-1")

        // Assert
        assertEquals(10.8, quantity, 0.001)
    }

    @Test
    fun `should_maintainStockAccuracy_when_multipleSequentialOperations`() = runTest {
        // Arrange
        setupBasicTestData()

        // Act - Simulate complex stock movements
        repository.insertStock("product-1", "site-1", 100.0)              // 100
        repository.updateStockDelta("product-1", "site-1", 50.0)          // 150
        repository.updateStockDelta("product-1", "site-1", -30.0)         // 120
        repository.setStockQuantity("product-1", "site-1", 80.0)          // 80 (inventory adjustment)
        repository.updateStockDelta("product-1", "site-1", 20.0)          // 100

        val quantity = repository.getStockQuantity("product-1", "site-1")

        // Assert
        assertEquals(100.0, quantity)
    }

    @Test
    fun `should_returnCorrectDetails_when_getCurrentStockByProductAndSite`() = runTest {
        // Arrange
        setupBasicTestData()
        repository.insertStock("product-1", "site-1", 50.0)

        // Act
        val stock = repository.getCurrentStockByProductAndSite("product-1", "site-1")

        // Assert
        assertNotNull(stock)
        assertEquals("product-1", stock.productId)
        assertEquals("site-1", stock.siteId)
        assertEquals("Aspirin", stock.productName)
        assertEquals("Main Warehouse", stock.siteName)
        assertEquals("Medical Supplies", stock.categoryName)
        assertEquals(50.0, stock.quantityOnHand)
        assertEquals(10.0, stock.minStock)
        assertEquals(100.0, stock.maxStock)
    }

    @Test
    fun `should_returnNull_when_getCurrentStockByProductAndSiteWithNoEntry`() = runTest {
        // Arrange
        setupBasicTestData()

        // Act
        val stock = repository.getCurrentStockByProductAndSite("product-1", "site-1")

        // Assert
        assertNull(stock)
    }

    @Test
    fun `should_handleMultipleProductsWithDifferentCategories_when_tracking`() = runTest {
        // Arrange
        createTestSite(id = "site-1")
        createTestCategory(id = "category-1", name = "Medical")
        createTestCategory(id = "category-2", name = "Surgical")
        createTestPackagingType()
        createTestProduct(id = "product-1", name = "Aspirin", categoryId = "category-1")
        createTestProduct(id = "product-2", name = "Scalpel", categoryId = "category-2")

        repository.insertStock("product-1", "site-1", 50.0)
        repository.insertStock("product-2", "site-1", 25.0)

        // Act
        val stockList = repository.getCurrentStockForSite("site-1")

        // Assert
        assertEquals(2, stockList.size)
        val aspirin = stockList.find { it.productId == "product-1" }
        val scalpel = stockList.find { it.productId == "product-2" }

        assertNotNull(aspirin)
        assertEquals("Medical", aspirin.categoryName)
        assertEquals(50.0, aspirin.quantityOnHand)

        assertNotNull(scalpel)
        assertEquals("Surgical", scalpel.categoryName)
        assertEquals(25.0, scalpel.quantityOnHand)
    }

    // ===== Integration Scenario =====

    @Test
    fun `should_workCorrectly_when_completeStockLifecycle`() = runTest {
        // Arrange - Setup product
        setupBasicTestData()

        // Act & Assert - Initial stock entry (receiving goods)
        repository.insertStock("product-1", "site-1", 100.0)
        assertEquals(100.0, repository.getStockQuantity("product-1", "site-1"))

        // Act & Assert - Sale (outbound movement)
        repository.updateStockDelta("product-1", "site-1", -20.0)
        assertEquals(80.0, repository.getStockQuantity("product-1", "site-1"))

        // Act & Assert - Restocking (inbound movement)
        repository.updateStockDelta("product-1", "site-1", 50.0)
        assertEquals(130.0, repository.getStockQuantity("product-1", "site-1"))

        // Act & Assert - Inventory adjustment (correction)
        repository.setStockQuantity("product-1", "site-1", 125.0)
        assertEquals(125.0, repository.getStockQuantity("product-1", "site-1"))

        // Act & Assert - More sales
        repository.updateStockDelta("product-1", "site-1", -75.0)
        assertEquals(50.0, repository.getStockQuantity("product-1", "site-1"))

        // Act & Assert - Product discontinued (remove stock)
        repository.deleteStock("product-1", "site-1")
        assertEquals(0.0, repository.getStockQuantity("product-1", "site-1"))
        assertFalse(repository.hasStockEntry("product-1", "site-1"))
    }

    @Test
    fun `should_maintainIntegrity_when_multiProductMultiSiteOperations`() = runTest {
        // Arrange - Create complex scenario
        createTestSite(id = "site-1", name = "Main Warehouse")
        createTestSite(id = "site-2", name = "Branch Store")
        createTestCategory()
        createTestPackagingType()
        createTestProduct(id = "product-1", name = "Aspirin", siteId = "site-1")
        createTestProduct(id = "product-2", name = "Aspirin Branch", siteId = "site-2")
        createTestProduct(id = "product-3", name = "Ibuprofen", siteId = "site-1")

        // Act - Initialize stock at all locations
        repository.insertStock("product-1", "site-1", 100.0)
        repository.insertStock("product-2", "site-2", 50.0)
        repository.insertStock("product-3", "site-1", 75.0)

        // Act - Simulate business operations
        repository.updateStockDelta("product-1", "site-1", -20.0)  // Sale at main
        repository.updateStockDelta("product-2", "site-2", -10.0)  // Sale at branch
        repository.updateStockDelta("product-3", "site-1", 25.0)   // Restock main

        // Assert - Verify each stock separately
        assertEquals(80.0, repository.getStockQuantity("product-1", "site-1"))
        assertEquals(40.0, repository.getStockQuantity("product-2", "site-2"))
        assertEquals(100.0, repository.getStockQuantity("product-3", "site-1"))

        // Assert - Verify site totals
        val site1Stock = repository.getCurrentStockForSite("site-1")
        val site2Stock = repository.getCurrentStockForSite("site-2")

        assertEquals(2, site1Stock.size)
        assertEquals(1, site2Stock.size)

        // Assert - Verify all stock
        val allStock = repository.getAllCurrentStock()
        assertEquals(3, allStock.size)
    }
}
