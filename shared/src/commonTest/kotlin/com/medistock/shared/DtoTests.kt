package com.medistock.shared

import com.medistock.shared.data.dto.*
import com.medistock.shared.domain.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SiteDtoTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testToModel() {
        val dto = SiteDto(
            id = "site-1",
            name = "Test Site",
            createdAt = 1000L,
            updatedAt = 2000L,
            createdBy = "user1",
            updatedBy = "user2",
            clientId = "client-123"
        )

        val model = dto.toModel()

        assertEquals("site-1", model.id)
        assertEquals("Test Site", model.name)
        assertEquals(1000L, model.createdAt)
        assertEquals(2000L, model.updatedAt)
        assertEquals("user1", model.createdBy)
        assertEquals("user2", model.updatedBy)
    }

    @Test
    fun testFromModel() {
        val model = Site(
            id = "site-1",
            name = "Test Site",
            createdAt = 1000L,
            updatedAt = 2000L,
            createdBy = "user1",
            updatedBy = "user2"
        )

        val dto = SiteDto.fromModel(model, "client-123")

        assertEquals("site-1", dto.id)
        assertEquals("Test Site", dto.name)
        assertEquals("client-123", dto.clientId)
    }

    @Test
    fun testJsonSerialization() {
        val dto = SiteDto(
            id = "site-1",
            name = "Test",
            createdAt = 1000L,
            updatedAt = 2000L,
            createdBy = "user",
            updatedBy = "user"
        )

        val jsonString = json.encodeToString(dto)

        // Verify snake_case is used
        assertTrue(jsonString.contains("created_at"))
        assertTrue(jsonString.contains("updated_at"))
        assertTrue(jsonString.contains("created_by"))
        assertTrue(jsonString.contains("updated_by"))
    }

    @Test
    fun testJsonDeserialization() {
        val jsonString = """
            {
                "id": "site-1",
                "name": "Test",
                "created_at": 1000,
                "updated_at": 2000,
                "created_by": "user",
                "updated_by": "user",
                "client_id": "client-1"
            }
        """.trimIndent()

        val dto = json.decodeFromString<SiteDto>(jsonString)

        assertEquals("site-1", dto.id)
        assertEquals("Test", dto.name)
        assertEquals(1000L, dto.createdAt)
        assertEquals("client-1", dto.clientId)
    }
}

class ProductDtoTest {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun testToModel() {
        val dto = ProductDto(
            id = "prod-1",
            name = "Test Product",
            unit = "kg",
            unitVolume = 1.5,
            packagingTypeId = "pkg-1",
            selectedLevel = 2,
            conversionFactor = 10.0,
            categoryId = "cat-1",
            marginType = "PERCENTAGE",
            marginValue = 20.0,
            description = "A test product",
            siteId = "site-1",
            minStock = 5.0,
            maxStock = 100.0,
            createdAt = 1000L,
            updatedAt = 2000L,
            createdBy = "user",
            updatedBy = "user"
        )

        val model = dto.toModel()

        assertEquals("prod-1", model.id)
        assertEquals("Test Product", model.name)
        assertEquals("kg", model.unit)
        assertEquals(1.5, model.unitVolume)
        assertEquals("pkg-1", model.packagingTypeId)
        assertEquals(2, model.selectedLevel)
        assertEquals(10.0, model.conversionFactor)
        assertEquals("cat-1", model.categoryId)
        assertEquals("PERCENTAGE", model.marginType)
        assertEquals(20.0, model.marginValue)
        assertEquals("site-1", model.siteId)
    }

    @Test
    fun testFromModel() {
        val model = Product(
            id = "prod-1",
            name = "Test Product",
            unit = "kg",
            unitVolume = 1.5,
            siteId = "site-1",
            createdAt = 1000L,
            updatedAt = 2000L,
            createdBy = "user",
            updatedBy = "user"
        )

        val dto = ProductDto.fromModel(model, "client-1")

        assertEquals("prod-1", dto.id)
        assertEquals("Test Product", dto.name)
        assertEquals("site-1", dto.siteId)
        assertEquals("client-1", dto.clientId)
    }

