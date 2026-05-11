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
 * Authenticated and public (no [AuthInterceptor]) stacks are separate singletons so order of
 * initialization cannot mix Gson/timeouts or skip auth on protected calls.
 */
object ApiClient {

    private const val CONNECT_TIMEOUT_SECONDS = 30L
    private const val READ_TIMEOUT_SECONDS = 30L
    private const val WRITE_TIMEOUT_SECONDS = 30L
    private const val PUBLIC_READ_TIMEOUT_SECONDS = 60L
    private const val PUBLIC_WRITE_TIMEOUT_SECONDS = 60L

    private var authenticatedRetrofit: Retrofit? = null
    private var authenticatedApiService: RemitosApiService? = null

    private var publicRetrofit: Retrofit? = null
    private var publicApiService: RemitosApiService? = null

    val isInitialized: Boolean
        get() = authenticatedRetrofit != null && authenticatedApiService != null

    /**
     * Get or create the authenticated Retrofit instance.
     */
    fun getRetrofit(authManager: AuthManager): Retrofit {
        return authenticatedRetrofit ?: synchronized(this) {
            authenticatedRetrofit ?: createAuthenticatedRetrofit(authManager).also {
                authenticatedRetrofit = it
            }
        }
    }

    /**
     * Get or create the authenticated API service (JWT via [AuthInterceptor] when applicable).
     */
    fun getApiService(authManager: AuthManager): RemitosApiService {
        return authenticatedApiService ?: synchronized(this) {
            authenticatedApiService ?: getRetrofit(authManager)
                .create(RemitosApiService::class.java)
                .also { authenticatedApiService = it }
        }
    }

    /**
     * Authenticated service if already created, otherwise null.
     */
    fun getApiServiceIfInitialized(): RemitosApiService? = authenticatedApiService

    /**
     * Public API calls: no auth interceptor, lenient Gson, extended read/write timeouts (e.g. device
     * registration, login, cloud OCR [scan] without bearer).
     */
    fun getPublicApiService(): RemitosApiService {
        return publicApiService ?: synchronized(this) {
            publicApiService ?: createPublicRetrofit()
                .create(RemitosApiService::class.java)
                .also { publicApiService = it }
        }
    }

    /**
     * Short-lived API client with a Bearer token (e.g. after login before AuthManager persists tokens).
     */
    fun createBearerApiService(accessToken: String): RemitosApiService {
        val baseUrl = FeatureFlags.backendBaseUrl
            ?: throw IllegalStateException("Backend base URL not configured. Call FeatureFlags.configureBackendMode() first.")
        val gson = GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .create()
        val client = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("Authorization", "Bearer $accessToken")
                        .build(),
                )
            }
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(RemitosApiService::class.java)
    }

    private fun createPublicRetrofit(): Retrofit {
        val baseUrl = FeatureFlags.backendBaseUrl
            ?: throw IllegalStateException("Backend base URL not configured.")

        val gson = GsonBuilder()
            .setLenient()
            .create()

        val client = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(PUBLIC_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(PUBLIC_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .apply {
                if (com.remitos.app.BuildConfig.DEBUG) {
                    addInterceptor(createLoggingInterceptor())
                }
                retryOnConnectionFailure(true)
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    /**
     * Reset all cached clients (tests or process reconfiguration).
     */
    fun reset() {
        synchronized(this) {
            authenticatedRetrofit = null
            authenticatedApiService = null
            publicRetrofit = null
            publicApiService = null
        }
    }

    private fun createAuthenticatedRetrofit(authManager: AuthManager): Retrofit {
        val baseUrl = FeatureFlags.backendBaseUrl
            ?: throw IllegalStateException("Backend base URL not configured. Call FeatureFlags.configureBackendMode() first.")

        val client = createAuthenticatedOkHttpClient(authManager)
        val gson = createAuthenticatedGson()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    private fun createAuthenticatedOkHttpClient(authManager: AuthManager): OkHttpClient {
        return OkHttpClient.Builder().apply {
            connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)

            addInterceptor(AuthInterceptor(authManager))

            if (com.remitos.app.BuildConfig.DEBUG) {
                addInterceptor(createLoggingInterceptor())
            }

            retryOnConnectionFailure(true)
        }.build()
    }

    private fun createLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    private fun createAuthenticatedGson(): Gson {
        return GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .create()
    }
}
