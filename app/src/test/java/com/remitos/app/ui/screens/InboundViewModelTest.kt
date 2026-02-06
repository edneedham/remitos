package com.remitos.app.ui.screens

import com.remitos.app.data.RemitosRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class InboundViewModelTest {
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
    fun save_withInvalidBultos_setsErrorState() {
        val viewModel = InboundViewModel(repository)
        viewModel.updateDraft(
            viewModel.draft.copy(
                senderCuit = "20-12345678-9",
                senderNombre = "ACME",
                senderApellido = "SA",
                destNombre = "Juan",
                destApellido = "Perez",
                destDireccion = "Calle 123",
                destTelefono = "111111111",
                cantBultosTotal = "0",
                remitoNumCliente = "RC-1",
                remitoNumInterno = "RI-1"
            )
        )

        viewModel.save()

        val state = viewModel.saveState
        assertTrue(state is SaveState.Error)
        assertEquals(
            "La cantidad de bultos debe ser mayor a cero.",
            (state as SaveState.Error).message
        )
        verifyNoInteractions(repository)
    }

    @Test
    fun save_success_resetsDraftAndClearsImage() = runTest {
        val viewModel = InboundViewModel(repository)
        whenever(repository.createInboundNote(org.mockito.kotlin.any())).thenReturn(1L)
        viewModel.updateDraft(
            viewModel.draft.copy(
                senderCuit = "20-12345678-9",
                senderNombre = "ACME",
                senderApellido = "SA",
                destNombre = "Juan",
                destApellido = "Perez",
                destDireccion = "Calle 123",
                destTelefono = "111111111",
                cantBultosTotal = "2",
                remitoNumCliente = "RC-2",
                remitoNumInterno = "RI-2"
            )
        )

        viewModel.save()
        advanceUntilIdle()

        assertTrue(viewModel.saveState is SaveState.Success)
        assertEquals(InboundDraftState(), viewModel.draft)
        assertNull(viewModel.selectedImageUri)
    }

    @Test
    fun save_persistsOcrMetadata() = runTest {
        val viewModel = InboundViewModel(repository)
        whenever(repository.createInboundNote(org.mockito.kotlin.any())).thenReturn(1L)
        viewModel.updateDraft(
            viewModel.draft.copy(
                senderCuit = "20-12345678-9",
                senderNombre = "ACME",
                senderApellido = "SA",
                destNombre = "Juan",
                destApellido = "Perez",
                destDireccion = "Calle 123",
                destTelefono = "111111111",
                cantBultosTotal = "2",
                remitoNumCliente = "RC-2",
                remitoNumInterno = "RI-2"
            )
        )
        viewModel.updateOcrMetadata("texto ocr", mapOf("sender_cuit" to 0.8f))

        viewModel.save()
        advanceUntilIdle()

        val captor = argumentCaptor<com.remitos.app.data.db.entity.InboundNoteEntity>()
        verify(repository).createInboundNote(captor.capture())
        assertEquals("texto ocr", captor.firstValue.ocrTextBlob)
        assertEquals("{\"sender_cuit\":0.8}", captor.firstValue.ocrConfidenceJson)
    }
}
