package com.remitos.app.data

import android.content.Context
import android.util.Log
import com.remitos.app.network.ApiClient
import com.remitos.app.network.RemitosApiService
import com.remitos.app.network.UserStatusResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

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
                val result = performFullSync()

                when (result) {
                    is SyncService.SyncResult.Success -> {
                        saveLastSyncTimestamp(result.serverTimestamp)
                        _syncState.value = SyncState.Success
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

    private suspend fun performFullSync(): SyncService.SyncResult {
        val userId = authManager.getCurrentUser()
            ?: return SyncService.SyncResult.Error("Not authenticated")

        val db = DatabaseManager.getDatabase(context, userId)
        val syncService = SyncService(context, authManager, db)
        val lastSyncTimestamp = getLastSyncTimestamp()

        return syncService.performFullSync(lastSyncTimestamp)
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
    }
}