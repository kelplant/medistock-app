package com.medistock.shared

import com.medistock.shared.domain.auth.AuthResult
import com.medistock.shared.domain.auth.AuthService
import com.medistock.shared.domain.auth.PasswordVerifier
import com.medistock.shared.domain.model.Module
import com.medistock.shared.domain.model.User
import com.medistock.shared.domain.model.UserPermission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for AuthResult sealed class
 */
class AuthResultTest {

    private val testUser = User(
        id = "user-1",
        username = "testuser",
        password = "hashedpwd",
        fullName = "Test User",
        isAdmin = false,
        isActive = true
    )

    @Test
    fun success_containsUser() {
        val result = AuthResult.Success(testUser)

        assertIs<AuthResult.Success>(result)
        assertEquals("testuser", result.user.username)
        assertEquals("Test User", result.user.fullName)
    }

    @Test
    fun success_isSuccessReturnsTrue() {
        val result: AuthResult = AuthResult.Success(testUser)

        assertTrue(result.isSuccess)
    }

    @Test
    fun success_userOrNullReturnsUser() {
        val result: AuthResult = AuthResult.Success(testUser)

        val user = result.userOrNull()
        assertNotNull(user)
        assertEquals("testuser", user.username)
    }

    @Test
    fun invalidCredentials_isSuccessReturnsFalse() {
        val result: AuthResult = AuthResult.InvalidCredentials

        assertFalse(result.isSuccess)
        assertNull(result.userOrNull())
    }

    @Test
    fun userInactive_isSuccessReturnsFalse() {
        val result: AuthResult = AuthResult.UserInactive

        assertFalse(result.isSuccess)
        assertNull(result.userOrNull())
    }

    @Test
    fun userNotFound_isSuccessReturnsFalse() {
        val result: AuthResult = AuthResult.UserNotFound

        assertFalse(result.isSuccess)
        assertNull(result.userOrNull())
    }

    @Test
    fun error_containsMessage() {
        val result = AuthResult.Error("Network timeout")

        assertIs<AuthResult.Error>(result)
        assertEquals("Network timeout", result.message)
        assertFalse(result.isSuccess)
        assertNull(result.userOrNull())
    }

    @Test
    fun allResultTypes_areDistinct() {
        val success = AuthResult.Success(testUser)
        val invalid = AuthResult.InvalidCredentials
        val inactive = AuthResult.UserInactive
        val notFound = AuthResult.UserNotFound
        val error = AuthResult.Error("Error")

        assertIs<AuthResult.Success>(success)
        assertIs<AuthResult.InvalidCredentials>(invalid)
        assertIs<AuthResult.UserInactive>(inactive)
        assertIs<AuthResult.UserNotFound>(notFound)
        assertIs<AuthResult.Error>(error)
    }
}

/**
 * Tests for Module enum
 */
class ModuleTest {

    @Test
    fun allModules_haveDisplayNames() {
        Module.entries.forEach { module ->
            assertTrue(module.displayName.isNotBlank(), "Module ${module.name} should have a display name")
        }
    }

    @Test
    fun module_count_is13() {
        assertEquals(13, Module.entries.size)
    }

    @Test
    fun fromName_existingModule_returnsModule() {
        val module = Module.fromName("STOCK")

        assertNotNull(module)
        assertEquals(Module.STOCK, module)
    }

    @Test
    fun fromName_caseInsensitive() {
        assertEquals(Module.STOCK, Module.fromName("stock"))
        assertEquals(Module.STOCK, Module.fromName("Stock"))
        assertEquals(Module.STOCK, Module.fromName("STOCK"))
    }

    @Test
    fun fromName_nonExistingModule_returnsNull() {
        val module = Module.fromName("NONEXISTENT")

        assertNull(module)
    }

    @Test
    fun allNames_returnsAllModuleNames() {
        val names = Module.allNames()

        assertEquals(13, names.size)
        assertTrue(names.contains("STOCK"))
        assertTrue(names.contains("SALES"))
        assertTrue(names.contains("PURCHASES"))
        assertTrue(names.contains("INVENTORY"))
        assertTrue(names.contains("TRANSFERS"))
        assertTrue(names.contains("ADMIN"))
        assertTrue(names.contains("PRODUCTS"))
        assertTrue(names.contains("SITES"))
        assertTrue(names.contains("CATEGORIES"))
        assertTrue(names.contains("USERS"))
        assertTrue(names.contains("CUSTOMERS"))
        assertTrue(names.contains("AUDIT"))
        assertTrue(names.contains("PACKAGING_TYPES"))
    }

