package com.remitos.app.ui.screens

import android.content.Context
import android.os.Environment
import com.remitos.app.data.db.entity.InboundNoteEntity
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility for exporting scanned barcode data to CSV format.
 */
object CsvExporter {

    private val csvDateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    /**
     * Export scanned barcodes to a CSV file.
     *
     * @param context Application context
     * @param inboundNote The inbound note associated with the scans
     * @param scannedItems List of scanned barcode items
     * @param scannedBy User who performed the scanning
     * @return Path to the exported CSV file
     */
    fun exportScannedBarcodes(
        context: Context,
        inboundNote: InboundNoteEntity,
        scannedItems: List<ScannedBarcodeItem>,
        scannedBy: String
    ): String {
        val timestamp = csvDateFormat.format(Date())
        val fileName = "escaneos_${inboundNote.remitoNumInterno.replace("/", "-")}_$timestamp.csv"

        // Use Downloads directory for easy access
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)

        FileWriter(file).use { writer ->
            // Write header
            writer.append("remito_numero,remito_fecha,remitente_cuit,remitente_nombre,destinatario_nombre,cantidad_bultos," +
                    "escaneado_en,escaneado_por,indice_paquete,gtin,lote,vencimiento,codigo_crudo,es_manual,sscc\n")

            // Write data rows
            scannedItems.forEachIndexed { index, item ->
                val scanDate = displayDateFormat.format(Date(item.scannedAt))
                val isManual = if (item.isManual) "Si" else "No"

                writer.append(
                    "${escapeCsv(inboundNote.remitoNumCliente)}," +
                            "${escapeCsv(displayDateFormat.format(Date(inboundNote.createdAt)))}," +
                            "${escapeCsv(inboundNote.senderCuit)}," +
                            "${escapeCsv("${inboundNote.senderNombre} ${inboundNote.senderApellido}")}," +
                            "${escapeCsv("${inboundNote.destNombre} ${inboundNote.destApellido}")}," +
                            "${inboundNote.cantBultosTotal}," +
                            "${escapeCsv(scanDate)}," +
                            "${escapeCsv(scannedBy)}," +
                            "${index + 1}," +
                            "${escapeCsv(item.gtin ?: "")}," +
                            "${escapeCsv(item.batchLot ?: "")}," +
                            "${escapeCsv(item.expiryDate ?: "")}," +
                            "${escapeCsv(item.rawValue)}," +
                            "$isManual," +
                            "${escapeCsv(item.sscc ?: "")}\n"
                )
            }
        }

        return file.absolutePath
    }

    /**
     * Export scanned barcodes from database packages.
     *
     * @param context Application context
     * @param inboundNote The inbound note
     * @param packages List of packages with barcode data
     * @param scannedBy User who performed the scanning
     * @return Path to the exported CSV file
     */
    fun exportPackagesToCsv(
        context: Context,
        inboundNote: InboundNoteEntity,
        packages: List<com.remitos.app.data.db.entity.InboundPackageEntity>,
        scannedBy: String
    ): String {
        val timestamp = csvDateFormat.format(Date())
        val fileName = "escaneos_${inboundNote.remitoNumInterno.replace("/", "-")}_$timestamp.csv"

        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)

        FileWriter(file).use { writer ->
            // Write header
            writer.append("remito_numero,remito_fecha,remitente_cuit,remitente_nombre,destinatario_nombre,cantidad_bultos," +
                    "escaneado_en,escaneado_por,indice_paquete,gtin,lote,vencimiento,codigo_crudo,es_manual,sscc\n")

            // Write data rows
            packages.forEach { packageEntity ->
                val scanDate = packageEntity.scannedAt?.let {
                    displayDateFormat.format(Date(it))
                } ?: ""
                val isManual = "No" // Assume automatic from database

                writer.append(
                    "${escapeCsv(inboundNote.remitoNumCliente)}," +
                            "${escapeCsv(displayDateFormat.format(Date(inboundNote.createdAt)))}," +
                            "${escapeCsv(inboundNote.senderCuit)}," +
                            "${escapeCsv("${inboundNote.senderNombre} ${inboundNote.senderApellido}")}," +
                            "${escapeCsv("${inboundNote.destNombre} ${inboundNote.destApellido}")}," +
                            "${inboundNote.cantBultosTotal}," +
                            "${escapeCsv(scanDate)}," +
                            "${escapeCsv(packageEntity.scannedBy ?: scannedBy)}," +
                            "${packageEntity.packageIndex}," +
                            "${escapeCsv(packageEntity.gtin ?: "")}," +
                            "${escapeCsv(packageEntity.batchLot ?: "")}," +
                            "${escapeCsv(packageEntity.expiryDate ?: "")}," +
                            "${escapeCsv(packageEntity.barcodeRaw ?: "")}," +
                            "$isManual," +
                            "${escapeCsv(packageEntity.sscc ?: "")}\n"
                )
            }
        }

        return file.absolutePath
    }

    /**
     * Export all given outbound lists to a CSV file in the cache directory for sharing.
     */
    fun exportFullActivityToCsv(context: Context, lists: List<com.remitos.app.data.db.entity.OutboundListEntity>): File {
        val timestamp = csvDateFormat.format(Date())
        val fileName = "actividad_repartos_$timestamp.csv"
        val file = File(context.cacheDir, fileName)

        FileWriter(file).use { writer ->
            writer.append("lista_numero,fecha_emision,chofer_nombre,chofer_apellido,estado\n")
            
            lists.forEach { list ->
                val issueDate = displayDateFormat.format(Date(list.issueDate))
                writer.append(
                    "${list.listNumber}," +
                    "${escapeCsv(issueDate)}," +
                    "${escapeCsv(list.driverNombre)}," +
                    "${escapeCsv(list.driverApellido)}," +
                    "${escapeCsv(list.status)}\n"
                )
            }
        }
        return file
    }

    /**
     * Escape special CSV characters.
     */
    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
