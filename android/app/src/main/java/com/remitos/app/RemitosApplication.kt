package com.remitos.app

import android.app.Application
import com.remitos.app.data.AuthManager
import com.remitos.app.data.DatabaseManager
import com.remitos.app.data.FeatureFlags
import com.remitos.app.data.RemitosRepository
import com.remitos.app.data.SessionManager
import com.remitos.app.data.SettingsStore
import com.remitos.app.data.TestDataGenerator
import com.remitos.app.data.db.AppDatabase
import com.remitos.app.network.RemitosApiService
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltAndroidApp
class RemitosApplication : Application() {

    @Inject
    lateinit var authManager: AuthManager
    
    @Inject
    lateinit var settingsStore: SettingsStore
    
    @Inject
    lateinit var apiService: RemitosApiService

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
        FeatureFlags.configureBackendMode("https://remitos-api-865349418409.southamerica-east1.run.app")
        super.onCreate()

        // Initialize session manager
        sessionManager = SessionManager(
            context = this,
            authManager = authManager,
            onSessionExpired = {
                clearCurrentUserContext()
            }
        )
        sessionManager.initialize(this)

        // Initialize with existing session if available
        runBlocking {
            initializeCurrentUserContext()
        }
    }

    /**
     * Initialize database and repository for the current logged-in user.
     * Call this after successful login.
     */
    suspend fun initializeCurrentUserContext(): Boolean {
        val userId = authManager.getCurrentUser()
        return if (userId != null) {
            currentDatabase = DatabaseManager.getDatabase(this, userId)
            currentRepository = currentDatabase?.let { RemitosRepository(it) }
            sessionManager.resetSession()
            
            // Auto-generate demo data for admin user on first login
            if (userId == "admin") {
                currentRepository?.let { repo ->
                    val existingNotes = repo.observeInboundNotes().first()
                    if (existingNotes.isEmpty()) {
                        val generator = TestDataGenerator(repo)
                        generator.generateTestData()
                        generator.seedUsageStats(this@RemitosApplication)
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
