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
            movementType = "IN",
            createdAt = 1000L,
            createdBy = "user"
        )

        val jsonString = json.encodeToString(dto)

        // movementType should be serialized as "type"
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
