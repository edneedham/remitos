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
        baseUrl: String? = null
    ) {
        enableBackendOcr = backendOcr
        enableImageUpload = imageUpload
        enableCloudSync = cloudSync
        backendBaseUrl = baseUrl
    }
}
