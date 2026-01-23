package com.medistock.shared

import com.medistock.shared.data.repository.SiteRepository
import com.medistock.shared.data.repository.CategoryRepository
import com.medistock.shared.data.repository.ProductRepository
import com.medistock.shared.data.repository.CustomerRepository
import com.medistock.shared.db.MedistockDatabase
import com.medistock.shared.domain.model.Site
import com.medistock.shared.domain.model.Category
import com.medistock.shared.domain.model.Product
import com.medistock.shared.domain.model.Customer
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlin.test.*

/**
 * Test suite for repository methods related to referential integrity.
 *
 * Tests the getActive(), getAll(), deactivate(), activate(), and observe methods
 * to ensure proper filtering by is_active flag.
 */
class RepositoryReferentialIntegrityTest {

    private lateinit var database: MedistockDatabase
    private lateinit var siteRepository: SiteRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var productRepository: ProductRepository
    private lateinit var customerRepository: CustomerRepository

    @BeforeTest
    fun setUp() {
        // Create in-memory database
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        MedistockDatabase.Schema.create(driver)
        database = MedistockDatabase(driver)

        // Initialize repositories
        siteRepository = SiteRepository(database)
        categoryRepository = CategoryRepository(database)
        productRepository = ProductRepository(database)
        customerRepository = CustomerRepository(database)
    }

    @AfterTest
    fun tearDown() {
        // Database cleanup handled by driver
    }

    // region SiteRepository Tests

