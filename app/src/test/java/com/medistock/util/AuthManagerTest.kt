package com.medistock.util

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.medistock.data.entities.User
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthManagerTest {

    private lateinit var authManager: AuthManager
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        authManager = AuthManager(context)
        // Clear any previous session
        authManager.logout()
    }

    @After
    fun teardown() {
        authManager.logout()
    }

    @Test
    fun login_savesUserSession() {
        // Given
        val user = User(
            id = "user-1",
            username = "testuser",
            fullName = "Test User",
            passwordHash = "hash",
            isAdmin = false,
            isActive = true,
            siteId = "site-1"
        )

        // When
        authManager.login(user)

        // Then
        assertTrue(authManager.isLoggedIn())
        assertEquals("user-1", authManager.getUserId())
        assertEquals("testuser", authManager.getUsername())
        assertEquals("Test User", authManager.getFullName())
        assertFalse(authManager.isAdmin())
    }

    @Test
    fun login_withAdminUser_savesAdminStatus() {
        // Given
        val adminUser = User(
            id = "admin-1",
            username = "admin",
            fullName = "Admin User",
            passwordHash = "hash",
            isAdmin = true,
            isActive = true,
            siteId = "site-1"
        )

        // When
        authManager.login(adminUser)

        // Then
        assertTrue(authManager.isLoggedIn())
        assertTrue(authManager.isAdmin())
    }

    @Test
    fun logout_clearsSession() {
        // Given
        val user = User(
            id = "user-1",
            username = "testuser",
            fullName = "Test User",
            passwordHash = "hash",
            isAdmin = false,
            isActive = true,
            siteId = "site-1"
        )
        authManager.login(user)
        assertTrue(authManager.isLoggedIn())

        // When
        authManager.logout()

        // Then
        assertFalse(authManager.isLoggedIn())
        assertNull(authManager.getUserId())
        assertEquals("", authManager.getUsername())
        assertEquals("", authManager.getFullName())
        assertFalse(authManager.isAdmin())
    }

    @Test
    fun isLoggedIn_returnsFalseByDefault() {
        // When/Then
        assertFalse(authManager.isLoggedIn())
    }

    @Test
    fun getUserId_returnsNullWhenNotLoggedIn() {
        // When/Then
        assertNull(authManager.getUserId())
    }

    @Test
    fun getUsername_returnsEmptyStringWhenNotLoggedIn() {
        // When/Then
        assertEquals("", authManager.getUsername())
    }

    @Test
    fun getFullName_returnsEmptyStringWhenNotLoggedIn() {
        // When/Then
        assertEquals("", authManager.getFullName())
    }

    @Test
    fun isAdmin_returnsFalseWhenNotLoggedIn() {
        // When/Then
        assertFalse(authManager.isAdmin())
    }

    @Test
    fun login_multipleTimes_replacesSession() {
        // Given
        val user1 = User(
            id = "user-1",
            username = "user1",
            fullName = "User One",
            passwordHash = "hash1",
            isAdmin = false,
            isActive = true,
            siteId = "site-1"
        )
        val user2 = User(
            id = "user-2",
            username = "user2",
            fullName = "User Two",
            passwordHash = "hash2",
            isAdmin = true,
            isActive = true,
            siteId = "site-2"
        )

        // When
        authManager.login(user1)
        assertEquals("user-1", authManager.getUserId())

        authManager.login(user2)

        // Then
        assertEquals("user-2", authManager.getUserId())
        assertEquals("user2", authManager.getUsername())
        assertEquals("User Two", authManager.getFullName())
        assertTrue(authManager.isAdmin())
    }

    @Test
    fun getInstance_returnsSameInstance() {
        // When
        val instance1 = AuthManager.getInstance(context)
        val instance2 = AuthManager.getInstance(context)

        // Then
        assertSame(instance1, instance2)
    }

    @Test
    fun sessionPersistsAcrossInstances() {
        // Given
        val user = User(
            id = "user-1",
            username = "testuser",
            fullName = "Test User",
            passwordHash = "hash",
            isAdmin = false,
            isActive = true,
            siteId = "site-1"
        )
        authManager.login(user)

        // When - Create new instance
        val newAuthManager = AuthManager(context)

        // Then - Session should persist
        assertTrue(newAuthManager.isLoggedIn())
        assertEquals("user-1", newAuthManager.getUserId())
        assertEquals("testuser", newAuthManager.getUsername())
    }
}
