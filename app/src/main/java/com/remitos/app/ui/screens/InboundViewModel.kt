package com.remitos.app.ui.screens

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.remitos.app.data.RemitosRepository
import com.remitos.app.data.SettingsStore
import com.remitos.app.data.db.entity.InboundNoteEntity
import com.remitos.app.ocr.OcrProcessor
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

        viewModelScope.launch {
            try {
                val enableCorrection = withContext(ioDispatcher) {
                    settingsStore?.getPerspectiveCorrectionEnabled() ?: true
                }
                val result = ocrProcessor.processImage(context, uri, enableCorrection)
                updateOcrMetadata(result.text, result.confidence)
                draft = draft.copy(
                    senderCuit = result.fields["sender_cuit"] ?: draft.senderCuit,
                    senderNombre = result.fields["sender_nombre"] ?: draft.senderNombre,
                    senderApellido = result.fields["sender_apellido"] ?: draft.senderApellido,
                    destNombre = result.fields["dest_nombre"] ?: draft.destNombre,
                    destApellido = result.fields["dest_apellido"] ?: draft.destApellido,
                    destDireccion = result.fields["dest_direccion"] ?: draft.destDireccion,
                    destTelefono = result.fields["dest_telefono"] ?: draft.destTelefono,
                    cantBultosTotal = result.fields["cant_bultos_total"] ?: draft.cantBultosTotal,
                    remitoNumCliente = result.fields["remito_num_cliente"] ?: draft.remitoNumCliente,
                    remitoNumInterno = result.fields["remito_num_interno"] ?: draft.remitoNumInterno
                )
            } catch (error: Exception) {
                Log.e("InboundViewModel", "Error al procesar OCR", error)
            } finally {
                isProcessing = false
                showMissingErrors = true
            }
        }
    }

    fun updateOcrMetadata(text: String?, confidence: Map<String, Float>?) {
        ocrTextBlob = text
        ocrConfidenceJson = if (confidence.isNullOrEmpty()) {
            null
        } else {
            confidence.entries.joinToString(prefix = "{", postfix = "}") { (key, value) ->
                "\"$key\":$value"
            }
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
                }

                draft = InboundDraftState()
                selectedImageUri = null
                ocrTextBlob = null
                ocrConfidenceJson = null
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
