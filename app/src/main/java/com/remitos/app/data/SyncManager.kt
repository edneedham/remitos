package com.remitos.app.data

import android.content.Context
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
                    // Network restored - wait 2-3 seconds then sync
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

        CoroutineScope(Dispatchers.IO).launch {
            try {
                _isSyncing.value = true
                _syncState.value = SyncState.Syncing
                _syncMessage.value = "Verificando estado..."

                // Check user status first
                val statusResponse = checkUserStatus()
                
                if (statusResponse != null) {
                    when {
                        statusResponse.userStatus != "active" -> {
                            _syncState.value = SyncState.UserSuspended
                            _syncMessage.value = null
                            return@launch
                        }
                        statusResponse.deviceStatus == "revoked" -> {
                            _syncState.value = SyncState.DeviceRevoked
                            _syncMessage.value = null
                            return@launch
                        }
                    }
                }

                // If user is active, do full sync
                _syncMessage.value = "Sincronizando datos..."
                performFullSync()

                _syncState.value = SyncState.Success
                _syncMessage.value = null
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "Error de sincronización")
                _syncMessage.value = null
            } finally {
                _isSyncing.value = false
            }
        }
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
            null
        }
    }

    private suspend fun performFullSync() {
        withContext(Dispatchers.IO) {
            try {
                val userId = authManager.getCurrentUser() ?: return@withContext
                val db = DatabaseManager.getOfflineDatabase(context)
                
                // Get unsynced data
                val unsyncedDocuments = db.localDocumentDao().getUnsynced()
                val unsyncedCodes = db.localScannedCodeDao().getUnsynced()

                // For now, we'll just update local activity timestamp
                // Full sync implementation would send unsynced data to server
                // and receive updates in response
                db.localSessionDao().updateLastActivity(System.currentTimeMillis())
            } catch (e: Exception) {
                // Log but don't fail
            }
        }
    }

    fun resetState() {
        _syncState.value = SyncState.Idle
    }
}
