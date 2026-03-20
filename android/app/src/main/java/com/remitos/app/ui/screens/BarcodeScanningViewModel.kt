package com.remitos.app.ui.screens

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.remitos.app.barcode.Gs1Parser
import com.remitos.app.data.DatabaseManager
import com.remitos.app.data.db.entity.InboundPackageEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

sealed class ScanState {
    data object Idle : ScanState()
    data object Scanning : ScanState()
    data class Success(val message: String) : ScanState()
    data class Error(val message: String) : ScanState()
}

@HiltViewModel
class BarcodeScanningViewModel @Inject constructor() : ViewModel() {

    private val gs1Parser = Gs1Parser()

    private val _scanState = mutableStateOf<ScanState>(ScanState.Idle)
    val scanState: State<ScanState> = _scanState

    private val _scannedItems = mutableStateListOf<ScannedBarcodeItem>()
    val scannedItems: List<ScannedBarcodeItem> = _scannedItems

    private val _expectedCount = mutableIntStateOf(0)
    val expectedCount: Int get() = _expectedCount.intValue

    private val _progress = mutableFloatStateOf(0f)
    val progress: State<Float> = _progress

    private var inboundNoteId: Long = 0
    private var currentUser: String = ""

    /**
     * Load the inbound note and set up the scanning session.
     */
    fun loadNote(noteId: Long, expected: Int) {
        inboundNoteId = noteId
        _expectedCount.value = expected
        updateProgress()

        // Get current user from session
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Note: In a real app, you'd get this from your session/auth system
                currentUser = "usuario"
            } catch (e: Exception) {
                currentUser = "unknown"
            }
        }
    }

    /**
     * Process a scanned barcode.
     */
    fun processBarcode(barcode: String, isManual: Boolean = false) {
        if (_scannedItems.size >= _expectedCount.value) {
            _scanState.value = ScanState.Error("Ya se completó el escaneo requerido")
            return
        }

        _scanState.value = ScanState.Scanning

        // Parse GS1 data
        val parsedData = gs1Parser.parse(barcode)

        val scannedItem = ScannedBarcodeItem(
            id = UUID.randomUUID().toString(),
            rawValue = barcode,
            gtin = parsedData.gtin,
            batchLot = parsedData.batch,
            expiryDate = parsedData.expiryDate,
            sscc = parsedData.sscc,
            scannedAt = System.currentTimeMillis(),
            isManual = isManual
        )

        _scannedItems.add(scannedItem)
        updateProgress()

        val gtinMsg = if (scannedItem.gtin != null) " (GTIN: ${scannedItem.gtin})" else ""
        _scanState.value = ScanState.Success("Código escaneado$gtinMsg")
    }

    /**
     * Update progress calculation.
     */
    private fun updateProgress() {
        _progress.floatValue = if (_expectedCount.intValue > 0) {
            (_scannedItems.size.toFloat() / _expectedCount.intValue).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    /**
     * Save all scanned barcodes to the database.
     */
    fun saveScans(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _scanState.value = ScanState.Scanning

                val db = DatabaseManager.getOfflineDatabase(context)

                // Get existing packages for this note
                val packages = db.inboundDao().getPackagesForNote(inboundNoteId)
                val now = System.currentTimeMillis()

                // Update each package with scanned data
                _scannedItems.forEachIndexed { index, item ->
                    val packageEntity = packages.getOrNull(index)
                    if (packageEntity != null) {
                        db.inboundDao().updatePackage(
                            packageEntity.copy(
                                barcodeRaw = item.rawValue,
                                gtin = item.gtin,
                                batchLot = item.batchLot,
                                expiryDate = item.expiryDate,
                                sscc = item.sscc,
                                scannedAt = now,
                                scannedBy = currentUser
                            )
                        )
                    }
                }

                withContext(Dispatchers.Main) {
                    _scanState.value = ScanState.Success("Escaneos guardados correctamente")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _scanState.value = ScanState.Error("Error al guardar: ${e.message}")
                }
            }
        }
    }

    /**
     * Export scanned data to CSV file.
     */
    fun exportToCsv(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _scanState.value = ScanState.Scanning

                val db = DatabaseManager.getOfflineDatabase(context)
                val note = db.inboundDao().getNoteById(inboundNoteId)

                if (note != null) {
                    val csvData = CsvExporter.exportScannedBarcodes(
                        context = context,
                        inboundNote = note,
                        scannedItems = _scannedItems.toList(),
                        scannedBy = currentUser
                    )

                    withContext(Dispatchers.Main) {
                        _scanState.value = ScanState.Success("CSV exportado: $csvData")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _scanState.value = ScanState.Error("No se encontró el remito")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _scanState.value = ScanState.Error("Error al exportar: ${e.message}")
                }
            }
        }
    }

    /**
     * Clear scan state.
     */
    fun clearScanState() {
        _scanState.value = ScanState.Idle
    }
}