    @Test
    fun testJsonSerializationWithSnakeCase() {
        val dto = ProductDto(
            id = "prod-1",
            name = "Test",
            unit = "kg",
            unitVolume = 1.0,
            siteId = "site-1",
            createdAt = 1000L,
            updatedAt = 2000L,
            createdBy = "user",
            updatedBy = "user"
        )

        val jsonString = json.encodeToString(dto)

        assertTrue(jsonString.contains("unit_volume"))
        assertTrue(jsonString.contains("site_id"))
        assertTrue(jsonString.contains("packaging_type_id"))
    }
}

class UserDtoTest {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun testToModel() {
        val dto = UserDto(
            id = "user-1",
            username = "testuser",
            password = "hashedpassword",
            fullName = "Test User",
            isAdmin = true,
            isActive = true,
            createdAt = 1000L,
            updatedAt = 2000L,
            createdBy = "admin",
            updatedBy = "admin"
        )

        val model = dto.toModel()

        assertEquals("user-1", model.id)
        assertEquals("testuser", model.username)
        assertEquals("hashedpassword", model.password)
        assertEquals("Test User", model.fullName)
        assertEquals(true, model.isAdmin)
        assertEquals(true, model.isActive)
    }

    @Test
    fun testJsonSerializationWithSnakeCase() {
        val dto = UserDto(
            id = "user-1",
            username = "test",
            password = "pass",
            fullName = "Test",
            isAdmin = true,
            isActive = true,
            createdAt = 1000L,
            updatedAt = 2000L,
            createdBy = "admin",
            updatedBy = "admin"
        )

        val jsonString = json.encodeToString(dto)

        assertTrue(jsonString.contains("full_name"))
        assertTrue(jsonString.contains("is_admin"))
        assertTrue(jsonString.contains("is_active"))
    }
}

class PurchaseBatchDtoTest {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun testToModel() {
        val dto = PurchaseBatchDto(
            id = "batch-1",
            productId = "prod-1",
            siteId = "site-1",
            batchNumber = "LOT-001",
            purchaseDate = 1000L,
            initialQuantity = 100.0,
            remainingQuantity = 50.0,
            purchasePrice = 10.5,
            supplierName = "Supplier A",
            expiryDate = 9999999L,
            isExhausted = false,
            createdAt = 1000L,
            updatedAt = 2000L,
            createdBy = "user",
            updatedBy = "user"
        )

        val model = dto.toModel()

        assertEquals("batch-1", model.id)
        assertEquals("prod-1", model.productId)
        assertEquals("site-1", model.siteId)
        assertEquals("LOT-001", model.batchNumber)
        assertEquals(100.0, model.initialQuantity)
        assertEquals(50.0, model.remainingQuantity)
        assertEquals(10.5, model.purchasePrice)
        assertEquals("Supplier A", model.supplierName)
        assertEquals(9999999L, model.expiryDate)
        assertEquals(false, model.isExhausted)
    }

    @Test
    fun testJsonSerializationWithSnakeCase() {
        val dto = PurchaseBatchDto(
            id = "batch-1",
            productId = "prod-1",
            siteId = "site-1",
            purchaseDate = 1000L,
            initialQuantity = 100.0,
            remainingQuantity = 50.0,
            purchasePrice = 10.5,
            createdAt = 1000L,
            updatedAt = 2000L,
            createdBy = "user",
            updatedBy = "user"
        )

        val jsonString = json.encodeToString(dto)

        assertTrue(jsonString.contains("product_id"))
        assertTrue(jsonString.contains("site_id"))
        assertTrue(jsonString.contains("purchase_date"))
        assertTrue(jsonString.contains("initial_quantity"))
        assertTrue(jsonString.contains("remaining_quantity"))
        assertTrue(jsonString.contains("purchase_price"))
        assertTrue(jsonString.contains("is_exhausted"))
    }
}

class SaleDtoTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testToModel() {
        val dto = SaleDto(
            id = "sale-1",
            customerName = "John Doe",
            customerId = "customer-1",
            date = 1000L,
            totalAmount = 150.50,
            siteId = "site-1",
            createdAt = 1000L,
            createdBy = "user"
        )