    @Test
    fun specificModules_haveCorrectDisplayNames() {
        assertEquals("Stock", Module.STOCK.displayName)
        assertEquals("Ventes", Module.SALES.displayName)
        assertEquals("Achats", Module.PURCHASES.displayName)
        assertEquals("Inventaire", Module.INVENTORY.displayName)
        assertEquals("Transferts", Module.TRANSFERS.displayName)
        assertEquals("Administration", Module.ADMIN.displayName)
        assertEquals("Produits", Module.PRODUCTS.displayName)
        assertEquals("Sites", Module.SITES.displayName)
        assertEquals("Catégories", Module.CATEGORIES.displayName)
        assertEquals("Utilisateurs", Module.USERS.displayName)
        assertEquals("Clients", Module.CUSTOMERS.displayName)
        assertEquals("Audit", Module.AUDIT.displayName)
        assertEquals("Types d'emballage", Module.PACKAGING_TYPES.displayName)
    }
}

/**
 * Tests for UserPermission model
 */
class UserPermissionTest {

    @Test
    fun userPermission_creation_withDefaults() {
        val permission = UserPermission(
            id = "perm-1",
            userId = "user-1",
            module = "STOCK"
        )

        assertEquals("perm-1", permission.id)
        assertEquals("user-1", permission.userId)
        assertEquals("STOCK", permission.module)
        assertFalse(permission.canView)
        assertFalse(permission.canCreate)
        assertFalse(permission.canEdit)
        assertFalse(permission.canDelete)
    }

    @Test
    fun userPermission_creation_withAllPermissions() {
        val permission = UserPermission(
            id = "perm-2",
            userId = "user-1",
            module = "ADMIN",
            canView = true,
            canCreate = true,
            canEdit = true,
            canDelete = true
        )

        assertTrue(permission.canView)
        assertTrue(permission.canCreate)
        assertTrue(permission.canEdit)
        assertTrue(permission.canDelete)
    }

    @Test
    fun userPermission_partialPermissions() {
        val permission = UserPermission(
            id = "perm-3",
            userId = "user-1",
            module = "SALES",
            canView = true,
            canCreate = true,
            canEdit = false,
            canDelete = false
        )

        assertTrue(permission.canView)
        assertTrue(permission.canCreate)
        assertFalse(permission.canEdit)
        assertFalse(permission.canDelete)
    }

    @Test
    fun userPermission_withTimestamps() {
        val now = 1705680000000L
        val permission = UserPermission(
            id = "perm-4",
            userId = "user-1",
            module = "PRODUCTS",
            canView = true,
            createdAt = now,
            updatedAt = now,
            createdBy = "admin",
            updatedBy = "admin"
        )

        assertEquals(now, permission.createdAt)
        assertEquals(now, permission.updatedAt)
        assertEquals("admin", permission.createdBy)
        assertEquals("admin", permission.updatedBy)
    }

    @Test
    fun userPermission_moduleAsString() {
        val permission = UserPermission(
            id = "perm-5",
            userId = "user-1",
            module = Module.INVENTORY.name
        )

        assertEquals("INVENTORY", permission.module)
        assertEquals(Module.INVENTORY, Module.fromName(permission.module))
    }
}

/**
 * Tests for PasswordVerifier interface
 */
class PasswordVerifierTest {

    @Test
    fun mockVerifier_acceptsMatchingPassword() {
        val verifier = object : PasswordVerifier {
            override fun verify(plainPassword: String, hashedPassword: String): Boolean {
                return plainPassword == "correct" && hashedPassword == "hash"
            }
        }

        assertTrue(verifier.verify("correct", "hash"))
        assertFalse(verifier.verify("wrong", "hash"))
    }

    @Test
    fun mockVerifier_rejectsEmptyPassword() {
        val verifier = object : PasswordVerifier {
            override fun verify(plainPassword: String, hashedPassword: String): Boolean {
                return plainPassword.isNotEmpty() && hashedPassword.isNotEmpty()
            }
        }

        assertFalse(verifier.verify("", "hash"))
        assertFalse(verifier.verify("password", ""))
    }
}
