package com.remitos.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.remitos.app.data.SettingsStore
import com.remitos.app.data.UploadTiming
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsStore: SettingsStore
) : ViewModel() {
    val perspectiveCorrectionEnabled: StateFlow<Boolean> =
        settingsStore.perspectiveCorrectionEnabled.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            true
        )

    val uploadTiming: StateFlow<UploadTiming> =
        settingsStore.uploadTiming.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            UploadTiming.IMMEDIATE
        )

    fun setPerspectiveCorrectionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setPerspectiveCorrectionEnabled(enabled)
        }
    }

    fun setUploadTiming(timing: UploadTiming) {
        viewModelScope.launch {
            settingsStore.setUploadTiming(timing)
        }
    }
}
