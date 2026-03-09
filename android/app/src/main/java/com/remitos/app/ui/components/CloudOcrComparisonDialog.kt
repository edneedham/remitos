package com.remitos.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.remitos.app.ocr.OcrComparisonResult

@Composable
fun CloudOcrComparisonDialog(
    comparison: OcrComparisonResult,
    onReplace: () -> Unit,
    onKeepOriginal: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        title = {
            Text(
                text = "Reconocimiento mejorado disponible",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "El reconocimiento en la nube encontró diferencias en los datos. ¿Querés usar los datos mejorados?",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                val local = comparison.localResult.fields
                val cloud = comparison.cloudResult?.fields
                
                if (cloud != null) {
                    ComparisonRow("Remitente", local["senderNombre"], cloud["senderNombre"])
                    ComparisonRow("Destinatario", local["destNombre"], cloud["destNombre"])
                    ComparisonRow("CUIT", local["senderCuit"], cloud["senderCuit"])
                    ComparisonRow("Dirección", local["destDireccion"], cloud["destDireccion"])
                    ComparisonRow("Bultos", local["cantBultosTotal"], cloud["cantBultosTotal"])
                    ComparisonRow("N° Remito", local["remitoNumCliente"], cloud["remitoNumCliente"])
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onReplace) {
                Text("Reemplazar")
            }
        },
        dismissButton = {
            TextButton(onClick = onKeepOriginal) {
                Text("Mantener Original")
            }
        }
    )
}

@Composable
private fun ComparisonRow(
    field: String,
    localValue: String?,
    cloudValue: String?
) {
    val differs = localValue != cloudValue
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = field,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        
        if (differs && cloudValue != null) {
            Text(
                text = cloudValue,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1.5f),
                textAlign = TextAlign.End
            )
        } else {
            Text(
                text = localValue ?: "-",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1.5f),
                textAlign = TextAlign.End
            )
        }
    }
}
