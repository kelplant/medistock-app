package com.medistock.shared.domain.auth

import com.medistock.shared.domain.model.User
import kotlin.test.*

/**
 * Tests for MigrateUserResponse.toUser() extension function.
 */
class MigrateUserResponseToUserTest {

    @Test
    fun `should_returnUser_when_responseHasValidUserData`() {
        // Arrange
        val migrateUserData = MigrateUserData(
            id = "user-123",
            username = "johndoe",
            name = "John Doe",
            isAdmin = false
        )
        val response = MigrateUserResponse(
            success = true,
            user = migrateUserData,
            session = MigrateSessionData(
                accessToken = "access-token",
                refreshToken = "refresh-token",
                expiresAt = 1700000000000L
            )
        )

        // Act
        val user = response.toUser()

        // Assert
        assertNotNull(user)
        assertEquals("user-123", user.id)
        assertEquals("johndoe", user.username)
        assertEquals("John Doe", user.fullName)
        assertFalse(user.isAdmin)
        assertTrue(user.isActive)
        assertEquals("", user.password)
        assertNull(user.language)
        assertEquals("user-123", user.createdBy)
        assertEquals("user-123", user.updatedBy)
        assertTrue(user.createdAt > 0)
        assertTrue(user.updatedAt > 0)
    }

    @Test
    fun `should_returnUser_when_userIsAdmin`() {
        // Arrange
        val migrateUserData = MigrateUserData(
            id = "admin-456",
            username = "admin",
            name = "Administrator",
            isAdmin = true
        )
        val response = MigrateUserResponse(
            success = true,
            user = migrateUserData
        )

        // Act
        val user = response.toUser()

        // Assert
        assertNotNull(user)
        assertTrue(user.isAdmin)
        assertEquals("admin-456", user.id)
        assertEquals("admin", user.username)
        assertEquals("Administrator", user.fullName)
    }

    @Test
    fun `should_returnNull_when_userDataIsNull`() {
        // Arrange
        val response = MigrateUserResponse(
            success = false,
            error = "User not found",
            user = null
        )

        // Act
        val user = response.toUser()

        // Assert
        assertNull(user)
    }

    @Test
    fun `should_setTimestamps_when_convertingToUser`() {
        // Arrange
        val migrateUserData = MigrateUserData(
            id = "user-789",
            username = "jane",
            name = "Jane Smith",
            isAdmin = false
        )
        val response = MigrateUserResponse(
            success = true,
            user = migrateUserData
        )

        // Act
        val user = response.toUser()

        // Assert
        assertNotNull(user)
        assertTrue(user.createdAt > 0, "createdAt should be set to current timestamp")
        assertTrue(user.updatedAt > 0, "updatedAt should be set to current timestamp")
        assertEquals(user.createdAt, user.updatedAt, "createdAt and updatedAt should be equal")
    }

    @Test
    fun `should_setPasswordToEmpty_when_convertingToUser`() {
        // Arrange
        val migrateUserData = MigrateUserData(
            id = "user-100",
            username = "testuser",
            name = "Test User",
            isAdmin = false
        )
        val response = MigrateUserResponse(
            success = true,
            user = migrateUserData
        )

        // Act
        val user = response.toUser()

        // Assert
        assertNotNull(user)
        assertEquals("", user.password, "Password should not be returned from server")
    }

    @Test
    fun `should_setIsActiveToTrue_when_convertingToUser`() {
        // Arrange
        val migrateUserData = MigrateUserData(
            id = "user-200",
            username = "activeuser",
            name = "Active User",
            isAdmin = false
        )
        val response = MigrateUserResponse(
            success = true,
            user = migrateUserData
        )

        // Act
        val user = response.toUser()

        // Assert
        assertNotNull(user)
        assertTrue(user.isActive, "User should be active if login succeeded")
    }
}

/**
 * Tests for parseAuthError() function.
 */
class ParseAuthErrorTest {

