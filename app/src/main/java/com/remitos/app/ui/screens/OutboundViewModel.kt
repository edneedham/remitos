package com.remitos.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.remitos.app.data.RemitosRepository
import com.remitos.app.data.db.entity.InboundNoteWithAvailable
import com.remitos.app.data.db.entity.OutboundLineEntity
import com.remitos.app.data.db.entity.OutboundListEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OutboundViewModel(
    repository: RemitosRepository
) : ViewModel() {
    private val repository = repository

    val inboundOptions: StateFlow<List<InboundOption>> = repository
        .observeInboundNotesWithAvailable()
        .map { notes ->
            notes.map { note ->
                InboundOption(
                    inboundNoteId = note.note.id,
                    label = buildInboundLabel(note),
                    availableCount = note.availableCount
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isSaving = MutableStateFlow(false)
    val saveState = MutableStateFlow<OutboundSaveState?>(null)
    val printPayload = MutableStateFlow<OutboundPrintPayload?>(null)

    fun save(draft: OutboundDraftState, availableCount: Int) {
        if (isSaving.value) return

        val error = validateDraft(draft, availableCount)
        if (error != null) {
            saveState.value = OutboundSaveState.Error(error)
            return
        }

        isSaving.value = true
        saveState.value = null

        viewModelScope.launch {
            try {
                val listNumber = withContext(Dispatchers.IO) {
                    repository.nextOutboundListNumber()
                }
                val now = System.currentTimeMillis()
                val list = OutboundListEntity(
                    listNumber = listNumber,
                    issueDate = now,
                    driverNombre = draft.driverNombre.trim(),
                    driverApellido = draft.driverApellido.trim(),
                    status = "abierta"
                )

                val line = OutboundLineEntity(
                    outboundListId = 0,
                    inboundNoteId = draft.selectedInboundNoteId ?: 0L,
                    deliveryNumber = draft.deliveryNumber.trim(),
                    recipientNombre = draft.recipientNombre.trim(),
                    recipientApellido = draft.recipientApellido.trim(),
                    recipientDireccion = draft.recipientDireccion.trim(),
                    recipientTelefono = draft.recipientTelefono.trim(),
                    packageQty = draft.cantidadBultos.toInt(),
                    allocatedPackageIds = "",
                    deliveredQty = 0,
                    returnedQty = 0
                )

                val listId = withContext(Dispatchers.IO) {
                    repository.createOutboundWithAllocation(list, line)
                }

                val payload = withContext(Dispatchers.IO) {
                    val savedList = repository.getOutboundList(listId)
                    val savedLines = repository.getOutboundLines(listId)
                    if (savedList != null) {
                        OutboundPrintPayload(savedList, savedLines)
                    } else {
                        null
                    }
                }

                printPayload.value = payload
                saveState.value = OutboundSaveState.Success
            } catch (error: Exception) {
                saveState.value = OutboundSaveState.Error("No se pudo guardar la lista. Intentá de nuevo.")
            } finally {
                isSaving.value = false
            }
        }
    }

    fun clearSaveState() {
        saveState.value = null
    }

    fun clearPrintPayload() {
        printPayload.value = null
    }

    fun validateDraft(draft: OutboundDraftState, availableCount: Int): String? {
        if (draft.driverNombre.isBlank() || draft.driverApellido.isBlank()) {
            return "Completá el nombre y apellido del chofer."
        }
        if (draft.deliveryNumber.isBlank()) {
            return "Completá el número de entrega."
        }
        if (draft.recipientNombre.isBlank() || draft.recipientApellido.isBlank()) {
            return "Completá el nombre y apellido del destinatario."
        }
        if (draft.recipientDireccion.isBlank()) {
            return "Completá la dirección del destinatario."
        }
        if (draft.recipientTelefono.isBlank()) {
            return "Completá el teléfono del destinatario."
        }
        val qty = draft.cantidadBultos.toIntOrNull() ?: 0
        if (qty <= 0) {
            return "La cantidad de bultos debe ser mayor a cero."
        }
        if (draft.selectedInboundNoteId == null) {
            return "Seleccioná un ingreso disponible."
        }
        if (availableCount < qty) {
            return "No hay suficientes bultos disponibles para este ingreso."
        }
        return null
    }

    private fun buildInboundLabel(note: InboundNoteWithAvailable): String {
        return "${note.note.senderApellido} ${note.note.senderNombre} • ${note.note.senderCuit}"
    }
}

data class OutboundDraftState(
    val driverNombre: String = "",
    val driverApellido: String = "",
    val deliveryNumber: String = "",
    val recipientNombre: String = "",
    val recipientApellido: String = "",
    val recipientDireccion: String = "",
    val recipientTelefono: String = "",
    val cantidadBultos: String = "",
    val selectedInboundNoteId: Long? = null
)

data class InboundOption(
    val inboundNoteId: Long,
    val label: String,
    val availableCount: Int
)

sealed interface OutboundSaveState {
    data object Success : OutboundSaveState
    data class Error(val message: String) : OutboundSaveState
}

data class OutboundPrintPayload(
    val list: OutboundListEntity,
    val lines: List<OutboundLineEntity>
)
