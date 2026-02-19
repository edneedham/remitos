package com.remitos.app.network

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.remitos.app.data.AuthManager
import com.remitos.app.data.FeatureFlags
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit API client configuration.
 * Provides configured Retrofit instance with interceptors.
 */
object ApiClient {

    private const val CONNECT_TIMEOUT_SECONDS = 30L
    private const val READ_TIMEOUT_SECONDS = 30L
    private const val WRITE_TIMEOUT_SECONDS = 30L

    private var retrofit: Retrofit? = null
    private var apiService: RemitosApiService? = null

    /**
     * Get or create the Retrofit instance.
     * @param authManager AuthManager for token injection
     * @return Configured Retrofit instance
     */
    fun getRetrofit(authManager: AuthManager): Retrofit {
        return retrofit ?: synchronized(this) {
            retrofit ?: createRetrofit(authManager).also {
                retrofit = it
            }
        }
    }

    /**
     * Get or create the API service.
     * @param authManager AuthManager for token injection
     * @return RemitosApiService instance
     */
    fun getApiService(authManager: AuthManager): RemitosApiService {
        return apiService ?: synchronized(this) {
            apiService ?: getRetrofit(authManager)
                .create(RemitosApiService::class.java)
                .also { apiService = it }
        }
    }

    /**
     * Reset the API client (useful for testing or changing base URL).
     */
    fun reset() {
        synchronized(this) {
            retrofit = null
            apiService = null
        }
    }

    /**
     * Check if the client is initialized.
     */
    fun isInitialized(): Boolean {
        return retrofit != null && apiService != null
    }

    private fun createRetrofit(authManager: AuthManager): Retrofit {
        val baseUrl = FeatureFlags.backendBaseUrl
            ?: throw IllegalStateException("Backend base URL not configured. Call FeatureFlags.configureBackendMode() first.")

        val client = createOkHttpClient(authManager)
        val gson = createGson()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    private fun createOkHttpClient(authManager: AuthManager): OkHttpClient {
        return OkHttpClient.Builder().apply {
            // Timeouts
            connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)

            // Add auth interceptor
            addInterceptor(AuthInterceptor(authManager))

            // Add logging interceptor in debug builds
            if (com.remitos.app.BuildConfig.DEBUG) {
                addInterceptor(createLoggingInterceptor())
            }

            // Retry on connection failure
            retryOnConnectionFailure(true)
        }.build()
    }

    private fun createLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    private fun createGson(): Gson {
        return GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .create()
    }
}
