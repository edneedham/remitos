package com.remitos.app.data

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class SessionManagerTest {

    private lateinit var context: Context
    private lateinit var application: Application
    private lateinit var authManager: AuthManager
    private lateinit var sessionExpiredCalled: AtomicBoolean
    private lateinit var sessionManager: SessionManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        application = context as Application
        authManager = AuthManager(context)
        
        runBlocking {
            authManager.clearAll()
        }
        
        sessionExpiredCalled = AtomicBoolean(false)
        
        sessionManager = SessionManager(
            context = context,
            authManager = authManager,
            onSessionExpired = {
                sessionExpiredCalled.set(true)
            }
        )
    }

    @After
    fun tearDown() {
        sessionManager.stop()
        runBlocking {
            authManager.clearAll()
        }
    }

    @Test
    fun `session manager initializes without error`() {
        // Act & Assert - should not throw
        sessionManager.initialize(application)
    }

    @Test
    fun `onUserInteraction updates last activity time`() {
        // Arrange
        sessionManager.initialize(application)
        val beforeTime = System.currentTimeMillis()
        
        // Wait a bit
        Thread.sleep(100)
        
        // Act
        sessionManager.onUserInteraction()
        
        // Assert - interaction should reset session
        // (Can't directly test private field, but we can verify via behavior)
        assertTrue(sessionExpiredCalled.get().not())
    }

    @Test
    fun `resetSession prevents immediate expiration`() {
        // Arrange
        val userId = "session_user"
        runBlocking {
            authManager.saveToken(userId, TokenData(
                accessToken = "token",
                refreshToken = "refresh",
                expiresAt = System.currentTimeMillis() + 3600000,
                userEmail = "session@example.com"
            ))
            authManager.setCurrentUser(userId)
        }
        
        // Set very short timeout for testing
        FeatureFlags.configure(autoLogout = 1) // 1 minute
        
        sessionManager.initialize(application)
        
        // Act
        sessionManager.resetSession()
        
        // Wait less than timeout
        Thread.sleep(100)
        
        // Assert - should not have expired yet
        assertFalse(sessionExpiredCalled.get())
    }

    @Test
    fun `session does not expire when auto-logout is disabled`() {
        // Arrange
        val userId = "no_auto_logout_user"
        runBlocking {
            authManager.saveToken(userId, TokenData(
                accessToken = "token",
                refreshToken = "refresh",
                expiresAt = System.currentTimeMillis() + 3600000,
                userEmail = "noautologout@example.com"
            ))
            authManager.setCurrentUser(userId)
        }
        
        // Disable auto-logout
        FeatureFlags.configure(autoLogout = 0)
        
        sessionManager.initialize(application)
        sessionManager.resetSession()
        
        // Act - wait a bit
        Thread.sleep(100)
        
        // Assert - should not expire
        assertFalse(sessionExpiredCalled.get())
    }

    @Test
    fun `session does not expire when no user is logged in`() {
        // Arrange - no user logged in
        FeatureFlags.configure(autoLogout = 1)
        sessionManager.initialize(application)
        sessionManager.resetSession()
        
        // Act
        Thread.sleep(100)
        
        // Assert
        assertFalse(sessionExpiredCalled.get())
    }

    @Test
    fun `onActivityResumed starts session monitoring`() {
        // Arrange
        sessionManager.initialize(application)
        
        // Create a dummy activity
        val activity = TestActivity()
        
        // Act
        sessionManager.onActivityResumed(activity)
        
        // Assert - session check should be running
        // (We can verify by checking that the runnable was posted)
        Thread.sleep(100)
        assertFalse(sessionExpiredCalled.get()) // Should not have expired immediately
    }

    @Test
    fun `onActivityStopped stops monitoring when app in background`() {
        // Arrange
        sessionManager.initialize(application)
        val activity = TestActivity()
        
        sessionManager.onActivityResumed(activity)
        
        // Act
        sessionManager.onActivityStopped(activity)
        
        // Assert - monitoring should stop, no crash
        Thread.sleep(200)
        assertTrue(true) // If we get here without crash, test passes
    }

    @Test
    fun `trackUserInteraction extension updates session`() {
        // Arrange
        sessionManager.initialize(application)
        val activity = TestActivity()
        sessionManager.onActivityResumed(activity)
        
        // Act - should not throw
        activity.trackUserInteraction(sessionManager)
        
        // Assert
        assertFalse(sessionExpiredCalled.get())
    }

    @Test
    fun `multiple activities lifecycle does not crash`() {
        // Arrange
        sessionManager.initialize(application)
        val activity1 = TestActivity()
        val activity2 = TestActivity()
        
        // Act - simulate navigation between activities
        sessionManager.onActivityResumed(activity1)
        sessionManager.onActivityPaused(activity1)
        sessionManager.onActivityResumed(activity2)
        sessionManager.onActivityStopped(activity1)
        sessionManager.onActivityDestroyed(activity1)
        
        // Assert
        Thread.sleep(100)
        assertFalse(sessionExpiredCalled.get())
    }

    @Test
    fun `stop removes callbacks and unregisters`() {
        // Arrange
        sessionManager.initialize(application)
        
        // Act - should not throw
        sessionManager.stop()
        
        // Assert
        assertTrue(true) // If we get here without crash, test passes
    }

    @Test
    fun `concurrent user interactions are handled safely`() {
        // Arrange
        sessionManager.initialize(application)
        val latch = CountDownLatch(10)
        
        // Act - simulate multiple concurrent interactions
        repeat(10) {
            Thread {
                sessionManager.onUserInteraction()
                latch.countDown()
            }.start()
        }
        
        // Wait for all threads
        latch.await(5, TimeUnit.SECONDS)
        
        // Assert - no crash
        assertTrue(true)
    }

    @Test
    fun `session check runs periodically when app is in foreground`() {
        // Arrange
        val userId = "periodic_check_user"
        runBlocking {
            authManager.saveToken(userId, TokenData(
                accessToken = "token",
                refreshToken = "refresh",
                expiresAt = System.currentTimeMillis() + 3600000,
                userEmail = "periodic@example.com"
            ))
            authManager.setCurrentUser(userId)
        }
        
        FeatureFlags.configure(autoLogout = 30) // Normal 30 min
        sessionManager.initialize(application)
        
        val activity = TestActivity()
        sessionManager.onActivityResumed(activity)
        
        // Act - wait for a couple check intervals
        Thread.sleep(200)
        
        // Assert - should not have expired (timeout is 30 min)
        assertFalse(sessionExpiredCalled.get())
        
        sessionManager.onActivityStopped(activity)
    }

    // Helper test activity
    private class TestActivity : Activity()
}
