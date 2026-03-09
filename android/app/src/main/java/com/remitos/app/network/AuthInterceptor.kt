package com.remitos.app.network

import com.remitos.app.data.AuthManager
import com.remitos.app.data.TokenData
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Result of token refresh attempt.
 */
sealed class RefreshResult {
    data object Success : RefreshResult()
    data object DeviceRevoked : RefreshResult()
    data object Failed : RefreshResult()
}

/**
 * OkHttp interceptor that adds JWT authentication header to requests.
 * Handles token expiration and refresh.
 * On refresh failure (device revoked), triggers re-registration flow.
 */
class AuthInterceptor(
    private val authManager: AuthManager,
    private val onDeviceRevoked: (() -> Unit)? = null
) : Interceptor {

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Skip if no authentication needed
        if (!requiresAuth(originalRequest)) {
            return chain.proceed(originalRequest)
        }

        val userId = authManager.getCurrentUser()
            ?: return chain.proceed(originalRequest)

        // Get token
        val tokenData = runBlocking { authManager.getToken(userId) }
            ?: return chain.proceed(originalRequest)

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
            val refreshResult = runBlocking { tryRefreshToken(userId, tokenData.refreshToken) }
            
            when (refreshResult) {
                is RefreshResult.Success -> {
                    // Retry request with new token
                    val newToken = runBlocking { authManager.getToken(userId) }
                    if (newToken != null) {
                        val retryRequest = originalRequest.newBuilder()
                            .header(AUTHORIZATION_HEADER, BEARER_PREFIX + newToken.accessToken)
                            .build()
                        return chain.proceed(retryRequest)
                    }
                }
                is RefreshResult.DeviceRevoked -> {
                    // Device revoked - clear all and trigger re-registration
                    runBlocking { authManager.revokeDeviceAndReRegister(userId) }
                    onDeviceRevoked?.invoke()
                }
                is RefreshResult.Failed -> {
                    // Refresh failed for other reasons - clear session
                    runBlocking { authManager.removeToken(userId) }
                }
            }
        }

        return response
    }

    /**
     * Check if the request requires authentication.
     */
    private fun requiresAuth(request: okhttp3.Request): Boolean {
        val path = request.url.encodedPath
        val publicEndpoints = listOf(
            "/auth/register",
            "/auth/login",
            "/auth/device",
            "/auth/refresh"
        )
        return publicEndpoints.none { path.endsWith(it) }
    }

    /**
     * Try to refresh the access token.
     * @return RefreshResult indicating success, device revoked, or failure
     */
    private suspend fun tryRefreshToken(userId: String, refreshToken: String): RefreshResult {
        return try {
            val service = ApiClient.getApiService(authManager)
            val response = service.refreshToken(RefreshTokenRequest(refreshToken))
            val statusCode = response.code()
            
            if (statusCode == 200 || statusCode == 201) {
                val authResponse = response.body()
                if (authResponse != null) {
                    val expiresAt = System.currentTimeMillis() + (authResponse.expiresIn * 1000)
                    val newTokenData = TokenData(
                        accessToken = authResponse.token,
                        refreshToken = authResponse.refreshToken,
                        expiresAt = expiresAt,
                        userEmail = authManager.getTokenSync(userId)?.userEmail ?: "",
                        userName = authManager.getTokenSync(userId)?.userName,
                        role = authManager.getTokenSync(userId)?.role
                    )
                    authManager.saveToken(userId, newTokenData)
                    RefreshResult.Success
                } else {
                    RefreshResult.Failed
                }
            } else if (statusCode == 401) {
                // Refresh token invalid/expired - device revoked
                RefreshResult.DeviceRevoked
            } else {
                RefreshResult.Failed
            }
        } catch (e: Exception) {
            RefreshResult.Failed
        }
    }
}