        val model = dto.toModel()

        assertEquals("sale-1", model.id)
        assertEquals("John Doe", model.customerName)
        assertEquals("customer-1", model.customerId)
        assertEquals(1000L, model.date)
        assertEquals(150.50, model.totalAmount)
        assertEquals("site-1", model.siteId)
    }

    @Test
    fun testFromModel() {
        val model = Sale(
            id = "sale-1",
            customerName = "John Doe",
            customerId = "customer-1",
            date = 1000L,
            totalAmount = 150.50,
            siteId = "site-1",
            createdAt = 1000L,
            createdBy = "user"
        )

        val dto = SaleDto.fromModel(model, "client-1")

        assertEquals("sale-1", dto.id)
        assertEquals("John Doe", dto.customerName)
        assertEquals("client-1", dto.clientId)
    }
}

class StockMovementDtoTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testToModel() {
        val dto = StockMovementDto(
            id = "mov-1",
            productId = "prod-1",
            siteId = "site-1",
            quantity = 10.0,
            type = "IN",
            date = 1000L,
            movementType = "IN",
            referenceId = "batch-1",
            notes = "Initial stock",
            createdAt = 1000L,
            createdBy = "user"
        )

        val model = dto.toModel()

        assertEquals("mov-1", model.id)
        assertEquals("prod-1", model.productId)
        assertEquals("site-1", model.siteId)
        assertEquals(10.0, model.quantity)
        assertEquals("IN", model.type)
        assertEquals("IN", model.movementType)
        assertEquals("batch-1", model.referenceId)
        assertEquals("Initial stock", model.notes)
    }

    @Test
    fun testJsonSerializationMapsTypeCorrectly() {
        val dto = StockMovementDto(
            id = "mov-1",
            productId = "prod-1",
            siteId = "site-1",
            quantity = 10.0,
            type = "IN",
            date = 1000L,
            createdAt = 1000L,
            createdBy = "user"
        )

        val jsonString = json.encodeToString(dto)

        // type should be serialized
        assertTrue(jsonString.contains("\"type\""))
        assertTrue(jsonString.contains("\"IN\""))
    }
}

class CustomerDtoTest {
    @Test
    fun testRoundTrip() {
        val original = Customer(
            id = "cust-1",
            name = "Test Customer",
            phone = "123456",
            email = "test@example.com",
            address = "123 Test St",
            notes = "VIP customer",
            createdAt = 1000L,
            updatedAt = 2000L,
            createdBy = "user",
            updatedBy = "user"
        )

        val dto = CustomerDto.fromModel(original, "client-1")
        val roundTrip = dto.toModel()

        assertEquals(original.id, roundTrip.id)
        assertEquals(original.name, roundTrip.name)
        assertEquals(original.phone, roundTrip.phone)
        assertEquals(original.email, roundTrip.email)
        assertEquals(original.address, roundTrip.address)
        assertEquals(original.notes, roundTrip.notes)
    }
}

class UserPermissionDtoTest {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun testToModel() {
        val dto = UserPermissionDto(
            id = "perm-1",
            userId = "user-1",
            module = "PRODUCTS",
            canView = true,
            canCreate = true,
            canEdit = false,
            canDelete = false,
            createdAt = 1000L,
            updatedAt = 2000L,
            createdBy = "admin",
            updatedBy = "admin"
        )

        val model = dto.toModel()

        assertEquals("perm-1", model.id)
        assertEquals("user-1", model.userId)
        assertEquals("PRODUCTS", model.module)
        assertEquals(true, model.canView)
        assertEquals(true, model.canCreate)
        assertEquals(false, model.canEdit)
        assertEquals(false, model.canDelete)
    }

