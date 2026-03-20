package com.remitos.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.remitos.app.data.OutboundLineStatus
import com.remitos.app.data.OutboundListStatus
import com.remitos.app.data.RemitosRepository
import com.remitos.app.data.SettingsStore
import com.remitos.app.data.TemplateConfig
import com.remitos.app.data.db.entity.OutboundLineEditHistoryEntity
import com.remitos.app.data.db.entity.OutboundLineStatusHistoryEntity
import com.remitos.app.data.db.entity.OutboundLineWithRemito
import com.remitos.app.data.db.entity.OutboundListEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class OutboundPreviewViewModel @Inject constructor(
    private val repository: RemitosRepository,
    private val settingsStore: SettingsStore
) : ViewModel() {
    private val _state = MutableStateFlow<OutboundPreviewState>(OutboundPreviewState.Loading)
    val state: StateFlow<OutboundPreviewState> = _state
    private val _historyState = MutableStateFlow<OutboundLineHistoryState?>(null)
    val historyState: StateFlow<OutboundLineHistoryState?> = _historyState
    private val _editState = MutableStateFlow<OutboundLineEditState?>(null)
    val editState: StateFlow<OutboundLineEditState?> = _editState

    suspend fun getTemplateConfig(): TemplateConfig {
        return settingsStore.getTemplateConfig()
    }

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
        if (line.status == status) return
        _state.value = current.copy(isUpdating = true, message = null)
        viewModelScope.launch {
            try {
                val deliveredQty = if (status == OutboundLineStatus.Entregado) line.packageQty else 0
                val returnedQty = 0
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

    fun loadLineHistory(lineId: Long) {
        val current = _state.value as? OutboundPreviewState.Ready ?: return
        val line = current.lines.firstOrNull { it.id == lineId } ?: return
        _historyState.value = OutboundLineHistoryState(
            line = line,
            statusEntries = emptyList(),
            editEntries = emptyList(),
            isLoading = true,
        )
        viewModelScope.launch {
            try {
                val statusEntries = withContext(Dispatchers.IO) {
                    repository.getOutboundLineStatusHistory(lineId)
                }
                val editEntries = withContext(Dispatchers.IO) {
                    repository.getOutboundLineEditHistory(lineId)
                }
                _historyState.value = OutboundLineHistoryState(
                    line = line,
                    statusEntries = statusEntries,
                    editEntries = editEntries,
                )
            } catch (error: Exception) {
                _historyState.value = OutboundLineHistoryState(
                    line = line,
                    statusEntries = emptyList(),
                    editEntries = emptyList(),
                    message = "No se pudo cargar el historial. Intentá de nuevo."
                )
            }
        }
    }

    fun clearLineHistory() {
        _historyState.value = null
    }

    fun loadLineEdit(lineId: Long) {
        val current = _state.value as? OutboundPreviewState.Ready ?: return
        val line = current.lines.firstOrNull { it.id == lineId } ?: return
        _editState.value = OutboundLineEditState(
            line = line,
            draft = OutboundLineEditDraft(
                lineId = line.id,
                deliveryNumber = line.deliveryNumber,
                recipientNombre = line.recipientNombre,
                recipientApellido = line.recipientApellido,
                recipientDireccion = line.recipientDireccion,
                recipientTelefono = line.recipientTelefono,
                missingQty = line.missingQty.toString(),
                reason = "",
            )
        )
    }

    fun updateLineEditDraft(draft: OutboundLineEditDraft) {
        val current = _editState.value ?: return
        _editState.value = current.copy(draft = draft, message = null)
    }

    fun saveLineEdit() {
        val current = _editState.value ?: return
        if (current.isSaving) return
        val draft = current.draft

        val validationError = validateEditDraft(draft)
        if (validationError != null) {
            _editState.value = current.copy(message = validationError)
            return
        }

        _editState.value = current.copy(isSaving = true, message = null)
        viewModelScope.launch {
            try {
                val missingQty = draft.missingQty.toIntOrNull() ?: 0
                withContext(Dispatchers.IO) {
                    repository.updateOutboundLineDetails(
                        lineId = draft.lineId,
                        deliveryNumber = draft.deliveryNumber.trim(),
                        recipientNombre = draft.recipientNombre.trim(),
                        recipientApellido = draft.recipientApellido.trim(),
                        recipientDireccion = draft.recipientDireccion.trim(),
                        recipientTelefono = draft.recipientTelefono.trim(),
                        missingQty = missingQty,
                        reason = draft.reason.trim(),
                    )
                }
                val state = _state.value as? OutboundPreviewState.Ready
                if (state != null) {
                    val updatedLines = state.lines.map { line ->
                        if (line.id == draft.lineId) {
                            line.copy(
                                deliveryNumber = draft.deliveryNumber.trim(),
                                recipientNombre = draft.recipientNombre.trim(),
                                recipientApellido = draft.recipientApellido.trim(),
                                recipientDireccion = draft.recipientDireccion.trim(),
                                recipientTelefono = draft.recipientTelefono.trim(),
                                missingQty = missingQty,
                            )
                        } else {
                            line
                        }
                    }
                    _state.value = state.copy(lines = updatedLines)
                }
                _editState.value = null
            } catch (error: Exception) {
                _editState.value = current.copy(
                    isSaving = false,
                    message = "No se pudo guardar los cambios. Intentá de nuevo."
                )
            }
        }
    }

    fun clearLineEdit() {
        _editState.value = null
    }

    private fun validateEditDraft(draft: OutboundLineEditDraft): String? {
        if (draft.deliveryNumber.isBlank()) return "Completá el número de entrega."
        if (draft.recipientNombre.isBlank() || draft.recipientApellido.isBlank()) {
            return "Completá el nombre y apellido del destinatario."
        }
        if (draft.recipientDireccion.isBlank()) return "Completá la dirección del destinatario."
        if (draft.recipientTelefono.isBlank()) return "Completá el teléfono del destinatario."
        val missingQty = draft.missingQty.toIntOrNull()
        if (missingQty == null || missingQty < 0) return "La cantidad de faltantes debe ser válida."
        if (draft.reason.isBlank()) return "Completá el motivo del cambio."
        return null
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

data class OutboundLineHistoryState(
    val line: OutboundLineWithRemito,
    val statusEntries: List<OutboundLineStatusHistoryEntity>,
    val editEntries: List<OutboundLineEditHistoryEntity>,
    val isLoading: Boolean = false,
    val message: String? = null,
)

data class OutboundLineEditState(
    val line: OutboundLineWithRemito,
    val draft: OutboundLineEditDraft,
    val isSaving: Boolean = false,
    val message: String? = null,
)

data class OutboundLineEditDraft(
    val lineId: Long,
    val deliveryNumber: String,
    val recipientNombre: String,
    val recipientApellido: String,
    val recipientDireccion: String,
    val recipientTelefono: String,
    val missingQty: String,
    val reason: String,
)

internal fun canCloseList(lines: List<OutboundLineWithRemito>): Boolean {
    return lines.isNotEmpty() && lines.all { isFinalStatus(it.status) }
}

internal fun isFinalStatus(status: String): Boolean {
    return status == OutboundLineStatus.Entregado
}
