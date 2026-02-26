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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Save
import com.remitos.app.print.OutboundListPrinter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.remitos.app.RemitosApplication
import com.remitos.app.ui.components.RemitosTextField
import com.remitos.app.ui.components.RemitosTopBar
import com.remitos.app.ui.components.SectionCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutboundListScreen(
    onBack: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
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

    var draft by remember {
        mutableStateOf(OutboundDraftState(lines = listOf(OutboundLineDraft(id = 0L))))
    }
    var nextLineId by remember { mutableStateOf(1L) }
    var expandedLineId by remember { mutableStateOf<Long?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scope = rememberCoroutineScope()

    fun updateLine(lineId: Long, updater: (OutboundLineDraft) -> OutboundLineDraft) {
        draft = draft.copy(
            lines = draft.lines.map { line ->
                if (line.id == lineId) updater(line) else line
            }
        )
    }

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

            val selectedIds = draft.lines.mapNotNull { it.selectedInboundNoteId }.toSet()

            draft.lines.forEachIndexed { index, line ->
                val selectedInbound = inboundOptions.firstOrNull { it.inboundNoteId == line.selectedInboundNoteId }
                val optionsForLine = inboundOptions.filter { option ->
                    option.inboundNoteId == line.selectedInboundNoteId || !selectedIds.contains(option.inboundNoteId)
                }

                SectionCard(
                    title = "Remito ${index + 1}",
                    icon = Icons.Outlined.Description,
                ) {
                    if (draft.lines.size > 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            IconButton(
                                onClick = {
                                    draft = draft.copy(lines = draft.lines.filter { it.id != line.id })
                                    if (expandedLineId == line.id) {
                                        expandedLineId = null
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = "Eliminar remito",
                                )
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = expandedLineId == line.id,
                        onExpandedChange = {
                            expandedLineId = if (expandedLineId == line.id) null else line.id
                        },
                    ) {
                        OutlinedTextField(
                            value = selectedInbound?.label ?: "",
                            onValueChange = {},
                            label = { Text("Seleccionar remito") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = expandedLineId == line.id,
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
                            expanded = expandedLineId == line.id,
                            onDismissRequest = { expandedLineId = null },
                        ) {
                            optionsForLine.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text("${option.label} • ${option.availableCount} disponibles")
                                    },
                                    onClick = {
                                        updateLine(line.id) {
                                            it.copy(selectedInboundNoteId = option.inboundNoteId)
                                        }
                                        expandedLineId = null
                                    },
                                )
                            }
                        }
                    }

                    if (selectedInbound != null) {
                        Text(
                            text = "Disponibles: ${selectedInbound.availableCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    RemitosTextField(
                        value = line.deliveryNumber,
                        onValueChange = { value ->
                            updateLine(line.id) { it.copy(deliveryNumber = value) }
                        },
                        label = "N° Entrega",
                        leadingIcon = Icons.Outlined.Numbers,
                    )
                    RemitosTextField(
                        value = line.recipientNombre,
                        onValueChange = { value ->
                            updateLine(line.id) { it.copy(recipientNombre = value) }
                        },
                        label = "Nombre destinatario",
                        leadingIcon = Icons.Outlined.Person,
                    )
                    RemitosTextField(
                        value = line.recipientApellido,
                        onValueChange = { value ->
                            updateLine(line.id) { it.copy(recipientApellido = value) }
                        },
                        label = "Apellido",
                    )
                    RemitosTextField(
                        value = line.recipientDireccion,
                        onValueChange = { value ->
                            updateLine(line.id) { it.copy(recipientDireccion = value) }
                        },
                        label = "Direccion",
                        leadingIcon = Icons.Outlined.Home,
                    )
                    RemitosTextField(
                        value = line.recipientTelefono,
                        onValueChange = { value ->
                            updateLine(line.id) { it.copy(recipientTelefono = value) }
                        },
                        label = "Telefono",
                        leadingIcon = Icons.Outlined.Phone,
                        keyboardType = KeyboardType.Phone,
                    )
                    RemitosTextField(
                        value = line.cantidadBultos,
                        onValueChange = { value ->
                            updateLine(line.id) { it.copy(cantidadBultos = value) }
                        },
                        label = "Bultos",
                        leadingIcon = Icons.Outlined.Inventory2,
                        keyboardType = KeyboardType.Number,
                    )
                }
            }

            TextButton(
                onClick = {
                    draft = draft.copy(
                        lines = draft.lines + OutboundLineDraft(id = nextLineId)
                    )
                    nextLineId += 1
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text("Agregar remito")
            }

            // Save button
            Button(
                onClick = { viewModel.save(draft, inboundOptions) },
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
                            viewModel.clearSaveState()
                            viewModel.clearPrintPayload()
                            if (payload != null) {
                                OutboundListPrinter(context).print(payload.list, payload.lines)
                            }
                            onBack()
                        },
                    ) {
                        Icon(
                            Icons.Outlined.Print,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Imprimir")
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(
                            onClick = {
                                val payload = printPayload
                                viewModel.clearSaveState()
                                viewModel.clearPrintPayload()
                                if (payload != null) {
                                    scope.launch {
                                        val file = OutboundListPrinter(context).saveToPdf(payload.list, payload.lines)
                                        if (file != null) {
                                            snackbarHostState.showSnackbar("PDF guardado en: ${file.absolutePath}")
                                        }
                                    }
                                }
                            },
                        ) {
                            Icon(
                                Icons.Outlined.Save,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("Guardar PDF")
                        }
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
