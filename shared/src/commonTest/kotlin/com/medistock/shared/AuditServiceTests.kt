package com.medistock.shared

import com.medistock.shared.domain.audit.AuditService
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Serializable
data class TestEntity(val id: String, val name: String, val value: Int = 0)

class AuditServiceCompanionTest {

    @Test
    fun testActionConstants() {
        assertEquals("INSERT", AuditService.ACTION_INSERT)
        assertEquals("UPDATE", AuditService.ACTION_UPDATE)
        assertEquals("DELETE", AuditService.ACTION_DELETE)
    }

    @Test
    fun testTableNameConstants() {
        assertEquals("sites", AuditService.TABLE_SITES)
        assertEquals("categories", AuditService.TABLE_CATEGORIES)
        assertEquals("products", AuditService.TABLE_PRODUCTS)
        assertEquals("customers", AuditService.TABLE_CUSTOMERS)
        assertEquals("packaging_types", AuditService.TABLE_PACKAGING_TYPES)
        assertEquals("users", AuditService.TABLE_USERS)
        assertEquals("user_permissions", AuditService.TABLE_USER_PERMISSIONS)
        assertEquals("purchase_batches", AuditService.TABLE_PURCHASE_BATCHES)
        assertEquals("sales", AuditService.TABLE_SALES)
        assertEquals("sale_items", AuditService.TABLE_SALE_ITEMS)
        assertEquals("stock_movements", AuditService.TABLE_STOCK_MOVEMENTS)
        assertEquals("product_transfers", AuditService.TABLE_PRODUCT_TRANSFERS)
        assertEquals("inventories", AuditService.TABLE_INVENTORIES)
        assertEquals("inventory_items", AuditService.TABLE_INVENTORY_ITEMS)
    }

    @Test
    fun testToJsonSerialization() {
        val entity = TestEntity(id = "test-1", name = "Test Name", value = 42)

        val json = AuditService.toJson(entity)

        assertTrue(json.contains("\"id\":\"test-1\""))
        assertTrue(json.contains("\"name\":\"Test Name\""))
        assertTrue(json.contains("\"value\":42"))
    }

    @Test
    fun testJsonSerializerIgnoresUnknownKeys() {
        val jsonString = """{"id":"1","name":"Test","unknownField":"ignored"}"""

        val entity = AuditService.json.decodeFromString<TestEntity>(jsonString)

        assertEquals("1", entity.id)
        assertEquals("Test", entity.name)
    }
}