    // region Invalid Credentials Tests
    @Test
    fun `should_returnInvalidCredentials_when_errorContainsInvalidCredentials`() {
        // Arrange
        val errorMessage = "Invalid credentials"

        // Act
        val result = parseAuthError(errorMessage)

        // Assert
        assertIs<OnlineFirstAuthResult.InvalidCredentials>(result)
    }

    @Test
    fun `should_returnInvalidCredentials_when_errorContainsInvalidPassword`() {
        // Arrange
        val errorMessage = "Invalid password"

        // Act
        val result = parseAuthError(errorMessage)

        // Assert
        assertIs<OnlineFirstAuthResult.InvalidCredentials>(result)
    }

    @Test
    fun `should_returnInvalidCredentials_when_errorContainsInvalidGrant`() {
        // Arrange
        val errorMessage = "invalid_grant"

        // Act
        val result = parseAuthError(errorMessage)

        // Assert
        assertIs<OnlineFirstAuthResult.InvalidCredentials>(result)
    }

    @Test
    fun `should_returnInvalidCredentials_when_errorIsCaseInsensitive`() {
        // Arrange & Act & Assert
        assertIs<OnlineFirstAuthResult.InvalidCredentials>(
            parseAuthError("INVALID CREDENTIALS")
        )
        assertIs<OnlineFirstAuthResult.InvalidCredentials>(
            parseAuthError("Invalid Password")
        )
        assertIs<OnlineFirstAuthResult.InvalidCredentials>(
            parseAuthError("Invalid_Grant")
        )
    }

    @Test
    fun `should_returnInvalidCredentials_when_errorContainsPartialMatch`() {
        // Arrange
        val errorMessage = "Authentication failed: Invalid credentials provided"

        // Act
        val result = parseAuthError(errorMessage)

        // Assert
        assertIs<OnlineFirstAuthResult.InvalidCredentials>(result)
    }
    // endregion

    // region User Inactive Tests
    @Test
    fun `should_returnUserInactive_when_errorContainsDeactivated`() {
        // Arrange
        val errorMessage = "User account is deactivated"

        // Act
        val result = parseAuthError(errorMessage)

        // Assert
        assertIs<OnlineFirstAuthResult.UserInactive>(result)
    }

    @Test
    fun `should_returnUserInactive_when_errorContainsInactive`() {
        // Arrange
        val errorMessage = "User is inactive"

        // Act
        val result = parseAuthError(errorMessage)

        // Assert
        assertIs<OnlineFirstAuthResult.UserInactive>(result)
    }

    @Test
    fun `should_returnUserInactive_when_errorIsCaseInsensitive`() {
        // Arrange & Act & Assert
        assertIs<OnlineFirstAuthResult.UserInactive>(
            parseAuthError("DEACTIVATED")
        )
        assertIs<OnlineFirstAuthResult.UserInactive>(
            parseAuthError("Inactive")
        )
    }
    // endregion

    // region User Not Found Tests
    @Test
    fun `should_returnUserNotFound_when_errorContainsNotFound`() {
        // Arrange
        val errorMessage = "User not found"

        // Act
        val result = parseAuthError(errorMessage)

        // Assert
        assertIs<OnlineFirstAuthResult.UserNotFound>(result)
    }

    @Test
    fun `should_returnUserNotFound_when_errorContainsUserNotFound`() {
        // Arrange
        val errorMessage = "User not found in database"

        // Act
        val result = parseAuthError(errorMessage)

        // Assert
        assertIs<OnlineFirstAuthResult.UserNotFound>(result)
    }

    @Test
    fun `should_returnUserNotFound_when_errorIsCaseInsensitive`() {
        // Arrange & Act & Assert
        assertIs<OnlineFirstAuthResult.UserNotFound>(
            parseAuthError("NOT FOUND")
        )
        assertIs<OnlineFirstAuthResult.UserNotFound>(
            parseAuthError("User Not Found")
        )
    }
    // endregion