    @Test
    fun testJsonSerializationWithSnakeCase() {
        val dto = UserPermissionDto(
            id = "perm-1",
            userId = "user-1",
            module = "PRODUCTS",
            canView = true,
            canCreate = true,
            canEdit = false,
            canDelete = false,
            createdAt = 1000L,
            updatedAt = 2000L,
            createdBy = "admin",
            updatedBy = "admin"
        )

        val jsonString = json.encodeToString(dto)

        assertTrue(jsonString.contains("user_id"))
        assertTrue(jsonString.contains("can_view"))
        assertTrue(jsonString.contains("can_create"))
        assertTrue(jsonString.contains("can_edit"))
        assertTrue(jsonString.contains("can_delete"))
    }
}

class CategoryDtoTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testToModel() {
        val dto = CategoryDto(
            id = "cat-1",
            name = "Test Category",
            createdAt = 1000L,
            updatedAt = 2000L,
            createdBy = "user",
            updatedBy = "user"
        )

        val model = dto.toModel()

        assertEquals("cat-1", model.id)
        assertEquals("Test Category", model.name)
        assertEquals(1000L, model.createdAt)
        assertEquals(2000L, model.updatedAt)
    }

    @Test
    fun testFromModel() {
        val model = Category(
            id = "cat-1",
            name = "Test Category",
            createdAt = 1000L,
            updatedAt = 2000L,
            createdBy = "user",
            updatedBy = "user"
        )

        val dto = CategoryDto.fromModel(model, "client-1")

        assertEquals("cat-1", dto.id)
        assertEquals("Test Category", dto.name)
        assertEquals("client-1", dto.clientId)
    }

    @Test
    fun testRoundTrip() {
        val original = Category(
            id = "cat-1",
            name = "Original",
            createdAt = 1000L,
            updatedAt = 2000L,
            createdBy = "user",
            updatedBy = "user"
        )

        val roundTrip = CategoryDto.fromModel(original).toModel()

        assertEquals(original.id, roundTrip.id)
        assertEquals(original.name, roundTrip.name)
    }
}

class PackagingTypeDtoTest {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun testToModel() {
        val dto = PackagingTypeDto(
            id = "pkg-1",
            name = "Box",
            level1Name = "Unit",
            level2Name = "Carton",
            level2Quantity = 10,
            defaultConversionFactor = 10.0,
            isActive = true,
            displayOrder = 1,
            createdAt = 1000L,
            updatedAt = 2000L,
            createdBy = "user",
            updatedBy = "user"
        )

        val model = dto.toModel()

        assertEquals("pkg-1", model.id)
        assertEquals("Box", model.name)
        assertEquals("Unit", model.level1Name)
        assertEquals("Carton", model.level2Name)
        assertEquals(10, model.level2Quantity)
        assertEquals(1000L, model.createdAt)
        assertEquals(2000L, model.updatedAt)
    }

    @Test
    fun testJsonSerializationWithSnakeCase() {
        val dto = PackagingTypeDto(
            id = "pkg-1",
            name = "Box",
            level1Name = "Unit",
            level2Name = "Carton",
            createdAt = 1000L,
            updatedAt = 2000L,
            createdBy = "user",
            updatedBy = "user"
        )

        val jsonString = json.encodeToString(dto)

        assertTrue(jsonString.contains("level1_name"))
        assertTrue(jsonString.contains("level2_name"))
        assertTrue(jsonString.contains("default_conversion_factor"))
        assertTrue(jsonString.contains("is_active"))
        assertTrue(jsonString.contains("display_order"))
    }
}

class ProductTransferDtoTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testToModel() {
        val dto = ProductTransferDto(
            id = "transfer-1",
            productId = "prod-1",
            fromSiteId = "site-1",
            toSiteId = "site-2",
            quantity = 10.0,
            status = "pending",
            notes = "Test transfer",
            createdAt = 1000L,
            updatedAt = 2000L,
            createdBy = "user",
            updatedBy = "user"
        )

        val model = dto.toModel()

