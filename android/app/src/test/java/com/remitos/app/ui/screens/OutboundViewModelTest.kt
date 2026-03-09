package com.remitos.app.ui.screens

import com.remitos.app.data.OutboundListStatus
import com.remitos.app.data.RemitosRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.After
import org.junit.Before
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class OutboundViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: RemitosRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mock()
        whenever(repository.observeInboundNotesWithAvailable()).thenReturn(flowOf(emptyList()))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun validateDraft_returnsErrorWhenMissingInboundSelection() {
        val viewModel = OutboundViewModel(repository, ioDispatcher = testDispatcher)
        val draft = OutboundDraftState(
            driverNombre = "Juan",
            driverApellido = "Perez",
            lines = listOf(
                OutboundLineDraft(
                    id = 1L,
                    deliveryNumber = "E-1",
                    recipientNombre = "Maria",
                    recipientApellido = "Lopez",
                    recipientDireccion = "Calle 123",
                    recipientTelefono = "111111111",
                    cantidadBultos = "2",
                    selectedInboundNoteId = null
                )
            )
        )

        val error = viewModel.validateDraft(
            draft,
            inboundOptions = listOf(
                InboundOption(
                    inboundNoteId = 10L,
                    label = "Remito 1",
                    availableCount = 5,
                    remitoNumCliente = "R-1"
                )
            )
        )

        assertEquals("Seleccioná un remito en la línea 1.", error)
    }

    @Test
    fun validateDraft_returnsErrorWhenNotEnoughAvailable() {
        val viewModel = OutboundViewModel(repository, ioDispatcher = testDispatcher)
        val draft = OutboundDraftState(
            driverNombre = "Juan",
            driverApellido = "Perez",
            lines = listOf(
                OutboundLineDraft(
                    id = 1L,
                    deliveryNumber = "E-1",
                    recipientNombre = "Maria",
                    recipientApellido = "Lopez",
                    recipientDireccion = "Calle 123",
                    recipientTelefono = "111111111",
                    cantidadBultos = "4",
                    selectedInboundNoteId = 10L
                )
            )
        )

        val error = viewModel.validateDraft(
            draft,
            inboundOptions = listOf(
                InboundOption(
                    inboundNoteId = 10L,
                    label = "Remito 1",
                    availableCount = 2,
                    remitoNumCliente = "R-1"
                )
            )
        )

        assertEquals("No hay suficientes bultos disponibles para el remito 1.", error)
    }

    @Test
    fun validateDraft_returnsNullForValidDraft() {
        val viewModel = OutboundViewModel(repository, ioDispatcher = testDispatcher)
        val draft = OutboundDraftState(
            driverNombre = "Juan",
            driverApellido = "Perez",
            lines = listOf(
                OutboundLineDraft(
                    id = 1L,
                    deliveryNumber = "E-1",
                    recipientNombre = "Maria",
                    recipientApellido = "Lopez",
                    recipientDireccion = "Calle 123",
                    recipientTelefono = "111111111",
                    cantidadBultos = "2",
                    selectedInboundNoteId = 10L
                )
            )
        )

        val error = viewModel.validateDraft(
            draft,
            inboundOptions = listOf(
                InboundOption(
                    inboundNoteId = 10L,
                    label = "Remito 1",
                    availableCount = 5,
                    remitoNumCliente = "R-1"
                )
            )
        )

        assertNull(error)
    }

    @Test
    fun save_success_setsSuccessState() = runTest {
        val viewModel = OutboundViewModel(repository, ioDispatcher = testDispatcher)
        whenever(repository.nextOutboundListNumber()).thenReturn(1L)
        whenever(repository.createOutboundWithAllocations(
            org.mockito.kotlin.any(),
            org.mockito.kotlin.any()
        )).thenReturn(1L)
        whenever(repository.getOutboundList(1L)).thenReturn(
            com.remitos.app.data.db.entity.OutboundListEntity(
                listNumber = 1,
                issueDate = 123L,
                driverNombre = "Juan",
                driverApellido = "Perez",
                checklistSignaturePath = null,
                checklistSignedAt = null,
                status = OutboundListStatus.Abierta
            )
        )
        whenever(repository.getOutboundLinesWithRemito(1L)).thenReturn(emptyList())

        val draft = OutboundDraftState(
            driverNombre = "Juan",
            driverApellido = "Perez",
            lines = listOf(
                OutboundLineDraft(
                    id = 1L,
                    deliveryNumber = "E-1",
                    recipientNombre = "Maria",
                    recipientApellido = "Lopez",
                    recipientDireccion = "Calle 123",
                    recipientTelefono = "111111111",
                    cantidadBultos = "2",
                    selectedInboundNoteId = 10L
                )
            )
        )

        viewModel.save(
            draft,
            inboundOptions = listOf(
                InboundOption(
                    inboundNoteId = 10L,
                    label = "Remito 1",
                    availableCount = 5,
                    remitoNumCliente = "R-1"
                )
            )
        )
        advanceUntilIdle()

        assertEquals(OutboundSaveState.Success, viewModel.saveState.value)
        assertEquals(1L, viewModel.printPayload.value?.list?.listNumber)
        verify(repository).createOutboundWithAllocations(
            org.mockito.kotlin.any(),
            org.mockito.kotlin.any()
        )
    }
}