    // region Generic Error Tests
    @Test
    fun `should_returnError_when_errorMessageIsNull`() {
        // Arrange
        val errorMessage: String? = null

        // Act
        val result = parseAuthError(errorMessage)

        // Assert
        assertIs<OnlineFirstAuthResult.Error>(result)
        assertEquals("Unknown error", result.message)
    }

    @Test
    fun `should_returnError_when_errorMessageIsUnknown`() {
        // Arrange
        val errorMessage = "Network timeout occurred"

        // Act
        val result = parseAuthError(errorMessage)

        // Assert
        assertIs<OnlineFirstAuthResult.Error>(result)
        assertEquals("Network timeout occurred", result.message)
    }

    @Test
    fun `should_returnError_when_errorMessageIsEmpty`() {
        // Arrange
        val errorMessage = ""

        // Act
        val result = parseAuthError(errorMessage)

        // Assert
        assertIs<OnlineFirstAuthResult.Error>(result)
        assertEquals("", result.message)
    }

    @Test
    fun `should_returnError_when_errorMessageContainsOtherText`() {
        // Arrange
        val errorMessages = listOf(
            "Database connection failed",
            "Server error 500",
            "Unexpected exception occurred",
            "Rate limit exceeded"
        )

        // Act & Assert
        errorMessages.forEach { message ->
            val result = parseAuthError(message)
            assertIs<OnlineFirstAuthResult.Error>(result)
            assertEquals(message, result.message)
        }
    }
    // endregion

    // region Edge Cases
    @Test
    fun `should_handleMultipleKeywords_when_errorContainsMultipleMatches`() {
        // Arrange - "Invalid credentials" should match first (higher priority)
        val errorMessage = "Invalid credentials - user not found"

        // Act
        val result = parseAuthError(errorMessage)

        // Assert
        assertIs<OnlineFirstAuthResult.InvalidCredentials>(result)
    }

    @Test
    fun `should_returnError_when_errorContainsOnlyWhitespace`() {
        // Arrange
        val errorMessage = "   "

        // Act
        val result = parseAuthError(errorMessage)

        // Assert
        assertIs<OnlineFirstAuthResult.Error>(result)
    }
    // endregion
}

/**
 * Tests for OnlineFirstAuthConfig.requiresOnlineAuth() logic.
 */
class OnlineFirstAuthConfigTest {

    // region No Local Users Tests
    @Test
    fun `should_requireOnlineAuth_when_noLocalUsersAndNoSession`() {
        // Arrange
        val hasLocalUsers = false
        val hasStoredSession = false

        // Act
        val result = OnlineFirstAuthConfig.requiresOnlineAuth(hasLocalUsers, hasStoredSession)

        // Assert
        assertTrue(result, "First login with no local users requires network")
    }

    @Test
    fun `should_requireOnlineAuth_when_noLocalUsersButHasSession`() {
        // Arrange
        val hasLocalUsers = false
        val hasStoredSession = true

        // Act
        val result = OnlineFirstAuthConfig.requiresOnlineAuth(hasLocalUsers, hasStoredSession)

        // Assert
        assertTrue(result, "First login with no local users requires network even with stored session")
    }
    // endregion

    // region Has Local Users Tests
    @Test
    fun `should_notRequireOnlineAuth_when_hasLocalUsersAndNoSession`() {
        // Arrange
        val hasLocalUsers = true
        val hasStoredSession = false

        // Act
        val result = OnlineFirstAuthConfig.requiresOnlineAuth(hasLocalUsers, hasStoredSession)

        // Assert
        assertFalse(result, "Offline authentication is allowed when local users exist")
    }

    @Test
    fun `should_notRequireOnlineAuth_when_hasLocalUsersAndHasSession`() {
        // Arrange
        val hasLocalUsers = true
        val hasStoredSession = true

        // Act
        val result = OnlineFirstAuthConfig.requiresOnlineAuth(hasLocalUsers, hasStoredSession)

        // Assert
        assertFalse(result, "Offline authentication is allowed when local users exist and session is stored")
    }
    // endregion

