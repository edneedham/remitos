package com.remitos.app.ui.screens

import com.remitos.app.data.RemitosRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.mock

class OutboundViewModelTest {
    @Test
    fun validateDraft_returnsErrorWhenMissingInboundSelection() {
        val viewModel = OutboundViewModel(mock<RemitosRepository>())
        val draft = OutboundDraftState(
            driverNombre = "Juan",
            driverApellido = "Perez",
            deliveryNumber = "E-1",
            recipientNombre = "Maria",
            recipientApellido = "Lopez",
            recipientDireccion = "Calle 123",
            recipientTelefono = "111111111",
            cantidadBultos = "2",
            selectedInboundNoteId = null
        )

        val error = viewModel.validateDraft(draft, availableCount = 5)

        assertEquals("Seleccioná un ingreso disponible.", error)
    }

    @Test
    fun validateDraft_returnsErrorWhenNotEnoughAvailable() {
        val viewModel = OutboundViewModel(mock<RemitosRepository>())
        val draft = OutboundDraftState(
            driverNombre = "Juan",
            driverApellido = "Perez",
            deliveryNumber = "E-1",
            recipientNombre = "Maria",
            recipientApellido = "Lopez",
            recipientDireccion = "Calle 123",
            recipientTelefono = "111111111",
            cantidadBultos = "4",
            selectedInboundNoteId = 10L
        )

        val error = viewModel.validateDraft(draft, availableCount = 2)

        assertEquals("No hay suficientes bultos disponibles para este ingreso.", error)
    }

    @Test
    fun validateDraft_returnsNullForValidDraft() {
        val viewModel = OutboundViewModel(mock<RemitosRepository>())
        val draft = OutboundDraftState(
            driverNombre = "Juan",
            driverApellido = "Perez",
            deliveryNumber = "E-1",
            recipientNombre = "Maria",
            recipientApellido = "Lopez",
            recipientDireccion = "Calle 123",
            recipientTelefono = "111111111",
            cantidadBultos = "2",
            selectedInboundNoteId = 10L
        )

        val error = viewModel.validateDraft(draft, availableCount = 5)

        assertNull(error)
    }
}
