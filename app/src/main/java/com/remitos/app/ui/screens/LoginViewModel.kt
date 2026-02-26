package com.remitos.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.remitos.app.data.AuthManager
import com.remitos.app.data.FeatureFlags
import com.remitos.app.data.TokenData
import com.remitos.app.data.UserInfo
import com.remitos.app.network.ApiClient
import com.remitos.app.network.LoginRequest
import com.remitos.app.network.RemitosApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

/**
 * UI states for the login screen.
 */
sealed class LoginUiState {
    data object Idle : LoginUiState()
    data object Loading : LoginUiState()
    data object Success : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

/**
 * ViewModel for the Login screen.
 * Handles authentication logic and account management.
 */
class LoginViewModel(
    private val authManager: AuthManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _accounts = MutableStateFlow<List<UserInfo>>(emptyList())
    val accounts: StateFlow<List<UserInfo>> = _accounts.asStateFlow()

    init {
        loadSavedAccounts()
    }

    /**
     * Load previously logged-in accounts for quick switching.
     */
    private fun loadSavedAccounts() {
        viewModelScope.launch {
            _accounts.value = authManager.listLoggedInUsers()
        }
    }

    /**
     * Attempt to login with company code, username and password.
     */
    fun login(companyCode: String, username: String, password: String, deviceName: String? = null) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading

            try {
                // Check if backend is configured
                if (FeatureFlags.backendBaseUrl == null) {
                    _uiState.value = LoginUiState.Error(
                        "Backend no configurado. Contacte al administrador."
                    )
                    return@launch
                }

                // Initialize API client if needed
                if (!ApiClient.isInitialized) {
                    ApiClient.getApiService(authManager)
                }

                val apiService = ApiClient.getApiService(authManager)

                // Make login request
                val response = apiService.login(
                    LoginRequest(
                        companyCode = companyCode.trim().uppercase(),
                        username = username.trim(),
                        password = password,
                        deviceName = deviceName
                    )
                )

                if (response.isSuccessful) {
                    val authResponse = response.body()
                    if (authResponse != null) {
                        // Save tokens - use username as identifier since we don't have user ID from this response
                        val userId = username.lowercase()
                        val tokenData = TokenData(
                            accessToken = authResponse.token,
                            refreshToken = authResponse.refreshToken,
                            expiresAt = calculateExpiryTime(authResponse.expiresIn),
                            userEmail = username,
                            userName = username
                        )

                        authManager.saveToken(userId, tokenData)
                        authManager.setCurrentUser(userId)

                        _uiState.value = LoginUiState.Success
                    } else {
                        _uiState.value = LoginUiState.Error("Respuesta vacía del servidor")
                    }
                } else {
                    val errorMessage = when (response.code()) {
                        401 -> "Código de empresa, usuario o contraseña incorrectos"
                        403 -> "Cuenta desactivada. Contacte al administrador."
                        429 -> "Demasiados intentos. Intente más tarde."
                        in 500..599 -> "Error del servidor. Intente más tarde."
                        else -> "Error de autenticación: ${response.code()}"
                    }
                    _uiState.value = LoginUiState.Error(errorMessage)
                }
            } catch (e: HttpException) {
                _uiState.value = LoginUiState.Error(
                    "Error de conexión: ${e.message ?: "Desconocido"}"
                )
            } catch (e: IOException) {
                _uiState.value = LoginUiState.Error(
                    "Sin conexión a internet. Verifique su red."
                )
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(
                    "Error inesperado: ${e.message ?: "Desconocido"}"
                )
            }
        }
    }

    /**
     * Switch to an already authenticated account.
     */
    suspend fun switchToAccount(userId: String): Boolean {
        return try {
            // Check if token is still valid
            if (authManager.isTokenExpired(userId)) {
                // TODO: Try to refresh token
                // For now, require re-login if expired
                return false
            }

            authManager.setCurrentUser(userId)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Reset the UI state to idle (useful after handling errors).
     */
    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }

    /**
     * Calculate token expiry time from expires_in seconds.
     */
    private fun calculateExpiryTime(expiresInSeconds: Int): Long {
        return System.currentTimeMillis() + (expiresInSeconds * 1000)
    }
}