    // region Edge Cases
    @Test
    fun `should_prioritizeLocalUsers_when_determiningAuthMode`() {
        // Arrange - Even without session, having local users means offline is OK
        val hasLocalUsers = true
        val hasStoredSession = false

        // Act
        val result = OnlineFirstAuthConfig.requiresOnlineAuth(hasLocalUsers, hasStoredSession)

        // Assert
        assertFalse(result, "Having local users should allow offline mode regardless of session")
    }
    // endregion
}

/**
 * Tests for OnlineFirstAuthResult sealed class.
 */
class OnlineFirstAuthResultTest {

    @Test
    fun `should_createSuccessResult_when_authenticationSucceeds`() {
        // Arrange
        val user = User(
            id = "user-1",
            username = "testuser",
            password = "",
            fullName = "Test User",
            isAdmin = false,
            isActive = true
        )

        // Act
        val result = OnlineFirstAuthResult.Success(
            user = user,
            accessToken = "access-token",
            refreshToken = "refresh-token",
            expiresAt = 1700000000000L
        )

        // Assert
        assertIs<OnlineFirstAuthResult.Success>(result)
        assertEquals("testuser", result.user.username)
        assertEquals("access-token", result.accessToken)
        assertEquals("refresh-token", result.refreshToken)
        assertEquals(1700000000000L, result.expiresAt)
    }

    @Test
    fun `should_createInvalidCredentialsResult_when_passwordIsWrong`() {
        // Arrange & Act
        val result = OnlineFirstAuthResult.InvalidCredentials

        // Assert
        assertIs<OnlineFirstAuthResult.InvalidCredentials>(result)
    }

    @Test
    fun `should_createUserNotFoundResult_when_userDoesNotExist`() {
        // Arrange & Act
        val result = OnlineFirstAuthResult.UserNotFound

        // Assert
        assertIs<OnlineFirstAuthResult.UserNotFound>(result)
    }

    @Test
    fun `should_createUserInactiveResult_when_userIsDeactivated`() {
        // Arrange & Act
        val result = OnlineFirstAuthResult.UserInactive

        // Assert
        assertIs<OnlineFirstAuthResult.UserInactive>(result)
    }

    @Test
    fun `should_createNetworkRequiredResult_when_networkIsNotAvailable`() {
        // Arrange & Act
        val result = OnlineFirstAuthResult.NetworkRequired

        // Assert
        assertIs<OnlineFirstAuthResult.NetworkRequired>(result)
    }

    @Test
    fun `should_createNotConfiguredResult_when_supabaseIsNotConfigured`() {
        // Arrange & Act
        val result = OnlineFirstAuthResult.NotConfigured

        // Assert
        assertIs<OnlineFirstAuthResult.NotConfigured>(result)
    }

    @Test
    fun `should_createErrorResult_when_unexpectedErrorOccurs`() {
        // Arrange & Act
        val result = OnlineFirstAuthResult.Error("Connection timeout")

        // Assert
        assertIs<OnlineFirstAuthResult.Error>(result)
        assertEquals("Connection timeout", result.message)
    }

    @Test
    fun `should_distinguishAllResultTypes_when_comparingDifferentResults`() {
        // Arrange
        val user = User(
            id = "user-1",
            username = "test",
            password = "",
            fullName = "Test",
            isAdmin = false,
            isActive = true
        )
        val success = OnlineFirstAuthResult.Success(user, "token", "refresh", 1000L)
        val invalid = OnlineFirstAuthResult.InvalidCredentials
        val notFound = OnlineFirstAuthResult.UserNotFound
        val inactive = OnlineFirstAuthResult.UserInactive
        val networkRequired = OnlineFirstAuthResult.NetworkRequired
        val notConfigured = OnlineFirstAuthResult.NotConfigured
        val error = OnlineFirstAuthResult.Error("Error")

        // Act & Assert
        assertIs<OnlineFirstAuthResult.Success>(success)
        assertIs<OnlineFirstAuthResult.InvalidCredentials>(invalid)
        assertIs<OnlineFirstAuthResult.UserNotFound>(notFound)
        assertIs<OnlineFirstAuthResult.UserInactive>(inactive)
        assertIs<OnlineFirstAuthResult.NetworkRequired>(networkRequired)
        assertIs<OnlineFirstAuthResult.NotConfigured>(notConfigured)
        assertIs<OnlineFirstAuthResult.Error>(error)
    }
}

