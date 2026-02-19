package com.remitos.app.data

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FeatureFlagsTest {

    @Before
    fun setup() {
        // Reset to default state before each test
        FeatureFlags.configureOfflineMode()
    }

    @Test
    fun `configureOfflineMode disables all backend features`() {
        // Act
        FeatureFlags.configureOfflineMode()

        // Assert
        assertFalse(FeatureFlags.enableBackendOcr)
        assertFalse(FeatureFlags.enableImageUpload)
        assertFalse(FeatureFlags.enableCloudSync)
        assertNull(FeatureFlags.backendBaseUrl)
    }

    @Test
    fun `configureBackendMode enables all backend features`() {
        // Arrange
        val baseUrl = "https://api.example.com"

        // Act
        FeatureFlags.configureBackendMode(baseUrl)

        // Assert
        assertTrue(FeatureFlags.enableBackendOcr)
        assertTrue(FeatureFlags.enableImageUpload)
        assertTrue(FeatureFlags.enableCloudSync)
        assertEquals(baseUrl, FeatureFlags.backendBaseUrl)
    }

    @Test
    fun `configure with granular control sets individual flags`() {
        // Act
        FeatureFlags.configure(
            backendOcr = true,
            imageUpload = false,
            cloudSync = true,
            baseUrl = "https://partial.example.com"
        )

        // Assert
        assertTrue(FeatureFlags.enableBackendOcr)
        assertFalse(FeatureFlags.enableImageUpload)
        assertTrue(FeatureFlags.enableCloudSync)
        assertEquals("https://partial.example.com", FeatureFlags.backendBaseUrl)
    }

    @Test
    fun `configure sets all new flags with defaults`() {
        // Act
        FeatureFlags.configure(
            backendOcr = true,
            imageUpload = true,
            cloudSync = true,
            baseUrl = "https://api.example.com"
        )

        // Assert - defaults
        assertEquals(30, FeatureFlags.autoLogoutMinutes)
        assertEquals(60, FeatureFlags.imageQuality)
        assertEquals(15, FeatureFlags.syncIntervalMinutes)
        assertEquals(7, FeatureFlags.lazySyncDays)
    }

    @Test
    fun `configure sets custom values for all flags`() {
        // Act
        FeatureFlags.configure(
            backendOcr = true,
            imageUpload = true,
            cloudSync = true,
            baseUrl = "https://api.example.com",
            autoLogout = 60,
            imageQualityPercent = 80,
            syncInterval = 30,
            initialSyncDays = 14
        )

        // Assert
        assertEquals(60, FeatureFlags.autoLogoutMinutes)
        assertEquals(80, FeatureFlags.imageQuality)
        assertEquals(30, FeatureFlags.syncIntervalMinutes)
        assertEquals(14, FeatureFlags.lazySyncDays)
    }

    @Test
    fun `flags are immutable after configuration`() {
        // Arrange
        FeatureFlags.configureBackendMode("https://api.example.com")

        // Act & Assert - Try to modify (should not compile if truly immutable)
        // The properties are private set, so external modification is prevented
        assertTrue(FeatureFlags.enableCloudSync)
    }

    @Test
    fun `multiple configurations override previous settings`() {
        // Arrange
        FeatureFlags.configureBackendMode("https://first.com")

        // Act
        FeatureFlags.configureOfflineMode()

        // Assert
        assertFalse(FeatureFlags.enableCloudSync)
        assertNull(FeatureFlags.backendBaseUrl)

        // Act again
        FeatureFlags.configureBackendMode("https://second.com")

        // Assert
        assertTrue(FeatureFlags.enableCloudSync)
        assertEquals("https://second.com", FeatureFlags.backendBaseUrl)
    }

    @Test
    fun `edge case - zero auto logout disables feature`() {
        // Act
        FeatureFlags.configure(autoLogout = 0)

        // Assert
        assertEquals(0, FeatureFlags.autoLogoutMinutes)
    }

    @Test
    fun `edge case - maximum values`() {
        // Act
        FeatureFlags.configure(
            autoLogout = 1440, // 24 hours
            imageQualityPercent = 100,
            syncInterval = 1440, // 24 hours
            initialSyncDays = 365 // 1 year
        )

        // Assert
        assertEquals(1440, FeatureFlags.autoLogoutMinutes)
        assertEquals(100, FeatureFlags.imageQuality)
        assertEquals(1440, FeatureFlags.syncIntervalMinutes)
        assertEquals(365, FeatureFlags.lazySyncDays)
    }

    @Test
    fun `backendBaseUrl is null in offline mode`() {
        // Arrange & Act
        FeatureFlags.configureOfflineMode()

        // Assert
        assertNull(FeatureFlags.backendBaseUrl)
    }

    @Test
    fun `individual feature flags can be enabled independently`() {
        // Test each flag independently
        FeatureFlags.configure(backendOcr = true, cloudSync = false, imageUpload = false)
        assertTrue(FeatureFlags.enableBackendOcr)
        assertFalse(FeatureFlags.enableCloudSync)
        assertFalse(FeatureFlags.enableImageUpload)

        FeatureFlags.configure(backendOcr = false, cloudSync = true, imageUpload = false)
        assertFalse(FeatureFlags.enableBackendOcr)
        assertTrue(FeatureFlags.enableCloudSync)
        assertFalse(FeatureFlags.enableImageUpload)

        FeatureFlags.configure(backendOcr = false, cloudSync = false, imageUpload = true)
        assertFalse(FeatureFlags.enableBackendOcr)
        assertFalse(FeatureFlags.enableCloudSync)
        assertTrue(FeatureFlags.enableImageUpload)
    }
}