        assertEquals("transfer-1", model.id)
        assertEquals("prod-1", model.productId)
        assertEquals("site-1", model.fromSiteId)
        assertEquals("site-2", model.toSiteId)
        assertEquals(10.0, model.quantity)
        assertEquals("pending", model.status)
        assertEquals("Test transfer", model.notes)
    }

    @Test
    fun testJsonSerializationWithSnakeCase() {
        val dto = ProductTransferDto(
            id = "transfer-1",
            productId = "prod-1",
            fromSiteId = "site-1",
            toSiteId = "site-2",
            quantity = 10.0,
            status = "pending",
            createdAt = 1000L,
            updatedAt = 2000L,
            createdBy = "user",
            updatedBy = "user"
        )

        val jsonString = json.encodeToString(dto)

        assertTrue(jsonString.contains("product_id"))
        assertTrue(jsonString.contains("from_site_id"))
        assertTrue(jsonString.contains("to_site_id"))
    }
}

class SaleItemDtoTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testToModel() {
        val dto = SaleItemDto(
            id = "item-1",
            saleId = "sale-1",
            productId = "prod-1",
            productName = "Test Product",
            unit = "kg",
            quantity = 5.0,
            unitPrice = 10.0,
            totalPrice = 50.0,
            createdAt = 1000L,
            createdBy = "user"
        )

        val model = dto.toModel()

        // Note: toModel() only sets the basic required fields
        assertEquals("item-1", model.id)
        assertEquals("sale-1", model.saleId)
        assertEquals("prod-1", model.productId)
        assertEquals(5.0, model.quantity)
        assertEquals(10.0, model.unitPrice)
        assertEquals(50.0, model.totalPrice)
    }

    @Test
    fun testJsonSerializationWithSnakeCase() {
        val dto = SaleItemDto(
            id = "item-1",
            saleId = "sale-1",
            productId = "prod-1",
            quantity = 5.0,
            unitPrice = 10.0,
            totalPrice = 50.0,
            createdAt = 1000L,
            createdBy = "user"
        )

        val jsonString = json.encodeToString(dto)

        assertTrue(jsonString.contains("sale_id"))
        assertTrue(jsonString.contains("product_id"))
        assertTrue(jsonString.contains("unit_price"))
        assertTrue(jsonString.contains("total_price"))
    }
}

class InventoryDtoTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testJsonSerializationWithSnakeCase() {
        val dto = InventoryDto(
            id = "inv-1",
            productId = "prod-1",
            siteId = "site-1",
            countDate = 1000L,
            countedQuantity = 50.0,
            theoreticalQuantity = 55.0,
            discrepancy = -5.0,
            reason = "Damaged items",
            countedBy = "user",
            notes = "Test inventory",
            createdAt = 1000L,
            createdBy = "user"
        )

        val jsonString = json.encodeToString(dto)

        assertTrue(jsonString.contains("product_id"))
        assertTrue(jsonString.contains("site_id"))
        assertTrue(jsonString.contains("count_date"))
        assertTrue(jsonString.contains("counted_quantity"))
        assertTrue(jsonString.contains("theoretical_quantity"))
        assertTrue(jsonString.contains("counted_by"))
        assertTrue(jsonString.contains("created_at"))
        assertTrue(jsonString.contains("created_by"))
    }
}

class SaleBatchAllocationDtoTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testToModel() {
        val dto = SaleBatchAllocationDto(
            id = "alloc-1",
            saleItemId = "item-1",
            batchId = "batch-1",
            quantityAllocated = 5.0,
            purchasePriceAtAllocation = 10.0,
            quantity = 5.0,
            unitCost = 10.0,
            createdAt = 1000L,
            createdBy = "user"
        )

        val model = dto.toModel()

        assertEquals("alloc-1", model.id)
        assertEquals("item-1", model.saleItemId)
        assertEquals("batch-1", model.batchId)
        assertEquals(5.0, model.quantityAllocated)
        assertEquals(10.0, model.purchasePriceAtAllocation)
    }

    @Test
    fun testJsonSerializationWithSnakeCase() {
        val dto = SaleBatchAllocationDto(
            id = "alloc-1",
            saleItemId = "item-1",
            batchId = "batch-1",
            quantityAllocated = 5.0,
            purchasePriceAtAllocation = 10.0,
            createdAt = 1000L,
            createdBy = "user"
        )

        val jsonString = json.encodeToString(dto)

        assertTrue(jsonString.contains("sale_item_id"))
        assertTrue(jsonString.contains("batch_id"))
        assertTrue(jsonString.contains("quantity_allocated"))
        assertTrue(jsonString.contains("purchase_price_at_allocation"))
    }
}

