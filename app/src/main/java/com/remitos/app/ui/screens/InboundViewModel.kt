package com.remitos.app.ui.screens

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.remitos.app.data.RemitosRepository
import com.remitos.app.ocr.OcrProcessor
import kotlinx.coroutines.launch

private const val CuitRegex = "\\b\\d{2}-\\d{8}-\\d{1}\\b"

class InboundViewModel(
    private val repository: RemitosRepository
) : ViewModel() {
    private val ocrProcessor = OcrProcessor()

    var draft by mutableStateOf(InboundDraftState())
        private set

    var selectedImageUri by mutableStateOf<Uri?>(null)
        private set

    var isProcessing by mutableStateOf(false)
        private set

    var ocrErrorMessage by mutableStateOf<String?>(null)
        private set

    fun updateDraft(value: InboundDraftState) {
        draft = value
    }

    fun updateImageUri(value: Uri?) {
        selectedImageUri = value
    }

    fun processImage(context: Context) {
        val uri = selectedImageUri ?: return
        ocrErrorMessage = null
        isProcessing = true

        viewModelScope.launch {
            try {
                val result = ocrProcessor.processImage(context, uri)
                draft = draft.copy(
                    senderCuit = result.fields["sender_cuit"] ?: draft.senderCuit,
                    cantBultosTotal = result.fields["cant_bultos_total"] ?: draft.cantBultosTotal
                )
            } catch (error: Exception) {
                ocrErrorMessage = "No se pudo procesar la imagen. Intentá de nuevo."
            } finally {
                isProcessing = false
            }
        }
    }

    fun save() {
        // TODO: validar y persistir el ingreso
    }
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
