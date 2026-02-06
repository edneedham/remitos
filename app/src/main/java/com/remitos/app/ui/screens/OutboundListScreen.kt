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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.remitos.app.RemitosApplication

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutboundListScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as RemitosApplication
    val viewModel: OutboundViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return OutboundViewModel(app.repository) as T
            }
        }
    )

    val inboundOptions by viewModel.inboundOptions.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()

    var draft by remember { mutableStateOf(OutboundDraftState()) }
    var inboundMenuExpanded by remember { mutableStateOf(false) }

    val selectedInbound = inboundOptions.firstOrNull { it.inboundNoteId == draft.selectedInboundNoteId }
    val availableCount = selectedInbound?.availableCount ?: 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva lista de reparto") },
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
            Text("Datos del chofer")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = draft.driverNombre,
                    onValueChange = { draft = draft.copy(driverNombre = it) },
                    label = { Text("Nombre") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = draft.driverApellido,
                    onValueChange = { draft = draft.copy(driverApellido = it) },
                    label = { Text("Apellido") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("Entrega")
            OutlinedTextField(
                value = draft.deliveryNumber,
                onValueChange = { draft = draft.copy(deliveryNumber = it) },
                label = { Text("Nº Entrega") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = draft.recipientNombre,
                    onValueChange = { draft = draft.copy(recipientNombre = it) },
                    label = { Text("Nombre destinatario") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = draft.recipientApellido,
                    onValueChange = { draft = draft.copy(recipientApellido = it) },
                    label = { Text("Apellido destinatario") },
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = draft.recipientDireccion,
                onValueChange = { draft = draft.copy(recipientDireccion = it) },
                label = { Text("Dirección") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.recipientTelefono,
                onValueChange = { draft = draft.copy(recipientTelefono = it) },
                label = { Text("Teléfono") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.cantidadBultos,
                onValueChange = { draft = draft.copy(cantidadBultos = it) },
                label = { Text("Bultos") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Ingreso disponible")
            ExposedDropdownMenuBox(
                expanded = inboundMenuExpanded,
                onExpandedChange = { inboundMenuExpanded = !inboundMenuExpanded }
            ) {
                OutlinedTextField(
                    value = selectedInbound?.label ?: "",
                    onValueChange = {},
                    label = { Text("Seleccionar ingreso") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = inboundMenuExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    readOnly = true
                )
                ExposedDropdownMenu(
                    expanded = inboundMenuExpanded,
                    onDismissRequest = { inboundMenuExpanded = false }
                ) {
                    inboundOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text("${option.label} • ${option.availableCount} disponibles") },
                            onClick = {
                                draft = draft.copy(selectedInboundNoteId = option.inboundNoteId)
                                inboundMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { viewModel.save(draft, availableCount) },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar lista")
            }
        }
    }

    when (val state = saveState) {
        is OutboundSaveState.Success -> {
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
                title = { Text("Lista guardada") },
                text = { Text("La lista de reparto se guardó correctamente.") }
            )
        }
        is OutboundSaveState.Error -> {
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
