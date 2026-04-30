package com.remitos.app.ui.screens

import com.remitos.app.data.AuthManager
import com.remitos.app.data.ImageUploadManager
import com.remitos.app.data.RemitosRepository
import com.remitos.app.data.SettingsStore
import com.remitos.app.ocr.OcrFieldKeys
import com.remitos.app.ocr.OcrProcessor
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
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@OptIn(ExperimentalCoroutinesApi::class)
class InboundViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: RemitosRepository
    private lateinit var settingsStore: SettingsStore
    private lateinit var ocrProcessor: OcrProcessor
    private lateinit var authManager: AuthManager
    private lateinit var imageUploadManager: ImageUploadManager

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mock()
        settingsStore = mock()
        wheneverBlocking { settingsStore.isFirstInboundOnboardingDone() } doReturn false
        wheneverBlocking { settingsStore.setFirstInboundOnboardingDone() } doReturn Unit
        ocrProcessor = mock()
        authManager = mock()
        imageUploadManager = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun save_withZeroBultos_succeeds() = runTest {
        val viewModel = InboundViewModel(
            repository,
            settingsStore,
            ocrProcessor,
            authManager,
            imageUploadManager,
            testDispatcher
        )
        whenever(repository.createInboundNote(any())).thenReturn(1L)
        viewModel.updateDraft(
            viewModel.uiState.value.draft.copy(
                senderCuit = "20-12345678-9",
                senderNombre = "ACME",
                senderApellido = "SA",
                destNombre = "Juan",
                destApellido = "Perez",
                destDireccion = "Calle 123",
                destTelefono = "111111111",
                cantBultosTotal = "0",
                remitoNumCliente = "RC-1"
            )
        )

        viewModel.save()
        advanceUntilIdle()

        val state = viewModel.uiState.value.saveState
        assertTrue(state is SaveState.Success)
    }

    @Test
    fun save_success_resetsDraftAndClearsImage() = runTest {
        val viewModel = InboundViewModel(
            repository,
            settingsStore,
            ocrProcessor,
            authManager,
            imageUploadManager,
            testDispatcher
        )
        whenever(repository.createInboundNote(org.mockito.kotlin.any())).thenReturn(1L)
        viewModel.updateDraft(
            viewModel.uiState.value.draft.copy(
                senderCuit = "20-12345678-9",
                senderNombre = "ACME",
                senderApellido = "SA",
                destNombre = "Juan",
                destApellido = "Perez",
                destDireccion = "Calle 123",
                destTelefono = "111111111",
                cantBultosTotal = "2",
                remitoNumCliente = "RC-2"
            )
        )

        viewModel.save()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.saveState is SaveState.Success)
        assertEquals(InboundDraftState(), viewModel.uiState.value.draft)
        assertNull(viewModel.uiState.value.selectedImageUri)
    }

    @Test
    fun save_persistsOcrMetadata() = runTest {
        val viewModel = InboundViewModel(
            repository,
            settingsStore,
            ocrProcessor,
            authManager,
            imageUploadManager,
            testDispatcher
        )
        whenever(repository.createInboundNote(org.mockito.kotlin.any())).thenReturn(1L)
        viewModel.updateDraft(
            viewModel.uiState.value.draft.copy(
                senderCuit = "20-12345678-9",
                senderNombre = "ACME",
                senderApellido = "SA",
                destNombre = "Juan",
                destApellido = "Perez",
                destDireccion = "Calle 123",
                destTelefono = "111111111",
                cantBultosTotal = "2",
                remitoNumCliente = "RC-2"
            )
        )
        viewModel.updateOcrMetadata("texto ocr", mapOf(OcrFieldKeys.SenderCuit to 0.8))

        viewModel.save()
        advanceUntilIdle()

        val captor = argumentCaptor<com.remitos.app.data.db.entity.InboundNoteEntity>()
        verify(repository).createInboundNote(captor.capture())
        assertEquals("texto ocr", captor.firstValue.ocrTextBlob)
        assertEquals("{\"${OcrFieldKeys.SenderCuit}\":0.8}", captor.firstValue.ocrConfidenceJson)
    }

    @Test
    fun save_defaultsExtraFieldsToEmptyJson() = runTest {
        val viewModel = InboundViewModel(
            repository,
            settingsStore,
            ocrProcessor,
            authManager,
            imageUploadManager,
            testDispatcher
        )
        whenever(repository.createInboundNote(any())).thenReturn(1L)
        viewModel.updateDraft(
            viewModel.uiState.value.draft.copy(
                senderCuit = "20-12345678-9",
                senderNombre = "ACME",
                senderApellido = "SA",
                destNombre = "Juan",
                destApellido = "Perez",
                destDireccion = "Calle 123",
                destTelefono = "111111111",
                cantBultosTotal = "2",
                remitoNumCliente = "RC-2"
            )
        )

        viewModel.save()
        advanceUntilIdle()

        val captor = argumentCaptor<com.remitos.app.data.db.entity.InboundNoteEntity>()
        verify(repository).createInboundNote(captor.capture())
        assertEquals("{}", captor.firstValue.extraFieldsJson)
    }
}
