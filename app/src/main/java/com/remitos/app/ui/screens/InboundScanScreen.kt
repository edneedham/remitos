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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.remitos.app.RemitosApplication

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboundScanScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as RemitosApplication
    val viewModel: InboundViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return InboundViewModel(app.repository) as T
            }
        }
    )

    val draft = viewModel.draft
    var showMissingDialog by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        viewModel.updateImageUri(uri)
    }

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
                Button(
                    onClick = { imagePicker.launch("image/*") },
                    enabled = !viewModel.isProcessing
                ) {
                    Text("Seleccionar imagen")
                }
                Button(
                    onClick = { viewModel.processImage(context) },
                    enabled = viewModel.selectedImageUri != null && !viewModel.isProcessing
                ) {
                    Text("Procesar OCR")
                }
            }

            if (viewModel.isProcessing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Datos detectados")
            OutlinedTextField(
                value = draft.senderCuit,
                onValueChange = { viewModel.updateDraft(draft.copy(senderCuit = it)) },
                label = { Text("CUIT Remitente") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.senderNombre,
                onValueChange = { viewModel.updateDraft(draft.copy(senderNombre = it)) },
                label = { Text("Nombre Remitente") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.senderApellido,
                onValueChange = { viewModel.updateDraft(draft.copy(senderApellido = it)) },
                label = { Text("Apellido Remitente") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.destNombre,
                onValueChange = { viewModel.updateDraft(draft.copy(destNombre = it)) },
                label = { Text("Nombre Destinatario") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.destApellido,
                onValueChange = { viewModel.updateDraft(draft.copy(destApellido = it)) },
                label = { Text("Apellido Destinatario") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.destDireccion,
                onValueChange = { viewModel.updateDraft(draft.copy(destDireccion = it)) },
                label = { Text("Dirección Destinatario") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.destTelefono,
                onValueChange = { viewModel.updateDraft(draft.copy(destTelefono = it)) },
                label = { Text("Teléfono Destinatario") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.cantBultosTotal,
                onValueChange = { viewModel.updateDraft(draft.copy(cantBultosTotal = it)) },
                label = { Text("Cantidad de bultos") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.remitoNumCliente,
                onValueChange = { viewModel.updateDraft(draft.copy(remitoNumCliente = it)) },
                label = { Text("Remito Nº Cliente") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.remitoNumInterno,
                onValueChange = { viewModel.updateDraft(draft.copy(remitoNumInterno = it)) },
                label = { Text("Remito Nº Interno") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (missing.isNotEmpty()) {
                        showMissingDialog = true
                    } else {
                        viewModel.save()
                    }
                },
                enabled = !viewModel.isSaving,
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

    val ocrErrorMessage = viewModel.ocrErrorMessage
    if (ocrErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearOcrError() },
            confirmButton = {
                TextButton(onClick = { viewModel.clearOcrError() }) { Text("Aceptar") }
            },
            title = { Text("Error de OCR") },
            text = { Text(ocrErrorMessage) }
        )
    }

    when (val state = viewModel.saveState) {
        is SaveState.Success -> {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearSaveState()
                            onBack()
                        }
                    ) {
                        Text("Aceptar")
                    }
                },
                title = { Text("Ingreso guardado") },
                text = { Text("El ingreso se guardó correctamente.") }
            )
        }
        is SaveState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.clearSaveState() },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearSaveState() }) { Text("Aceptar") }
                },
                title = { Text("Error al guardar") },
                text = { Text(state.message) }
            )
        }
        null -> Unit
    }
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
