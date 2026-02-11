package com.remitos.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.remitos.app.data.RemitosRepository
import com.remitos.app.data.db.entity.OutboundLineWithRemito
import com.remitos.app.data.db.entity.OutboundListEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OutboundPreviewViewModel(
    private val repository: RemitosRepository
) : ViewModel() {
    private val _state = MutableStateFlow<OutboundPreviewState>(OutboundPreviewState.Loading)
    val state: StateFlow<OutboundPreviewState> = _state

    fun load(listId: Long) {
        if (listId <= 0L) {
            _state.value = OutboundPreviewState.Error("No se encontro la lista para previsualizar.")
            return
        }
        _state.value = OutboundPreviewState.Loading
        viewModelScope.launch {
            try {
                val list = withContext(Dispatchers.IO) {
                    repository.getOutboundList(listId)
                }
                if (list == null) {
                    _state.value = OutboundPreviewState.Error("No se encontro la lista para previsualizar.")
                    return@launch
                }
                val lines = withContext(Dispatchers.IO) {
                    repository.getOutboundLinesWithRemito(listId)
                }
                _state.value = OutboundPreviewState.Ready(list, lines)
            } catch (error: Exception) {
                _state.value = OutboundPreviewState.Error(
                    "No se pudo cargar la vista previa. Intentá de nuevo."
                )
            }
        }
    }
}

sealed interface OutboundPreviewState {
    data object Loading : OutboundPreviewState
    data class Ready(
        val list: OutboundListEntity,
        val lines: List<OutboundLineWithRemito>
    ) : OutboundPreviewState
    data class Error(val message: String) : OutboundPreviewState
}
