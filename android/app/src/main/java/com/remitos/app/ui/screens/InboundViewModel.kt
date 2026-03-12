package com.remitos.app.ui.screens

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.remitos.app.data.InboundNoteStatus
import com.remitos.app.data.RemitosRepository
import com.remitos.app.data.SettingsStore
import com.remitos.app.data.db.entity.DebugLogEntity
import com.remitos.app.data.db.entity.InboundNoteEntity
import com.remitos.app.ocr.OcrFieldKeys
import com.remitos.app.ocr.OcrProcessor
import com.remitos.app.ocr.OcrProcessingException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.remitos.app.network.NetworkChecker

private const val CuitRegex = "\\b\\d{2}-\\d{8}-\\d{1}\\b"

data class InboundUiState(
    val draft: InboundDraftState = InboundDraftState(),
    val selectedImageUri: Uri? = null,
    val isProcessing: Boolean = false,
    val ocrTextBlob: String? = null,
    val ocrConfidenceJson: String? = null,
    val isSaving: Boolean = false,
    val saveState: SaveState? = null,
    val showMissingErrors: Boolean = false,
    val showManualEntryPrompt: Boolean = false,
    val showOfflineModeMessage: Boolean = false
)

class InboundViewModel(
    private val repository: RemitosRepository,
    private val settingsStore: SettingsStore? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val ocrProcessor: OcrProcessor = OcrProcessor(),
    private val authManager: com.remitos.app.data.AuthManager? = null,
) : ViewModel() {
    private var lastOcrFields: Map<String, String> = emptyMap()

    private val _uiState = MutableStateFlow(InboundUiState())
    val uiState: StateFlow<InboundUiState> = _uiState.asStateFlow()

    fun updateDraft(value: InboundDraftState) {
        _uiState.update { it.copy(draft = value) }
    }

    fun updateImageUri(value: Uri?) {
        _uiState.update { it.copy(
            selectedImageUri = value,
            showMissingErrors = false,
            showManualEntryPrompt = false
        ) }
    }

    fun processImage(context: Context) {
        val uri = _uiState.value.selectedImageUri ?: return
        _uiState.update { it.copy(
            isProcessing = true,
            showOfflineModeMessage = false
        ) }
        val startTimeMs = SystemClock.elapsedRealtime()
        val scanId = startTimeMs

        viewModelScope.launch {
            var parseSuccess = false
            var debugLog: DebugLogEntity? = null
            
            var isOffline = false
            if (authManager != null) {
                val isReachable = NetworkChecker.isServerReachable(authManager)
                if (!isReachable) {
                    isOffline = true
                    _uiState.update { it.copy(showOfflineModeMessage = true) }
                }
            }
            
            try {
                withContext(ioDispatcher) {
                    settingsStore?.recordScanStarted()
                }
                val enableCorrection = withContext(ioDispatcher) {
                    settingsStore?.getPerspectiveCorrectionEnabled() ?: true
                }
                val result = ocrProcessor.processImage(context, uri, enableCorrection)
                parseSuccess = isParseSuccessful(result.fields)
                lastOcrFields = result.fields
                val confidenceJson = confidenceToJson(result.confidence)
                updateOcrMetadata(result.text, result.confidence)
                val currentDraft = _uiState.value.draft
                _uiState.update { it.copy(draft = currentDraft.copy(
                    senderCuit = result.fields[OcrFieldKeys.SenderCuit] ?: currentDraft.senderCuit,
                    senderNombre = result.fields[OcrFieldKeys.SenderNombre] ?: currentDraft.senderNombre,
                    senderApellido = result.fields[OcrFieldKeys.SenderApellido] ?: currentDraft.senderApellido,
                    destNombre = result.fields[OcrFieldKeys.DestNombre] ?: currentDraft.destNombre,
                    destApellido = result.fields[OcrFieldKeys.DestApellido] ?: currentDraft.destApellido,
                    destDireccion = result.fields[OcrFieldKeys.DestDireccion] ?: currentDraft.destDireccion,
                    destTelefono = result.fields[OcrFieldKeys.DestTelefono] ?: currentDraft.destTelefono,
                    cantBultosTotal = result.fields[OcrFieldKeys.CantBultosTotal] ?: currentDraft.cantBultosTotal,
                    remitoNumCliente = result.fields[OcrFieldKeys.RemitoNumCliente] ?: currentDraft.remitoNumCliente
                )) }
                debugLog = DebugLogEntity(
                    createdAt = System.currentTimeMillis(),
                    scanId = scanId,
                    ocrConfidenceJson = confidenceJson,
                    preprocessTimeMs = result.preprocessTimeMs,
                    failureReason = null,
                    imageWidth = result.imageWidth,
                    imageHeight = result.imageHeight,
                    deviceModel = Build.MODEL,
                    parsingErrorSummary = result.parsingErrorSummary,
                )
            } catch (error: OcrProcessingException) {
                Log.e("InboundViewModel", "Error al procesar OCR", error)
                debugLog = DebugLogEntity(
                    createdAt = System.currentTimeMillis(),
                    scanId = scanId,
                    ocrConfidenceJson = null,
                    preprocessTimeMs = error.debugInfo.preprocessTimeMs,
                    failureReason = error.cause?.message ?: "Error de OCR",
                    imageWidth = error.debugInfo.imageWidth,
                    imageHeight = error.debugInfo.imageHeight,
                    deviceModel = Build.MODEL,
                    parsingErrorSummary = null,
                )
            } catch (error: Exception) {
                Log.e("InboundViewModel", "Error al procesar OCR", error)
                debugLog = DebugLogEntity(
                    createdAt = System.currentTimeMillis(),
                    scanId = scanId,
                    ocrConfidenceJson = null,
                    preprocessTimeMs = null,
                    failureReason = error.message ?: "Error de OCR",
                    imageWidth = null,
                    imageHeight = null,
                    deviceModel = Build.MODEL,
                    parsingErrorSummary = null,
                )
            } finally {
                val durationMs = SystemClock.elapsedRealtime() - startTimeMs
                withContext(ioDispatcher) {
                    settingsStore?.recordScanResult(durationMs, parseSuccess)
                    debugLog?.let { repository.insertDebugLog(it) }
                }
                _uiState.update { it.copy(
                    isProcessing = false,
                    showMissingErrors = true,
                    showManualEntryPrompt = !parseSuccess
                ) }
            }
        }
    }

    fun updateOcrMetadata(text: String?, confidence: Map<String, Double>?) {
        _uiState.update { it.copy(
            ocrTextBlob = text,
            ocrConfidenceJson = confidenceToJson(confidence)
        ) }
    }

    private fun confidenceToJson(confidence: Map<String, Double>?): String? {
        if (confidence.isNullOrEmpty()) return null
        return confidence.entries.joinToString(prefix = "{", postfix = "}") { (key, value) ->
            "\"$key\":$value"
        }
    }

    fun save() {
        val currentState = _uiState.value
        if (currentState.isSaving) return

        val cantBultos = currentState.draft.cantBultosTotal.toIntOrNull() ?: 0
        if (cantBultos <= 0) {
            _uiState.update { it.copy(saveState = SaveState.Error("La cantidad de bultos debe ser mayor a cero.")) }
            return
        }

        _uiState.update { it.copy(isSaving = true, saveState = null) }

        viewModelScope.launch {
            try {
                val stateSnapshot = _uiState.value
                val now = System.currentTimeMillis()
                val manualCorrection = hasManualCorrections(lastOcrFields, stateSnapshot.draft)
                val note = InboundNoteEntity(
                    senderCuit = stateSnapshot.draft.senderCuit.trim(),
                    senderNombre = stateSnapshot.draft.senderNombre.trim(),
                    senderApellido = stateSnapshot.draft.senderApellido.trim(),
                    destNombre = stateSnapshot.draft.destNombre.trim(),
                    destApellido = stateSnapshot.draft.destApellido.trim(),
                    destDireccion = stateSnapshot.draft.destDireccion.trim(),
                    destTelefono = stateSnapshot.draft.destTelefono.trim(),
                    cantBultosTotal = cantBultos,
                    remitoNumCliente = stateSnapshot.draft.remitoNumCliente.trim(),
                    remitoNumInterno = "",
                    status = InboundNoteStatus.Activa,
                    scanImagePath = stateSnapshot.selectedImageUri?.toString(),
                    ocrTextBlob = stateSnapshot.ocrTextBlob,
                    ocrConfidenceJson = stateSnapshot.ocrConfidenceJson,
                    createdAt = now,
                    updatedAt = now
                )

                val noteId: Long
                val cantBultosParsed = stateSnapshot.draft.cantBultosTotal.trim().toIntOrNull() ?: 0

                withContext(ioDispatcher) {
                    noteId = repository.createInboundNote(note)
                    if (manualCorrection) {
                        settingsStore?.recordManualCorrection()
                    }
                }

                lastOcrFields = emptyMap()
                _uiState.update { it.copy(
                    draft = InboundDraftState(),
                    selectedImageUri = null,
                    ocrTextBlob = null,
                    ocrConfidenceJson = null,
                    saveState = SaveState.Success(noteId, cantBultosParsed),
                    showMissingErrors = false,
                    showManualEntryPrompt = false
                ) }
            } catch (error: Exception) {
                _uiState.update { it.copy(saveState = SaveState.Error("No se pudo guardar el ingreso. Intentá de nuevo.")) }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun clearManualEntryPrompt() {
        _uiState.update { it.copy(showManualEntryPrompt = false) }
    }

    fun clearSaveState() {
        _uiState.update { it.copy(saveState = null) }
    }
}

sealed interface SaveState {
    data class Success(val noteId: Long, val packageCount: Int) : SaveState
    data class Error(val message: String) : SaveState
}

data class InboundDraftState(
    val senderCuit: String = "",
    val senderNombre: String = "",
    val senderApellido: String = "",
    val destNombre: String = "",
    val destApellido: String = "",
    val destDireccion: String = "",
    val destTelefono: String = "",
    val cantBultosTotal: String = "",
    val remitoNumCliente: String = "",
    val remitoNumInterno: String = ""
) {
    fun missingFields(): List<MissingField> {
        val missing = mutableListOf<MissingField>()
        if (!Regex(CuitRegex).containsMatchIn(senderCuit)) missing.add(MissingField.Cuit)
        if (senderNombre.isBlank()) missing.add(MissingField.SenderNombre)
        if (senderApellido.isBlank()) missing.add(MissingField.SenderApellido)
        if (destNombre.isBlank()) missing.add(MissingField.DestNombre)
        if (destApellido.isBlank()) missing.add(MissingField.DestApellido)
        if (destDireccion.isBlank()) missing.add(MissingField.DestDireccion)
        if (destTelefono.isBlank()) missing.add(MissingField.DestTelefono)
        if (cantBultosTotal.toIntOrNull() == null || cantBultosTotal.toIntOrNull() ?: 0 <= 0) {
            missing.add(MissingField.CantBultos)
        }
        if (remitoNumCliente.isBlank()) missing.add(MissingField.RemitoCliente)
        return missing
    }
}

enum class MissingField(val label: String) {
    Cuit("CUIT Remitente"),
    SenderNombre("Nombre Remitente"),
    SenderApellido("Apellido Remitente"),
    DestNombre("Nombre Destinatario"),
    DestApellido("Apellido Destinatario"),
    DestDireccion("Dirección Destinatario"),
    DestTelefono("Teléfono Destinatario"),
    CantBultos("Cantidad de bultos"),
    RemitoCliente("Número de remito de cliente")
}
