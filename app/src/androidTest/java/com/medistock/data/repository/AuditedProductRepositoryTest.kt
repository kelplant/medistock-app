package com.medistock.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.medistock.data.db.AppDatabase
import com.medistock.data.entities.Product
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class AuditedProductRepositoryTest {

    private lateinit var repository: AuditedProductRepository
    private lateinit var context: Context
    private lateinit var database: AppDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repository = AuditedProductRepository(context)
        database = AppDatabase.getInstance(context)

        // Clear database
        database.clearAllTables()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insert_createsProductAndLogsAudit() = runTest {
        // Given
        val product = Product(
            name = "Test Product",
            unit = "mg",
            unitVolume = 1.0,
            categoryId = null,
            marginType = null,
            marginValue = null,
            siteId = "site-1"
        )

        // When
        val productId = repository.insert(product)

        // Then
        assertNotNull(productId)
        val retrievedProduct = repository.getById(productId).first()
        assertNotNull(retrievedProduct)
        assertEquals("Test Product", retrievedProduct?.name)
    }

    @Test
    fun update_updatesProductAndLogsChanges() = runTest {
        // Given
        val oldProduct = Product(
            id = "product-1",
            name = "Old Name",
            unit = "mg",
            unitVolume = 1.0,
            categoryId = null,
            marginType = null,
            marginValue = null,
            siteId = "site-1"
        )
        repository.insert(oldProduct)

        val newProduct = oldProduct.copy(name = "New Name")

        // When
        repository.update(oldProduct, newProduct)

        // Then
        val retrievedProduct = repository.getById("product-1").first()
        assertEquals("New Name", retrievedProduct?.name)
    }

    @Test
    fun delete_removesProductAndLogsAudit() = runTest {
        // Given
        val product = Product(
            name = "To Delete",
            unit = "mg",
            unitVolume = 1.0,
            categoryId = null,
            marginType = null,
            marginValue = null,
            siteId = "site-1"
        )
        repository.insert(product)

        // When
        repository.delete(product)

        // Then
        val retrievedProduct = repository.getById(product.id).first()
        assertNull(retrievedProduct)
    }

    @Test
    fun getProductsForSite_filtersCorrectly() = runTest {
        // Given
        val product1 = Product(
            name = "Product Site 1",
            unit = "mg",
            unitVolume = 1.0,
            categoryId = null,
            marginType = null,
            marginValue = null,
            siteId = "site-1"
        )
        val product2 = Product(
            name = "Product Site 2",
            unit = "mg",
            unitVolume = 1.0,
            categoryId = null,
            marginType = null,
            marginValue = null,
            siteId = "site-2"
        )
        repository.insert(product1)
        repository.insert(product2)

        // When
        val products = repository.getProductsForSite("site-1").first()

        // Then
        assertEquals(1, products.size)
        assertEquals("Product Site 1", products[0].name)
    }
}
