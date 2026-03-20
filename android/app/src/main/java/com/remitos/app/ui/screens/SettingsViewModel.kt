package com.remitos.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.remitos.app.data.SettingsStore
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

    fun setPerspectiveCorrectionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setPerspectiveCorrectionEnabled(enabled)
        }
    }
}
