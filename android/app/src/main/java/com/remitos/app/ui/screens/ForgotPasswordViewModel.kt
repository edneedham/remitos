package com.remitos.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
import com.remitos.app.data.FeatureFlags
import com.remitos.app.network.ForgotPasswordRequest
import com.remitos.app.network.RemitosApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject

sealed class ForgotPasswordUiState {
    data object Idle : ForgotPasswordUiState()
    data object Loading : ForgotPasswordUiState()
    data object Success : ForgotPasswordUiState()
    data class Error(val message: String) : ForgotPasswordUiState()
}

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow<ForgotPasswordUiState>(ForgotPasswordUiState.Idle)
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun submit(companyCode: String, username: String) {
        viewModelScope.launch {
            val base = FeatureFlags.backendBaseUrl
            if (base.isNullOrBlank()) {
                _uiState.value =
                    ForgotPasswordUiState.Error("Backend no configurado.")
                return@launch
            }

            _uiState.value = ForgotPasswordUiState.Loading
            try {
                val service = buildPublicApiService(base)
                if (service == null) {
                    _uiState.value =
                        ForgotPasswordUiState.Error("No se pudo conectar al servidor.")
                    return@launch
                }

                val response = withContext(Dispatchers.IO) {
                    service.forgotPassword(
                        ForgotPasswordRequest(
                            companyCode = companyCode.trim().uppercase(),
                            username = username.trim(),
                        ),
                    )
                }

                if (response.isSuccessful) {
                    _uiState.value = ForgotPasswordUiState.Success
                } else {
                    val raw = response.errorBody()?.string()?.trim().orEmpty()
                    _uiState.value = ForgotPasswordUiState.Error(
                        raw.ifEmpty { "No se pudo enviar la solicitud (${response.code()})." },
                    )
                }
            } catch (e: Exception) {
                _uiState.value =
                    ForgotPasswordUiState.Error(e.message ?: "Error de red.")
            }
        }
    }

    private fun buildPublicApiService(baseUrl: String): RemitosApiService? {
        return try {
            val gson = GsonBuilder().setLenient().create()
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
            Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(RemitosApiService::class.java)
        } catch (_: Exception) {
            null
        }
    }
}
