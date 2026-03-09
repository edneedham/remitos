package com.remitos.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.remitos.app.data.SettingsStore
import com.remitos.app.data.UsageStats
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ActivityViewModel(private val settingsStore: SettingsStore) : ViewModel() {
    val usageStats: StateFlow<UsageStats> = settingsStore.usageStats.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        UsageStats(
            totalScans = 0L,
            successfulParses = 0L,
            manualCorrections = 0L,
            totalScanTimeMs = 0L,
            lastScanTimeMs = 0L,
        ),
    )
}
