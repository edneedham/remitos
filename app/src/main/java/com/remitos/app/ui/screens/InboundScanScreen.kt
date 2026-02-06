package com.remitos.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private const val CuitRegex = "\\b\\d{2}-\\d{8}-\\d{1}\\b"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboundScanScreen(onBack: () -> Unit) {
    var draft by remember { mutableStateOf(InboundDraft()) }
    var showMissingDialog by remember { mutableStateOf(false) }

    val missing = draft.missingFields()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ingreso por OCR") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Volver") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Escanear documento")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { /* TODO: selector de imagen */ }) {
                    Text("Seleccionar imagen")
                }
                Button(onClick = { /* TODO: ejecutar OCR */ }) {
                    Text("Procesar OCR")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Datos detectados")
            OutlinedTextField(
                value = draft.senderCuit,
                onValueChange = { draft = draft.copy(senderCuit = it) },
                label = { Text("CUIT Remitente") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.senderNombre,
                onValueChange = { draft = draft.copy(senderNombre = it) },
                label = { Text("Nombre Remitente") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.senderApellido,
                onValueChange = { draft = draft.copy(senderApellido = it) },
                label = { Text("Apellido Remitente") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.destNombre,
                onValueChange = { draft = draft.copy(destNombre = it) },
                label = { Text("Nombre Destinatario") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.destApellido,
                onValueChange = { draft = draft.copy(destApellido = it) },
                label = { Text("Apellido Destinatario") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.destDireccion,
                onValueChange = { draft = draft.copy(destDireccion = it) },
                label = { Text("Dirección Destinatario") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.destTelefono,
                onValueChange = { draft = draft.copy(destTelefono = it) },
                label = { Text("Teléfono Destinatario") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.cantBultosTotal,
                onValueChange = { draft = draft.copy(cantBultosTotal = it) },
                label = { Text("Cantidad de bultos") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.remitoNumCliente,
                onValueChange = { draft = draft.copy(remitoNumCliente = it) },
                label = { Text("Remito Nº Cliente") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.remitoNumInterno,
                onValueChange = { draft = draft.copy(remitoNumInterno = it) },
                label = { Text("Remito Nº Interno") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    showMissingDialog = missing.isNotEmpty()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar ingreso")
            }
        }
    }

    if (showMissingDialog) {
        MissingFieldsDialog(
            missing = missing,
            onDismiss = { showMissingDialog = false },
            onConfirm = { showMissingDialog = false }
        )
    }
}

private data class InboundDraft(
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

private enum class MissingField(val label: String) {
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

@Composable
private fun MissingFieldsDialog(
    missing: List<MissingField>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Aceptar") }
        },
        title = { Text("Completar datos faltantes") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                missing.forEach { field ->
                    Text("• ${field.label}")
                }
            }
        }
    )
}
