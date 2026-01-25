package com.medistock.shared

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class PlatformTest {
    @Test
    fun testPlatformName() {
        val platform = getPlatform()
        assertTrue(platform.name.isNotEmpty(), "Platform name should not be empty")
    }
}

class SiteModelTest {
    @Test
    fun testSiteCreation() {
        val site = com.medistock.shared.domain.model.Site(
            id = "test-id",
            name = "Test Site"
        )
        assertEquals("test-id", site.id)
        assertEquals("Test Site", site.name)
    }

    @Test
    fun testSiteWithTimestamps() {
        val now = 1705680000000L
        val site = com.medistock.shared.domain.model.Site(
            id = "site-1",
            name = "Pharmacy 1",
            createdAt = now,
            updatedAt = now,
            createdBy = "admin",
            updatedBy = "admin"
        )
        assertEquals(now, site.createdAt)
        assertEquals("admin", site.createdBy)
    }
}

class ProductModelTest {
    @Test
    fun testProductCreation() {
        val product = com.medistock.shared.domain.model.Product(
            id = "prod-1",
            name = "Paracetamol 500mg",
            packagingTypeId = "packaging-1",
            unitVolume = 1.0,
            siteId = "site-1"
        )
        assertEquals("Paracetamol 500mg", product.name)
        assertEquals("packaging-1", product.packagingTypeId)
        assertEquals(1, product.selectedLevel)
    }

    @Test
    fun testProductWithCategory() {
        val product = com.medistock.shared.domain.model.Product(
            id = "prod-2",
            name = "Ibuprofen 400mg",
            packagingTypeId = "packaging-2",
            selectedLevel = 2,
            unitVolume = 1.0,
            categoryId = "cat-1",
            siteId = "site-1",
            minStock = 10.0,
            maxStock = 100.0
        )
        assertEquals("cat-1", product.categoryId)
        assertEquals(10.0, product.minStock)
        assertEquals(100.0, product.maxStock)
        assertEquals(2, product.selectedLevel)
    }
}

class UserModelTest {
    @Test
    fun testUserCreation() {
        val user = com.medistock.shared.domain.model.User(
            id = "user-1",
            username = "admin",
            password = "hashed_password",
            fullName = "Administrator"
        )
        assertEquals("admin", user.username)
        assertEquals("Administrator", user.fullName)
    }

    @Test
    fun testAdminUser() {
        val admin = com.medistock.shared.domain.model.User(
            id = "user-admin",
            username = "superadmin",
            password = "hashed",
            fullName = "Super Admin",
            isAdmin = true,
            isActive = true
        )
        assertTrue(admin.isAdmin)
        assertTrue(admin.isActive)
    }
}

class PurchaseBatchModelTest {
    @Test
    fun testBatchCreation() {
        val batch = com.medistock.shared.domain.model.PurchaseBatch(
            id = "batch-1",
            productId = "prod-1",
            siteId = "site-1",
            purchaseDate = 1705680000000L,
            initialQuantity = 100.0,
            remainingQuantity = 100.0,
            purchasePrice = 5.50
        )
        assertEquals(100.0, batch.initialQuantity)
        assertEquals(5.50, batch.purchasePrice)
    }

    @Test
    fun testExhaustedBatch() {
        val batch = com.medistock.shared.domain.model.PurchaseBatch(
            id = "batch-2",
            productId = "prod-1",
            siteId = "site-1",
            purchaseDate = 1705680000000L,
            initialQuantity = 50.0,
            remainingQuantity = 0.0,
            purchasePrice = 10.0,
            isExhausted = true
        )
        assertTrue(batch.isExhausted)
        assertEquals(0.0, batch.remainingQuantity)
    }
}

class SaleModelTest {
    @Test
    fun testSaleCreation() {
        val sale = com.medistock.shared.domain.model.Sale(
            id = "sale-1",
            customerName = "John Doe",
            date = 1705680000000L,
            totalAmount = 150.0,
            siteId = "site-1"
        )
        assertEquals("John Doe", sale.customerName)
        assertEquals(150.0, sale.totalAmount)
    }

    @Test
    fun testSaleItem() {
        val saleItem = com.medistock.shared.domain.model.SaleItem(
            id = "item-1",
            saleId = "sale-1",
            productId = "prod-1",
            quantity = 5.0,
            unitPrice = 10.0,
            totalPrice = 50.0
        )
        assertEquals(5.0, saleItem.quantity)
        assertEquals(50.0, saleItem.totalPrice)
    }
}
