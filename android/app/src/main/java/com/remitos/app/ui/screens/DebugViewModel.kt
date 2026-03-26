package com.remitos.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.remitos.app.data.RemitosRepository
import com.remitos.app.data.db.entity.DebugLogEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class DebugViewModel @Inject constructor(
    val repository: RemitosRepository
) : ViewModel() {
    val logs: StateFlow<List<DebugLogEntity>> = repository.observeDebugLogs().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    suspend fun createTestRemito(): Long {
        return withContext(Dispatchers.IO) {
            repository.createTestInboundNote()
        }
    }
}
