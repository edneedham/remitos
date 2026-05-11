package com.remitos.app

import android.app.Application
import android.util.Log
import com.remitos.app.data.AuthManager
import com.remitos.app.data.DatabaseManager
import com.remitos.app.data.FeatureFlags
import com.remitos.app.data.RemitosRepository
import com.remitos.app.data.SessionManager
import com.remitos.app.data.SettingsStore
import com.remitos.app.data.TestDataGenerator
import com.remitos.app.data.db.AppDatabase
import com.remitos.app.network.AuthInterceptor
import com.remitos.app.network.AuthNetworkSideEffects
import com.remitos.app.network.RemitosApiService
import com.remitos.app.notifications.OperationalNotifier
import com.remitos.app.dev.DevSeedDefaults
import com.remitos.app.workers.ImageUploadWorkerScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

@HiltAndroidApp
class RemitosApplication : Application() {

    private val applicationJob = SupervisorJob()
    private val userContextMutex = Mutex()

    /**
     * App-wide scope for startup and background work tied to process lifetime.
     * Uses [SupervisorJob] so one child failure does not cancel siblings.
     */
    val applicationScope: CoroutineScope = CoroutineScope(applicationJob + Dispatchers.Default)

    @Inject
    lateinit var authManager: AuthManager
    
    @Inject
    lateinit var settingsStore: SettingsStore
    
    @Inject
    lateinit var apiService: RemitosApiService

    @Inject
    lateinit var operationalNotifier: OperationalNotifier

    // Session manager for auto-logout
    lateinit var sessionManager: SessionManager
        private set

    // Current user context - these will be initialized after login
    var currentDatabase: AppDatabase? = null
        private set
    var currentRepository: RemitosRepository? = null
        private set
    
    /**
     * Legacy repository accessor for backward compatibility.
     * Use requireRepository() for null-safe access.
     */
    val repository: RemitosRepository
        get() = requireRepository()

    override fun onCreate() {
        if (BuildConfig.DEBUG) {
            Log.i(
                "RemitosApplication",
                "BACKEND_BASE_URL=${BuildConfig.BACKEND_BASE_URL} (use debug build for local API; release uses production)",
            )
        }
        FeatureFlags.configureBackendMode(BuildConfig.BACKEND_BASE_URL)
        super.onCreate()

        // Initialize session manager
        sessionManager = SessionManager(
            context = this,
            authManager = authManager,
            onSessionExpired = {
                clearCurrentUserContext()
            },
            onBeforeAutoLogout = {
                operationalNotifier.notifyInactivityLogout()
            },
        )
        sessionManager.initialize(this)

        AuthInterceptor.sideEffects = object : AuthNetworkSideEffects {
            override fun onDeviceRevokedFromRefresh() {
                operationalNotifier.notifyDeviceRevoked()
            }

            override fun onRefreshTokenFailed() {
                operationalNotifier.notifyAuthSessionLost()
            }
        }

        // Initialize with existing session if available (non-blocking; Splash may also call this)
        applicationScope.launch {
            initializeCurrentUserContext()
        }

        // Schedule background image upload worker (re-enqueues if periodic interval changed)
        ImageUploadWorkerScheduler.scheduleOrUpdate(this)
    }

    /**
     * Initialize database and repository for the current logged-in user.
     * Call this after successful login. Serialized so concurrent callers (e.g. Application scope
     * and Splash) do not race on [currentRepository].
     */
    suspend fun initializeCurrentUserContext(): Boolean = userContextMutex.withLock {
        val userId = authManager.getCurrentUser()
        return@withLock if (userId != null) {
            currentDatabase = DatabaseManager.getDatabase(this, userId)
            currentRepository = currentDatabase?.let { RemitosRepository(it) }
            sessionManager.resetSession()

            // Auto-generate demo data for local seed owner (or legacy offline admin) on first login
            if (DevSeedDefaults.isSeedOwnerForDemoData(userId)) {
                currentRepository?.let { repo ->
                    val existingNotes = repo.observeInboundNotes().first()
                    if (existingNotes.isEmpty()) {
                        val generator = TestDataGenerator(repo)
                        generator.generateTestData()
                        generator.seedTemplateConfig(this@RemitosApplication)
                    }
                }
            }

            true
        } else {
            currentDatabase = DatabaseManager.getOfflineDatabase(this)
            currentRepository = currentDatabase?.let { RemitosRepository(it) }
            false
        }
    }

    /**
     * Switch to a different user account.
     */
    suspend fun switchUser(userId: String): Boolean {
        clearCurrentUserContext()
        authManager.setCurrentUser(userId)
        return initializeCurrentUserContext()
    }

    /**
     * Logout current user and clear context.
     */
    suspend fun logoutCurrentUser(deleteLocalData: Boolean = false) {
        val userId = authManager.getCurrentUser()

        if (deleteLocalData && userId != null) {
            DatabaseManager.deleteDatabase(this, userId)
        } else {
            clearCurrentUserContext()
        }

        userId?.let { authManager.removeToken(it) }
    }

    /**
     * Clear current database and repository references.
     */
    private fun clearCurrentUserContext() {
        currentRepository = null
        currentDatabase?.close()
        currentDatabase = null
    }

    /**
     * Get the active repository. Throws if not initialized.
     */
    fun requireRepository(): RemitosRepository {
        return currentRepository ?: throw IllegalStateException(
            "Repository not initialized. Call initializeCurrentUserContext() first."
        )
    }

    /**
     * Get the active database. Throws if not initialized.
     */
    fun requireDatabase(): AppDatabase {
        return currentDatabase ?: throw IllegalStateException(
            "Database not initialized. Call initializeCurrentUserContext() first."
        )
    }
}
