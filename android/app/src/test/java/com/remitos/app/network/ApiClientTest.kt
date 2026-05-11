package com.remitos.app.network

import com.remitos.app.data.AuthManager
import com.remitos.app.data.FeatureFlags
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

class ApiClientTest {

    @Before
    fun setup() {
        ApiClient.reset()
        FeatureFlags.configureBackendMode("https://api.test.example/")
    }

    @After
    fun tearDown() {
        ApiClient.reset()
        FeatureFlags.configureOfflineMode()
    }

    @Test
    fun `getPublicApiService does not initialize authenticated stack`() {
        ApiClient.getPublicApiService()
        assertFalse(ApiClient.isInitialized)
    }

    @Test
    fun `public and authenticated services are different instances`() {
        val authManager = mock<AuthManager>()
        val authenticated = ApiClient.getApiService(authManager)
        val publicService = ApiClient.getPublicApiService()
        assertNotSame(publicService, authenticated)
    }

    @Test
    fun `initializing public first does not replace authenticated client`() {
        val authManager = mock<AuthManager>()
        ApiClient.getPublicApiService()
        val authenticated = ApiClient.getApiService(authManager)
        val publicAgain = ApiClient.getPublicApiService()
        assertSame(publicAgain, ApiClient.getPublicApiService())
        assertSame(authenticated, ApiClient.getApiService(authManager))
    }

    @Test
    fun `reset clears both clients`() {
        val authManager = mock<AuthManager>()
        ApiClient.getApiService(authManager)
        ApiClient.getPublicApiService()
        assertTrue(ApiClient.isInitialized)
        ApiClient.reset()
        assertFalse(ApiClient.isInitialized)
        assertTrue(ApiClient.getApiServiceIfInitialized() == null)
    }
}
