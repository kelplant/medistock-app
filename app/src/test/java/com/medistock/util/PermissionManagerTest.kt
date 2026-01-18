package com.medistock.util

import com.medistock.data.dao.UserPermissionDao
import com.medistock.data.entities.UserPermission
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
        val result = permissionManager.canView(Modules.STOCK)

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
            module = Modules.STOCK,
            canView = true,
            canCreate = false,
            canEdit = false,
            canDelete = false
        )
        whenever(userPermissionDao.getPermissionForModule("user-1", Modules.STOCK))
            .thenReturn(permission)

        // When
        val result = permissionManager.canView(Modules.STOCK)

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
            module = Modules.STOCK,
            canView = false,
            canCreate = false,
            canEdit = false,
            canDelete = false
        )
        whenever(userPermissionDao.getPermissionForModule("user-1", Modules.STOCK))
            .thenReturn(permission)

        // When
        val result = permissionManager.canView(Modules.STOCK)

        // Then
        assertFalse(result)
    }

    @Test
    fun canView_noUserId_returnsFalse() = runTest {
        // Given
        whenever(authManager.isAdmin()).thenReturn(false)
        whenever(authManager.getUserId()).thenReturn(null)

        // When
        val result = permissionManager.canView(Modules.STOCK)

        // Then
        assertFalse(result)
    }

    @Test
    fun canView_noPermissionRecord_returnsFalse() = runTest {
        // Given
        whenever(authManager.isAdmin()).thenReturn(false)
        whenever(authManager.getUserId()).thenReturn("user-1")
        whenever(userPermissionDao.getPermissionForModule("user-1", Modules.STOCK))
            .thenReturn(null)

        // When
        val result = permissionManager.canView(Modules.STOCK)

        // Then
        assertFalse(result)
    }

    @Test
    fun canCreate_adminUser_returnsTrue() = runTest {
        // Given
        whenever(authManager.isAdmin()).thenReturn(true)

        // When
        val result = permissionManager.canCreate(Modules.SALES)

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
            module = Modules.SALES,
            canView = true,
            canCreate = true,
            canEdit = false,
            canDelete = false
        )
        whenever(userPermissionDao.getPermissionForModule("user-1", Modules.SALES))
            .thenReturn(permission)

        // When
        val result = permissionManager.canCreate(Modules.SALES)

        // Then
        assertTrue(result)
    }

    @Test
    fun canEdit_adminUser_returnsTrue() = runTest {
        // Given
        whenever(authManager.isAdmin()).thenReturn(true)

        // When
        val result = permissionManager.canEdit(Modules.PURCHASES)

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
            module = Modules.PURCHASES,
            canView = true,
            canCreate = true,
            canEdit = true,
            canDelete = false
        )
        whenever(userPermissionDao.getPermissionForModule("user-1", Modules.PURCHASES))
            .thenReturn(permission)

        // When
        val result = permissionManager.canEdit(Modules.PURCHASES)

        // Then
        assertTrue(result)
    }

    @Test
    fun canDelete_adminUser_returnsTrue() = runTest {
        // Given
        whenever(authManager.isAdmin()).thenReturn(true)

        // When
        val result = permissionManager.canDelete(Modules.INVENTORY)

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
            module = Modules.INVENTORY,
            canView = true,
            canCreate = true,
            canEdit = true,
            canDelete = true
        )
        whenever(userPermissionDao.getPermissionForModule("user-1", Modules.INVENTORY))
            .thenReturn(permission)

        // When
        val result = permissionManager.canDelete(Modules.INVENTORY)

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
                module = Modules.STOCK,
                canView = true,
                canCreate = true,
                canEdit = false,
                canDelete = false
            ),
            UserPermission(
                id = "perm-2",
                userId = userId,
                module = Modules.SALES,
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
        assertEquals(Modules.STOCK, result[0].module)
        assertEquals(Modules.SALES, result[1].module)
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
        assertTrue(permissionManager.canView(Modules.STOCK))
        assertTrue(permissionManager.canView(Modules.SALES))
        assertTrue(permissionManager.canView(Modules.PURCHASES))
        assertTrue(permissionManager.canView(Modules.INVENTORY))
        assertTrue(permissionManager.canView(Modules.TRANSFERS))
        assertTrue(permissionManager.canView(Modules.ADMIN))
        assertTrue(permissionManager.canView(Modules.PRODUCTS))
        assertTrue(permissionManager.canView(Modules.SITES))
        assertTrue(permissionManager.canView(Modules.CATEGORIES))
        assertTrue(permissionManager.canView(Modules.USERS))
    }

    @Test
    fun permissionHierarchy_viewDoesNotImplyCreate() = runTest {
        // Given
        whenever(authManager.isAdmin()).thenReturn(false)
        whenever(authManager.getUserId()).thenReturn("user-1")
        val permission = UserPermission(
            id = "perm-1",
            userId = "user-1",
            module = Modules.PRODUCTS,
            canView = true,
            canCreate = false,
            canEdit = false,
            canDelete = false
        )
        whenever(userPermissionDao.getPermissionForModule("user-1", Modules.PRODUCTS))
            .thenReturn(permission)

        // When/Then
        assertTrue(permissionManager.canView(Modules.PRODUCTS))
        assertFalse(permissionManager.canCreate(Modules.PRODUCTS))
        assertFalse(permissionManager.canEdit(Modules.PRODUCTS))
        assertFalse(permissionManager.canDelete(Modules.PRODUCTS))
    }
}
