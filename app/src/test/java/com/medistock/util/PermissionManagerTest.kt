package com.medistock.util

import com.medistock.data.dao.UserPermissionDao
import com.medistock.data.entities.UserPermission
import com.medistock.shared.domain.model.Module
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class PermissionManagerTest {

    private lateinit var permissionManager: PermissionManager
    private lateinit var userPermissionDao: UserPermissionDao
    private lateinit var authManager: AuthManager

    @Before
    fun setup() {
        userPermissionDao = mock()
        authManager = mock()
        permissionManager = PermissionManager(userPermissionDao, authManager)
    }

    @Test
    fun canView_adminUser_returnsTrue() = runTest {
        // Given
        whenever(authManager.isAdmin()).thenReturn(true)

        // When
        val result = permissionManager.canView(Module.STOCK)

        // Then
        assertTrue(result)
        verify(userPermissionDao, never()).getPermissionForModule(any(), any())
    }

    @Test
    fun canView_nonAdminWithPermission_returnsTrue() = runTest {
        // Given
        whenever(authManager.isAdmin()).thenReturn(false)
        whenever(authManager.getUserId()).thenReturn("user-1")
        val permission = UserPermission(
            id = "perm-1",
            userId = "user-1",
            module = Module.STOCK.name,
            canView = true,
            canCreate = false,
            canEdit = false,
            canDelete = false
        )
        whenever(userPermissionDao.getPermissionForModule("user-1", Module.STOCK.name))
            .thenReturn(permission)

        // When
        val result = permissionManager.canView(Module.STOCK)

        // Then
        assertTrue(result)
    }

    @Test
    fun canView_nonAdminWithoutPermission_returnsFalse() = runTest {
        // Given
        whenever(authManager.isAdmin()).thenReturn(false)
        whenever(authManager.getUserId()).thenReturn("user-1")
        val permission = UserPermission(
            id = "perm-1",
            userId = "user-1",
            module = Module.STOCK.name,
            canView = false,
            canCreate = false,
            canEdit = false,
            canDelete = false
        )
        whenever(userPermissionDao.getPermissionForModule("user-1", Module.STOCK.name))
            .thenReturn(permission)

        // When
        val result = permissionManager.canView(Module.STOCK)

        // Then
        assertFalse(result)
    }

    @Test
    fun canView_noUserId_returnsFalse() = runTest {
        // Given
        whenever(authManager.isAdmin()).thenReturn(false)
        whenever(authManager.getUserId()).thenReturn(null)

        // When
        val result = permissionManager.canView(Module.STOCK)

        // Then
        assertFalse(result)
    }

    @Test
    fun canView_noPermissionRecord_returnsFalse() = runTest {
        // Given
        whenever(authManager.isAdmin()).thenReturn(false)
        whenever(authManager.getUserId()).thenReturn("user-1")
        whenever(userPermissionDao.getPermissionForModule("user-1", Module.STOCK.name))
            .thenReturn(null)

        // When
        val result = permissionManager.canView(Module.STOCK)

        // Then
        assertFalse(result)
    }

    @Test
    fun canCreate_adminUser_returnsTrue() = runTest {
        // Given
        whenever(authManager.isAdmin()).thenReturn(true)

        // When
        val result = permissionManager.canCreate(Module.SALES)

        // Then
        assertTrue(result)
    }

    @Test
    fun canCreate_nonAdminWithPermission_returnsTrue() = runTest {
        // Given
        whenever(authManager.isAdmin()).thenReturn(false)
        whenever(authManager.getUserId()).thenReturn("user-1")
        val permission = UserPermission(
            id = "perm-1",
            userId = "user-1",
            module = Module.SALES.name,
            canView = true,
            canCreate = true,
            canEdit = false,
            canDelete = false
        )
        whenever(userPermissionDao.getPermissionForModule("user-1", Module.SALES.name))
            .thenReturn(permission)

        // When
        val result = permissionManager.canCreate(Module.SALES)

        // Then
        assertTrue(result)
    }

    @Test
    fun canEdit_adminUser_returnsTrue() = runTest {
        // Given
        whenever(authManager.isAdmin()).thenReturn(true)

        // When
        val result = permissionManager.canEdit(Module.PURCHASES)

        // Then
        assertTrue(result)
    }

    @Test
    fun canEdit_nonAdminWithPermission_returnsTrue() = runTest {
        // Given
        whenever(authManager.isAdmin()).thenReturn(false)
        whenever(authManager.getUserId()).thenReturn("user-1")
        val permission = UserPermission(
            id = "perm-1",
            userId = "user-1",
            module = Module.PURCHASES.name,
            canView = true,
            canCreate = true,
            canEdit = true,
            canDelete = false
        )
        whenever(userPermissionDao.getPermissionForModule("user-1", Module.PURCHASES.name))
            .thenReturn(permission)

        // When
        val result = permissionManager.canEdit(Module.PURCHASES)

        // Then
        assertTrue(result)
    }

    @Test
    fun canDelete_adminUser_returnsTrue() = runTest {
        // Given
        whenever(authManager.isAdmin()).thenReturn(true)

        // When
        val result = permissionManager.canDelete(Module.INVENTORY)

        // Then
        assertTrue(result)
    }

    @Test
    fun canDelete_nonAdminWithPermission_returnsTrue() = runTest {
        // Given
        whenever(authManager.isAdmin()).thenReturn(false)
        whenever(authManager.getUserId()).thenReturn("user-1")
        val permission = UserPermission(
            id = "perm-1",
            userId = "user-1",
            module = Module.INVENTORY.name,
            canView = true,
            canCreate = true,
            canEdit = true,
            canDelete = true
        )
        whenever(userPermissionDao.getPermissionForModule("user-1", Module.INVENTORY.name))
            .thenReturn(permission)

        // When
        val result = permissionManager.canDelete(Module.INVENTORY)

        // Then
        assertTrue(result)
    }

    @Test
    fun getUserPermissions_returnsAllPermissions() = runTest {
        // Given
        val userId = "user-1"
        whenever(authManager.getUserId()).thenReturn(userId)
        val permissions = listOf(
            UserPermission(
                id = "perm-1",
                userId = userId,
                module = Module.STOCK.name,
                canView = true,
                canCreate = true,
                canEdit = false,
                canDelete = false
            ),
            UserPermission(
                id = "perm-2",
                userId = userId,
                module = Module.SALES.name,
                canView = true,
                canCreate = false,
                canEdit = false,
                canDelete = false
            )
        )
        whenever(userPermissionDao.getPermissionsForUser(userId)).thenReturn(permissions)

        // When
        val result = permissionManager.getUserPermissions()

        // Then
        assertEquals(2, result.size)
        assertEquals(Module.STOCK.name, result[0].module)
        assertEquals(Module.SALES.name, result[1].module)
    }

    @Test
    fun getUserPermissions_noUserId_returnsEmptyList() = runTest {
        // Given
        whenever(authManager.getUserId()).thenReturn(null)

        // When
        val result = permissionManager.getUserPermissions()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun allPermissionChecks_testAllModules() = runTest {
        // Given
        whenever(authManager.isAdmin()).thenReturn(true)

        // When/Then - Test all modules
        assertTrue(permissionManager.canView(Module.STOCK))
        assertTrue(permissionManager.canView(Module.SALES))
        assertTrue(permissionManager.canView(Module.PURCHASES))
        assertTrue(permissionManager.canView(Module.INVENTORY))
        assertTrue(permissionManager.canView(Module.TRANSFERS))
        assertTrue(permissionManager.canView(Module.ADMIN))
        assertTrue(permissionManager.canView(Module.PRODUCTS))
        assertTrue(permissionManager.canView(Module.SITES))
        assertTrue(permissionManager.canView(Module.CATEGORIES))
        assertTrue(permissionManager.canView(Module.USERS))
    }

    @Test
    fun permissionHierarchy_viewDoesNotImplyCreate() = runTest {
        // Given
        whenever(authManager.isAdmin()).thenReturn(false)
        whenever(authManager.getUserId()).thenReturn("user-1")
        val permission = UserPermission(
            id = "perm-1",
            userId = "user-1",
            module = Module.PRODUCTS.name,
            canView = true,
            canCreate = false,
            canEdit = false,
            canDelete = false
        )
        whenever(userPermissionDao.getPermissionForModule("user-1", Module.PRODUCTS.name))
            .thenReturn(permission)

        // When/Then
        assertTrue(permissionManager.canView(Module.PRODUCTS))
        assertFalse(permissionManager.canCreate(Module.PRODUCTS))
        assertFalse(permissionManager.canEdit(Module.PRODUCTS))
        assertFalse(permissionManager.canDelete(Module.PRODUCTS))
    }
}
