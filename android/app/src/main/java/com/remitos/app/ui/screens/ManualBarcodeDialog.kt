package com.remitos.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.remitos.app.barcode.Gs1Parser
import com.remitos.app.ui.theme.BrandBlue

/**
 * Dialog for manual barcode entry with GS1 field support.
 */
@Composable
fun ManualBarcodeDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var rawBarcode by remember { mutableStateOf("") }
    var gtin by remember { mutableStateOf("") }
    var batchLot by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var showGs1Fields by remember { mutableStateOf(false) }

    val gs1Parser = remember { Gs1Parser() }

    // Parse raw barcode to detect GS1
    fun parseRawBarcode() {
        if (rawBarcode.isNotEmpty()) {
            val parsed = gs1Parser.parse(rawBarcode)
            if (parsed.hasGs1Data()) {
                gtin = parsed.gtin ?: ""
                batchLot = parsed.batch ?: ""
                expiryDate = parsed.expiryDate ?: ""
                showGs1Fields = true
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ingreso manual") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Ingresá el código de barras manualmente:",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = rawBarcode,
                    onValueChange = {
                        rawBarcode = it
                        parseRawBarcode()
                    },
                    label = { Text("Código de barras") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { parseRawBarcode() }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandBlue,
                        unfocusedBorderColor = BrandBlue,
                        focusedLabelColor = BrandBlue,
                        unfocusedLabelColor = BrandBlue
                    )
                )

                // Show GS1 parsed fields if detected
                if (showGs1Fields || gtin.isNotEmpty() || batchLot.isNotEmpty() || expiryDate.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Datos GS1 detectados:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = gtin,
                        onValueChange = { gtin = it },
                        label = { Text("GTIN") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandBlue,
                            unfocusedBorderColor = BrandBlue,
                            focusedLabelColor = BrandBlue,
                            unfocusedLabelColor = BrandBlue
                        )
                    )

                    OutlinedTextField(
                        value = batchLot,
                        onValueChange = { batchLot = it },
                        label = { Text("Lote") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandBlue,
                            unfocusedBorderColor = BrandBlue,
                            focusedLabelColor = BrandBlue,
                            unfocusedLabelColor = BrandBlue
                        )
                    )

                    OutlinedTextField(
                        value = expiryDate,
                        onValueChange = { expiryDate = it },
                        label = { Text("Vencimiento (DD/MM/AAAA)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandBlue,
                            unfocusedBorderColor = BrandBlue,
                            focusedLabelColor = BrandBlue,
                            unfocusedLabelColor = BrandBlue
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "O ingresá los datos GS1 directamente:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (rawBarcode.isNotEmpty()) {
                        // Construct GS1-128 format if we have GS1 fields
                        val barcode = if (gtin.isNotEmpty()) {
                            buildGs1Barcode(gtin, batchLot, expiryDate)
                        } else {
                            rawBarcode
                        }
                        onConfirm(barcode)
                    }
                },
                enabled = rawBarcode.isNotEmpty() || gtin.isNotEmpty()
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

/**
 * Build a GS1-128 barcode string from individual fields.
 */
private fun buildGs1Barcode(
    gtin: String,
    batchLot: String? = null,
    expiryDate: String? = null
): String {
    val sb = StringBuilder()

    // Add GTIN (AI 01)
    sb.append("]C101")
    sb.append(gtin.padStart(14, '0'))

    // Add Batch/Lot (AI 10) if present
    batchLot?.let {
        if (it.isNotEmpty()) {
            sb.append("10")
            sb.append(it)
            sb.append('\u001D') // FNC1 separator
        }
    }

    // Add Expiry Date (AI 17) if present
    expiryDate?.let {
        if (it.isNotEmpty()) {
            sb.append("17")
            // Parse DD/MM/YYYY to YYMMDD
            val parts = it.split("/")
            if (parts.size == 3) {
                val day = parts[0].padStart(2, '0')
                val month = parts[1].padStart(2, '0')
                val year = parts[2].takeLast(2).padStart(2, '0')
                sb.append(year)
                sb.append(month)
                sb.append(day)
            }
        }
    }

    return sb.toString()
}
