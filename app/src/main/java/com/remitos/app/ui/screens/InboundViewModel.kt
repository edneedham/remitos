package com.remitos.app.ui.screens

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

private const val CuitRegex = "\\b\\d{2}-\\d{8}-\\d{1}\\b"

class InboundViewModel(
    private val repository: RemitosRepository,
    private val settingsStore: SettingsStore? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val ocrProcessor: OcrProcessor = OcrProcessor(),
) : ViewModel() {
    private var lastOcrFields: Map<String, String> = emptyMap()

    var draft by mutableStateOf(InboundDraftState())
        private set

    var selectedImageUri by mutableStateOf<Uri?>(null)
        private set

    var isProcessing by mutableStateOf(false)
        private set

    var ocrTextBlob by mutableStateOf<String?>(null)
        private set

    var ocrConfidenceJson by mutableStateOf<String?>(null)
        private set

    var isSaving by mutableStateOf(false)
        private set

    var saveState by mutableStateOf<SaveState?>(null)
        private set

    var showMissingErrors by mutableStateOf(false)
        private set

    fun updateDraft(value: InboundDraftState) {
        draft = value
    }

    fun updateImageUri(value: Uri?) {
        selectedImageUri = value
        showMissingErrors = false
    }

    fun processImage(context: Context) {
        val uri = selectedImageUri ?: return
        isProcessing = true
        val startTimeMs = SystemClock.elapsedRealtime()
        val scanId = startTimeMs

        viewModelScope.launch {
            var parseSuccess = false
            var debugLog: DebugLogEntity? = null
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
                draft = draft.copy(
                    senderCuit = result.fields[OcrFieldKeys.SenderCuit] ?: draft.senderCuit,
                    senderNombre = result.fields[OcrFieldKeys.SenderNombre] ?: draft.senderNombre,
                    senderApellido = result.fields[OcrFieldKeys.SenderApellido] ?: draft.senderApellido,
                    destNombre = result.fields[OcrFieldKeys.DestNombre] ?: draft.destNombre,
                    destApellido = result.fields[OcrFieldKeys.DestApellido] ?: draft.destApellido,
                    destDireccion = result.fields[OcrFieldKeys.DestDireccion] ?: draft.destDireccion,
                    destTelefono = result.fields[OcrFieldKeys.DestTelefono] ?: draft.destTelefono,
                    cantBultosTotal = result.fields[OcrFieldKeys.CantBultosTotal] ?: draft.cantBultosTotal,
                    remitoNumCliente = result.fields[OcrFieldKeys.RemitoNumCliente] ?: draft.remitoNumCliente,
                    remitoNumInterno = result.fields[OcrFieldKeys.RemitoNumInterno] ?: draft.remitoNumInterno
                )
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
                isProcessing = false
                showMissingErrors = true
            }
        }
    }

    fun updateOcrMetadata(text: String?, confidence: Map<String, Float>?) {
        ocrTextBlob = text
        ocrConfidenceJson = confidenceToJson(confidence)
    }

    private fun confidenceToJson(confidence: Map<String, Float>?): String? {
        if (confidence.isNullOrEmpty()) return null
        return confidence.entries.joinToString(prefix = "{", postfix = "}") { (key, value) ->
            "\"$key\":$value"
        }
    }

    fun save() {
        if (isSaving) return

        val cantBultos = draft.cantBultosTotal.toIntOrNull() ?: 0
        if (cantBultos <= 0) {
            saveState = SaveState.Error("La cantidad de bultos debe ser mayor a cero.")
            return
        }

        isSaving = true
        saveState = null

        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val manualCorrection = hasManualCorrections(lastOcrFields, draft)
                val note = InboundNoteEntity(
                    senderCuit = draft.senderCuit.trim(),
                    senderNombre = draft.senderNombre.trim(),
                    senderApellido = draft.senderApellido.trim(),
                    destNombre = draft.destNombre.trim(),
                    destApellido = draft.destApellido.trim(),
                    destDireccion = draft.destDireccion.trim(),
                    destTelefono = draft.destTelefono.trim(),
                    cantBultosTotal = cantBultos,
                    remitoNumCliente = draft.remitoNumCliente.trim(),
                    remitoNumInterno = draft.remitoNumInterno.trim(),
                    scanImagePath = selectedImageUri?.toString(),
                    ocrTextBlob = ocrTextBlob,
                    ocrConfidenceJson = ocrConfidenceJson,
                    createdAt = now,
                    updatedAt = now
                )

                withContext(ioDispatcher) {
                    repository.createInboundNote(note)
                    if (manualCorrection) {
                        settingsStore?.recordManualCorrection()
                    }
                }

                draft = InboundDraftState()
                selectedImageUri = null
                ocrTextBlob = null
                ocrConfidenceJson = null
                lastOcrFields = emptyMap()
                saveState = SaveState.Success
                showMissingErrors = false
            } catch (error: Exception) {
                saveState = SaveState.Error("No se pudo guardar el ingreso. Intentá de nuevo.")
            } finally {
                isSaving = false
            }
        }
    }

    fun clearSaveState() {
        saveState = null
    }
}

sealed interface SaveState {
    data object Success : SaveState
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
        if (remitoNumInterno.isBlank()) missing.add(MissingField.RemitoInterno)
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
    RemitoCliente("Remito Nº Cliente"),
    RemitoInterno("Remito Nº Interno")
}
