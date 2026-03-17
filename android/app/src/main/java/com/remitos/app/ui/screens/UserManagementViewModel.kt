package com.remitos.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.remitos.app.data.db.entity.LocalUserEntity
import com.remitos.app.data.DatabaseManager
import com.remitos.app.network.CreateOperatorRequest
import com.remitos.app.network.RemitosApiService
import com.remitos.app.network.UpdateOperatorPasswordRequest
import com.remitos.app.network.UpdateOperatorStatusRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserManagementViewModel(
    private val apiService: RemitosApiService,
    private val db: DatabaseManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(UserManagementUiState())
    val uiState: StateFlow<UserManagementUiState> = _uiState

    init {
        loadUsers()
    }

    fun loadUsers(context: android.content.Context) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val localDb = DatabaseManager.getOfflineDatabase(context)
                
                // Fetch from backend
                val response = withContext(Dispatchers.IO) { apiService.getOperators() }
                if (response.isSuccessful && response.body() != null) {
                    val operators = response.body()!!
                    
                    // Sync to local DB
                    val now = System.currentTimeMillis()
                    val entities = operators.map { op ->
                        LocalUserEntity(
                            id = op.id,
                            username = op.username ?: op.email ?: "",
                            role = op.role,
                            passwordHash = null, // don't override local pin/hash from backend
                            status = op.status,
                            warehouseId = null,
                            lastSyncedAt = now
                        )
                    }
                    
                    withContext(Dispatchers.IO) {
                        localDb.localUserDao().insertAll(entities)
                    }
                }
                
                // Read latest from local DB
                localDb.localUserDao().observeAll().collect { localUsers ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        users = localUsers
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al cargar usuarios: ${e.message}"
                )
            }
        }
    }

    fun createOperator(context: android.content.Context, email: String, password: String) {
        _uiState.value = _uiState.value.copy(isSaving = true, error = null)
        viewModelScope.launch {
            try {
                val req = CreateOperatorRequest(
                    email = email.ifBlank { null },
                    password = password
                )
                val response = withContext(Dispatchers.IO) { apiService.createOperator(req) }
                if (response.isSuccessful) {
                    loadUsers(context)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = "Error al crear operador"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Error de red: ${e.message}"
                )
            }
        }
    }

    fun toggleStatus(context: android.content.Context, userId: String, currentStatus: String) {
        val newStatus = if (currentStatus == "active") "suspended" else "active"
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.updateOperatorStatus(userId, UpdateOperatorStatusRequest(newStatus))
                }
                if (response.isSuccessful) {
                    val localDb = DatabaseManager.getOfflineDatabase(context)
                    val user = localDb.localUserDao().getById(userId)
                    if (user != null) {
                        localDb.localUserDao().update(user.copy(status = newStatus))
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Error al actualizar estado"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error de red: ${e.message}"
                )
            }
        }
    }

    fun resetPassword(context: android.content.Context, userId: String, newPassword: String) {
        _uiState.value = _uiState.value.copy(isSaving = true, error = null)
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.updateOperatorPassword(userId, UpdateOperatorPasswordRequest(newPassword))
                }
                if (response.isSuccessful) {
                    val localDb = DatabaseManager.getOfflineDatabase(context)
                    val user = localDb.localUserDao().getById(userId)
                    if (user != null) {
                        localDb.localUserDao().update(user.copy(passwordHash = newPassword)) // offline fallback
                    }
                    _uiState.value = _uiState.value.copy(isSaving = false)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = "Error al restablecer contraseña"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Error de red: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun loadUsers() {
        // Initial state before context is available
    }

    companion object {
        fun provideFactory(
            apiService: RemitosApiService,
            db: DatabaseManager
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return UserManagementViewModel(apiService, db) as T
            }
        }
    }
}

data class UserManagementUiState(
    val users: List<LocalUserEntity> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)