    @Test
    fun `should_returnAllSites_when_getAllCalled`() = runTest {
        // Arrange
        val now = Clock.System.now().toEpochMilliseconds()
        val site1 = Site("site-1", "Active Site", isActive = true, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        val site2 = Site("site-2", "Inactive Site", isActive = false, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        siteRepository.insert(site1)
        siteRepository.insert(site2)

        // Act
        val sites = siteRepository.getAll()

        // Assert
        assertEquals(2, sites.size)
        assertTrue(sites.any { it.id == "site-1" })
        assertTrue(sites.any { it.id == "site-2" })
    }

    @Test
    fun `should_returnOnlyActiveSites_when_getActiveCalled`() = runTest {
        // Arrange
        val now = Clock.System.now().toEpochMilliseconds()
        val site1 = Site("site-1", "Active Site", isActive = true, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        val site2 = Site("site-2", "Inactive Site", isActive = false, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        val site3 = Site("site-3", "Another Active", isActive = true, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        siteRepository.insert(site1)
        siteRepository.insert(site2)
        siteRepository.insert(site3)

        // Act
        val activeSites = siteRepository.getActive()

        // Assert
        assertEquals(2, activeSites.size)
        assertTrue(activeSites.all { it.isActive })
        assertFalse(activeSites.any { it.id == "site-2" })
    }

    @Test
    fun `should_deactivateSite_when_deactivateCalled`() = runTest {
        // Arrange
        val now = Clock.System.now().toEpochMilliseconds()
        val site = Site("site-1", "Test Site", isActive = true, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        siteRepository.insert(site)

        // Act
        siteRepository.deactivate("site-1", "testuser")

        // Assert
        val retrieved = siteRepository.getById("site-1")
        assertNotNull(retrieved)
        assertFalse(retrieved.isActive)
        assertEquals("testuser", retrieved.updatedBy)
    }

    @Test
    fun `should_activateSite_when_activateCalled`() = runTest {
        // Arrange
        val now = Clock.System.now().toEpochMilliseconds()
        val site = Site("site-1", "Test Site", isActive = false, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        siteRepository.insert(site)

        // Act
        siteRepository.activate("site-1", "testuser")

        // Assert
        val retrieved = siteRepository.getById("site-1")
        assertNotNull(retrieved)
        assertTrue(retrieved.isActive)
        assertEquals("testuser", retrieved.updatedBy)
    }

    @Test
    fun `should_observeOnlyActiveSites_when_observeActiveCalled`() = runTest {
        // Arrange
        val now = Clock.System.now().toEpochMilliseconds()
        val site1 = Site("site-1", "Active Site", isActive = true, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        val site2 = Site("site-2", "Inactive Site", isActive = false, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        siteRepository.insert(site1)
        siteRepository.insert(site2)

        // Act
        val sites = siteRepository.observeActive().first()

        // Assert
        assertEquals(1, sites.size)
        assertEquals("site-1", sites.first().id)
        assertTrue(sites.first().isActive)
    }

    @Test
    fun `should_upsertSiteWithActiveFlag_when_upsertCalled`() = runTest {
        // Arrange
        val now = Clock.System.now().toEpochMilliseconds()
        val site = Site("site-1", "Test Site", isActive = false, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")

        // Act
        siteRepository.upsert(site)

        // Assert
        val retrieved = siteRepository.getById("site-1")
        assertNotNull(retrieved)
        assertFalse(retrieved.isActive)
    }

    // endregion

    // region CategoryRepository Tests

    @Test
    fun `should_returnAllCategories_when_getAllCalled`() = runTest {
        // Arrange
        val now = Clock.System.now().toEpochMilliseconds()
        val cat1 = Category("cat-1", "Active Cat", isActive = true, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        val cat2 = Category("cat-2", "Inactive Cat", isActive = false, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        categoryRepository.insert(cat1)
        categoryRepository.insert(cat2)

        // Act
        val categories = categoryRepository.getAll()

        // Assert
        assertEquals(2, categories.size)
    }

    @Test
    fun `should_returnOnlyActiveCategories_when_getActiveCalled`() = runTest {
        // Arrange
        val now = Clock.System.now().toEpochMilliseconds()
        val cat1 = Category("cat-1", "Active Cat", isActive = true, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        val cat2 = Category("cat-2", "Inactive Cat", isActive = false, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        categoryRepository.insert(cat1)
        categoryRepository.insert(cat2)

        // Act
        val activeCategories = categoryRepository.getActive()

        // Assert
        assertEquals(1, activeCategories.size)
        assertEquals("cat-1", activeCategories.first().id)
        assertTrue(activeCategories.first().isActive)
    }

    @Test
    fun `should_deactivateCategory_when_deactivateCalled`() = runTest {
        // Arrange
        val now = Clock.System.now().toEpochMilliseconds()
        val category = Category("cat-1", "Test Category", isActive = true, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        categoryRepository.insert(category)

        // Act
        categoryRepository.deactivate("cat-1", "testuser")

        // Assert
        val retrieved = categoryRepository.getById("cat-1")
        assertNotNull(retrieved)
        assertFalse(retrieved.isActive)
        assertEquals("testuser", retrieved.updatedBy)
    }

    @Test
    fun `should_activateCategory_when_activateCalled`() = runTest {
        // Arrange
        val now = Clock.System.now().toEpochMilliseconds()
        val category = Category("cat-1", "Test Category", isActive = false, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        categoryRepository.insert(category)

        // Act
        categoryRepository.activate("cat-1", "testuser")

        // Assert
        val retrieved = categoryRepository.getById("cat-1")
        assertNotNull(retrieved)
        assertTrue(retrieved.isActive)
    }

    @Test
    fun `should_observeOnlyActiveCategories_when_observeActiveCalled`() = runTest {
        // Arrange
        val now = Clock.System.now().toEpochMilliseconds()
        val cat1 = Category("cat-1", "Active Cat", isActive = true, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        val cat2 = Category("cat-2", "Inactive Cat", isActive = false, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        categoryRepository.insert(cat1)
        categoryRepository.insert(cat2)

        // Act
        val categories = categoryRepository.observeActive().first()

        // Assert
        assertEquals(1, categories.size)
        assertTrue(categories.all { it.isActive })
    }

    // endregion

    // region ProductRepository Tests

    @Test
    fun `should_returnAllProducts_when_getAllCalled`() = runTest {
        // Arrange
        val now = Clock.System.now().toEpochMilliseconds()
        val prod1 = Product("prod-1", "Active Product", "unit", 1.0, siteId = "site-1", isActive = true, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        val prod2 = Product("prod-2", "Inactive Product", "unit", 1.0, siteId = "site-1", isActive = false, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        productRepository.insert(prod1)
        productRepository.insert(prod2)

        // Act
        val products = productRepository.getAll()

        // Assert
        assertEquals(2, products.size)
    }

    @Test
    fun `should_returnOnlyActiveProducts_when_getActiveCalled`() = runTest {
        // Arrange
        val now = Clock.System.now().toEpochMilliseconds()
        val prod1 = Product("prod-1", "Active Product", "unit", 1.0, siteId = "site-1", isActive = true, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        val prod2 = Product("prod-2", "Inactive Product", "unit", 1.0, siteId = "site-1", isActive = false, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        productRepository.insert(prod1)
        productRepository.insert(prod2)

        // Act
        val activeProducts = productRepository.getActive()

        // Assert
        assertEquals(1, activeProducts.size)
        assertEquals("prod-1", activeProducts.first().id)
        assertTrue(activeProducts.first().isActive)
    }

    @Test
    fun `should_returnOnlyActiveProductsForSite_when_getActiveBySiteCalled`() = runTest {
        // Arrange
        val now = Clock.System.now().toEpochMilliseconds()
        val prod1 = Product("prod-1", "Active Product Site 1", "unit", 1.0, siteId = "site-1", isActive = true, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        val prod2 = Product("prod-2", "Inactive Product Site 1", "unit", 1.0, siteId = "site-1", isActive = false, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        val prod3 = Product("prod-3", "Active Product Site 2", "unit", 1.0, siteId = "site-2", isActive = true, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        productRepository.insert(prod1)
        productRepository.insert(prod2)
        productRepository.insert(prod3)

        // Act
        val activeProductsSite1 = productRepository.getActiveBySite("site-1")

        // Assert
        assertEquals(1, activeProductsSite1.size)
        assertEquals("prod-1", activeProductsSite1.first().id)
        assertEquals("site-1", activeProductsSite1.first().siteId)
        assertTrue(activeProductsSite1.first().isActive)
    }

    @Test
    fun `should_deactivateProduct_when_deactivateCalled`() = runTest {
        // Arrange
        val now = Clock.System.now().toEpochMilliseconds()
        val product = Product("prod-1", "Test Product", "unit", 1.0, siteId = "site-1", isActive = true, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        productRepository.insert(product)

        // Act
        productRepository.deactivate("prod-1", "testuser")

        // Assert
        val retrieved = productRepository.getById("prod-1")
        assertNotNull(retrieved)
        assertFalse(retrieved.isActive)
        assertEquals("testuser", retrieved.updatedBy)
    }

    @Test
    fun `should_activateProduct_when_activateCalled`() = runTest {
        // Arrange
        val now = Clock.System.now().toEpochMilliseconds()
        val product = Product("prod-1", "Test Product", "unit", 1.0, siteId = "site-1", isActive = false, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        productRepository.insert(product)

        // Act
        productRepository.activate("prod-1", "testuser")

        // Assert
        val retrieved = productRepository.getById("prod-1")
        assertNotNull(retrieved)
        assertTrue(retrieved.isActive)
    }

    @Test
    fun `should_observeOnlyActiveProducts_when_observeActiveCalled`() = runTest {
        // Arrange
        val now = Clock.System.now().toEpochMilliseconds()
        val prod1 = Product("prod-1", "Active Product", "unit", 1.0, siteId = "site-1", isActive = true, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        val prod2 = Product("prod-2", "Inactive Product", "unit", 1.0, siteId = "site-1", isActive = false, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        productRepository.insert(prod1)
        productRepository.insert(prod2)

        // Act
        val products = productRepository.observeActive().first()

        // Assert
        assertEquals(1, products.size)
        assertTrue(products.all { it.isActive })
    }

    @Test
    fun `should_observeOnlyActiveProductsForSite_when_observeActiveBySiteCalled`() = runTest {
        // Arrange
        val now = Clock.System.now().toEpochMilliseconds()
        val prod1 = Product("prod-1", "Active Site 1", "unit", 1.0, siteId = "site-1", isActive = true, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        val prod2 = Product("prod-2", "Inactive Site 1", "unit", 1.0, siteId = "site-1", isActive = false, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        val prod3 = Product("prod-3", "Active Site 2", "unit", 1.0, siteId = "site-2", isActive = true, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        productRepository.insert(prod1)
        productRepository.insert(prod2)
        productRepository.insert(prod3)

        // Act
        val products = productRepository.observeActiveBySite("site-1").first()

        // Assert
        assertEquals(1, products.size)
        assertEquals("prod-1", products.first().id)
        assertTrue(products.all { it.isActive && it.siteId == "site-1" })
    }

    @Test
    fun `should_getBySiteIncludingInactive_when_getBySiteCalled`() = runTest {
        // Arrange
        val now = Clock.System.now().toEpochMilliseconds()
        val prod1 = Product("prod-1", "Active Product", "unit", 1.0, siteId = "site-1", isActive = true, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        val prod2 = Product("prod-2", "Inactive Product", "unit", 1.0, siteId = "site-1", isActive = false, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        productRepository.insert(prod1)
        productRepository.insert(prod2)

        // Act
        val products = productRepository.getBySite("site-1")

        // Assert
        assertEquals(2, products.size)
    }

    // endregion

    // region CustomerRepository Tests

    @Test
    fun `should_returnAllCustomers_when_getAllCalled`() = runTest {
        // Arrange
        val now = Clock.System.now().toEpochMilliseconds()
        val cust1 = Customer("cust-1", "Active Customer", isActive = true, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        val cust2 = Customer("cust-2", "Inactive Customer", isActive = false, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        customerRepository.insert(cust1)
        customerRepository.insert(cust2)

        // Act
        val customers = customerRepository.getAll()

        // Assert
        assertEquals(2, customers.size)
    }

    @Test
    fun `should_returnOnlyActiveCustomers_when_getActiveCalled`() = runTest {
        // Arrange
        val now = Clock.System.now().toEpochMilliseconds()
        val cust1 = Customer("cust-1", "Active Customer", isActive = true, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        val cust2 = Customer("cust-2", "Inactive Customer", isActive = false, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        customerRepository.insert(cust1)
        customerRepository.insert(cust2)

        // Act
        val activeCustomers = customerRepository.getActive()

        // Assert
        assertEquals(1, activeCustomers.size)
        assertEquals("cust-1", activeCustomers.first().id)
        assertTrue(activeCustomers.first().isActive)
    }

    @Test
    fun `should_returnOnlyActiveCustomersForSite_when_getActiveBySiteCalled`() = runTest {
        // Arrange
        val now = Clock.System.now().toEpochMilliseconds()
        val cust1 = Customer("cust-1", "Active Site 1", siteId = "site-1", isActive = true, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        val cust2 = Customer("cust-2", "Inactive Site 1", siteId = "site-1", isActive = false, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        val cust3 = Customer("cust-3", "Active Site 2", siteId = "site-2", isActive = true, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        customerRepository.insert(cust1)
        customerRepository.insert(cust2)
        customerRepository.insert(cust3)

        // Act
        val activeCustomersSite1 = customerRepository.getActiveBySite("site-1")

        // Assert
        assertEquals(1, activeCustomersSite1.size)
        assertEquals("cust-1", activeCustomersSite1.first().id)
        assertEquals("site-1", activeCustomersSite1.first().siteId)
        assertTrue(activeCustomersSite1.first().isActive)
    }

    @Test
    fun `should_deactivateCustomer_when_deactivateCalled`() = runTest {
        // Arrange
        val now = Clock.System.now().toEpochMilliseconds()
        val customer = Customer("cust-1", "Test Customer", isActive = true, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        customerRepository.insert(customer)

        // Act
        customerRepository.deactivate("cust-1", "testuser")

        // Assert
        val retrieved = customerRepository.getById("cust-1")
        assertNotNull(retrieved)
        assertFalse(retrieved.isActive)
        assertEquals("testuser", retrieved.updatedBy)
    }

    @Test
    fun `should_activateCustomer_when_activateCalled`() = runTest {
        // Arrange
        val now = Clock.System.now().toEpochMilliseconds()
        val customer = Customer("cust-1", "Test Customer", isActive = false, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        customerRepository.insert(customer)

        // Act
        customerRepository.activate("cust-1", "testuser")

        // Assert
        val retrieved = customerRepository.getById("cust-1")
        assertNotNull(retrieved)
        assertTrue(retrieved.isActive)
    }

    @Test
    fun `should_observeOnlyActiveCustomers_when_observeActiveCalled`() = runTest {
        // Arrange
        val now = Clock.System.now().toEpochMilliseconds()
        val cust1 = Customer("cust-1", "Active Customer", isActive = true, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        val cust2 = Customer("cust-2", "Inactive Customer", isActive = false, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        customerRepository.insert(cust1)
        customerRepository.insert(cust2)

        // Act
        val customers = customerRepository.observeActive().first()

        // Assert
        assertEquals(1, customers.size)
        assertTrue(customers.all { it.isActive })
    }

    @Test
    fun `should_observeOnlyActiveCustomersForSite_when_observeActiveBySiteCalled`() = runTest {
        // Arrange
        val now = Clock.System.now().toEpochMilliseconds()
        val cust1 = Customer("cust-1", "Active Site 1", siteId = "site-1", isActive = true, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        val cust2 = Customer("cust-2", "Inactive Site 1", siteId = "site-1", isActive = false, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        val cust3 = Customer("cust-3", "Active Site 2", siteId = "site-2", isActive = true, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        customerRepository.insert(cust1)
        customerRepository.insert(cust2)
        customerRepository.insert(cust3)

        // Act
        val customers = customerRepository.observeActiveBySite("site-1").first()

        // Assert
        assertEquals(1, customers.size)
        assertEquals("cust-1", customers.first().id)
        assertTrue(customers.all { it.isActive && it.siteId == "site-1" })
    }

    // endregion

    // region Edge Cases

    @Test
    fun `should_returnEmptyList_when_noActiveEntitiesExist`() = runTest {
        // Arrange - insert only inactive entities
        val now = Clock.System.now().toEpochMilliseconds()
        val site = Site("site-1", "Inactive Site", isActive = false, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        siteRepository.insert(site)

        // Act
        val activeSites = siteRepository.getActive()

        // Assert
        assertEquals(0, activeSites.size)
    }

    @Test
    fun `should_updateIsActiveFlag_when_updateCalled`() = runTest {
        // Arrange
        val now = Clock.System.now().toEpochMilliseconds()
        val site = Site("site-1", "Test Site", isActive = true, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        siteRepository.insert(site)

        // Act - update with isActive = false
        val updatedSite = site.copy(isActive = false, updatedBy = "testuser")
        siteRepository.update(updatedSite)

        // Assert
        val retrieved = siteRepository.getById("site-1")
        assertNotNull(retrieved)
        assertFalse(retrieved.isActive)
    }

    @Test
    fun `should_preserveIsActiveFlag_when_insertingEntity`() = runTest {
        // Arrange
        val now = Clock.System.now().toEpochMilliseconds()
        val inactiveCategory = Category("cat-1", "Inactive Category", isActive = false, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")

        // Act
        categoryRepository.insert(inactiveCategory)

        // Assert
        val retrieved = categoryRepository.getById("cat-1")
        assertNotNull(retrieved)
        assertFalse(retrieved.isActive)
    }

    @Test
    fun `should_handleNullSiteId_when_filteringActiveCustomers`() = runTest {
        // Arrange
        val now = Clock.System.now().toEpochMilliseconds()
        val cust1 = Customer("cust-1", "Customer No Site", siteId = null, isActive = true, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        val cust2 = Customer("cust-2", "Customer With Site", siteId = "site-1", isActive = true, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        customerRepository.insert(cust1)
        customerRepository.insert(cust2)

        // Act
        val activeCustomers = customerRepository.getActive()

        // Assert
        assertEquals(2, activeCustomers.size)
        assertTrue(activeCustomers.any { it.siteId == null })
        assertTrue(activeCustomers.any { it.siteId == "site-1" })
    }

    @Test
    fun `should_reflectChangesImmediately_when_observingActiveEntities`() = runTest {
        // Arrange
        val now = Clock.System.now().toEpochMilliseconds()
        val site = Site("site-1", "Test Site", isActive = true, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        siteRepository.insert(site)

        // Act - get initial observation
        val initialSites = siteRepository.observeActive().first()
        assertEquals(1, initialSites.size)

        // Deactivate the site
        siteRepository.deactivate("site-1", "admin")

        // Get new observation
        val afterDeactivation = siteRepository.observeActive().first()

        // Assert
        assertEquals(0, afterDeactivation.size)
    }

    @Test
    fun `should_handleDefaultIsActiveValue_when_insertingNewEntity`() = runTest {
        // Arrange - use default isActive value (should be true)
        val now = Clock.System.now().toEpochMilliseconds()
        val product = Product(
            id = "prod-1",
            name = "New Product",
            unit = "unit",
            unitVolume = 1.0,
            siteId = "site-1",
            createdAt = now,
            updatedAt = now,
            createdBy = "admin",
            updatedBy = "admin"
            // isActive not specified, defaults to true
        )

        // Act
        productRepository.insert(product)

        // Assert
        val retrieved = productRepository.getById("prod-1")
        assertNotNull(retrieved)
        assertTrue(retrieved.isActive)
    }

    // endregion

    // region Integration Tests

    @Test
    fun `should_excludeDeactivatedEntitiesFromActiveQueries_when_multipleOperations`() = runTest {
        // Arrange
        val now = Clock.System.now().toEpochMilliseconds()
        val site1 = Site("site-1", "Site 1", isActive = true, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        val site2 = Site("site-2", "Site 2", isActive = true, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        val site3 = Site("site-3", "Site 3", isActive = true, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        siteRepository.insert(site1)
        siteRepository.insert(site2)
        siteRepository.insert(site3)

        // Act - deactivate site-2
        siteRepository.deactivate("site-2", "admin")

        // Assert
        val allSites = siteRepository.getAll()
        val activeSites = siteRepository.getActive()

        assertEquals(3, allSites.size)
        assertEquals(2, activeSites.size)
        assertFalse(activeSites.any { it.id == "site-2" })
        assertTrue(allSites.any { it.id == "site-2" && !it.isActive })
    }

    @Test
    fun `should_reappearInActiveQueries_when_entityReactivated`() = runTest {
        // Arrange
        val now = Clock.System.now().toEpochMilliseconds()
        val product = Product("prod-1", "Test Product", "unit", 1.0, siteId = "site-1", isActive = true, createdAt = now, updatedAt = now, createdBy = "admin", updatedBy = "admin")
        productRepository.insert(product)

        // Act - deactivate then reactivate
        productRepository.deactivate("prod-1", "admin")
        val afterDeactivate = productRepository.getActive()

        productRepository.activate("prod-1", "admin")
        val afterReactivate = productRepository.getActive()

        // Assert
        assertEquals(0, afterDeactivate.size)
        assertEquals(1, afterReactivate.size)
        assertEquals("prod-1", afterReactivate.first().id)
    }

    // endregion
}
