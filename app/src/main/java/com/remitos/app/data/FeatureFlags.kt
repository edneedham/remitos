package com.remitos.app.data

/**
 * Feature flags for controlling offline vs backend modes.
 * Used to gradually introduce backend integration without breaking offline functionality.
 */
object FeatureFlags {
    /**
     * When true, the app can use backend services for enhanced OCR.
     * When false, only on-device OCR is available (offline mode).
     */
    var enableBackendOcr: Boolean = false
        private set

    /**
     * When true, scan images are uploaded to the backend for processing.
     * When false, OCR is performed locally on the device.
     */
    var enableImageUpload: Boolean = false
        private set

    /**
     * When true, audit logs and status history are synced with the backend.
     * When false, all data remains local.
     */
    var enableCloudSync: Boolean = false
        private set

    /**
     * Backend API base URL. Null when backend features are disabled.
     */
    var backendBaseUrl: String? = null
        private set
    
    /**
     * Auto-logout time in minutes. 0 = disabled.
     * After this period of inactivity, user is automatically logged out.
     */
    var autoLogoutMinutes: Int = 30
        private set
    
    /**
     * Image compression quality (0-100). Lower = smaller files.
     * Recommended: 60% for ~500KB images.
     */
    var imageQuality: Int = 60
        private set
    
    /**
     * Sync interval in minutes. How often to run background sync.
     */
    var syncIntervalMinutes: Int = 15
        private set
    
    /**
     * Number of days of recent history to load on initial login.
     */
    var lazySyncDays: Int = 7
        private set

    /**
     * Configure feature flags for offline-only mode (default beta behavior).
     */
    fun configureOfflineMode() {
        enableBackendOcr = false
        enableImageUpload = false
        enableCloudSync = false
        backendBaseUrl = null
    }

    /**
     * Configure feature flags for backend integration mode.
     * @param baseUrl The backend API base URL
     */
    fun configureBackendMode(baseUrl: String) {
        enableBackendOcr = true
        enableImageUpload = true
        enableCloudSync = true
        backendBaseUrl = baseUrl
    }

    /**
     * Configure feature flags with granular control.
     */
    fun configure(
        backendOcr: Boolean = false,
        imageUpload: Boolean = false,
        cloudSync: Boolean = false,
        baseUrl: String? = null,
        autoLogout: Int = 30,
        imageQualityPercent: Int = 60,
        syncInterval: Int = 15,
        initialSyncDays: Int = 7
    ) {
        enableBackendOcr = backendOcr
        enableImageUpload = imageUpload
        enableCloudSync = cloudSync
        backendBaseUrl = baseUrl
        autoLogoutMinutes = autoLogout
        imageQuality = imageQualityPercent
        syncIntervalMinutes = syncInterval
        lazySyncDays = initialSyncDays
    }
}
