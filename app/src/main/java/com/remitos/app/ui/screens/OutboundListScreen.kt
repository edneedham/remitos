package com.remitos.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.remitos.app.RemitosApplication
import com.remitos.app.print.OutboundListPrinter
import com.remitos.app.ui.components.RemitosTextField
import com.remitos.app.ui.components.RemitosTopBar
import com.remitos.app.ui.components.SectionCard

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
        },
    )

    val inboundOptions by viewModel.inboundOptions.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val printPayload by viewModel.printPayload.collectAsStateWithLifecycle()

    var draft by remember { mutableStateOf(OutboundDraftState()) }
    var inboundMenuExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val selectedInbound = inboundOptions.firstOrNull { it.inboundNoteId == draft.selectedInboundNoteId }
    val availableCount = selectedInbound?.availableCount ?: 0

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            RemitosTopBar(
                title = "Nueva lista de reparto",
                onBack = onBack,
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Driver section
            SectionCard(
                title = "Datos del chofer",
                icon = Icons.Outlined.LocalShipping,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RemitosTextField(
                        value = draft.driverNombre,
                        onValueChange = { draft = draft.copy(driverNombre = it) },
                        label = "Nombre",
                        leadingIcon = Icons.Outlined.Person,
                        modifier = Modifier.weight(1f),
                    )
                    RemitosTextField(
                        value = draft.driverApellido,
                        onValueChange = { draft = draft.copy(driverApellido = it) },
                        label = "Apellido",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Delivery section
            SectionCard(
                title = "Entrega",
                icon = Icons.Outlined.Description,
            ) {
                RemitosTextField(
                    value = draft.deliveryNumber,
                    onValueChange = { draft = draft.copy(deliveryNumber = it) },
                    label = "N° Entrega",
                    leadingIcon = Icons.Outlined.Numbers,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RemitosTextField(
                        value = draft.recipientNombre,
                        onValueChange = { draft = draft.copy(recipientNombre = it) },
                        label = "Nombre destinatario",
                        leadingIcon = Icons.Outlined.Person,
                        modifier = Modifier.weight(1f),
                    )
                    RemitosTextField(
                        value = draft.recipientApellido,
                        onValueChange = { draft = draft.copy(recipientApellido = it) },
                        label = "Apellido",
                        modifier = Modifier.weight(1f),
                    )
                }
                RemitosTextField(
                    value = draft.recipientDireccion,
                    onValueChange = { draft = draft.copy(recipientDireccion = it) },
                    label = "Direccion",
                    leadingIcon = Icons.Outlined.Home,
                )
                RemitosTextField(
                    value = draft.recipientTelefono,
                    onValueChange = { draft = draft.copy(recipientTelefono = it) },
                    label = "Telefono",
                    leadingIcon = Icons.Outlined.Phone,
                    keyboardType = KeyboardType.Phone,
                )
                RemitosTextField(
                    value = draft.cantidadBultos,
                    onValueChange = { draft = draft.copy(cantidadBultos = it) },
                    label = "Bultos",
                    leadingIcon = Icons.Outlined.Inventory2,
                    keyboardType = KeyboardType.Number,
                )
            }

            // Inbound selection section
            SectionCard(
                title = "Ingreso disponible",
                icon = Icons.Outlined.Inventory2,
            ) {
                ExposedDropdownMenuBox(
                    expanded = inboundMenuExpanded,
                    onExpandedChange = { inboundMenuExpanded = !inboundMenuExpanded },
                ) {
                    OutlinedTextField(
                        value = selectedInbound?.label ?: "",
                        onValueChange = {},
                        label = { Text("Seleccionar ingreso") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = inboundMenuExpanded,
                            )
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        readOnly = true,
                        shape = MaterialTheme.shapes.small,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.secondary,
                            focusedLabelColor = MaterialTheme.colorScheme.secondary,
                        ),
                    )
                    ExposedDropdownMenu(
                        expanded = inboundMenuExpanded,
                        onDismissRequest = { inboundMenuExpanded = false },
                    ) {
                        inboundOptions.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text("${option.label} • ${option.availableCount} disponibles")
                                },
                                onClick = {
                                    draft = draft.copy(selectedInboundNoteId = option.inboundNoteId)
                                    inboundMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            // Save button
            Button(
                onClick = { viewModel.save(draft, availableCount) },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Icon(
                    Icons.Outlined.Save,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    "Guardar lista",
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Dialogs
    when (val state = saveState) {
        is OutboundSaveState.Success -> {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {
                    TextButton(
                        onClick = {
                            val payload = printPayload
                            if (payload != null) {
                                OutboundListPrinter(context).print(payload.list, payload.lines)
                            }
                            viewModel.clearSaveState()
                            viewModel.clearPrintPayload()
                            onBack()
                        },
                    ) {
                        if (printPayload != null) {
                            Icon(
                                Icons.Outlined.Print,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                        }
                        Text(if (printPayload != null) "Imprimir" else "Aceptar")
                    }
                },
                dismissButton = if (printPayload != null) {
                    {
                        TextButton(
                            onClick = {
                                viewModel.clearSaveState()
                                viewModel.clearPrintPayload()
                                onBack()
                            },
                        ) {
                            Text("Cerrar")
                        }
                    }
                } else {
                    null
                },
                title = { Text("Lista guardada") },
                text = { Text("La lista de reparto se guardo correctamente.") },
            )
        }
        is OutboundSaveState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.clearSaveState() },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearSaveState() }) { Text("Aceptar") }
                },
                title = { Text("Error al guardar") },
                text = { Text(state.message) },
            )
        }
        null -> Unit
    }
}
