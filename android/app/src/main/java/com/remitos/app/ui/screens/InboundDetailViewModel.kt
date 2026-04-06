package com.remitos.app.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.remitos.app.data.InboundNoteStatus
import com.remitos.app.data.RemitosRepository
import com.remitos.app.data.db.entity.InboundNoteEntity
import com.remitos.app.data.db.entity.InboundPackageEntity
import com.remitos.app.ocr.FieldDisplayItem
import com.remitos.app.ocr.FieldNames
import com.remitos.app.ocr.OcrFieldKeys
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject

import android.content.Context
import android.widget.Toast

@HiltViewModel
class InboundDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: RemitosRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private val noteId: Long = savedStateHandle["noteId"] ?: 0L
    private var currentNote: InboundNoteEntity? = null

    private val _uiState = MutableStateFlow(InboundDetailUiState())
    val uiState: StateFlow<InboundDetailUiState> = _uiState

    private val _saveState = MutableStateFlow<InboundDetailSaveState?>(null)
    val saveState: StateFlow<InboundDetailSaveState?> = _saveState

    init {
        loadNote()
    }

    fun updateDraft(value: InboundDraftState) {
        _uiState.value = _uiState.value.copy(draft = value)
    }

    fun save() {
        val state = _uiState.value
        val note = currentNote ?: return
        if (state.isSaving || state.isVoiding) return
        if (state.status == InboundNoteStatus.Anulada) {
            _saveState.value = InboundDetailSaveState.Error("El ingreso está anulado.")
            return
        }

        val draft = state.draft
        val missing = draft.missingFields()
        if (missing.isNotEmpty()) {
            _uiState.value = state.copy(showMissingErrors = true)
            _saveState.value = InboundDetailSaveState.Error("Completá los datos requeridos.")
            return
        }

        val cantBultos = draft.cantBultosTotal.toIntOrNull() ?: 0
        if (cantBultos <= 0) {
            _uiState.value = state.copy(showMissingErrors = true)
            _saveState.value = InboundDetailSaveState.Error("La cantidad de bultos debe ser mayor a cero.")
            return
        }

        _uiState.value = state.copy(isSaving = true)
        _saveState.value = null
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val updated = note.copy(
                    senderCuit = draft.senderCuit.trim(),
                    senderNombre = draft.senderNombre.trim(),
                    senderApellido = draft.senderApellido.trim(),
                    destNombre = draft.destNombre.trim(),
                    destApellido = draft.destApellido.trim(),
                    destDireccion = draft.destDireccion.trim(),
                    destTelefono = draft.destTelefono.trim(),
                    cantBultosTotal = cantBultos,
                    remitoNumCliente = draft.remitoNumCliente.trim(),
                    remitoNumInterno = note.remitoNumInterno,
                    updatedAt = now,
                )
                withContext(ioDispatcher) {
                    repository.updateInboundNote(updated)
                }
                currentNote = updated
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    updatedAt = now,
                    showMissingErrors = false,
                )
                _saveState.value = InboundDetailSaveState.Success
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false)
                _saveState.value = InboundDetailSaveState.Error(
                    error.message ?: "No se pudo guardar el ingreso. Intentá de nuevo."
                )
            }
        }
    }

    fun voidNote() {
        val state = _uiState.value
        if (state.isSaving || state.isVoiding) return
        _uiState.value = state.copy(isVoiding = true)
        viewModelScope.launch {
            try {
                withContext(ioDispatcher) {
                    repository.voidInboundNote(noteId)
                }
                val updatedAt = System.currentTimeMillis()
                currentNote = currentNote?.copy(status = InboundNoteStatus.Anulada, updatedAt = updatedAt)
                _uiState.value = _uiState.value.copy(
                    isVoiding = false,
                    status = InboundNoteStatus.Anulada,
                    updatedAt = updatedAt,
                )
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(isVoiding = false)
                _saveState.value = InboundDetailSaveState.Error(
                    error.message ?: "No se pudo anular el ingreso. Intentá de nuevo."
                )
            }
        }
    }

    fun clearSaveState() {
        _saveState.value = null
    }

    fun showExportDialog() {
        val draft = _uiState.value.draft
        
        // Generate filename with destinatario
        val destName = draft.destNombre
            .take(30)
            .replace(" ", "_")
            .ifBlank { "remito" }
        
        val remitoNum = draft.remitoNumInterno.replace("/", "-")
        val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        
        val suggestedName = "${destName}_${remitoNum}_$date.csv".take(100)
        
        _uiState.value = _uiState.value.copy(
            showExportDialog = true,
            suggestedExportFilename = suggestedName
        )
    }
    
    fun hideExportDialog() {
        _uiState.value = _uiState.value.copy(showExportDialog = false)
        // Filename will be regenerated fresh next time
    }

    fun exportToCsv(context: Context, customFilename: String) {
        viewModelScope.launch {
            val note = currentNote ?: return@launch
            val fieldSections = _uiState.value.fieldSections
            
            try {
                val filePath = CsvExporter.exportRemitoToCsv(
                    context = context,
                    inboundNote = note,
                    fieldSections = fieldSections,
                    customFilename = customFilename
                )
                
                // Show success toast with file path
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Exportado a: $filePath",
                        Toast.LENGTH_LONG
                    ).show()
                }
                
                _saveState.value = InboundDetailSaveState.Success
            } catch (e: Exception) {
                // Show error toast
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Error al exportar: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                _saveState.value = InboundDetailSaveState.Error("Error al exportar CSV: ${e.message}")
            }
        }
    }

    private fun loadNote() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            val note = withContext(ioDispatcher) { repository.getInboundNote(noteId) }
            val packages = withContext(ioDispatcher) { repository.getPackagesForNote(noteId) }
            if (note == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "No se encontró el ingreso.",
                )
            } else {
                currentNote = note
                val scannedCount = packages.count { it.barcodeRaw != null }
                val fieldSections = buildFieldSections(note)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    noteId = note.id,
                    status = note.status,
                    createdAt = note.createdAt,
                    updatedAt = note.updatedAt,
                    scanImagePath = note.scanImagePath,
                    draft = InboundDraftState(
                        senderCuit = note.senderCuit,
                        senderNombre = note.senderNombre,
                        senderApellido = note.senderApellido,
                        destNombre = note.destNombre,
                        destApellido = note.destApellido,
                        destDireccion = note.destDireccion,
                        destTelefono = note.destTelefono,
                        cantBultosTotal = note.cantBultosTotal.toString(),
                        remitoNumCliente = note.remitoNumCliente,
                        remitoNumInterno = note.remitoNumInterno,
                    ),
                    packages = packages,
                    scannedCount = scannedCount,
                    fieldSections = fieldSections,
                )
            }
        }
    }
    
    private fun buildFieldSections(note: InboundNoteEntity): Map<String, List<FieldDisplayItem>> {
        val allFields = mutableListOf<FieldDisplayItem>()
        
        // Add canonical fields
        allFields.add(FieldDisplayItem(OcrFieldKeys.SenderCuit, FieldNames.getFriendlyName(OcrFieldKeys.SenderCuit), note.senderCuit, FieldNames.getSectionForField(OcrFieldKeys.SenderCuit)))
        allFields.add(FieldDisplayItem(OcrFieldKeys.SenderNombre, FieldNames.getFriendlyName(OcrFieldKeys.SenderNombre), note.senderNombre, FieldNames.getSectionForField(OcrFieldKeys.SenderNombre)))
        allFields.add(FieldDisplayItem(OcrFieldKeys.SenderApellido, FieldNames.getFriendlyName(OcrFieldKeys.SenderApellido), note.senderApellido, FieldNames.getSectionForField(OcrFieldKeys.SenderApellido)))
        allFields.add(FieldDisplayItem(OcrFieldKeys.DestNombre, FieldNames.getFriendlyName(OcrFieldKeys.DestNombre), note.destNombre, FieldNames.getSectionForField(OcrFieldKeys.DestNombre)))
        allFields.add(FieldDisplayItem(OcrFieldKeys.DestApellido, FieldNames.getFriendlyName(OcrFieldKeys.DestApellido), note.destApellido, FieldNames.getSectionForField(OcrFieldKeys.DestApellido)))
        allFields.add(FieldDisplayItem(OcrFieldKeys.DestDireccion, FieldNames.getFriendlyName(OcrFieldKeys.DestDireccion), note.destDireccion, FieldNames.getSectionForField(OcrFieldKeys.DestDireccion)))
        allFields.add(FieldDisplayItem(OcrFieldKeys.DestTelefono, FieldNames.getFriendlyName(OcrFieldKeys.DestTelefono), note.destTelefono, FieldNames.getSectionForField(OcrFieldKeys.DestTelefono)))
        allFields.add(FieldDisplayItem(OcrFieldKeys.CantBultosTotal, FieldNames.getFriendlyName(OcrFieldKeys.CantBultosTotal), note.cantBultosTotal.toString(), FieldNames.getSectionForField(OcrFieldKeys.CantBultosTotal)))
        allFields.add(FieldDisplayItem(OcrFieldKeys.RemitoNumCliente, FieldNames.getFriendlyName(OcrFieldKeys.RemitoNumCliente), note.remitoNumCliente, FieldNames.getSectionForField(OcrFieldKeys.RemitoNumCliente)))
        
        // Parse extra fields from JSON
        if (note.extraFieldsJson.isNotBlank() && note.extraFieldsJson != "{}") {
            try {
                val json = JSONObject(note.extraFieldsJson)
                json.keys().forEach { key ->
                    val value = json.getString(key)
                    if (value.isNotBlank()) {
                        allFields.add(FieldDisplayItem(key, FieldNames.getFriendlyName(key), value, FieldNames.getSectionForField(key)))
                    }
                }
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }
        
        // Group by section
        return allFields.groupBy { it.section }
    }
}

data class InboundDetailUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isVoiding: Boolean = false,
    val showMissingErrors: Boolean = false,
    val errorMessage: String? = null,
    val noteId: Long = 0L,
    val status: String = InboundNoteStatus.Activa,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val scanImagePath: String? = null,
    val draft: InboundDraftState = InboundDraftState(),
    val packages: List<InboundPackageEntity> = emptyList(),
    val scannedCount: Int = 0,
    val fieldSections: Map<String, List<FieldDisplayItem>> = emptyMap(),
    val showExportDialog: Boolean = false,
    val suggestedExportFilename: String = "",
)

sealed interface InboundDetailSaveState {
    data object Success : InboundDetailSaveState
    data class Error(val message: String) : InboundDetailSaveState
}