class ProductPriceDtoTest {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun testToModel() {
        val dto = ProductPriceDto(
            id = "price-1",
            productId = "prod-1",
            effectiveDate = 1000L,
            purchasePrice = 50.0,
            sellingPrice = 75.0,
            source = "manual",
            createdAt = 1000L,
            updatedAt = 2000L,
            createdBy = "user",
            updatedBy = "user"
        )

        val model = dto.toModel()

        assertEquals("price-1", model.id)
        assertEquals("prod-1", model.productId)
        assertEquals(1000L, model.effectiveDate)
        assertEquals(50.0, model.purchasePrice)
        assertEquals(75.0, model.sellingPrice)
        assertEquals("manual", model.source)
        assertEquals(1000L, model.createdAt)
        assertEquals(2000L, model.updatedAt)
    }

    @Test
    fun testFromModel() {
        val model = ProductPrice(
            id = "price-1",
            productId = "prod-1",
            effectiveDate = 1000L,
            purchasePrice = 50.0,
            sellingPrice = 75.0,
            source = "calculated",
            createdAt = 1000L,
            updatedAt = 2000L,
            createdBy = "user",
            updatedBy = "user"
        )

        val dto = ProductPriceDto.fromModel(model, "client-1")

        assertEquals("price-1", dto.id)
        assertEquals("prod-1", dto.productId)
        assertEquals(1000L, dto.effectiveDate)
        assertEquals(50.0, dto.purchasePrice)
        assertEquals(75.0, dto.sellingPrice)
        assertEquals("calculated", dto.source)
        assertEquals("client-1", dto.clientId)
    }

    @Test
    fun testJsonSerializationWithSnakeCase() {
        val dto = ProductPriceDto(
            id = "price-1",
            productId = "prod-1",
            effectiveDate = 1000L,
            purchasePrice = 50.0,
            sellingPrice = 75.0,
            source = "manual",
            createdAt = 1000L,
            updatedAt = 2000L,
            createdBy = "user",
            updatedBy = "user"
        )

        val jsonString = json.encodeToString(dto)

        assertTrue(jsonString.contains("product_id"))
        assertTrue(jsonString.contains("effective_date"))
        assertTrue(jsonString.contains("purchase_price"))
        assertTrue(jsonString.contains("selling_price"))
        assertTrue(jsonString.contains("created_at"))
        assertTrue(jsonString.contains("updated_at"))
    }
}

class CurrentStockDtoTest {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun testJsonSerializationWithSnakeCase() {
        val dto = CurrentStockDto(
            productId = "prod-1",
            productName = "Test Product",
            description = "A test product",
            siteId = "site-1",
            siteName = "Test Site",
            currentStock = 100.0,
            minStock = 10.0,
            maxStock = 200.0,
            stockStatus = "OK"
        )

        val jsonString = json.encodeToString(dto)

        assertTrue(jsonString.contains("product_id"))
        assertTrue(jsonString.contains("product_name"))
        assertTrue(jsonString.contains("site_id"))
        assertTrue(jsonString.contains("site_name"))
        assertTrue(jsonString.contains("current_stock"))
        assertTrue(jsonString.contains("min_stock"))
        assertTrue(jsonString.contains("max_stock"))
        assertTrue(jsonString.contains("stock_status"))
    }

    @Test
    fun testJsonDeserializationWithSnakeCase() {
        val jsonString = """
            {
                "product_id": "prod-1",
                "product_name": "Test Product",
                "description": "A test product",
                "site_id": "site-1",
                "site_name": "Test Site",
                "current_stock": 100.0,
                "min_stock": 10.0,
                "max_stock": 200.0,
                "stock_status": "OK"
            }
        """.trimIndent()

        val dto = json.decodeFromString<CurrentStockDto>(jsonString)

        assertEquals("prod-1", dto.productId)
        assertEquals("Test Product", dto.productName)
        assertEquals("A test product", dto.description)
        assertEquals("site-1", dto.siteId)
        assertEquals("Test Site", dto.siteName)
        assertEquals(100.0, dto.currentStock)
        assertEquals(10.0, dto.minStock)
        assertEquals(200.0, dto.maxStock)
        assertEquals("OK", dto.stockStatus)
    }

