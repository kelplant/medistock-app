package com.medistock.shared.data.repository

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.medistock.shared.db.MedistockDatabase
import com.medistock.shared.domain.notification.NotificationEvent
import com.medistock.shared.domain.notification.NotificationPriority
import com.medistock.shared.domain.notification.NotificationType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Unit tests for NotificationRepository.
 * Tests cover CRUD operations, filtering, state management, and Flow observers.
 */
class NotificationRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var database: MedistockDatabase
    private lateinit var repository: NotificationRepository

    @BeforeTest
    fun setUp() {
        // Create in-memory SQLite database for testing
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        MedistockDatabase.Schema.create(driver)
        database = MedistockDatabase(driver)
        repository = NotificationRepository(database)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    // ===== Helper Functions =====

    private fun createTestNotification(
        id: String = "notif-1",
        type: NotificationType = NotificationType.LOW_STOCK,
        priority: NotificationPriority = NotificationPriority.MEDIUM,
        title: String = "Test Notification",
        message: String = "Test message",
        referenceId: String? = "ref-1",
        referenceType: String? = "product",
        siteId: String? = "site-1",
        deepLink: String? = null,
        createdAt: Long = 1705680000000L,
        isDisplayed: Boolean = false,
        isDismissed: Boolean = false
    ): NotificationEvent {
        return NotificationEvent(
            id = id,
            type = type,
            priority = priority,
            title = title,
            message = message,
            referenceId = referenceId,
            referenceType = referenceType,
            siteId = siteId,
            deepLink = deepLink,
            createdAt = createdAt,
            isDisplayed = isDisplayed,
            isDismissed = isDismissed
        )
    }

    // ===== Insert Tests =====

    @Test
    fun `should_insertNotification_when_validDataProvided`() = runTest {
        // Arrange
        val notification = createTestNotification()

        // Act
        repository.insert(notification)
        val result = repository.getById(notification.id)

        // Assert
        assertNotNull(result)
        assertEquals(notification.id, result.id)
        assertEquals(notification.title, result.title)
        assertEquals(notification.message, result.message)
        assertEquals(notification.type, result.type)
        assertEquals(notification.priority, result.priority)
        assertEquals(notification.referenceId, result.referenceId)
        assertEquals(notification.siteId, result.siteId)
        assertEquals(notification.createdAt, result.createdAt)
        assertEquals(notification.isDisplayed, result.isDisplayed)
        assertEquals(notification.isDismissed, result.isDismissed)
    }

    @Test
    fun `should_insertMultipleNotifications_when_differentIds`() = runTest {
        // Arrange
        val notification1 = createTestNotification(id = "notif-1", title = "First")
        val notification2 = createTestNotification(id = "notif-2", title = "Second")

        // Act
        repository.insert(notification1)
        repository.insert(notification2)
        val all = repository.getAll()

        // Assert
        assertEquals(2, all.size)
        assertTrue(all.any { it.id == "notif-1" })
        assertTrue(all.any { it.id == "notif-2" })
    }

    @Test
    fun `should_preserveDisplayedState_when_insertingNotification`() = runTest {
        // Arrange
        val notification = createTestNotification(isDisplayed = true, isDismissed = true)

        // Act
        repository.insert(notification)
        val result = repository.getById(notification.id)

        // Assert
        assertNotNull(result)
        assertTrue(result.isDisplayed)
        assertTrue(result.isDismissed)
    }

    @Test
    fun `should_insertNotificationWithNullFields_when_optionalFieldsNull`() = runTest {
        // Arrange
        val notification = createTestNotification(
            referenceId = null,
            referenceType = null,
            siteId = null,
            deepLink = null
        )

        // Act
        repository.insert(notification)
        val result = repository.getById(notification.id)

        // Assert
        assertNotNull(result)
        assertNull(result.referenceId)
        assertNull(result.referenceType)
        assertNull(result.siteId)
        assertNull(result.deepLink)
    }

    // ===== Upsert Tests =====

    @Test
    fun `should_insertNewNotification_when_upsertingNonExistingId`() = runTest {
        // Arrange
        val notification = createTestNotification()

        // Act
        repository.upsert(notification)
        val result = repository.getById(notification.id)

        // Assert
        assertNotNull(result)
        assertEquals(notification.id, result.id)
        assertEquals(notification.title, result.title)
    }

    @Test
    fun `should_preserveDisplayedState_when_upsertingExistingNotification`() = runTest {
        // Arrange
        val original = createTestNotification(
            title = "Original",
            isDisplayed = true,
            isDismissed = false
        )
        repository.insert(original)

        // Act - Upsert with updated content but no display state
        val updated = createTestNotification(
            title = "Updated",
            isDisplayed = false,
            isDismissed = false
        )
        repository.upsert(updated)
        val result = repository.getById(original.id)

        // Assert - Display state should be preserved from original
        assertNotNull(result)
        assertEquals("Updated", result.title)
        assertTrue(result.isDisplayed, "isDisplayed should be preserved from original")
        assertFalse(result.isDismissed)
    }

    @Test
    fun `should_preserveDismissedState_when_upsertingExistingNotification`() = runTest {
        // Arrange
        val original = createTestNotification(
            title = "Original",
            isDisplayed = true,
            isDismissed = true
        )
        repository.insert(original)

        // Act
        val updated = createTestNotification(
            title = "Updated",
            isDisplayed = false,
            isDismissed = false
        )
        repository.upsert(updated)
        val result = repository.getById(original.id)

        // Assert - Both states should be preserved
        assertNotNull(result)
        assertEquals("Updated", result.title)
        assertTrue(result.isDisplayed, "isDisplayed should be preserved")
        assertTrue(result.isDismissed, "isDismissed should be preserved")
    }

    @Test
    fun `should_setDefaultStates_when_upsertingNewNotification`() = runTest {
        // Arrange
        val notification = createTestNotification()

        // Act
        repository.upsert(notification)
        val result = repository.getById(notification.id)

        // Assert - New notification should have default (false) states
        assertNotNull(result)
        assertFalse(result.isDisplayed)
        assertFalse(result.isDismissed)
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
    fun `should_returnAllNotifications_when_multipleExist`() = runTest {
        // Arrange
        repository.insert(createTestNotification(id = "notif-1", createdAt = 1000L))
        repository.insert(createTestNotification(id = "notif-2", createdAt = 2000L))
        repository.insert(createTestNotification(id = "notif-3", createdAt = 3000L))

        // Act
        val result = repository.getAll()

        // Assert
        assertEquals(3, result.size)
        // Verify order: most recent first (DESC)
        assertEquals("notif-3", result[0].id)
        assertEquals("notif-2", result[1].id)
        assertEquals("notif-1", result[2].id)
    }

    @Test
    fun `should_returnOnlyUndisplayed_when_getUndisplayed`() = runTest {
        // Arrange
        repository.insert(createTestNotification(id = "notif-1", isDisplayed = false, isDismissed = false))
        repository.insert(createTestNotification(id = "notif-2", isDisplayed = true, isDismissed = false))
        repository.insert(createTestNotification(id = "notif-3", isDisplayed = false, isDismissed = false))

        // Act
        val result = repository.getUndisplayed()

        // Assert
        assertEquals(2, result.size)
        assertTrue(result.all { !it.isDisplayed && !it.isDismissed })
        assertTrue(result.any { it.id == "notif-1" })
        assertTrue(result.any { it.id == "notif-3" })
    }

    @Test
    fun `should_excludeDismissed_when_getUndisplayed`() = runTest {
        // Arrange
        repository.insert(createTestNotification(id = "notif-1", isDisplayed = false, isDismissed = true))
        repository.insert(createTestNotification(id = "notif-2", isDisplayed = false, isDismissed = false))

        // Act
        val result = repository.getUndisplayed()

        // Assert
        assertEquals(1, result.size)
        assertEquals("notif-2", result[0].id)
    }

    @Test
    fun `should_returnOnlyUndismissed_when_getUndismissed`() = runTest {
        // Arrange
        repository.insert(createTestNotification(id = "notif-1", isDismissed = false))
        repository.insert(createTestNotification(id = "notif-2", isDismissed = true))
        repository.insert(createTestNotification(id = "notif-3", isDismissed = false))

        // Act
        val result = repository.getUndismissed()

        // Assert
        assertEquals(2, result.size)
        assertTrue(result.all { !it.isDismissed })
        assertTrue(result.any { it.id == "notif-1" })
        assertTrue(result.any { it.id == "notif-3" })
    }

    @Test
    fun `should_includeDisplayed_when_getUndismissed`() = runTest {
        // Arrange
        repository.insert(createTestNotification(id = "notif-1", isDisplayed = true, isDismissed = false))
        repository.insert(createTestNotification(id = "notif-2", isDisplayed = false, isDismissed = false))

        // Act
        val result = repository.getUndismissed()

        // Assert
        assertEquals(2, result.size)
        assertTrue(result.any { it.id == "notif-1" && it.isDisplayed })
        assertTrue(result.any { it.id == "notif-2" && !it.isDisplayed })
    }

    @Test
    fun `should_returnByType_when_getByTypeWithMatchingNotifications`() = runTest {
        // Arrange
        repository.insert(createTestNotification(id = "notif-1", type = NotificationType.LOW_STOCK))
        repository.insert(createTestNotification(id = "notif-2", type = NotificationType.PRODUCT_EXPIRED))
        repository.insert(createTestNotification(id = "notif-3", type = NotificationType.LOW_STOCK))

        // Act
        val result = repository.getByType(NotificationType.LOW_STOCK)

        // Assert
        assertEquals(2, result.size)
        assertTrue(result.all { it.type == NotificationType.LOW_STOCK })
    }

    @Test
    fun `should_returnEmptyList_when_getByTypeWithNoMatches`() = runTest {
        // Arrange
        repository.insert(createTestNotification(type = NotificationType.LOW_STOCK))

        // Act
        val result = repository.getByType(NotificationType.PRODUCT_EXPIRED)

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun `should_returnBySite_when_getBySiteWithMatchingNotifications`() = runTest {
        // Arrange
        repository.insert(createTestNotification(id = "notif-1", siteId = "site-1"))
        repository.insert(createTestNotification(id = "notif-2", siteId = "site-2"))
        repository.insert(createTestNotification(id = "notif-3", siteId = "site-1"))

        // Act
        val result = repository.getBySite("site-1")

        // Assert
        assertEquals(2, result.size)
        assertTrue(result.all { it.siteId == "site-1" })
    }

    @Test
    fun `should_returnEmptyList_when_getBySiteWithNoMatches`() = runTest {
        // Arrange
        repository.insert(createTestNotification(siteId = "site-1"))

        // Act
        val result = repository.getBySite("site-2")

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun `should_handleNullSiteId_when_getBySite`() = runTest {
        // Arrange
        repository.insert(createTestNotification(id = "notif-1", siteId = null))
        repository.insert(createTestNotification(id = "notif-2", siteId = "site-1"))

        // Act
        val result = repository.getBySite("site-1")

        // Assert
        assertEquals(1, result.size)
        assertEquals("notif-2", result[0].id)
    }

    // ===== Count Tests =====

    @Test
    fun `should_returnZero_when_countUndisplayedWithNoNotifications`() = runTest {
        // Act
        val count = repository.countUndisplayed()

        // Assert
        assertEquals(0L, count)
    }

    @Test
    fun `should_returnCorrectCount_when_countUndisplayed`() = runTest {
        // Arrange
        repository.insert(createTestNotification(id = "notif-1", isDisplayed = false, isDismissed = false))
        repository.insert(createTestNotification(id = "notif-2", isDisplayed = true, isDismissed = false))
        repository.insert(createTestNotification(id = "notif-3", isDisplayed = false, isDismissed = false))
        repository.insert(createTestNotification(id = "notif-4", isDisplayed = false, isDismissed = true))

        // Act
        val count = repository.countUndisplayed()

        // Assert
        assertEquals(2L, count)
    }

    @Test
    fun `should_excludeDismissed_when_countUndisplayed`() = runTest {
        // Arrange
        repository.insert(createTestNotification(id = "notif-1", isDisplayed = false, isDismissed = true))

        // Act
        val count = repository.countUndisplayed()

        // Assert
        assertEquals(0L, count)
    }

    @Test
    fun `should_returnZero_when_countUndismissedWithNoNotifications`() = runTest {
        // Act
        val count = repository.countUndismissed()

        // Assert
        assertEquals(0L, count)
    }

    @Test
    fun `should_returnCorrectCount_when_countUndismissed`() = runTest {
        // Arrange
        repository.insert(createTestNotification(id = "notif-1", isDismissed = false))
        repository.insert(createTestNotification(id = "notif-2", isDismissed = true))
        repository.insert(createTestNotification(id = "notif-3", isDismissed = false))

        // Act
        val count = repository.countUndismissed()

        // Assert
        assertEquals(2L, count)
    }

    @Test
    fun `should_includeDisplayed_when_countUndismissed`() = runTest {
        // Arrange
        repository.insert(createTestNotification(id = "notif-1", isDisplayed = true, isDismissed = false))
        repository.insert(createTestNotification(id = "notif-2", isDisplayed = false, isDismissed = false))

        // Act
        val count = repository.countUndismissed()

        // Assert
        assertEquals(2L, count)
    }

    // ===== Update State Tests =====

    @Test
    fun `should_markAsDisplayed_when_notificationExists`() = runTest {
        // Arrange
        val notification = createTestNotification(isDisplayed = false)
        repository.insert(notification)

        // Act
        repository.markAsDisplayed(notification.id)
        val result = repository.getById(notification.id)

        // Assert
        assertNotNull(result)
        assertTrue(result.isDisplayed)
    }

    @Test
    fun `should_notAffectOtherFields_when_markAsDisplayed`() = runTest {
        // Arrange
        val notification = createTestNotification(isDisplayed = false, isDismissed = false)
        repository.insert(notification)

        // Act
        repository.markAsDisplayed(notification.id)
        val result = repository.getById(notification.id)

        // Assert
        assertNotNull(result)
        assertTrue(result.isDisplayed)
        assertFalse(result.isDismissed)
        assertEquals(notification.title, result.title)
        assertEquals(notification.message, result.message)
    }

    @Test
    fun `should_markAsDismissed_when_notificationExists`() = runTest {
        // Arrange
        val notification = createTestNotification(isDismissed = false)
        repository.insert(notification)

        // Act
        repository.markAsDismissed(notification.id)
        val result = repository.getById(notification.id)

        // Assert
        assertNotNull(result)
        assertTrue(result.isDismissed)
    }

    @Test
    fun `should_notAffectOtherFields_when_markAsDismissed`() = runTest {
        // Arrange
        val notification = createTestNotification(isDisplayed = true, isDismissed = false)
        repository.insert(notification)

        // Act
        repository.markAsDismissed(notification.id)
        val result = repository.getById(notification.id)

        // Assert
        assertNotNull(result)
        assertTrue(result.isDismissed)
        assertTrue(result.isDisplayed)
        assertEquals(notification.title, result.title)
    }

    @Test
    fun `should_dismissAll_when_multipleNotificationsExist`() = runTest {
        // Arrange
        repository.insert(createTestNotification(id = "notif-1", isDismissed = false))
        repository.insert(createTestNotification(id = "notif-2", isDismissed = false))
        repository.insert(createTestNotification(id = "notif-3", isDismissed = true))

        // Act
        repository.dismissAll()
        val all = repository.getAll()

        // Assert
        assertEquals(3, all.size)
        assertTrue(all.all { it.isDismissed })
    }

    @Test
    fun `should_returnZero_when_countUndismissedAfterDismissAll`() = runTest {
        // Arrange
        repository.insert(createTestNotification(id = "notif-1", isDismissed = false))
        repository.insert(createTestNotification(id = "notif-2", isDismissed = false))

        // Act
        repository.dismissAll()
        val count = repository.countUndismissed()

        // Assert
        assertEquals(0L, count)
    }

    // ===== Delete Tests =====

    @Test
    fun `should_deleteNotification_when_idExists`() = runTest {
        // Arrange
        val notification = createTestNotification()
        repository.insert(notification)

        // Act
        repository.delete(notification.id)
        val result = repository.getById(notification.id)

        // Assert
        assertNull(result)
    }

    @Test
    fun `should_notAffectOthers_when_deleteOneNotification`() = runTest {
        // Arrange
        repository.insert(createTestNotification(id = "notif-1"))
        repository.insert(createTestNotification(id = "notif-2"))

        // Act
        repository.delete("notif-1")
        val all = repository.getAll()

        // Assert
        assertEquals(1, all.size)
        assertEquals("notif-2", all[0].id)
    }

    @Test
    fun `should_doNothing_when_deleteNonExistingId`() = runTest {
        // Arrange
        repository.insert(createTestNotification(id = "notif-1"))

        // Act
        repository.delete("non-existent")
        val all = repository.getAll()

        // Assert
        assertEquals(1, all.size)
    }

    @Test
    fun `should_deleteOldNotifications_when_olderThanTimestamp`() = runTest {
        // Arrange
        val now = 1705680000000L
        val oneHourAgo = now - 3600000L
        val twoDaysAgo = now - (2 * 24 * 3600000L)

        repository.insert(createTestNotification(id = "notif-recent", createdAt = now))
        repository.insert(createTestNotification(id = "notif-hour", createdAt = oneHourAgo))
        repository.insert(createTestNotification(id = "notif-old", createdAt = twoDaysAgo))

        // Act - Delete notifications older than 1 day
        val oneDayAgo = now - (24 * 3600000L)
        repository.deleteOld(oneDayAgo)
        val remaining = repository.getAll()

        // Assert
        assertEquals(2, remaining.size)
        assertTrue(remaining.any { it.id == "notif-recent" })
        assertTrue(remaining.any { it.id == "notif-hour" })
        assertFalse(remaining.any { it.id == "notif-old" })
    }

    @Test
    fun `should_deleteAllOld_when_allOlderThanTimestamp`() = runTest {
        // Arrange
        val now = 1705680000000L
        val old1 = now - (10 * 24 * 3600000L)
        val old2 = now - (20 * 24 * 3600000L)

        repository.insert(createTestNotification(id = "notif-1", createdAt = old1))
        repository.insert(createTestNotification(id = "notif-2", createdAt = old2))

        // Act
        repository.deleteOld(now)
        val remaining = repository.getAll()

        // Assert
        assertTrue(remaining.isEmpty())
    }

    @Test
    fun `should_deleteNone_when_noNotificationsOlderThanTimestamp`() = runTest {
        // Arrange
        val now = 1705680000000L
        repository.insert(createTestNotification(id = "notif-1", createdAt = now))

        // Act
        val veryOld = now - (365 * 24 * 3600000L)
        repository.deleteOld(veryOld)
        val remaining = repository.getAll()

        // Assert
        assertEquals(1, remaining.size)
    }

    @Test
    fun `should_deleteDismissedOnly_when_deleteDismissed`() = runTest {
        // Arrange
        repository.insert(createTestNotification(id = "notif-1", isDismissed = true))
        repository.insert(createTestNotification(id = "notif-2", isDismissed = false))
        repository.insert(createTestNotification(id = "notif-3", isDismissed = true))

        // Act
        repository.deleteDismissed()
        val remaining = repository.getAll()

        // Assert
        assertEquals(1, remaining.size)
        assertEquals("notif-2", remaining[0].id)
        assertFalse(remaining[0].isDismissed)
    }

    @Test
    fun `should_deleteAll_when_allAreDismissed`() = runTest {
        // Arrange
        repository.insert(createTestNotification(id = "notif-1", isDismissed = true))
        repository.insert(createTestNotification(id = "notif-2", isDismissed = true))

        // Act
        repository.deleteDismissed()
        val remaining = repository.getAll()

        // Assert
        assertTrue(remaining.isEmpty())
    }

    @Test
    fun `should_deleteNone_when_noDismissedNotifications`() = runTest {
        // Arrange
        repository.insert(createTestNotification(id = "notif-1", isDismissed = false))
        repository.insert(createTestNotification(id = "notif-2", isDismissed = false))

        // Act
        repository.deleteDismissed()
        val remaining = repository.getAll()

        // Assert
        assertEquals(2, remaining.size)
    }

    // ===== Flow Observer Tests =====

    @Test
    fun `should_emitAllNotifications_when_observeAll`() = runTest {
        // Arrange
        repository.insert(createTestNotification(id = "notif-1"))
        repository.insert(createTestNotification(id = "notif-2"))

        // Act
        val result = repository.observeAll().first()

        // Assert
        assertEquals(2, result.size)
        assertTrue(result.any { it.id == "notif-1" })
        assertTrue(result.any { it.id == "notif-2" })
    }

    @Test
    fun `should_emitUpdates_when_observeAllAndDataChanges`() = runTest {
        // Arrange
        repository.insert(createTestNotification(id = "notif-1"))

        // Act
        val initial = repository.observeAll().first()
        repository.insert(createTestNotification(id = "notif-2"))
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
    fun `should_emitOnlyUndismissed_when_observeUndismissed`() = runTest {
        // Arrange
        repository.insert(createTestNotification(id = "notif-1", isDismissed = false))
        repository.insert(createTestNotification(id = "notif-2", isDismissed = true))
        repository.insert(createTestNotification(id = "notif-3", isDismissed = false))

        // Act
        val result = repository.observeUndismissed().first()

        // Assert
        assertEquals(2, result.size)
        assertTrue(result.all { !it.isDismissed })
        assertTrue(result.any { it.id == "notif-1" })
        assertTrue(result.any { it.id == "notif-3" })
    }

    @Test
    fun `should_emitUpdates_when_observeUndismissedAndDismissed`() = runTest {
        // Arrange
        repository.insert(createTestNotification(id = "notif-1", isDismissed = false))

        // Act
        val initial = repository.observeUndismissed().first()
        repository.markAsDismissed("notif-1")
        val updated = repository.observeUndismissed().first()

        // Assert
        assertEquals(1, initial.size)
        assertEquals(0, updated.size)
    }

    // ===== Edge Cases =====

    @Test
    fun `should_handleAllNotificationTypes_when_inserting`() = runTest {
        // Arrange & Act
        NotificationType.entries.forEachIndexed { index, type ->
            repository.insert(createTestNotification(id = "notif-$index", type = type))
        }

        val all = repository.getAll()

        // Assert
        assertEquals(NotificationType.entries.size, all.size)
        NotificationType.entries.forEach { type ->
            assertTrue(all.any { it.type == type })
        }
    }

    @Test
    fun `should_handleAllPriorityLevels_when_inserting`() = runTest {
        // Arrange & Act
        NotificationPriority.entries.forEachIndexed { index, priority ->
            repository.insert(createTestNotification(id = "notif-$index", priority = priority))
        }

        val all = repository.getAll()

        // Assert
        assertEquals(NotificationPriority.entries.size, all.size)
        NotificationPriority.entries.forEach { priority ->
            assertTrue(all.any { it.priority == priority })
        }
    }

    @Test
    fun `should_handleEmptyStrings_when_insertingNotification`() = runTest {
        // Arrange
        val notification = createTestNotification(
            title = "",
            message = "",
            referenceType = ""
        )

        // Act
        repository.insert(notification)
        val result = repository.getById(notification.id)

        // Assert
        assertNotNull(result)
        assertEquals("", result.title)
        assertEquals("", result.message)
        assertEquals("", result.referenceType)
    }

    @Test
    fun `should_handleLongStrings_when_insertingNotification`() = runTest {
        // Arrange
        val longString = "x".repeat(1000)
        val notification = createTestNotification(
            title = longString,
            message = longString
        )

        // Act
        repository.insert(notification)
        val result = repository.getById(notification.id)

        // Assert
        assertNotNull(result)
        assertEquals(longString, result.title)
        assertEquals(longString, result.message)
    }

    @Test
    fun `should_handleSpecialCharacters_when_insertingNotification`() = runTest {
        // Arrange
        val specialChars = "Special: <>&\"'éàù çñ 中文 🔔"
        val notification = createTestNotification(
            title = specialChars,
            message = specialChars
        )

        // Act
        repository.insert(notification)
        val result = repository.getById(notification.id)

        // Assert
        assertNotNull(result)
        assertEquals(specialChars, result.title)
        assertEquals(specialChars, result.message)
    }

    @Test
    fun `should_handleZeroTimestamp_when_insertingNotification`() = runTest {
        // Arrange
        val notification = createTestNotification(createdAt = 0L)

        // Act
        repository.insert(notification)
        val result = repository.getById(notification.id)

        // Assert
        assertNotNull(result)
        assertEquals(0L, result.createdAt)
    }

    @Test
    fun `should_handleMaxLongTimestamp_when_insertingNotification`() = runTest {
        // Arrange
        val notification = createTestNotification(createdAt = Long.MAX_VALUE)

        // Act
        repository.insert(notification)
        val result = repository.getById(notification.id)

        // Assert
        assertNotNull(result)
        assertEquals(Long.MAX_VALUE, result.createdAt)
    }

    // ===== Integration Scenarios =====

    @Test
    fun `should_workCorrectly_when_completeNotificationLifecycle`() = runTest {
        // Arrange - Insert new notification
        val notification = createTestNotification(isDisplayed = false, isDismissed = false)
        repository.insert(notification)

        // Act & Assert - Verify initial state
        var result = repository.getById(notification.id)
        assertNotNull(result)
        assertFalse(result.isDisplayed)
        assertFalse(result.isDismissed)

        // Act & Assert - Mark as displayed
        repository.markAsDisplayed(notification.id)
        result = repository.getById(notification.id)
        assertNotNull(result)
        assertTrue(result.isDisplayed)
        assertFalse(result.isDismissed)

        // Act & Assert - Mark as dismissed
        repository.markAsDismissed(notification.id)
        result = repository.getById(notification.id)
        assertNotNull(result)
        assertTrue(result.isDisplayed)
        assertTrue(result.isDismissed)

        // Act & Assert - Delete
        repository.delete(notification.id)
        result = repository.getById(notification.id)
        assertNull(result)
    }

    @Test
    fun `should_maintainCounts_when_changingNotificationStates`() = runTest {
        // Arrange
        repository.insert(createTestNotification(id = "notif-1", isDisplayed = false, isDismissed = false))
        repository.insert(createTestNotification(id = "notif-2", isDisplayed = false, isDismissed = false))

        // Act & Assert - Initial counts
        assertEquals(2L, repository.countUndisplayed())
        assertEquals(2L, repository.countUndismissed())

        // Act & Assert - Mark one as displayed
        repository.markAsDisplayed("notif-1")
        assertEquals(1L, repository.countUndisplayed())
        assertEquals(2L, repository.countUndismissed())

        // Act & Assert - Mark one as dismissed
        repository.markAsDismissed("notif-2")
        assertEquals(0L, repository.countUndisplayed())
        assertEquals(1L, repository.countUndismissed())

        // Act & Assert - Mark last as dismissed
        repository.markAsDismissed("notif-1")
        assertEquals(0L, repository.countUndisplayed())
        assertEquals(0L, repository.countUndismissed())
    }

    @Test
    fun `should_workCorrectly_when_upsertingAndPreservingStatesInRealScenario`() = runTest {
        // Arrange - Simulate first sync from server
        val notification = createTestNotification(
            title = "Low Stock Alert",
            message = "Product ABC is running low"
        )
        repository.upsert(notification)

        // Act & Assert - User views notification
        repository.markAsDisplayed(notification.id)
        var result = repository.getById(notification.id)
        assertNotNull(result)
        assertTrue(result.isDisplayed)

        // Act & Assert - Server sends updated notification (sync)
        val updatedNotification = notification.copy(
            message = "Product ABC is critically low - only 2 units remaining"
        )
        repository.upsert(updatedNotification)
        result = repository.getById(notification.id)

        // Assert - Display state should be preserved despite content update
        assertNotNull(result)
        assertEquals("Product ABC is critically low - only 2 units remaining", result.message)
        assertTrue(result.isDisplayed, "Display state should be preserved during sync")
    }
}
