package com.remitos.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.remitos.app.RemitosApplication
import com.remitos.app.data.OutboundLineStatus
import com.remitos.app.data.OutboundListStatus
import com.remitos.app.data.db.entity.OutboundLineStatusHistoryEntity
import com.remitos.app.data.db.entity.OutboundLineWithRemito
import com.remitos.app.data.db.entity.OutboundListEntity
import com.remitos.app.data.db.entity.OutboundLineEditHistoryEntity
import com.remitos.app.print.OutboundListPrinter
import com.remitos.app.ui.components.RemitosTopBar
import com.remitos.app.ui.components.RemitosTextField
import com.remitos.app.ui.theme.BrandBlue
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutboundPreviewScreen(
    listId: Long,
    onBack: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as RemitosApplication
    val viewModel: OutboundPreviewViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return OutboundPreviewViewModel(app.repository) as T
            }
        },
    )

    val state by viewModel.state.collectAsStateWithLifecycle()
    val historyState by viewModel.historyState.collectAsStateWithLifecycle()
    val editState by viewModel.editState.collectAsStateWithLifecycle()
    var showConfirmDialog by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scope = rememberCoroutineScope()

    LaunchedEffect(listId) {
        viewModel.load(listId)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            RemitosTopBar(
                title = "Vista previa",
                onBack = onBack,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            when (val current = state) {
                is OutboundPreviewState.Loading -> {
                    PreviewPlaceholder("Cargando vista previa...")
                }
                is OutboundPreviewState.Error -> {
                    PreviewPlaceholder(current.message)
                }
                is OutboundPreviewState.Ready -> {
                    PreviewCard(
                        state = current,
                        onUpdateOutcome = { lineId, status ->
                            viewModel.updateLineOutcome(lineId, status)
                        },
                        onOpenHistory = { lineId ->
                            viewModel.loadLineHistory(lineId)
                        },
                        onEdit = { lineId ->
                            viewModel.loadLineEdit(lineId)
                        },
                    )

                    if (current.message != null) {
                        Text(
                            text = current.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Button(
                        onClick = { showConfirmDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Print,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Imprimir")
                    }

                    Button(
                        onClick = { viewModel.closeList(current.list.id) },
                        enabled = current.list.status == OutboundListStatus.Abierta && canCloseList(current.lines),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Done,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (current.isClosing) "Cerrando…" else "Cerrar lista")
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showConfirmDialog) {
        val ready = state as? OutboundPreviewState.Ready
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        if (ready != null) {
                            scope.launch {
                                val config = app.settingsStore.getTemplateConfig()
                                OutboundListPrinter(context).print(ready.list, ready.lines, config)
                                onBack()
                            }
                        }
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("Cancelar") }
            },
            title = { Text("Confirmar impresion") },
            text = { Text("Confirmas imprimir la lista de reparto?") },
        )
    }

    if (historyState != null) {
        LineHistoryDialog(
            state = historyState,
            onDismiss = { viewModel.clearLineHistory() }
        )
    }

    if (editState != null) {
        LineEditDialog(
            state = editState,
            onDismiss = { viewModel.clearLineEdit() },
            onUpdateDraft = { draft -> viewModel.updateLineEditDraft(draft) },
            onSave = { viewModel.saveLineEdit() },
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutboundPreviewSampleScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as RemitosApplication
    var state by remember { mutableStateOf(samplePreviewState()) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            RemitosTopBar(
                title = "Checklist de muestra",
                onBack = onBack,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            PreviewCard(
                state = state,
                onUpdateOutcome = { lineId, status ->
                    state = updateSampleOutcome(state, lineId, status)
                },
                onOpenHistory = {},
                onEdit = {},
            )

            Text(
                text = "Vista de ejemplo sin datos reales.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
                onClick = { showConfirmDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Print,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Imprimir")
            }

            Button(
                onClick = { state = closeSampleList(state) },
                enabled = state.list.status == OutboundListStatus.Abierta && canCloseList(state.lines),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Done,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cerrar lista")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        scope.launch {
                            val config = app.settingsStore.getTemplateConfig()
                            OutboundListPrinter(context).print(state.list, state.lines, config)
                        }
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("Cancelar") }
            },
            title = { Text("Confirmar impresion") },
            text = { Text("Confirmas imprimir la lista de reparto?") },
        )
    }

}

@Composable
private fun PreviewPlaceholder(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PreviewCard(
    state: OutboundPreviewState.Ready,
    onUpdateOutcome: (Long, String) -> Unit,
    onOpenHistory: (Long) -> Unit,
    onEdit: (Long) -> Unit,
) {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val date = Instant.ofEpochMilli(state.list.issueDate)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Lista Nº ${state.list.listNumber}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Fecha de emisión: ${formatter.format(date)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Chofer: ${state.list.driverNombre} ${state.list.driverApellido}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Estado: ${listStatusLabel(state.list.status)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(6.dp))

            PreviewHeaderRow()

            state.lines.forEach { line ->
                PreviewLineRow(
                    line = line,
                    allowActions = state.list.status == OutboundListStatus.Abierta &&
                        !isFinalStatus(line.status),
                    onDelivered = { currentLine ->
                        onUpdateOutcome(currentLine.id, OutboundLineStatus.Entregado)
                    },
                    onReturned = { currentLine ->
                        onUpdateOutcome(currentLine.id, OutboundLineStatus.EnDeposito)
                    },
                    onHistory = { currentLine ->
                        onOpenHistory(currentLine.id)
                    },
                    onEdit = { currentLine ->
                        onEdit(currentLine.id)
                    },
                )
            }
        }
    }
}

@Composable
private fun PreviewHeaderRow() {
    Row(modifier = Modifier.fillMaxWidth()) {
        PreviewCell("Remito", 1.2f, true)
        PreviewCell("Entrega", 1.1f, true)
        PreviewCell("Destinatario", 1.6f, true)
        PreviewCell("Direccion", 2.2f, true)
        PreviewCell("Teléfono", 1.4f, true)
        PreviewCell("Bultos", 0.8f, true)
    }
}

@Composable
private fun PreviewLineRow(
    line: OutboundLineWithRemito,
    allowActions: Boolean,
    onDelivered: (OutboundLineWithRemito) -> Unit,
    onReturned: (OutboundLineWithRemito) -> Unit,
    onHistory: (OutboundLineWithRemito) -> Unit,
    onEdit: (OutboundLineWithRemito) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            PreviewCell(line.remitoNumCliente, 1.2f)
            PreviewCell(line.deliveryNumber, 1.1f)
            PreviewCell("${line.recipientNombre} ${line.recipientApellido}", 1.6f)
            PreviewCell(line.recipientDireccion, 2.2f)
            PreviewCell(line.recipientTelefono, 1.4f)
            PreviewCell(line.packageQty.toString(), 0.8f)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Estado: ${lineStatusLabel(line.status)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (line.missingQty > 0) {
                Text(
                    text = "Faltantes: ${line.missingQty}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (allowActions) {
                    OutlinedButton(onClick = { onDelivered(line) }) {
                        Text("Entregado")
                    }
                    OutlinedButton(onClick = { onReturned(line) }) {
                        Text("Volvió al depósito")
                    }
                }
                TextButton(onClick = { onHistory(line) }) {
                    Text("Historial")
                }
                TextButton(onClick = { onEdit(line) }) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Editar")
                }
            }
        }
    }
}

@Composable
private fun RowScope.PreviewCell(
    text: String,
    weight: Float,
    isHeader: Boolean = false,
) {
    Text(
        text = text,
        modifier = Modifier
            .weight(weight)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        style = if (isHeader) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
        fontWeight = if (isHeader) FontWeight.SemiBold else FontWeight.Normal,
        color = if (isHeader) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun LineHistoryDialog(
    state: OutboundLineHistoryState?,
    onDismiss: () -> Unit,
) {
    if (state == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        },
        title = { Text("Historial del remito ${state.line.remitoNumCliente}") },
        text = {
            when {
                state.isLoading -> {
                    Text("Cargando historial...")
                }
                state.message != null -> {
                    Text(state.message)
                }
                state.statusEntries.isEmpty() && state.editEntries.isEmpty() -> {
                    Text("No hay historial registrado.")
                }
                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (state.statusEntries.isNotEmpty()) {
                            Text(
                                text = "Estados",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            state.statusEntries.forEach { entry ->
                                LineHistoryRow(entry)
                            }
                        }
                        if (state.editEntries.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Cambios",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            state.editEntries.forEach { entry ->
                                LineEditHistoryRow(entry)
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun LineHistoryRow(entry: OutboundLineStatusHistoryEntity) {
    Text(
        text = "${lineStatusLabel(entry.status)} · ${formatHistoryTimestamp(entry.createdAt)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun LineEditHistoryRow(entry: OutboundLineEditHistoryEntity) {
    Text(
        text = "${editFieldLabel(entry.fieldName)}: ${entry.oldValue} → ${entry.newValue}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = "${formatHistoryTimestamp(entry.createdAt)} · ${entry.reason}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
    )
}

@Composable
private fun LineEditDialog(
    state: OutboundLineEditState?,
    onDismiss: () -> Unit,
    onUpdateDraft: (OutboundLineEditDraft) -> Unit,
    onSave: () -> Unit,
) {
    if (state == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = !state.isSaving,
            ) {
                Text(if (state.isSaving) "Guardando..." else "Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        title = { Text("Editar remito ${state.line.remitoNumCliente}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (state.message != null) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                RemitosTextField(
                    value = state.draft.deliveryNumber,
                    onValueChange = { onUpdateDraft(state.draft.copy(deliveryNumber = it)) },
                    label = "N° Entrega",
                )
                RemitosTextField(
                    value = state.draft.recipientNombre,
                    onValueChange = { onUpdateDraft(state.draft.copy(recipientNombre = it)) },
                    label = "Nombre destinatario",
                )
                RemitosTextField(
                    value = state.draft.recipientApellido,
                    onValueChange = { onUpdateDraft(state.draft.copy(recipientApellido = it)) },
                    label = "Apellido",
                )
                RemitosTextField(
                    value = state.draft.recipientDireccion,
                    onValueChange = { onUpdateDraft(state.draft.copy(recipientDireccion = it)) },
                    label = "Dirección",
                )
                RemitosTextField(
                    value = state.draft.recipientTelefono,
                    onValueChange = { onUpdateDraft(state.draft.copy(recipientTelefono = it)) },
                    label = "Teléfono",
                    keyboardType = KeyboardType.Phone,
                )
                OutlinedTextField(
                    value = state.draft.missingQty,
                    onValueChange = { value ->
                        onUpdateDraft(state.draft.copy(missingQty = value))
                    },
                    label = { Text("Bultos faltantes") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandBlue,
                        unfocusedBorderColor = BrandBlue,
                        focusedLabelColor = BrandBlue,
                        unfocusedLabelColor = BrandBlue,
                    ),
                )
                OutlinedTextField(
                    value = state.draft.reason,
                    onValueChange = { value ->
                        onUpdateDraft(state.draft.copy(reason = value))
                    },
                    label = { Text("Motivo del cambio") },
                    singleLine = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandBlue,
                        unfocusedBorderColor = BrandBlue,
                        focusedLabelColor = BrandBlue,
                        unfocusedLabelColor = BrandBlue,
                    ),
                )
            }
        }
    )
}

private fun samplePreviewState(): OutboundPreviewState.Ready {
    val list = OutboundListEntity(
        id = 1L,
        listNumber = 1024L,
        issueDate = System.currentTimeMillis(),
        driverNombre = "Laura",
        driverApellido = "García",
        checklistSignaturePath = null,
        checklistSignedAt = null,
        status = OutboundListStatus.Abierta,
    )
    val lines = listOf(
        OutboundLineWithRemito(
            id = 11L,
            outboundListId = 1L,
            inboundNoteId = 101L,
            deliveryNumber = "E-123",
            recipientNombre = "Marcos",
            recipientApellido = "Vera",
            recipientDireccion = "Av. Libertad 123",
            recipientTelefono = "1144556677",
            packageQty = 2,
            allocatedPackageIds = "1,2",
            status = OutboundLineStatus.EnDeposito,
            deliveredQty = 0,
            returnedQty = 0,
            missingQty = 0,
            remitoNumCliente = "R-1001",
            remitoNumInterno = "RI-000101",
        ),
        OutboundLineWithRemito(
            id = 12L,
            outboundListId = 1L,
            inboundNoteId = 102L,
            deliveryNumber = "E-124",
            recipientNombre = "Sofia",
            recipientApellido = "Ibarra",
            recipientDireccion = "Mitre 456",
            recipientTelefono = "1133221100",
            packageQty = 1,
            allocatedPackageIds = "3",
            status = OutboundLineStatus.EnDeposito,
            deliveredQty = 0,
            returnedQty = 0,
            missingQty = 0,
            remitoNumCliente = "R-1002",
            remitoNumInterno = "RI-000102",
        ),
    )
    return OutboundPreviewState.Ready(list = list, lines = lines)
}

private fun updateSampleOutcome(
    state: OutboundPreviewState.Ready,
    lineId: Long,
    status: String,
): OutboundPreviewState.Ready {
    val lines = state.lines.map { line ->
        if (line.id == lineId) {
            val deliveredQty = if (status == OutboundLineStatus.Entregado) line.packageQty else 0
            val returnedQty = 0
            line.copy(status = status, deliveredQty = deliveredQty, returnedQty = returnedQty)
        } else {
            line
        }
    }
    return state.copy(lines = lines)
}

private fun closeSampleList(state: OutboundPreviewState.Ready): OutboundPreviewState.Ready {
    if (!canCloseList(state.lines)) return state
    return state.copy(list = state.list.copy(status = OutboundListStatus.Cerrada))
}

private fun lineStatusLabel(status: String): String {
    return when (status) {
        OutboundLineStatus.EnDeposito -> "En depósito"
        OutboundLineStatus.EnTransito -> "En tránsito"
        OutboundLineStatus.Entregado -> "Entregado"
        else -> status
    }
}

private fun formatHistoryTimestamp(timestamp: Long): String {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale("es", "AR"))
    return Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
}

private fun listStatusLabel(status: String): String {
    return when (status) {
        OutboundListStatus.Abierta -> "Abierta"
        OutboundListStatus.Cerrada -> "Cerrada"
        else -> status
    }
}

private fun editFieldLabel(field: String): String {
    return when (field) {
        "delivery_number" -> "N° Entrega"
        "recipient_nombre" -> "Nombre"
        "recipient_apellido" -> "Apellido"
        "recipient_direccion" -> "Dirección"
        "recipient_telefono" -> "Teléfono"
        "missing_qty" -> "Faltantes"
        else -> field
    }
}
