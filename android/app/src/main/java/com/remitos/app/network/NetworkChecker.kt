package com.remitos.app.network

import com.remitos.app.data.AuthManager
import com.remitos.app.data.FeatureFlags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object NetworkChecker {
    private const val HEALTH_CHECK_TIMEOUT_SECONDS = 5L
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(HEALTH_CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(HEALTH_CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(HEALTH_CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
    
    suspend fun isServerReachable(authManager: AuthManager): Boolean = withContext(Dispatchers.IO) {
        try {
            val baseUrl = FeatureFlags.backendBaseUrl ?: return@withContext false
            val url = "$baseUrl/health"
            
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            
            val response = httpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}
