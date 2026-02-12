package com.remitos.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.remitos.app.data.RemitosRepository
import com.remitos.app.data.db.entity.DebugLogEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class DebugViewModel(private val repository: RemitosRepository) : ViewModel() {
    val logs: StateFlow<List<DebugLogEntity>> = repository.observeDebugLogs().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
}
