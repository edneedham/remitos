package com.remitos.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.remitos.app.data.InboundNoteStatus
import com.remitos.app.data.RemitosRepository
import com.remitos.app.data.db.entity.InboundNoteEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InboundDetailViewModel(
    private val noteId: Long,
    private val repository: RemitosRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private var currentNote: InboundNoteEntity? = null

    private val _uiState = MutableStateFlow(InboundDetailUiState())
    val uiState: StateFlow<InboundDetailUiState> = _uiState

    private val _saveState = MutableStateFlow<InboundDetailSaveState?>(null)
    val saveState: StateFlow<InboundDetailSaveState?> = _saveState

    init {
        loadNote()
    }

    fun updateDraft(value: InboundDraftState) {
        _uiState.value = _uiState.value.copy(draft = value)
    }

    fun save() {
        val state = _uiState.value
        val note = currentNote ?: return
        if (state.isSaving || state.isVoiding) return
        if (state.status == InboundNoteStatus.Anulada) {
            _saveState.value = InboundDetailSaveState.Error("El ingreso está anulado.")
            return
        }

        val draft = state.draft
        val missing = draft.missingFields()
        if (missing.isNotEmpty()) {
            _uiState.value = state.copy(showMissingErrors = true)
            _saveState.value = InboundDetailSaveState.Error("Completá los datos requeridos.")
            return
        }

        val cantBultos = draft.cantBultosTotal.toIntOrNull() ?: 0
        if (cantBultos <= 0) {
            _uiState.value = state.copy(showMissingErrors = true)
            _saveState.value = InboundDetailSaveState.Error("La cantidad de bultos debe ser mayor a cero.")
            return
        }

        _uiState.value = state.copy(isSaving = true)
        _saveState.value = null
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val updated = note.copy(
                    senderCuit = draft.senderCuit.trim(),
                    senderNombre = draft.senderNombre.trim(),
                    senderApellido = draft.senderApellido.trim(),
                    destNombre = draft.destNombre.trim(),
                    destApellido = draft.destApellido.trim(),
                    destDireccion = draft.destDireccion.trim(),
                    destTelefono = draft.destTelefono.trim(),
                    cantBultosTotal = cantBultos,
                    remitoNumCliente = draft.remitoNumCliente.trim(),
                    remitoNumInterno = note.remitoNumInterno,
                    updatedAt = now,
                )
                withContext(ioDispatcher) {
                    repository.updateInboundNote(updated)
                }
                currentNote = updated
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    updatedAt = now,
                    showMissingErrors = false,
                )
                _saveState.value = InboundDetailSaveState.Success
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false)
                _saveState.value = InboundDetailSaveState.Error(
                    error.message ?: "No se pudo guardar el ingreso. Intentá de nuevo."
                )
            }
        }
    }

    fun voidNote() {
        val state = _uiState.value
        if (state.isSaving || state.isVoiding) return
        _uiState.value = state.copy(isVoiding = true)
        viewModelScope.launch {
            try {
                withContext(ioDispatcher) {
                    repository.voidInboundNote(noteId)
                }
                val updatedAt = System.currentTimeMillis()
                currentNote = currentNote?.copy(status = InboundNoteStatus.Anulada, updatedAt = updatedAt)
                _uiState.value = _uiState.value.copy(
                    isVoiding = false,
                    status = InboundNoteStatus.Anulada,
                    updatedAt = updatedAt,
                )
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(isVoiding = false)
                _saveState.value = InboundDetailSaveState.Error(
                    error.message ?: "No se pudo anular el ingreso. Intentá de nuevo."
                )
            }
        }
    }

    fun clearSaveState() {
        _saveState.value = null
    }

    private fun loadNote() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            val note = withContext(ioDispatcher) { repository.getInboundNote(noteId) }
            if (note == null) {
                _uiState.value = InboundDetailUiState(
                    isLoading = false,
                    errorMessage = "No se encontró el ingreso.",
                )
            } else {
                currentNote = note
                _uiState.value = InboundDetailUiState(
                    isLoading = false,
                    noteId = note.id,
                    status = note.status,
                    createdAt = note.createdAt,
                    updatedAt = note.updatedAt,
                    scanImagePath = note.scanImagePath,
                    draft = InboundDraftState(
                        senderCuit = note.senderCuit,
                        senderNombre = note.senderNombre,
                        senderApellido = note.senderApellido,
                        destNombre = note.destNombre,
                        destApellido = note.destApellido,
                        destDireccion = note.destDireccion,
                        destTelefono = note.destTelefono,
                        cantBultosTotal = note.cantBultosTotal.toString(),
                        remitoNumCliente = note.remitoNumCliente,
                        remitoNumInterno = note.remitoNumInterno,
                    ),
                )
            }
        }
    }
}

data class InboundDetailUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isVoiding: Boolean = false,
    val showMissingErrors: Boolean = false,
    val errorMessage: String? = null,
    val noteId: Long = 0L,
    val status: String = InboundNoteStatus.Activa,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val scanImagePath: String? = null,
    val draft: InboundDraftState = InboundDraftState(),
)

sealed interface InboundDetailSaveState {
    data object Success : InboundDetailSaveState
    data class Error(val message: String) : InboundDetailSaveState
}
