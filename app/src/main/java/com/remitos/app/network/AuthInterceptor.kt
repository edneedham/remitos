package com.remitos.app.network

import com.remitos.app.data.AuthManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp interceptor that adds JWT authentication header to requests.
 * Handles token expiration and refresh.
 */
class AuthInterceptor(
    private val authManager: AuthManager
) : Interceptor {

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
        private const val RETRY_COUNT = 1
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Skip if no authentication needed
        if (!requiresAuth(originalRequest)) {
            return chain.proceed(originalRequest)
        }

        val userId = authManager.getCurrentUser()
            ?: return chain.proceed(originalRequest) // No user, proceed without auth

        // Get token
        val tokenData = runBlocking { authManager.getToken(userId) }
            ?: return chain.proceed(originalRequest) // No token, proceed without auth

        // Check if token needs refresh
        val isExpired = runBlocking { authManager.isTokenExpired(userId) }
        
        // Build request with authorization header
        val authenticatedRequest = originalRequest.newBuilder()
            .header(AUTHORIZATION_HEADER, BEARER_PREFIX + tokenData.accessToken)
            .build()

        // Proceed with request
        val response = chain.proceed(authenticatedRequest)

        // Handle 401 Unauthorized - token expired or invalid
        if (response.code == 401) {
            response.close()
            
            // Try to refresh token
            val refreshed = runBlocking { tryRefreshToken(userId, tokenData.refreshToken) }
            
            if (refreshed) {
                // Retry request with new token
                val newToken = runBlocking { authManager.getToken(userId) }
                if (newToken != null) {
                    val retryRequest = originalRequest.newBuilder()
                        .header(AUTHORIZATION_HEADER, BEARER_PREFIX + newToken.accessToken)
                        .build()
                    return chain.proceed(retryRequest)
                }
            }
            
            // Refresh failed - clear session and return original 401
            runBlocking { authManager.removeToken(userId) }
        }

        return response
    }

    /**
     * Check if the request requires authentication.
     * Public endpoints like login/register don't need auth.
     */
    private fun requiresAuth(request: okhttp3.Request): Boolean {
        val path = request.url.encodedPath
        
        // List of public endpoints that don't require authentication
        val publicEndpoints = listOf(
            "/auth/register",
            "/auth/login",
            "/auth/refresh"
        )
        
        return publicEndpoints.none { path.endsWith(it) }
    }

    /**
     * Try to refresh the access token.
     * @return true if refresh successful, false otherwise
     */
    private suspend fun tryRefreshToken(userId: String, refreshToken: String): Boolean {
        return try {
            // Note: This would need to be injected or use a static API client
            // For now, we'll assume the token refresh is handled elsewhere
            // This is a placeholder for the refresh logic
            false
        } catch (e: Exception) {
            false
        }
    }
}