    @Test
    fun testOptionalFields() {
        val dto = CurrentStockDto(
            productId = "prod-1",
            productName = "Test Product",
            siteId = "site-1",
            siteName = "Test Site",
            currentStock = 50.0,
            stockStatus = "LOW"
        )

        assertNull(dto.description)
        assertNull(dto.minStock)
        assertNull(dto.maxStock)
    }
}

class AuditHistoryDtoTest {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun testToModel() {
        val dto = AuditHistoryDto(
            id = "audit-1",
            entityType = "Product",
            entityId = "prod-1",
            actionType = "UPDATE",
            fieldName = "price",
            oldValue = "50.0",
            newValue = "60.0",
            changedBy = "user-1",
            changedAt = 1000L,
            siteId = "site-1",
            description = "Price updated"
        )

        val model = dto.toModel()

        assertEquals("audit-1", model.id)
        assertEquals("Product", model.entityType)
        assertEquals("prod-1", model.entityId)
        assertEquals("UPDATE", model.actionType)
        assertEquals("price", model.fieldName)
        assertEquals("50.0", model.oldValue)
        assertEquals("60.0", model.newValue)
        assertEquals("user-1", model.changedBy)
        assertEquals(1000L, model.changedAt)
        assertEquals("site-1", model.siteId)
        assertEquals("Price updated", model.description)
    }

    @Test
    fun testFromModel() {
        val model = AuditHistory(
            id = "audit-1",
            entityType = "Product",
            entityId = "prod-1",
            actionType = "DELETE",
            fieldName = null,
            oldValue = null,
            newValue = null,
            changedBy = "user-1",
            changedAt = 1000L,
            siteId = "site-1",
            description = "Product deleted"
        )

        val dto = AuditHistoryDto.fromModel(model, "client-1")

        assertEquals("audit-1", dto.id)
        assertEquals("Product", dto.entityType)
        assertEquals("prod-1", dto.entityId)
        assertEquals("DELETE", dto.actionType)
        assertNull(dto.fieldName)
        assertNull(dto.oldValue)
        assertNull(dto.newValue)
        assertEquals("user-1", dto.changedBy)
        assertEquals(1000L, dto.changedAt)
        assertEquals("client-1", dto.clientId)
    }

    @Test
    fun testJsonSerializationWithSnakeCase() {
        val dto = AuditHistoryDto(
            id = "audit-1",
            entityType = "Product",
            entityId = "prod-1",
            actionType = "CREATE",
            fieldName = "name",
            oldValue = null,
            newValue = "New Product",
            changedBy = "user-1",
            changedAt = 1000L,
            siteId = "site-1"
        )

        val jsonString = json.encodeToString(dto)

        assertTrue(jsonString.contains("entity_type"))
        assertTrue(jsonString.contains("entity_id"))
        assertTrue(jsonString.contains("action_type"))
        assertTrue(jsonString.contains("field_name"))
        assertTrue(jsonString.contains("old_value"))
        assertTrue(jsonString.contains("new_value"))
        assertTrue(jsonString.contains("changed_by"))
        assertTrue(jsonString.contains("changed_at"))
        assertTrue(jsonString.contains("site_id"))
    }

    @Test
    fun testOptionalFields() {
        val dto = AuditHistoryDto(
            id = "audit-1",
            entityType = "Product",
            entityId = "prod-1",
            actionType = "DELETE",
            changedBy = "user-1",
            changedAt = 1000L
        )

        assertNull(dto.fieldName)
        assertNull(dto.oldValue)
        assertNull(dto.newValue)
        assertNull(dto.siteId)
        assertNull(dto.description)
        assertNull(dto.clientId)
    }
}
