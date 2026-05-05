package com.remitos.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.remitos.app.data.AuthManager
import com.remitos.app.data.FeatureFlags
import com.remitos.app.network.ApiClient
import com.remitos.app.network.ChangePasswordRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed class ChangePasswordUiState {
    data object Idle : ChangePasswordUiState()
    data object Loading : ChangePasswordUiState()
    data object Success : ChangePasswordUiState()
    data class Error(val message: String) : ChangePasswordUiState()
}

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val authManager: AuthManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChangePasswordUiState>(ChangePasswordUiState.Idle)
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    fun submit(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            if (FeatureFlags.backendBaseUrl == null) {
                _uiState.value = ChangePasswordUiState.Error("Backend no configurado.")
                return@launch
            }
            if (!ApiClient.isInitialized) {
                ApiClient.getApiService(authManager)
            }
            val api = ApiClient.getApiService(authManager)

            _uiState.value = ChangePasswordUiState.Loading
            try {
                val response = withContext(Dispatchers.IO) {
                    api.changePassword(
                        ChangePasswordRequest(
                            currentPassword = currentPassword,
                            newPassword = newPassword,
                        ),
                    )
                }
                if (response.isSuccessful) {
                    _uiState.value = ChangePasswordUiState.Success
                } else {
                    val raw = response.errorBody()?.string()?.trim().orEmpty()
                    _uiState.value = ChangePasswordUiState.Error(
                        raw.ifEmpty { "No se pudo actualizar (${response.code()})." },
                    )
                }
            } catch (e: Exception) {
                _uiState.value =
                    ChangePasswordUiState.Error(e.message ?: "Error de red.")
            }
        }
    }
}
