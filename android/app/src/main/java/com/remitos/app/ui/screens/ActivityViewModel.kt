package com.remitos.app.ui.screens

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.remitos.app.data.OutboundSearchFilters
import com.remitos.app.data.RemitosRepository
import com.remitos.app.data.SettingsStore
import com.remitos.app.data.UsageStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
    private val repository: RemitosRepository
) : ViewModel() {
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

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState

    fun exportData(context: Context) {
        _exportState.value = ExportState.Exporting
        viewModelScope.launch {
            try {
                val csvFile = withContext(Dispatchers.IO) {
                    // Fetch data
                    val outboundLists = repository.searchOutboundLists(OutboundSearchFilters(query = ""))
                    
                    // Convert to CSV and save to cache
                    CsvExporter.exportFullActivityToCsv(context, outboundLists)
                }
                
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    csvFile
                )
                
                _exportState.value = ExportState.Success(uri)
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Error al exportar datos")
            }
        }
    }

    fun clearExportState() {
        _exportState.value = ExportState.Idle
    }
}

sealed interface ExportState {
    data object Idle : ExportState
    data object Exporting : ExportState
    data class Success(val uri: Uri) : ExportState
    data class Error(val message: String) : ExportState
}