/**
 * Tests for MigrateUserData, MigrateSessionData, and MigrateUserRequest models.
 */
class MigrateModelsTest {

    @Test
    fun `should_createMigrateUserData_when_allFieldsProvided`() {
        // Arrange & Act
        val userData = MigrateUserData(
            id = "user-123",
            username = "johndoe",
            name = "John Doe",
            isAdmin = true
        )

        // Assert
        assertEquals("user-123", userData.id)
        assertEquals("johndoe", userData.username)
        assertEquals("John Doe", userData.name)
        assertTrue(userData.isAdmin)
    }

    @Test
    fun `should_createMigrateSessionData_when_tokensProvided`() {
        // Arrange & Act
        val sessionData = MigrateSessionData(
            accessToken = "access-token-123",
            refreshToken = "refresh-token-456",
            expiresAt = 1700000000000L
        )

        // Assert
        assertEquals("access-token-123", sessionData.accessToken)
        assertEquals("refresh-token-456", sessionData.refreshToken)
        assertEquals(1700000000000L, sessionData.expiresAt)
    }

    @Test
    fun `should_createMigrateSessionData_when_expiresAtIsNull`() {
        // Arrange & Act
        val sessionData = MigrateSessionData(
            accessToken = "access-token",
            refreshToken = "refresh-token",
            expiresAt = null
        )

        // Assert
        assertEquals("access-token", sessionData.accessToken)
        assertEquals("refresh-token", sessionData.refreshToken)
        assertNull(sessionData.expiresAt)
    }

    @Test
    fun `should_createMigrateUserRequest_when_credentialsProvided`() {
        // Arrange & Act
        val request = MigrateUserRequest(
            username = "testuser",
            password = "password123"
        )

        // Assert
        assertEquals("testuser", request.username)
        assertEquals("password123", request.password)
    }

    @Test
    fun `should_createMigrateUserResponse_when_successIsTrue`() {
        // Arrange
        val userData = MigrateUserData(
            id = "user-1",
            username = "test",
            name = "Test User",
            isAdmin = false
        )
        val sessionData = MigrateSessionData(
            accessToken = "token",
            refreshToken = "refresh",
            expiresAt = 1000L
        )

        // Act
        val response = MigrateUserResponse(
            success = true,
            message = "Login successful",
            user = userData,
            session = sessionData
        )

        // Assert
        assertTrue(response.success)
        assertEquals("Login successful", response.message)
        assertNotNull(response.user)
        assertNotNull(response.session)
        assertNull(response.error)
    }

    @Test
    fun `should_createMigrateUserResponse_when_successIsFalse`() {
        // Arrange & Act
        val response = MigrateUserResponse(
            success = false,
            error = "Invalid credentials",
            message = null,
            user = null,
            session = null
        )

        // Assert
        assertFalse(response.success)
        assertEquals("Invalid credentials", response.error)
        assertNull(response.message)
        assertNull(response.user)
        assertNull(response.session)
    }

    @Test
    fun `should_createMigrateUserResponse_when_onlyRequiredFieldsProvided`() {
        // Arrange & Act
        val response = MigrateUserResponse(
            success = false
        )

        // Assert
        assertFalse(response.success)
        assertNull(response.message)
        assertNull(response.error)
        assertNull(response.user)
        assertNull(response.session)
    }
}
