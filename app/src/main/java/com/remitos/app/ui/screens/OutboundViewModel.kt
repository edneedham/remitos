package com.remitos.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.remitos.app.data.OutboundLineStatus
import com.remitos.app.data.OutboundListStatus
import com.remitos.app.data.RemitosRepository
import com.remitos.app.data.db.entity.InboundNoteWithAvailable
import com.remitos.app.data.db.entity.OutboundLineEntity
import com.remitos.app.data.db.entity.OutboundListEntity
import com.remitos.app.data.db.entity.OutboundLineWithRemito
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OutboundViewModel(
    repository: RemitosRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val repository = repository

    val inboundOptions: StateFlow<List<InboundOption>> = repository
        .observeInboundNotesWithAvailable()
        .map { notes ->
            notes.map { note ->
                InboundOption(
                    inboundNoteId = note.note.id,
                    label = buildInboundLabel(note),
                    availableCount = note.availableCount,
                    remitoNumCliente = note.note.remitoNumCliente
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isSaving = MutableStateFlow(false)
    val saveState = MutableStateFlow<OutboundSaveState?>(null)
    val printPayload = MutableStateFlow<OutboundPrintPayload?>(null)

    fun save(draft: OutboundDraftState, inboundOptions: List<InboundOption>) {
        if (isSaving.value) return

        val error = validateDraft(draft, inboundOptions)
        if (error != null) {
            saveState.value = OutboundSaveState.Error(error)
            return
        }

        isSaving.value = true
        saveState.value = null

        viewModelScope.launch {
            try {
                val listNumber = withContext(ioDispatcher) {
                    repository.nextOutboundListNumber()
                }
                val now = System.currentTimeMillis()
                val list = OutboundListEntity(
                    listNumber = listNumber,
                    issueDate = now,
                    driverNombre = draft.driverNombre.trim(),
                    driverApellido = draft.driverApellido.trim(),
                    checklistSignaturePath = null,
                    checklistSignedAt = null,
                    status = OutboundListStatus.Abierta
                )

                val lines = draft.lines.map { lineDraft ->
                    OutboundLineEntity(
                        outboundListId = 0,
                        inboundNoteId = lineDraft.selectedInboundNoteId ?: 0L,
                        deliveryNumber = lineDraft.deliveryNumber.trim(),
                        recipientNombre = lineDraft.recipientNombre.trim(),
                        recipientApellido = lineDraft.recipientApellido.trim(),
                        recipientDireccion = lineDraft.recipientDireccion.trim(),
                        recipientTelefono = lineDraft.recipientTelefono.trim(),
                        packageQty = lineDraft.cantidadBultos.toInt(),
                        allocatedPackageIds = "",
                        status = OutboundLineStatus.EnDeposito,
                        deliveredQty = 0,
                        returnedQty = 0
                    )
                }

                val listId = withContext(ioDispatcher) {
                    repository.createOutboundWithAllocations(list, lines)
                }

                val payload = withContext(ioDispatcher) {
                    val savedList = repository.getOutboundList(listId)
                    val savedLines = repository.getOutboundLinesWithRemito(listId)
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

    fun validateDraft(draft: OutboundDraftState, inboundOptions: List<InboundOption>): String? {
        if (draft.driverNombre.isBlank() || draft.driverApellido.isBlank()) {
            return "Completá el nombre y apellido del chofer."
        }
        if (draft.lines.isEmpty()) {
            return "Agregá al menos un remito."
        }

        val availableByNote = inboundOptions.associateBy({ it.inboundNoteId }, { it.availableCount })
        val seenInboundNotes = mutableSetOf<Long>()

        draft.lines.forEachIndexed { index, line ->
            val lineNumber = index + 1
            if (line.deliveryNumber.isBlank()) {
                return "Completá el número de entrega en el remito $lineNumber."
            }
            if (line.recipientNombre.isBlank() || line.recipientApellido.isBlank()) {
                return "Completá el nombre y apellido del destinatario en el remito $lineNumber."
            }
            if (line.recipientDireccion.isBlank()) {
                return "Completá la dirección del destinatario en el remito $lineNumber."
            }
            if (line.recipientTelefono.isBlank()) {
                return "Completá el teléfono del destinatario en el remito $lineNumber."
            }
            val qty = line.cantidadBultos.toIntOrNull() ?: 0
            if (qty <= 0) {
                return "La cantidad de bultos debe ser mayor a cero en el remito $lineNumber."
            }
            val inboundId = line.selectedInboundNoteId
                ?: return "Seleccioná un remito en la línea $lineNumber."
            if (!seenInboundNotes.add(inboundId)) {
                return "El remito de la línea $lineNumber ya fue seleccionado."
            }
            val available = availableByNote[inboundId] ?: 0
            if (available < qty) {
                return "No hay suficientes bultos disponibles para el remito $lineNumber."
            }
        }
        return null
    }

    private fun buildInboundLabel(note: InboundNoteWithAvailable): String {
        return "Remito ${note.note.remitoNumCliente} • ${note.note.senderApellido} ${note.note.senderNombre}"
    }
}

data class OutboundDraftState(
    val driverNombre: String = "",
    val driverApellido: String = "",
    val lines: List<OutboundLineDraft> = emptyList()
)

data class OutboundLineDraft(
    val id: Long,
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
    val availableCount: Int,
    val remitoNumCliente: String
)

sealed interface OutboundSaveState {
    data object Success : OutboundSaveState
    data class Error(val message: String) : OutboundSaveState
}

data class OutboundPrintPayload(
    val list: OutboundListEntity,
    val lines: List<OutboundLineWithRemito>
)
