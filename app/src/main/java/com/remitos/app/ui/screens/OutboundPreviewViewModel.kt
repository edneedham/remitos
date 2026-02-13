package com.remitos.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.remitos.app.data.OutboundLineStatus
import com.remitos.app.data.OutboundListStatus
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

    fun markInTransit(listId: Long) {
        val current = _state.value as? OutboundPreviewState.Ready ?: return
        if (current.isUpdating) return
        _state.value = current.copy(isUpdating = true, message = null)
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.markOutboundInTransit(listId)
                }
                val updatedLines = current.lines.map { line ->
                    if (isFinalStatus(line.status)) line else line.copy(status = OutboundLineStatus.EnTransito)
                }
                _state.value = current.copy(lines = updatedLines, isUpdating = false)
            } catch (error: Exception) {
                _state.value = current.copy(
                    isUpdating = false,
                    message = "No se pudo marcar en tránsito. Intentá de nuevo."
                )
            }
        }
    }

    fun updateLineOutcome(lineId: Long, status: String) {
        val current = _state.value as? OutboundPreviewState.Ready ?: return
        if (current.isUpdating) return
        val line = current.lines.firstOrNull { it.id == lineId } ?: return
        _state.value = current.copy(isUpdating = true, message = null)
        viewModelScope.launch {
            try {
        val deliveredQty = if (status == OutboundLineStatus.Entregado) line.packageQty else 0
        val returnedQty = if (status == OutboundLineStatus.Devuelto) line.packageQty else 0
                withContext(Dispatchers.IO) {
                    repository.updateOutboundLineOutcome(lineId, status, deliveredQty, returnedQty)
                }
                val updatedLines = current.lines.map {
                    if (it.id == lineId) {
                        it.copy(status = status, deliveredQty = deliveredQty, returnedQty = returnedQty)
                    } else {
                        it
                    }
                }
                _state.value = current.copy(lines = updatedLines, isUpdating = false)
            } catch (error: Exception) {
                _state.value = current.copy(
                    isUpdating = false,
                    message = "No se pudo actualizar el estado. Intentá de nuevo."
                )
            }
        }
    }

    fun closeList(listId: Long) {
        val current = _state.value as? OutboundPreviewState.Ready ?: return
        if (current.isClosing) return
        if (!canCloseList(current.lines)) {
            _state.value = current.copy(message = "Aún hay entregas pendientes.")
            return
        }
        _state.value = current.copy(isClosing = true, message = null)
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.closeOutboundList(listId)
                }
                val updatedList = current.list.copy(status = OutboundListStatus.Cerrada)
                _state.value = current.copy(list = updatedList, isClosing = false)
            } catch (error: Exception) {
                _state.value = current.copy(
                    isClosing = false,
                    message = "No se pudo cerrar la lista. Intentá de nuevo."
                )
            }
        }
    }
}

sealed interface OutboundPreviewState {
    data object Loading : OutboundPreviewState
    data class Ready(
        val list: OutboundListEntity,
        val lines: List<OutboundLineWithRemito>,
        val isUpdating: Boolean = false,
        val isClosing: Boolean = false,
        val message: String? = null,
    ) : OutboundPreviewState
    data class Error(val message: String) : OutboundPreviewState
}

internal fun canCloseList(lines: List<OutboundLineWithRemito>): Boolean {
    return lines.isNotEmpty() && lines.all { isFinalStatus(it.status) }
}

internal fun isFinalStatus(status: String): Boolean {
    return status == OutboundLineStatus.Entregado || status == OutboundLineStatus.Devuelto
}
