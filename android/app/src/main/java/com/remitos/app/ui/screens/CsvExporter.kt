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
 * Utility for exporting data to CSV format.
 */
object CsvExporter {

    private val csvDateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

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
     * Export a single remito with all fields (canonical + extra) to CSV.
     *
     * @param context Application context
     * @param inboundNote The inbound note with all fields
     * @param fieldSections Map of section names to list of field display items
     * @param customFilename Optional custom filename (if null, auto-generates)
     * @return Path to the exported CSV file
     */
    fun exportRemitoToCsv(
        context: Context,
        inboundNote: InboundNoteEntity,
        fieldSections: Map<String, List<com.remitos.app.ocr.FieldDisplayItem>>,
        customFilename: String
    ): String {
        // Use custom filename, ensure .csv extension
        val fileName = if (customFilename.endsWith(".csv", ignoreCase = true)) {
            customFilename
        } else {
            "$customFilename.csv"
        }

        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)

        // Flatten all fields from all sections
        val allFields = fieldSections.values.flatten()
        
        FileWriter(file).use { writer ->
            // Write header - all field labels
            val header = allFields.joinToString(",") { field ->
                escapeCsv(field.label)
            }
            writer.append(header)
            writer.append("\n")

            // Write data row - all field values
            val dataRow = allFields.joinToString(",") { field ->
                escapeCsv(field.value)
            }
            writer.append(dataRow)
            writer.append("\n")
        }

        return file.absolutePath
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
