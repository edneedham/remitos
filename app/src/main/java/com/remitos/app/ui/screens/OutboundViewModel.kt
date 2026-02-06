package com.remitos.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.remitos.app.data.RemitosRepository
import com.remitos.app.data.db.entity.InboundNoteWithAvailable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class OutboundViewModel(
    repository: RemitosRepository
) : ViewModel() {
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
