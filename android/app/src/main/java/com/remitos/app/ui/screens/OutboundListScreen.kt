package com.remitos.app.ui.screens

import androidx.compose.ui.res.stringResource
import com.remitos.app.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.remitos.app.RemitosApplication
import com.remitos.app.print.OutboundListPrinter
import com.remitos.app.ui.components.RemitosTextField
import com.remitos.app.ui.components.RemitosTextFieldVariant
import com.remitos.app.ui.components.RemitosTopBar
import com.remitos.app.ui.theme.BrandBlue
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

    val draft by viewModel.draftState.collectAsStateWithLifecycle()
    var expandedLineId by remember { mutableStateOf<Long?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .background(Color.White),
        containerColor = Color.White,
        topBar = {
            RemitosTopBar(
                title = stringResource(R.string.nueva_lista_de_reparto),
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
                .background(Color.White)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Driver section
            RepartoCard(
                title = stringResource(R.string.datos_del_chofer),
                icon = Icons.Outlined.LocalShipping,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RemitosTextField(
                        value = draft.driverNombre,
                        onValueChange = { viewModel.updateDriverNombre(it) },
                        label = stringResource(R.string.nombre),
                        leadingIcon = Icons.Outlined.Person,
                        modifier = Modifier.weight(1f),
                        variant = RemitosTextFieldVariant.Reversed,
                    )
                    RemitosTextField(
                        value = draft.driverApellido,
                        onValueChange = { viewModel.updateDriverApellido(it) },
                        label = stringResource(R.string.apellido),
                        modifier = Modifier.weight(1f),
                        variant = RemitosTextFieldVariant.Reversed,
                    )
                }
            }

            val selectedIds = draft.lines.mapNotNull { it.selectedInboundNoteId }.toSet()

            draft.lines.forEachIndexed { index, line ->
                val selectedInbound = inboundOptions.firstOrNull { it.inboundNoteId == line.selectedInboundNoteId }
                val optionsForLine = inboundOptions.filter { option ->
                    option.inboundNoteId == line.selectedInboundNoteId || !selectedIds.contains(option.inboundNoteId)
                }

                RepartoCard(
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
                                    viewModel.removeLine(line.id)
                                    if (expandedLineId == line.id) {
                                        expandedLineId = null
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = stringResource(R.string.eliminar_remito),
                                    tint = BrandBlue,
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
                            label = { Text(stringResource(R.string.seleccionar_remito), color = BrandBlue) },
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
                                focusedBorderColor = BrandBlue,
                                unfocusedBorderColor = BrandBlue,
                                focusedLabelColor = BrandBlue,
                                unfocusedLabelColor = BrandBlue,
                                focusedTextColor = BrandBlue,
                                unfocusedTextColor = BrandBlue,
                                cursorColor = BrandBlue,
                            ),
                        )
                        ExposedDropdownMenu(
                            expanded = expandedLineId == line.id,
                            onDismissRequest = { expandedLineId = null },
                            modifier = Modifier.background(Color.White),
                        ) {
                            optionsForLine.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "${option.label} • ${option.availableCount} disponibles",
                                            color = BrandBlue,
                                        )
                                    },
                                    onClick = {
                                        viewModel.updateLine(line.id) {
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
                            viewModel.updateLine(line.id) { it.copy(deliveryNumber = value) }
                        },
                        label = stringResource(R.string.n_entrega),
                        leadingIcon = Icons.Outlined.Numbers,
                        variant = RemitosTextFieldVariant.Reversed,
                    )
                    RemitosTextField(
                        value = line.recipientNombre,
                        onValueChange = { value ->
                            viewModel.updateLine(line.id) { it.copy(recipientNombre = value) }
                        },
                        label = stringResource(R.string.nombre_destinatario_1),
                        leadingIcon = Icons.Outlined.Person,
                        variant = RemitosTextFieldVariant.Reversed,
                    )
                    RemitosTextField(
                        value = line.recipientApellido,
                        onValueChange = { value ->
                            viewModel.updateLine(line.id) { it.copy(recipientApellido = value) }
                        },
                        label = stringResource(R.string.apellido),
                        variant = RemitosTextFieldVariant.Reversed,
                    )
                    RemitosTextField(
                        value = line.recipientDireccion,
                        onValueChange = { value ->
                            viewModel.updateLine(line.id) { it.copy(recipientDireccion = value) }
                        },
                        label = stringResource(R.string.direccion),
                        leadingIcon = Icons.Outlined.Home,
                        variant = RemitosTextFieldVariant.Reversed,
                    )
                    RemitosTextField(
                        value = line.recipientTelefono,
                        onValueChange = { value ->
                            viewModel.updateLine(line.id) { it.copy(recipientTelefono = value) }
                        },
                        label = stringResource(R.string.telefono),
                        leadingIcon = Icons.Outlined.Phone,
                        keyboardType = KeyboardType.Phone,
                        variant = RemitosTextFieldVariant.Reversed,
                    )
                    RemitosTextField(
                        value = line.cantidadBultos,
                        onValueChange = { value ->
                            viewModel.updateLine(line.id) { it.copy(cantidadBultos = value) }
                        },
                        label = stringResource(R.string.bultos),
                        leadingIcon = Icons.Outlined.Inventory2,
                        keyboardType = KeyboardType.Number,
                        variant = RemitosTextFieldVariant.Reversed,
                    )
                }
            }

            TextButton(
                onClick = {
                    viewModel.addLine()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = BrandBlue,
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(stringResource(R.string.agregar_remito), color = BrandBlue)
            }

            // Save button
            Button(
                onClick = { viewModel.save(inboundOptions) },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandBlue,
                ),
            ) {
                Icon(
                    Icons.Outlined.Save,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    stringResource(R.string.guardar_lista),
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
                        Text(stringResource(R.string.imprimir))
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
                            Text(stringResource(R.string.guardar_pdf))
                        }
                        TextButton(
                            onClick = {
                                viewModel.clearSaveState()
                                viewModel.clearPrintPayload()
                                onBack()
                            },
                        ) {
                            Text(stringResource(R.string.cerrar))
                        }
                    }
                },
                title = { Text(stringResource(R.string.lista_guardada)) },
                text = { Text(stringResource(R.string.la_lista_de_reparto_se_guardo_correctamente)) },
            )
        }
        is OutboundSaveState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.clearSaveState() },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearSaveState() }) { Text(stringResource(R.string.aceptar)) }
                },
                title = { Text(stringResource(R.string.error_al_guardar)) },
                text = { Text(state.message) },
            )
        }
        null -> Unit
    }
}

@Composable
private fun RepartoCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .background(Color.White),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = BrandBlue,
                )
            }
            content()
        }
    }
}
