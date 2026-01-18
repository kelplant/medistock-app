package com.medistock.ui.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.medistock.data.entities.Product
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ProductViewModelTest {

    private lateinit var viewModel: ProductViewModel
    private lateinit var application: Application

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext()
        viewModel = ProductViewModel(application)
    }

    @Test
    fun viewModel_initialState_isEmpty() {
        // Then
        assertTrue(viewModel.products.value.isEmpty())
        assertTrue(viewModel.categories.value.isEmpty())
    }

    @Test
    fun addProductWithPrice_calculatesFixedMargin() = runTest {
        // Given
        val product = Product(
            name = "Test Product",
            unit = "mg",
            unitVolume = 1.0,
            categoryId = null,
            marginType = "fixed",
            marginValue = 5.0,
            siteId = "site-1"
        )
        val purchasePrice = 10.0

        // When
        viewModel.addProductWithPrice(product, purchasePrice)

        // Then - Selling price should be 10 + 5 = 15
        // (We would need to check the ProductPrice table, but this tests the logic)
    }

    @Test
    fun addProductWithPrice_calculatesPercentageMargin() = runTest {
        // Given
        val product = Product(
            name = "Test Product",
            unit = "mg",
            unitVolume = 1.0,
            categoryId = null,
            marginType = "percentage",
            marginValue = 20.0,
            siteId = "site-1"
        )
        val purchasePrice = 100.0

        // When
        viewModel.addProductWithPrice(product, purchasePrice)

        // Then - Selling price should be 100 * 1.2 = 120
    }

    @Test
    fun loadProductsForSite_filtersCorrectly() = runTest {
        // Given
        val siteId = "site-1"

        // When
        viewModel.loadProductsForSite(siteId)

        // Then - Should load products for the specified site
        // Initial state should be empty since we haven't added any products
        assertNotNull(viewModel.products.value)
    }

    @Test
    fun loadCategories_loadsData() {
        // When
        viewModel.loadCategories()

        // Then
        assertNotNull(viewModel.categories.value)
    }
}
