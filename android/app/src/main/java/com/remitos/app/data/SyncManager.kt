package com.remitos.app.data

import android.content.Context
import android.util.Log
import com.remitos.app.network.ApiClient
import com.remitos.app.network.UserStatusResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import com.remitos.app.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
sealed class SyncState {
    data object Idle : SyncState()
    data object Syncing : SyncState()
    data object Success : SyncState()
    data class Error(val message: String) : SyncState()
    data object UserSuspended : SyncState()
    data object DeviceRevoked : SyncState()
}

class SyncManager(
    private val context: Context,
    private val authManager: AuthManager,
    private val networkMonitor: NetworkMonitor
) {
    companion object {
        private const val TAG = "SyncManager"
        private const val PREFS_NAME = "sync_prefs"
        private const val KEY_LAST_SYNC_TIMESTAMP = "last_sync_timestamp"
        private const val MAX_SYNC_ATTEMPTS = 3
        private const val SYNC_RETRY_BASE_DELAY_MS = 1200L
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    /** One-shot informational message (e.g. uploads gated); UI shows as Snackbar then calls [consumeSyncSnackbarNotice]. */
    private val _syncSnackbarNotice = MutableStateFlow<String?>(null)
    val syncSnackbarNotice: StateFlow<String?> = _syncSnackbarNotice.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private var wasOffline = false

    fun startMonitoring() {
        CoroutineScope(Dispatchers.IO).launch {
            networkMonitor.isOnline.collect { isOnline ->
                if (isOnline && !wasOffline) {
                    delay(2500)
                    if (networkMonitor.isCurrentlyOnline()) {
                        syncIfNeeded()
                    }
                }
                wasOffline = !isOnline
            }
        }
    }

    fun syncIfNeeded() {
        if (_isSyncing.value) return
        if (!FeatureFlags.enableCloudSync) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                _isSyncing.value = true
                _syncState.value = SyncState.Syncing
                _syncMessage.value = "Verificando estado..."

                val statusResponse = checkUserStatus()

                if (statusResponse != null) {
                    when {
                        statusResponse.userStatus != "active" -> {
                            _isSyncing.value = false
                            _syncState.value = SyncState.UserSuspended
                            _syncMessage.value = null
                            return@launch
                        }
                        statusResponse.deviceStatus == "revoked" -> {
                            _isSyncing.value = false
                            _syncState.value = SyncState.DeviceRevoked
                            _syncMessage.value = null
                            return@launch
                        }
                    }
                }

                _syncMessage.value = "Sincronizando datos..."
                val result = performFullSyncWithRetries()

                when (result) {
                    is SyncService.SyncResult.Success -> {
                        saveLastSyncTimestamp(result.serverTimestamp)
                        _syncState.value = SyncState.Success
                        if (result.uploadsWereBlockedByEntitlement) {
                            _syncSnackbarNotice.value =
                                context.getString(R.string.sync_uploads_blocked_entitlement)
                        }
                    }
                    is SyncService.SyncResult.Error -> {
                        _syncState.value = SyncState.Error(result.message)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sync failed", e)
                _syncState.value = SyncState.Error(e.message ?: "Error de sincronización")
            } finally {
                _isSyncing.value = false
                _syncMessage.value = null
            }
        }
    }

    private suspend fun performFullSyncWithRetries(): SyncService.SyncResult {
        var lastError: SyncService.SyncResult.Error? = null
        repeat(MAX_SYNC_ATTEMPTS) { attempt ->
            when (val r = performFullSyncOnce()) {
                is SyncService.SyncResult.Success -> return r
                is SyncService.SyncResult.Error -> {
                    lastError = r
                    if (r.isTransient && attempt < MAX_SYNC_ATTEMPTS - 1) {
                        delay(SYNC_RETRY_BASE_DELAY_MS * (attempt + 1))
                    } else {
                        return r
                    }
                }
            }
        }
        return lastError ?: SyncService.SyncResult.Error(
            "Error de sincronización.",
            isTransient = false,
        )
    }

    private suspend fun performFullSyncOnce(): SyncService.SyncResult {
        val userId = authManager.getCurrentUser()
            ?: return SyncService.SyncResult.Error(
                "No autenticado. Iniciá sesión para sincronizar.",
                isTransient = false,
            )

        val db = DatabaseManager.getDatabase(context, userId)
        val syncService = SyncService(context, authManager, db)
        val lastSyncTimestamp = getLastSyncTimestamp()

        return syncService.performFullSync(lastSyncTimestamp)
    }

    fun consumeSyncSnackbarNotice() {
        _syncSnackbarNotice.value = null
    }

    private suspend fun checkUserStatus(): UserStatusResponse? {
        return try {
            val userId = authManager.getCurrentUser() ?: return null
            val token = authManager.getTokenSync(userId) ?: return null

            val service = ApiClient.getApiService(authManager)
            val response = service.getUserStatus()

            if (response.isSuccessful) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.d(TAG, "User status check failed (offline?)", e)
            null
        }
    }

    private fun getLastSyncTimestamp(): Long {
        return prefs.getLong(KEY_LAST_SYNC_TIMESTAMP, 0L)
    }

    private fun saveLastSyncTimestamp(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_SYNC_TIMESTAMP, timestamp).apply()
    }

    fun resetState() {
        _syncState.value = SyncState.Idle
        _syncSnackbarNotice.value = null
    }
}