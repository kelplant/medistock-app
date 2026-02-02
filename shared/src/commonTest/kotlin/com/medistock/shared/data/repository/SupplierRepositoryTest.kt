package com.medistock.shared.data.repository

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.medistock.shared.db.MedistockDatabase
import com.medistock.shared.domain.model.Supplier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Unit tests for SupplierRepository.
 * Tests cover CRUD operations, active/inactive filtering,
 * search functionality, and soft delete operations (activate/deactivate).
 */
class SupplierRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var database: MedistockDatabase
    private lateinit var repository: SupplierRepository

    @BeforeTest
    fun setUp() {
        // Create in-memory SQLite database for testing
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        MedistockDatabase.Schema.create(driver)
        database = MedistockDatabase(driver)
        repository = SupplierRepository(database)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    // ===== Helper Functions =====

    private fun createTestSupplier(
        id: String = "supplier-1",
        name: String = "Acme Pharmaceuticals",
        phone: String? = "+1234567890",
        email: String? = "contact@acme.com",
        address: String? = "123 Main St",
        notes: String? = "Reliable supplier",
        isActive: Boolean = true,
        createdAt: Long = 1705680000000L,
        updatedAt: Long = 1705680000000L,
        createdBy: String = "user-1",
        updatedBy: String = "user-1"
    ): Supplier {
        return Supplier(
            id = id,
            name = name,
            phone = phone,
            email = email,
            address = address,
            notes = notes,
            isActive = isActive,
            createdAt = createdAt,
            updatedAt = updatedAt,
            createdBy = createdBy,
            updatedBy = updatedBy
        )
    }

    // ===== Insert Tests =====

    @Test
    fun `should_insertSupplier_when_validDataProvided`() = runTest {
        // Arrange
        val supplier = createTestSupplier()

        // Act
        repository.insert(supplier)
        val result = repository.getById(supplier.id)

        // Assert
        assertNotNull(result)
        assertEquals(supplier.id, result.id)
        assertEquals(supplier.name, result.name)
        assertEquals(supplier.phone, result.phone)
        assertEquals(supplier.email, result.email)
        assertEquals(supplier.address, result.address)
        assertEquals(supplier.notes, result.notes)
        assertEquals(supplier.isActive, result.isActive)
        assertEquals(supplier.createdAt, result.createdAt)
        assertEquals(supplier.updatedAt, result.updatedAt)
        assertEquals(supplier.createdBy, result.createdBy)
        assertEquals(supplier.updatedBy, result.updatedBy)
    }

    @Test
    fun `should_insertMultipleSuppliers_when_differentIds`() = runTest {
        // Arrange
        val supplier1 = createTestSupplier(id = "supplier-1", name = "Acme Corp")
        val supplier2 = createTestSupplier(id = "supplier-2", name = "Beta Ltd")

        // Act
        repository.insert(supplier1)
        repository.insert(supplier2)
        val all = repository.getAll()

        // Assert
        assertEquals(2, all.size)
        assertTrue(all.any { it.id == "supplier-1" })
        assertTrue(all.any { it.id == "supplier-2" })
    }

    @Test
    fun `should_insertSupplierWithNullFields_when_optionalFieldsNull`() = runTest {
        // Arrange
        val supplier = createTestSupplier(
            phone = null,
            email = null,
            address = null,
            notes = null
        )

        // Act
        repository.insert(supplier)
        val result = repository.getById(supplier.id)

        // Assert
        assertNotNull(result)
        assertNull(result.phone)
        assertNull(result.email)
        assertNull(result.address)
        assertNull(result.notes)
    }

    @Test
    fun `should_insertInactiveSupplier_when_isActiveFalse`() = runTest {
        // Arrange
        val supplier = createTestSupplier(isActive = false)

        // Act
        repository.insert(supplier)
        val result = repository.getById(supplier.id)

        // Assert
        assertNotNull(result)
        assertFalse(result.isActive)
    }

    // ===== Upsert Tests =====

    @Test
    fun `should_insertNewSupplier_when_upsertingNonExistingId`() = runTest {
        // Arrange
        val supplier = createTestSupplier()

        // Act
        repository.upsert(supplier)
        val result = repository.getById(supplier.id)

        // Assert
        assertNotNull(result)
        assertEquals(supplier.id, result.id)
        assertEquals(supplier.name, result.name)
    }

    @Test
    fun `should_replaceSupplier_when_upsertingExistingId`() = runTest {
        // Arrange
        val original = createTestSupplier(name = "Original Name", phone = "111111")
        repository.insert(original)

        // Act
        val updated = createTestSupplier(name = "Updated Name", phone = "222222")
        repository.upsert(updated)
        val result = repository.getById(original.id)

        // Assert
        assertNotNull(result)
        assertEquals("Updated Name", result.name)
        assertEquals("222222", result.phone)
    }

    @Test
    fun `should_replaceAllFields_when_upsertingExistingSupplier`() = runTest {
        // Arrange
        val original = createTestSupplier(
            name = "Original",
            phone = "111",
            email = "old@example.com",
            address = "Old Address",
            notes = "Old notes",
            isActive = true
        )
        repository.insert(original)

        // Act
        val updated = createTestSupplier(
            name = "Updated",
            phone = "222",
            email = "new@example.com",
            address = "New Address",
            notes = "New notes",
            isActive = false
        )
        repository.upsert(updated)
        val result = repository.getById(original.id)

        // Assert
        assertNotNull(result)
        assertEquals("Updated", result.name)
        assertEquals("222", result.phone)
        assertEquals("new@example.com", result.email)
        assertEquals("New Address", result.address)
        assertEquals("New notes", result.notes)
        assertFalse(result.isActive)
    }

    // ===== Read Tests =====

    @Test
    fun `should_returnNull_when_getByIdNotExists`() = runTest {
        // Act
        val result = repository.getById("non-existent")

        // Assert
        assertNull(result)
    }

    @Test
    fun `should_returnEmptyList_when_getAllWithNoData`() = runTest {
        // Act
        val result = repository.getAll()

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun `should_returnAllSuppliers_when_multipleExist`() = runTest {
        // Arrange
        repository.insert(createTestSupplier(id = "supplier-1", name = "Alpha Corp"))
        repository.insert(createTestSupplier(id = "supplier-2", name = "Beta Ltd"))
        repository.insert(createTestSupplier(id = "supplier-3", name = "Gamma Inc"))

        // Act
        val result = repository.getAll()

        // Assert
        assertEquals(3, result.size)
        // Verify alphabetical order by name
        assertEquals("Alpha Corp", result[0].name)
        assertEquals("Beta Ltd", result[1].name)
        assertEquals("Gamma Inc", result[2].name)
    }

    @Test
    fun `should_returnSuppliersInAlphabeticalOrder_when_getAll`() = runTest {
        // Arrange
        repository.insert(createTestSupplier(id = "supplier-1", name = "Zebra Supplies"))
        repository.insert(createTestSupplier(id = "supplier-2", name = "Alpha Medical"))
        repository.insert(createTestSupplier(id = "supplier-3", name = "Mega Pharma"))

        // Act
        val result = repository.getAll()

        // Assert
        assertEquals(3, result.size)
        assertEquals("Alpha Medical", result[0].name)
        assertEquals("Mega Pharma", result[1].name)
        assertEquals("Zebra Supplies", result[2].name)
    }

    // ===== Active Supplier Tests =====

    @Test
    fun `should_returnOnlyActiveSuppliers_when_getActive`() = runTest {
        // Arrange
        repository.insert(createTestSupplier(id = "supplier-1", name = "Active 1", isActive = true))
        repository.insert(createTestSupplier(id = "supplier-2", name = "Inactive", isActive = false))
        repository.insert(createTestSupplier(id = "supplier-3", name = "Active 2", isActive = true))

        // Act
        val result = repository.getActive()

        // Assert
        assertEquals(2, result.size)
        assertTrue(result.all { it.isActive })
        assertTrue(result.any { it.id == "supplier-1" })
        assertTrue(result.any { it.id == "supplier-3" })
    }

    @Test
    fun `should_returnEmptyList_when_getActiveWithNoActiveSuppliers`() = runTest {
        // Arrange
        repository.insert(createTestSupplier(id = "supplier-1", isActive = false))
        repository.insert(createTestSupplier(id = "supplier-2", isActive = false))

        // Act
        val result = repository.getActive()

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun `should_returnAllSuppliers_when_getActiveWithAllActive`() = runTest {
        // Arrange
        repository.insert(createTestSupplier(id = "supplier-1", isActive = true))
        repository.insert(createTestSupplier(id = "supplier-2", isActive = true))

        // Act
        val result = repository.getActive()

        // Assert
        assertEquals(2, result.size)
    }

    // ===== Search Tests =====

    @Test
    fun `should_returnMatchingSuppliers_when_searchByName`() = runTest {
        // Arrange
        repository.insert(createTestSupplier(id = "supplier-1", name = "Acme Pharmaceuticals", phone = "111"))
        repository.insert(createTestSupplier(id = "supplier-2", name = "Beta Medical", phone = "222"))
        repository.insert(createTestSupplier(id = "supplier-3", name = "Acme Supplies", phone = "333"))

        // Act
        val result = repository.search("Acme")

        // Assert
        assertEquals(2, result.size)
        assertTrue(result.all { it.name.contains("Acme") })
    }

    @Test
    fun `should_returnMatchingSuppliers_when_searchByPhone`() = runTest {
        // Arrange
        repository.insert(createTestSupplier(id = "supplier-1", name = "Acme Corp", phone = "+1-555-1234"))
        repository.insert(createTestSupplier(id = "supplier-2", name = "Beta Ltd", phone = "+1-555-5678"))
        repository.insert(createTestSupplier(id = "supplier-3", name = "Gamma Inc", phone = "+1-999-1234"))

        // Act
        val result = repository.search("555")

        // Assert
        assertEquals(2, result.size)
        assertTrue(result.all { it.phone?.contains("555") == true })
    }

    @Test
    fun `should_beCaseInsensitive_when_searching`() = runTest {
        // Arrange
        repository.insert(createTestSupplier(id = "supplier-1", name = "Acme Pharmaceuticals"))

        // Act
        val result1 = repository.search("ACME")
        val result2 = repository.search("acme")
        val result3 = repository.search("AcMe")

        // Assert
        assertEquals(1, result1.size)
        assertEquals(1, result2.size)
        assertEquals(1, result3.size)
    }

    @Test
    fun `should_returnEmptyList_when_searchWithNoMatches`() = runTest {
        // Arrange
        repository.insert(createTestSupplier(name = "Acme Corp"))

        // Act
        val result = repository.search("Nonexistent")

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun `should_returnAllSuppliers_when_searchWithEmptyQuery`() = runTest {
        // Arrange
        repository.insert(createTestSupplier(id = "supplier-1", name = "Acme Corp"))
        repository.insert(createTestSupplier(id = "supplier-2", name = "Beta Ltd"))

        // Act
        val result = repository.search("")

        // Assert
        assertEquals(2, result.size)
    }

    @Test
    fun `should_searchPartialMatches_when_queryIsSubstring`() = runTest {
        // Arrange
        repository.insert(createTestSupplier(id = "supplier-1", name = "Medical Supplies Inc"))
        repository.insert(createTestSupplier(id = "supplier-2", name = "Surgical Equipment"))
        repository.insert(createTestSupplier(id = "supplier-3", name = "Dental Supplies"))

        // Act
        val result = repository.search("Supplies")

        // Assert
        assertEquals(2, result.size)
        assertTrue(result.any { it.id == "supplier-1" })
        assertTrue(result.any { it.id == "supplier-3" })
    }

    // ===== Update Tests =====

    @Test
    fun `should_updateSupplier_when_validDataProvided`() = runTest {
        // Arrange
        val original = createTestSupplier(name = "Original Name", phone = "111")
        repository.insert(original)

        // Act
        val updated = original.copy(name = "Updated Name", phone = "222", updatedAt = 1705690000000L)
        repository.update(updated)
        val result = repository.getById(original.id)

        // Assert
        assertNotNull(result)
        assertEquals("Updated Name", result.name)
        assertEquals("222", result.phone)
        assertEquals(1705690000000L, result.updatedAt)
    }

    @Test
    fun `should_updateAllFields_when_updating`() = runTest {
        // Arrange
        val original = createTestSupplier(
            name = "Original",
            phone = "111",
            email = "old@example.com",
            address = "Old Address",
            notes = "Old notes",
            isActive = true
        )
        repository.insert(original)

        // Act
        val updated = original.copy(
            name = "Updated",
            phone = "222",
            email = "new@example.com",
            address = "New Address",
            notes = "New notes",
            isActive = false,
            updatedAt = 1705690000000L,
            updatedBy = "user-2"
        )
        repository.update(updated)
        val result = repository.getById(original.id)

        // Assert
        assertNotNull(result)
        assertEquals("Updated", result.name)
        assertEquals("222", result.phone)
        assertEquals("new@example.com", result.email)
        assertEquals("New Address", result.address)
        assertEquals("New notes", result.notes)
        assertFalse(result.isActive)
        assertEquals(1705690000000L, result.updatedAt)
        assertEquals("user-2", result.updatedBy)
    }

    @Test
    fun `should_notAffectOtherSuppliers_when_updating`() = runTest {
        // Arrange
        repository.insert(createTestSupplier(id = "supplier-1", name = "First"))
        repository.insert(createTestSupplier(id = "supplier-2", name = "Second"))

        // Act
        val updated = createTestSupplier(id = "supplier-1", name = "First Updated")
        repository.update(updated)

        val result1 = repository.getById("supplier-1")
        val result2 = repository.getById("supplier-2")

        // Assert
        assertNotNull(result1)
        assertNotNull(result2)
        assertEquals("First Updated", result1.name)
        assertEquals("Second", result2.name)
    }

    @Test
    fun `should_preserveCreatedFields_when_updating`() = runTest {
        // Arrange
        val original = createTestSupplier(
            createdAt = 1705680000000L,
            createdBy = "user-1"
        )
        repository.insert(original)

        // Act
        val updated = original.copy(
            name = "Updated Name",
            updatedAt = 1705690000000L,
            updatedBy = "user-2"
        )
        repository.update(updated)
        val result = repository.getById(original.id)

        // Assert
        assertNotNull(result)
        assertEquals(1705680000000L, result.createdAt)
        assertEquals("user-1", result.createdBy)
        assertEquals("Updated Name", result.name)
        assertEquals(1705690000000L, result.updatedAt)
        assertEquals("user-2", result.updatedBy)
    }

    // ===== Delete Tests =====

    @Test
    fun `should_deleteSupplier_when_idExists`() = runTest {
        // Arrange
        val supplier = createTestSupplier()
        repository.insert(supplier)

        // Act
        repository.delete(supplier.id)
        val result = repository.getById(supplier.id)

        // Assert
        assertNull(result)
    }

    @Test
    fun `should_notAffectOthers_when_deleteOneSupplier`() = runTest {
        // Arrange
        repository.insert(createTestSupplier(id = "supplier-1"))
        repository.insert(createTestSupplier(id = "supplier-2"))

        // Act
        repository.delete("supplier-1")
        val all = repository.getAll()

        // Assert
        assertEquals(1, all.size)
        assertEquals("supplier-2", all[0].id)
    }

    @Test
    fun `should_doNothing_when_deleteNonExistingId`() = runTest {
        // Arrange
        repository.insert(createTestSupplier(id = "supplier-1"))

        // Act
        repository.delete("non-existent")
        val all = repository.getAll()

        // Assert
        assertEquals(1, all.size)
    }

    // ===== Deactivate Tests (Soft Delete) =====

    @Test
    fun `should_deactivateSupplier_when_idExists`() = runTest {
        // Arrange
        val supplier = createTestSupplier(isActive = true)
        repository.insert(supplier)

        // Act
        repository.deactivate(supplier.id, "user-2")
        val result = repository.getById(supplier.id)

        // Assert
        assertNotNull(result)
        assertFalse(result.isActive)
        assertEquals("user-2", result.updatedBy)
        assertTrue(result.updatedAt > supplier.updatedAt)
    }

    @Test
    fun `should_notAffectOtherFields_when_deactivate`() = runTest {
        // Arrange
        val supplier = createTestSupplier(
            name = "Test Supplier",
            phone = "123456",
            email = "test@example.com",
            isActive = true
        )
        repository.insert(supplier)

        // Act
        repository.deactivate(supplier.id, "user-2")
        val result = repository.getById(supplier.id)

        // Assert
        assertNotNull(result)
        assertFalse(result.isActive)
        assertEquals(supplier.name, result.name)
        assertEquals(supplier.phone, result.phone)
        assertEquals(supplier.email, result.email)
        assertEquals(supplier.address, result.address)
        assertEquals(supplier.notes, result.notes)
    }

    @Test
    fun `should_deactivateAlreadyInactive_when_deactivateCalledTwice`() = runTest {
        // Arrange
        val supplier = createTestSupplier(isActive = true)
        repository.insert(supplier)

        // Act
        repository.deactivate(supplier.id, "user-2")
        repository.deactivate(supplier.id, "user-3")
        val result = repository.getById(supplier.id)

        // Assert
        assertNotNull(result)
        assertFalse(result.isActive)
        assertEquals("user-3", result.updatedBy)
    }

    @Test
    fun `should_excludeFromActive_when_supplierDeactivated`() = runTest {
        // Arrange
        repository.insert(createTestSupplier(id = "supplier-1", isActive = true))
        repository.insert(createTestSupplier(id = "supplier-2", isActive = true))

        // Act
        repository.deactivate("supplier-1", "user-2")
        val activeSuppliers = repository.getActive()

        // Assert
        assertEquals(1, activeSuppliers.size)
        assertEquals("supplier-2", activeSuppliers[0].id)
    }

    // ===== Activate Tests =====

    @Test
    fun `should_activateSupplier_when_idExists`() = runTest {
        // Arrange
        val supplier = createTestSupplier(isActive = false)
        repository.insert(supplier)

        // Act
        repository.activate(supplier.id, "user-2")
        val result = repository.getById(supplier.id)

        // Assert
        assertNotNull(result)
        assertTrue(result.isActive)
        assertEquals("user-2", result.updatedBy)
        assertTrue(result.updatedAt > supplier.updatedAt)
    }

    @Test
    fun `should_notAffectOtherFields_when_activate`() = runTest {
        // Arrange
        val supplier = createTestSupplier(
            name = "Test Supplier",
            phone = "123456",
            email = "test@example.com",
            isActive = false
        )
        repository.insert(supplier)

        // Act
        repository.activate(supplier.id, "user-2")
        val result = repository.getById(supplier.id)

        // Assert
        assertNotNull(result)
        assertTrue(result.isActive)
        assertEquals(supplier.name, result.name)
        assertEquals(supplier.phone, result.phone)
        assertEquals(supplier.email, result.email)
        assertEquals(supplier.address, result.address)
        assertEquals(supplier.notes, result.notes)
    }

    @Test
    fun `should_activateAlreadyActive_when_activateCalledTwice`() = runTest {
        // Arrange
        val supplier = createTestSupplier(isActive = false)
        repository.insert(supplier)

        // Act
        repository.activate(supplier.id, "user-2")
        repository.activate(supplier.id, "user-3")
        val result = repository.getById(supplier.id)

        // Assert
        assertNotNull(result)
        assertTrue(result.isActive)
        assertEquals("user-3", result.updatedBy)
    }

    @Test
    fun `should_includeInActive_when_supplierActivated`() = runTest {
        // Arrange
        repository.insert(createTestSupplier(id = "supplier-1", isActive = false))
        repository.insert(createTestSupplier(id = "supplier-2", isActive = true))

        // Act
        repository.activate("supplier-1", "user-2")
        val activeSuppliers = repository.getActive()

        // Assert
        assertEquals(2, activeSuppliers.size)
        assertTrue(activeSuppliers.any { it.id == "supplier-1" })
        assertTrue(activeSuppliers.any { it.id == "supplier-2" })
    }

    // ===== Flow Observer Tests =====

    @Test
    fun `should_emitAllSuppliers_when_observeAll`() = runTest {
        // Arrange
        repository.insert(createTestSupplier(id = "supplier-1"))
        repository.insert(createTestSupplier(id = "supplier-2"))

        // Act
        val result = repository.observeAll().first()

        // Assert
        assertEquals(2, result.size)
        assertTrue(result.any { it.id == "supplier-1" })
        assertTrue(result.any { it.id == "supplier-2" })
    }

    @Test
    fun `should_emitUpdates_when_observeAllAndDataChanges`() = runTest {
        // Arrange
        repository.insert(createTestSupplier(id = "supplier-1"))

        // Act
        val initial = repository.observeAll().first()
        repository.insert(createTestSupplier(id = "supplier-2"))
        val updated = repository.observeAll().first()

        // Assert
        assertEquals(1, initial.size)
        assertEquals(2, updated.size)
    }

    @Test
    fun `should_emitEmptyList_when_observeAllWithNoData`() = runTest {
        // Act
        val result = repository.observeAll().first()

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun `should_emitOnlyActiveSuppliers_when_observeActive`() = runTest {
        // Arrange
        repository.insert(createTestSupplier(id = "supplier-1", isActive = true))
        repository.insert(createTestSupplier(id = "supplier-2", isActive = false))
        repository.insert(createTestSupplier(id = "supplier-3", isActive = true))

        // Act
        val result = repository.observeActive().first()

        // Assert
        assertEquals(2, result.size)
        assertTrue(result.all { it.isActive })
        assertTrue(result.any { it.id == "supplier-1" })
        assertTrue(result.any { it.id == "supplier-3" })
    }

    @Test
    fun `should_emitUpdates_when_observeActiveAndSupplierDeactivated`() = runTest {
        // Arrange
        repository.insert(createTestSupplier(id = "supplier-1", isActive = true))

        // Act
        val initial = repository.observeActive().first()
        repository.deactivate("supplier-1", "user-2")
        val updated = repository.observeActive().first()

        // Assert
        assertEquals(1, initial.size)
        assertEquals(0, updated.size)
    }

    // ===== Edge Cases =====

    @Test
    fun `should_handleEmptyStrings_when_insertingSupplier`() = runTest {
        // Arrange
        val supplier = createTestSupplier(
            name = "",
            phone = "",
            email = "",
            address = "",
            notes = ""
        )

        // Act
        repository.insert(supplier)
        val result = repository.getById(supplier.id)

        // Assert
        assertNotNull(result)
        assertEquals("", result.name)
        assertEquals("", result.phone)
        assertEquals("", result.email)
        assertEquals("", result.address)
        assertEquals("", result.notes)
    }

    @Test
    fun `should_handleLongStrings_when_insertingSupplier`() = runTest {
        // Arrange
        val longString = "x".repeat(1000)
        val supplier = createTestSupplier(
            name = longString,
            address = longString,
            notes = longString
        )

        // Act
        repository.insert(supplier)
        val result = repository.getById(supplier.id)

        // Assert
        assertNotNull(result)
        assertEquals(longString, result.name)
        assertEquals(longString, result.address)
        assertEquals(longString, result.notes)
    }

    @Test
    fun `should_handleSpecialCharacters_when_insertingSupplier`() = runTest {
        // Arrange
        val specialChars = "Special: <>&\"'éàù çñ 中文 🏥"
        val supplier = createTestSupplier(
            name = specialChars,
            address = specialChars,
            notes = specialChars
        )

        // Act
        repository.insert(supplier)
        val result = repository.getById(supplier.id)

        // Assert
        assertNotNull(result)
        assertEquals(specialChars, result.name)
        assertEquals(specialChars, result.address)
        assertEquals(specialChars, result.notes)
    }

    @Test
    fun `should_handleZeroTimestamp_when_insertingSupplier`() = runTest {
        // Arrange
        val supplier = createTestSupplier(createdAt = 0L, updatedAt = 0L)

        // Act
        repository.insert(supplier)
        val result = repository.getById(supplier.id)

        // Assert
        assertNotNull(result)
        assertEquals(0L, result.createdAt)
        assertEquals(0L, result.updatedAt)
    }

    @Test
    fun `should_handleMaxLongTimestamp_when_insertingSupplier`() = runTest {
        // Arrange
        val supplier = createTestSupplier(
            createdAt = Long.MAX_VALUE,
            updatedAt = Long.MAX_VALUE
        )

        // Act
        repository.insert(supplier)
        val result = repository.getById(supplier.id)

        // Assert
        assertNotNull(result)
        assertEquals(Long.MAX_VALUE, result.createdAt)
        assertEquals(Long.MAX_VALUE, result.updatedAt)
    }

    // ===== Integration Scenarios =====

    @Test
    fun `should_workCorrectly_when_completeSupplierLifecycle`() = runTest {
        // Arrange - Create new supplier
        val supplier = createTestSupplier(isActive = true)
        repository.insert(supplier)

        // Act & Assert - Verify initial state
        var result = repository.getById(supplier.id)
        assertNotNull(result)
        assertTrue(result.isActive)

        // Act & Assert - Update supplier information
        val updated = supplier.copy(
            name = "Updated Name",
            phone = "999999",
            updatedAt = 1705690000000L
        )
        repository.update(updated)
        result = repository.getById(supplier.id)
        assertNotNull(result)
        assertEquals("Updated Name", result.name)
        assertEquals("999999", result.phone)

        // Act & Assert - Deactivate supplier (soft delete)
        repository.deactivate(supplier.id, "user-2")
        result = repository.getById(supplier.id)
        assertNotNull(result)
        assertFalse(result.isActive)

        // Act & Assert - Reactivate supplier
        repository.activate(supplier.id, "user-3")
        result = repository.getById(supplier.id)
        assertNotNull(result)
        assertTrue(result.isActive)

        // Act & Assert - Hard delete
        repository.delete(supplier.id)
        result = repository.getById(supplier.id)
        assertNull(result)
    }

    @Test
    fun `should_maintainIntegrity_when_multiSupplierOperations`() = runTest {
        // Arrange - Create multiple suppliers with different active states
        repository.insert(createTestSupplier(id = "supplier-1", name = "Supplier A", isActive = true))
        repository.insert(createTestSupplier(id = "supplier-2", name = "Supplier B", isActive = true))
        repository.insert(createTestSupplier(id = "supplier-3", name = "Supplier C", isActive = false))
        repository.insert(createTestSupplier(id = "supplier-4", name = "Supplier D", isActive = false))

        // Assert - Verify active filtering
        val allActive = repository.getActive()
        assertEquals(2, allActive.size)
        assertTrue(allActive.any { it.id == "supplier-1" })
        assertTrue(allActive.any { it.id == "supplier-2" })

        // Assert - Verify all suppliers
        val all = repository.getAll()
        assertEquals(4, all.size)

        // Assert - Verify alphabetical order
        assertEquals("Supplier A", all[0].name)
        assertEquals("Supplier B", all[1].name)
        assertEquals("Supplier C", all[2].name)
        assertEquals("Supplier D", all[3].name)
    }

    @Test
    fun `should_workCorrectly_when_upsertingForSyncOperations`() = runTest {
        // Arrange - Simulate first sync from server
        val supplier = createTestSupplier(
            name = "Acme Pharmaceuticals",
            phone = "111111",
            isActive = true
        )
        repository.upsert(supplier)

        // Act & Assert - Verify initial sync
        var result = repository.getById(supplier.id)
        assertNotNull(result)
        assertEquals("Acme Pharmaceuticals", result.name)
        assertEquals("111111", result.phone)

        // Act & Assert - Simulate subsequent sync with updated data
        val updatedSupplier = supplier.copy(
            name = "Acme Pharmaceuticals Inc.",
            phone = "222222",
            email = "updated@acme.com",
            updatedAt = 1705690000000L
        )
        repository.upsert(updatedSupplier)
        result = repository.getById(supplier.id)

        // Assert - All fields should be updated
        assertNotNull(result)
        assertEquals("Acme Pharmaceuticals Inc.", result.name)
        assertEquals("222222", result.phone)
        assertEquals("updated@acme.com", result.email)
    }
}
