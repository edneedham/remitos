package com.remitos.app.ui.screens

import com.remitos.app.data.RemitosRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun validateDraft_returnsErrorWhenMissingInboundSelection() {
        val viewModel = OutboundViewModel(repository)
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
        val viewModel = OutboundViewModel(repository)
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
        val viewModel = OutboundViewModel(repository)
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

    @Test
    fun save_success_setsSuccessState() = runTest {
        val viewModel = OutboundViewModel(repository)
        whenever(repository.nextOutboundListNumber()).thenReturn(1L)
        whenever(repository.createOutboundWithAllocation(
            org.mockito.kotlin.any(),
            org.mockito.kotlin.any()
        )).thenReturn(1L)
        whenever(repository.getOutboundList(1L)).thenReturn(
            com.remitos.app.data.db.entity.OutboundListEntity(
                listNumber = 1,
                issueDate = 123L,
                driverNombre = "Juan",
                driverApellido = "Perez",
                status = "abierta"
            )
        )
        whenever(repository.getOutboundLines(1L)).thenReturn(emptyList())

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

        viewModel.save(draft, availableCount = 5)
        advanceUntilIdle()

        assertEquals(OutboundSaveState.Success, viewModel.saveState.value)
        assertEquals(1L, viewModel.printPayload.value?.list?.listNumber)
        verify(repository).createOutboundWithAllocation(
            org.mockito.kotlin.any(),
            org.mockito.kotlin.any()
        )
    }
}
