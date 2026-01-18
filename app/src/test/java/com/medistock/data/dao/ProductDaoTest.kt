package com.medistock.data.dao

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.medistock.data.db.AppDatabase
import com.medistock.data.entities.Category
import com.medistock.data.entities.Product
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
class ProductDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var productDao: ProductDao
    private lateinit var categoryDao: CategoryDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        productDao = database.productDao()
        categoryDao = database.categoryDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertProduct_insertsProductCorrectly() = runTest {
        // Given
        val product = Product(
            id = "test-product-1",
            name = "Paracetamol",
            unit = "comprimé",
            unitVolume = 1.0,
            categoryId = null,
            marginType = "PERCENTAGE",
            marginValue = 20.0,
            siteId = "site-1"
        )

        // When
        productDao.insert(product)

        // Then
        val products = productDao.getAll().first()
        assertEquals(1, products.size)
        assertEquals("Paracetamol", products[0].name)
        assertEquals("comprimé", products[0].unit)
    }

    @Test
    fun getById_returnsCorrectProduct() = runTest {
        // Given
        val product1 = Product(
            id = "product-1",
            name = "Product 1",
            unit = "mg",
            unitVolume = 1.0,
            categoryId = null,
            marginType = null,
            marginValue = null,
            siteId = "site-1"
        )
        val product2 = Product(
            id = "product-2",
            name = "Product 2",
            unit = "ml",
            unitVolume = 1.0,
            categoryId = null,
            marginType = null,
            marginValue = null,
            siteId = "site-1"
        )

        productDao.insert(product1)
        productDao.insert(product2)

        // When
        val result = productDao.getById("product-1").first()

        // Then
        assertNotNull(result)
        assertEquals("Product 1", result?.name)
        assertEquals("product-1", result?.id)
    }

    @Test
    fun updateProduct_updatesCorrectly() = runTest {
        // Given
        val product = Product(
            id = "product-1",
            name = "Old Name",
            unit = "mg",
            unitVolume = 1.0,
            categoryId = null,
            marginType = null,
            marginValue = null,
            siteId = "site-1"
        )
        productDao.insert(product)

        // When
        val updatedProduct = product.copy(name = "New Name")
        productDao.update(updatedProduct)

        // Then
        val result = productDao.getById("product-1").first()
        assertEquals("New Name", result?.name)
    }

    @Test
    fun deleteProduct_removesProduct() = runTest {
        // Given
        val product = Product(
            id = "product-1",
            name = "To Delete",
            unit = "mg",
            unitVolume = 1.0,
            categoryId = null,
            marginType = null,
            marginValue = null,
            siteId = "site-1"
        )
        productDao.insert(product)

        // When
        productDao.delete(product)

        // Then
        val result = productDao.getById("product-1").first()
        assertNull(result)
    }

    @Test
    fun getProductsForSite_filtersCorrectly() = runTest {
        // Given
        val productSite1 = Product(
            id = "product-1",
            name = "Product Site 1",
            unit = "mg",
            unitVolume = 1.0,
            categoryId = null,
            marginType = null,
            marginValue = null,
            siteId = "site-1"
        )
        val productSite2 = Product(
            id = "product-2",
            name = "Product Site 2",
            unit = "mg",
            unitVolume = 1.0,
            categoryId = null,
            marginType = null,
            marginValue = null,
            siteId = "site-2"
        )

        productDao.insert(productSite1)
        productDao.insert(productSite2)

        // When
        val site1Products = productDao.getProductsForSite("site-1").first()

        // Then
        assertEquals(1, site1Products.size)
        assertEquals("Product Site 1", site1Products[0].name)
    }

    @Test
    fun getAllWithCategory_joinsCorrectly() = runTest {
        // Given
        val category = Category(
            id = "cat-1",
            name = "Antibiotics"
        )
        categoryDao.insert(category)

        val product = Product(
            id = "product-1",
            name = "Amoxicillin",
            unit = "mg",
            unitVolume = 1.0,
            categoryId = "cat-1",
            marginType = null,
            marginValue = null,
            siteId = "site-1"
        )
        productDao.insert(product)

        // When
        val productsWithCategory = productDao.getAllWithCategory().first()

        // Then
        assertEquals(1, productsWithCategory.size)
        assertEquals("Amoxicillin", productsWithCategory[0].name)
        assertEquals("Antibiotics", productsWithCategory[0].categoryName)
    }

    @Test
    fun getAllWithCategory_handlesMissingCategory() = runTest {
        // Given
        val product = Product(
            id = "product-1",
            name = "Product Without Category",
            unit = "mg",
            unitVolume = 1.0,
            categoryId = null,
            marginType = null,
            marginValue = null,
            siteId = "site-1"
        )
        productDao.insert(product)

        // When
        val productsWithCategory = productDao.getAllWithCategory().first()

        // Then
        assertEquals(1, productsWithCategory.size)
        assertEquals("Product Without Category", productsWithCategory[0].name)
        assertNull(productsWithCategory[0].categoryName)
    }

    @Test
    fun getProductsWithCategoryForSite_filtersAndJoins() = runTest {
        // Given
        val category1 = Category(
            id = "cat-1",
            name = "Category 1"
        )
        categoryDao.insert(category1)

        val productSite1 = Product(
            id = "product-1",
            name = "Product Site 1",
            unit = "mg",
            unitVolume = 1.0,
            categoryId = "cat-1",
            marginType = null,
            marginValue = null,
            siteId = "site-1"
        )
        val productSite2 = Product(
            id = "product-2",
            name = "Product Site 2",
            unit = "mg",
            unitVolume = 1.0,
            categoryId = null,
            marginType = null,
            marginValue = null,
            siteId = "site-2"
        )

        productDao.insert(productSite1)
        productDao.insert(productSite2)

        // When
        val site1Products = productDao.getProductsWithCategoryForSite("site-1").first()

        // Then
        assertEquals(1, site1Products.size)
        assertEquals("Product Site 1", site1Products[0].name)
        assertEquals("Category 1", site1Products[0].categoryName)
    }

    @Test
    fun insertProduct_withOnConflictReplace_replacesExisting() = runTest {
        // Given
        val product1 = Product(
            id = "product-1",
            name = "Original Name",
            unit = "mg",
            unitVolume = 1.0,
            categoryId = null,
            marginType = null,
            marginValue = null,
            siteId = "site-1"
        )
        productDao.insert(product1)

        // When - Insert with same ID
        val product2 = Product(
            id = "product-1",
            name = "Replaced Name",
            unit = "ml",
            unitVolume = 2.0,
            categoryId = null,
            marginType = null,
            marginValue = null,
            siteId = "site-1"
        )
        productDao.insert(product2)

        // Then
        val products = productDao.getAll().first()
        assertEquals(1, products.size)
        assertEquals("Replaced Name", products[0].name)
        assertEquals("ml", products[0].unit)
    }
}
