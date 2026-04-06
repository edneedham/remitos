package com.remitos.app.ocr

object FieldNames {
    private val canonicalNames = mapOf(
        OcrFieldKeys.SenderCuit to "CUIT Remitente",
        OcrFieldKeys.SenderNombre to "Nombre Remitente",
        OcrFieldKeys.SenderApellido to "Apellido Remitente",
        OcrFieldKeys.DestNombre to "Nombre Destinatario",
        OcrFieldKeys.DestApellido to "Apellido Destinatario",
        OcrFieldKeys.DestDireccion to "Dirección Destinatario",
        OcrFieldKeys.DestTelefono to "Teléfono Destinatario",
        OcrFieldKeys.CantBultosTotal to "Cantidad de Bultos",
        OcrFieldKeys.RemitoNumCliente to "Número de Remito Cliente",
        OcrFieldKeys.Transportista to "Transportista",
        OcrFieldKeys.Localidad to "Localidad",
        OcrFieldKeys.IvaCondicion to "Condición IVA",
        OcrFieldKeys.Fecha to "Fecha",
        OcrFieldKeys.RecibidoPor to "Recibido Por"
    )
    
    fun getFriendlyName(label: String): String {
        // First check canonical names
        canonicalNames[label]?.let { return it }
        
        // Then check if it's an extra field with known mapping
        val normalized = label.lowercase()
            .replace("á", "a")
            .replace("é", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ú", "u")
            .replace("_", " ")
        
        // Try to match normalized version
        canonicalNames.entries.find { 
            it.key.lowercase().replace("_", " ") == normalized 
        }?.let { return it.value }
        
        // Otherwise, format the raw label nicely
        return label.replace("_", " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }
    
    fun getSectionForField(label: String): String {
        return when {
            label.contains("sender") -> "Remitente"
            label.contains("dest") -> "Destinatario"
            label.contains("remito") || label.contains("cant_bultos") || label == OcrFieldKeys.Fecha -> "Documento"
            label == OcrFieldKeys.Transportista || label == OcrFieldKeys.Localidad -> "Transporte"
            label == OcrFieldKeys.IvaCondicion -> "Fiscal"
            else -> "Otros Datos"
        }
    }
}

data class FieldDisplayItem(
    val key: String,
    val label: String,
    val value: String,
    val section: String
)
