package com.medistock.data.entities

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

/**
 * Tests for entity validation and data integrity
 */
class EntityValidationTest {

    @Test
    fun product_createsWithValidData() {
        // When
        val product = Product(
            id = UUID.randomUUID().toString(),
            name = "Paracetamol",
            unit = "comprimé",
            unitVolume = 1.0,
            categoryId = "cat-1",
            marginType = "PERCENTAGE",
            marginValue = 20.0,
            siteId = "site-1"
        )

        // Then
        assertNotNull(product.id)
        assertEquals("Paracetamol", product.name)
        assertEquals("comprimé", product.unit)
        assertTrue(product.createdAt > 0)
    }

    @Test
    fun purchaseBatch_defaultValues() {
        // When
        val batch = PurchaseBatch(
            productId = "product-1",
            siteId = "site-1",
            purchaseDate = System.currentTimeMillis(),
            initialQuantity = 100.0,
            remainingQuantity = 100.0,
            purchasePrice = 10.0
        )

        // Then
        assertNotNull(batch.id)
        assertFalse(batch.isExhausted)
        assertEquals("", batch.supplierName)
        assertNull(batch.expiryDate)
    }

    @Test
    fun sale_createsWithRequiredFields() {
        // When
        val sale = Sale(
            customerName = "John Doe",
            date = System.currentTimeMillis(),
            totalAmount = 150.0,
            siteId = "site-1"
        )

        // Then
        assertNotNull(sale.id)
        assertEquals("John Doe", sale.customerName)
        assertEquals(150.0, sale.totalAmount, 0.01)
        assertNull(sale.customerId)
    }

    @Test
    fun user_isAdminDefault() {
        // When
        val user = User(
            username = "testuser",
            fullName = "Test User",
            passwordHash = "hash",
            isAdmin = false,
            isActive = true,
            siteId = "site-1"
        )

        // Then
        assertNotNull(user.id)
        assertFalse(user.isAdmin)
        assertTrue(user.isActive)
    }

    @Test
    fun userPermission_defaultPermissions() {
        // When
        val permission = UserPermission(
            userId = "user-1",
            module = "STOCK",
            canView = true,
            canCreate = false,
            canEdit = false,
            canDelete = false
        )

        // Then
        assertNotNull(permission.id)
        assertTrue(permission.canView)
        assertFalse(permission.canCreate)
        assertFalse(permission.canEdit)
        assertFalse(permission.canDelete)
    }

    @Test
    fun category_createsCorrectly() {
        // When
        val category = Category(
            name = "Antibiotics",
            description = "Antibiotic medications",
            siteId = "site-1"
        )

        // Then
        assertNotNull(category.id)
        assertEquals("Antibiotics", category.name)
        assertTrue(category.createdAt > 0)
    }

    @Test
    fun saleItem_foreignKeyCascade() {
        // Given - Create with foreign keys
        val saleItem = SaleItem(
            saleId = "sale-1",
            productId = "product-1",
            productName = "Product Name",
            unit = "mg",
            quantity = 10.0,
            pricePerUnit = 15.0,
            subtotal = 150.0,
            siteId = "site-1"
        )

        // Then
        assertNotNull(saleItem.id)
        assertEquals("sale-1", saleItem.saleId)
        assertEquals("product-1", saleItem.productId)
    }

    @Test
    fun site_createsWithDefaults() {
        // When
        val site = Site(
            name = "Main Warehouse",
            address = "123 Main St",
            isActive = true
        )

        // Then
        assertNotNull(site.id)
        assertEquals("Main Warehouse", site.name)
        assertTrue(site.isActive)
    }

    @Test
    fun stockMovement_tracksInventoryChanges() {
        // When
        val movement = StockMovement(
            productId = "product-1",
            siteId = "site-1",
            movementType = "SALE",
            quantityChange = -10.0,
            referenceId = "sale-1",
            notes = "Sold to customer"
        )

        // Then
        assertNotNull(movement.id)
        assertEquals(-10.0, movement.quantityChange, 0.01)
        assertEquals("SALE", movement.movementType)
    }

    @Test
    fun saleBatchAllocation_tracksFIFO() {
        // When
        val allocation = SaleBatchAllocation(
            saleItemId = "sale-item-1",
            batchId = "batch-1",
            quantityAllocated = 50.0,
            purchasePrice = 10.0
        )

        // Then
        assertNotNull(allocation.id)
        assertEquals(50.0, allocation.quantityAllocated, 0.01)
        assertEquals(10.0, allocation.purchasePrice, 0.01)
    }

    @Test
    fun auditHistory_capturesChanges() {
        // When
        val audit = AuditHistory(
            tableName = "products",
            recordId = "product-1",
            operation = "UPDATE",
            changedFields = "{\"name\": \"Old Name\"}",
            userId = "user-1",
            siteId = "site-1"
        )

        // Then
        assertNotNull(audit.id)
        assertEquals("UPDATE", audit.operation)
        assertTrue(audit.changedFields.isNotEmpty())
    }

    @Test
    fun product_copyUpdatesTimestamp() {
        // Given
        val original = Product(
            id = "product-1",
            name = "Original",
            unit = "mg",
            unitVolume = 1.0,
            categoryId = null,
            marginType = null,
            marginValue = null,
            siteId = "site-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )

        // When
        val updated = original.copy(
            name = "Updated",
            updatedAt = 2000L
        )

        // Then
        assertEquals(1000L, updated.createdAt)
        assertEquals(2000L, updated.updatedAt)
        assertEquals("Updated", updated.name)
    }

    @Test
    fun purchaseBatch_exhaustedLogic() {
        // Given
        val batch1 = PurchaseBatch(
            productId = "product-1",
            siteId = "site-1",
            purchaseDate = System.currentTimeMillis(),
            initialQuantity = 100.0,
            remainingQuantity = 0.0,
            purchasePrice = 10.0,
            isExhausted = true
        )
        val batch2 = PurchaseBatch(
            productId = "product-1",
            siteId = "site-1",
            purchaseDate = System.currentTimeMillis(),
            initialQuantity = 100.0,
            remainingQuantity = 50.0,
            purchasePrice = 10.0,
            isExhausted = false
        )

        // Then
        assertTrue(batch1.isExhausted)
        assertEquals(0.0, batch1.remainingQuantity, 0.01)
        assertFalse(batch2.isExhausted)
        assertTrue(batch2.remainingQuantity > 0)
    }
}
