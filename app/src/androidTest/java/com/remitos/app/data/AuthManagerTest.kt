package com.remitos.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthManagerTest {

    private lateinit var context: Context
    private lateinit var authManager: AuthManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        authManager = AuthManager(context)
    }

    @After
    fun tearDown() = runBlocking {
        authManager.clearAll()
    }

    @Test
    fun `saveToken and getToken stores and retrieves token correctly`() = runBlocking {
        // Arrange
        val userId = "user_123"
        val tokenData = TokenData(
            accessToken = "access_token_abc",
            refreshToken = "refresh_token_xyz",
            expiresAt = System.currentTimeMillis() + 3600000, // 1 hour from now
            userEmail = "test@example.com",
            userName = "Test User"
        )

        // Act
        authManager.saveToken(userId, tokenData)
        val retrieved = authManager.getToken(userId)

        // Assert
        assertNotNull(retrieved)
        assertEquals(tokenData.accessToken, retrieved?.accessToken)
        assertEquals(tokenData.refreshToken, retrieved?.refreshToken)
        assertEquals(tokenData.expiresAt, retrieved?.expiresAt)
        assertEquals(tokenData.userEmail, retrieved?.userEmail)
        assertEquals(tokenData.userName, retrieved?.userName)
    }

    @Test
    fun `getToken returns null for non-existent user`() = runBlocking {
        // Act
        val token = authManager.getToken("non_existent_user")

        // Assert
        assertNull(token)
    }

    @Test
    fun `removeToken deletes token and user from list`() = runBlocking {
        // Arrange
        val userId = "user_to_remove"
        val tokenData = TokenData(
            accessToken = "token",
            refreshToken = "refresh",
            expiresAt = System.currentTimeMillis() + 3600000,
            userEmail = "remove@example.com"
        )
        authManager.saveToken(userId, tokenData)

        // Act
        authManager.removeToken(userId)

        // Assert
        assertNull(authManager.getToken(userId))
        val users = authManager.listLoggedInUsers()
        assertFalse(users.any { it.userId == userId })
    }

    @Test
    fun `isTokenExpired returns false for valid token`() = runBlocking {
        // Arrange
        val userId = "user_valid"
        val tokenData = TokenData(
            accessToken = "token",
            refreshToken = "refresh",
            expiresAt = System.currentTimeMillis() + 3600000, // 1 hour from now
            userEmail = "valid@example.com"
        )
        authManager.saveToken(userId, tokenData)

        // Act
        val isExpired = authManager.isTokenExpired(userId)

        // Assert
        assertFalse(isExpired)
    }

    @Test
    fun `isTokenExpired returns true for expired token`() = runBlocking {
        // Arrange
        val userId = "user_expired"
        val tokenData = TokenData(
            accessToken = "token",
            refreshToken = "refresh",
            expiresAt = System.currentTimeMillis() - 1000, // 1 second ago
            userEmail = "expired@example.com"
        )
        authManager.saveToken(userId, tokenData)

        // Act
        val isExpired = authManager.isTokenExpired(userId)

        // Assert
        assertTrue(isExpired)
    }

    @Test
    fun `isTokenExpired returns true when token is close to expiry`() = runBlocking {
        // Arrange - Token expires in 3 minutes (buffer is 5 minutes)
        val userId = "user_close"
        val tokenData = TokenData(
            accessToken = "token",
            refreshToken = "refresh",
            expiresAt = System.currentTimeMillis() + 180000, // 3 minutes from now
            userEmail = "close@example.com"
        )
        authManager.saveToken(userId, tokenData)

        // Act
        val isExpired = authManager.isTokenExpired(userId)

        // Assert - Should be considered expired (within 5 min buffer)
        assertTrue(isExpired)
    }

    @Test
    fun `setCurrentUser and getCurrentUser works correctly`() {
        // Arrange
        val userId = "current_user_123"

        // Act
        authManager.setCurrentUser(userId)
        val current = authManager.getCurrentUser()

        // Assert
        assertEquals(userId, current)
    }

    @Test
    fun `getCurrentUser returns null when not set`() {
        // Act
        val current = authManager.getCurrentUser()

        // Assert
        assertNull(current)
    }

    @Test
    fun `listLoggedInUsers returns users sorted by last login`() = runBlocking {
        // Arrange
        val user1 = TokenData("token1", "refresh1", System.currentTimeMillis() + 3600000, "user1@example.com", "User One")
        val user2 = TokenData("token2", "refresh2", System.currentTimeMillis() + 3600000, "user2@example.com", "User Two")
        val user3 = TokenData("token3", "refresh3", System.currentTimeMillis() + 3600000, "user3@example.com", "User Three")

        // Save in different order
        authManager.saveToken("user_c", user3)
        Thread.sleep(100) // Ensure different timestamps
        authManager.saveToken("user_a", user1)
        Thread.sleep(100)
        authManager.saveToken("user_b", user2)

        // Act
        val users = authManager.listLoggedInUsers()

        // Assert - Should be sorted by lastLoginAt descending (most recent first)
        assertEquals(3, users.size)
        assertEquals("user_b", users[0].userId) // Most recent
        assertEquals("user_a", users[1].userId)
        assertEquals("user_c", users[2].userId) // Oldest
    }

    @Test
    fun `hasLoggedInUsers returns false when no users`() = runBlocking {
        // Act
        val hasUsers = authManager.hasLoggedInUsers()

        // Assert
        assertFalse(hasUsers)
    }

    @Test
    fun `hasLoggedInUsers returns true when users exist`() = runBlocking {
        // Arrange
        val tokenData = TokenData("token", "refresh", System.currentTimeMillis() + 3600000, "test@example.com")
        authManager.saveToken("some_user", tokenData)

        // Act
        val hasUsers = authManager.hasLoggedInUsers()

        // Assert
        assertTrue(hasUsers)
    }

    @Test
    fun `getUserInfo returns correct user info`() = runBlocking {
        // Arrange
        val userId = "info_user"
        val tokenData = TokenData("token", "refresh", System.currentTimeMillis() + 3600000, "info@example.com", "Info User")
        authManager.saveToken(userId, tokenData)

        // Act
        val userInfo = authManager.getUserInfo(userId)

        // Assert
        assertNotNull(userInfo)
        assertEquals(userId, userInfo?.userId)
        assertEquals("info@example.com", userInfo?.email)
        assertEquals("Info User", userInfo?.name)
    }

    @Test
    fun `getUserInfo returns null for non-existent user`() = runBlocking {
        // Act
        val userInfo = authManager.getUserInfo("non_existent")

        // Assert
        assertNull(userInfo)
    }

    @Test
    fun `saving token for existing user updates timestamp`() = runBlocking {
        // Arrange
        val userId = "update_user"
        val tokenData1 = TokenData("token1", "refresh1", System.currentTimeMillis() + 3600000, "update@example.com")
        authManager.saveToken(userId, tokenData1)
        
        val initialUsers = authManager.listLoggedInUsers()
        val initialTimestamp = initialUsers.find { it.userId == userId }?.lastLoginAt
        
        Thread.sleep(100) // Wait to ensure different timestamp

        // Act - Save again
        val tokenData2 = TokenData("token2", "refresh2", System.currentTimeMillis() + 3600000, "update@example.com")
        authManager.saveToken(userId, tokenData2)

        // Assert
        val updatedUsers = authManager.listLoggedInUsers()
        val updatedTimestamp = updatedUsers.find { it.userId == userId }?.lastLoginAt
        
        assertTrue(updatedTimestamp!! > initialTimestamp!!)
        
        // Token should be updated
        val token = authManager.getToken(userId)
        assertEquals("token2", token?.accessToken)
    }

    @Test
    fun `clearAll removes all tokens and users`() = runBlocking {
        // Arrange
        authManager.saveToken("user1", TokenData("t1", "r1", System.currentTimeMillis() + 3600000, "u1@example.com"))
        authManager.saveToken("user2", TokenData("t2", "r2", System.currentTimeMillis() + 3600000, "u2@example.com"))
        authManager.setCurrentUser("user1")

        // Act
        authManager.clearAll()

        // Assert
        assertNull(authManager.getCurrentUser())
        assertFalse(authManager.hasLoggedInUsers())
        assertNull(authManager.getToken("user1"))
        assertNull(authManager.getToken("user2"))
    }

    @Test
    fun `removeToken clears current user if it was the current user`() = runBlocking {
        // Arrange
        val userId = "current_to_remove"
        authManager.saveToken(userId, TokenData("token", "refresh", System.currentTimeMillis() + 3600000, "test@example.com"))
        authManager.setCurrentUser(userId)

        // Act
        authManager.removeToken(userId)

        // Assert
        assertNull(authManager.getCurrentUser())
    }
}
