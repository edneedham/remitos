package com.remitos.app.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BorderColor
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.remitos.app.RemitosApplication
import com.remitos.app.data.OutboundLineStatus
import com.remitos.app.data.OutboundListStatus
import com.remitos.app.print.OutboundListPrinter
import com.remitos.app.ui.components.RemitosTopBar
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showSignatureDialog by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

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
                    )

                    if (current.message != null) {
                        Text(
                            text = current.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (current.list.checklistSignaturePath == null) {
                        OutlinedButton(
                            onClick = { showSignatureDialog = true },
                            enabled = !current.isSigning && current.list.status == OutboundListStatus.Abierta,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.BorderColor,
                                contentDescription = null,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (current.isSigning) "Firmando…" else "Firmar checklist")
                        }
                    } else {
                        Text(
                            text = "Checklist firmada",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }

                    Button(
                        onClick = { showConfirmDialog = true },
                        enabled = current.list.checklistSignaturePath != null,
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
                            OutboundListPrinter(context).print(ready.list, ready.lines)
                            onBack()
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

    if (showSignatureDialog) {
        SignatureCaptureDialog(
            onDismiss = { showSignatureDialog = false },
            onConfirm = { bitmap ->
                val ready = state as? OutboundPreviewState.Ready ?: return@SignatureCaptureDialog
                val path = saveSignatureBitmap(context, ready.list.id, bitmap)
                if (path != null) {
                    viewModel.signChecklist(ready.list.id, path, System.currentTimeMillis())
                }
                showSignatureDialog = false
            },
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
                    allowActions = state.list.checklistSignaturePath != null &&
                        state.list.status == OutboundListStatus.Abierta &&
                        !isFinalStatus(line.status),
                    onDelivered = { currentLine ->
                        onUpdateOutcome(currentLine.id, OutboundLineStatus.Entregado)
                    },
                    onReturned = { currentLine ->
                        onUpdateOutcome(currentLine.id, OutboundLineStatus.Devuelto)
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
    line: com.remitos.app.data.db.entity.OutboundLineWithRemito,
    allowActions: Boolean,
    onDelivered: (com.remitos.app.data.db.entity.OutboundLineWithRemito) -> Unit,
    onReturned: (com.remitos.app.data.db.entity.OutboundLineWithRemito) -> Unit,
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
            if (allowActions) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onDelivered(line) }) {
                        Text("Entregado")
                    }
                    OutlinedButton(onClick = { onReturned(line) }) {
                        Text("Devuelto")
                    }
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
private fun SignatureCaptureDialog(
    onDismiss: () -> Unit,
    onConfirm: (Bitmap) -> Unit,
) {
    val configuration = LocalConfiguration.current
    val widthPx = (configuration.screenWidthDp * 0.85f).toInt().coerceAtLeast(1)
    val heightPx = (configuration.screenHeightDp * 0.25f).toInt().coerceAtLeast(1)
    val path = remember { androidx.compose.ui.graphics.Path() }
    var hasSignature by remember { mutableStateOf(false) }

    val strokeColor = MaterialTheme.colorScheme.onSurface
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Firma del chofer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    path.moveTo(offset.x, offset.y)
                                    hasSignature = true
                                },
                                onDrag = { change, _ ->
                                    path.lineTo(change.position.x, change.position.y)
                                }
                            )
                        }
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        drawPath(
                            path = path,
                            color = strokeColor,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(
                        onClick = {
                            path.reset()
                            hasSignature = false
                        }
                    ) {
                        Icon(imageVector = Icons.Outlined.Replay, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Borrar")
                    }
                    Text(
                        text = if (hasSignature) "Firma registrada" else "Firmá en el recuadro",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)
                    val paint = Paint().apply {
                        color = Color.BLACK
                        strokeWidth = 4f
                        style = Paint.Style.STROKE
                        isAntiAlias = true
                    }
                    canvas.drawPath(path.asAndroidPath(), paint)
                    onConfirm(bitmap)
                },
                enabled = hasSignature,
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

private fun saveSignatureBitmap(
    context: android.content.Context,
    listId: Long,
    bitmap: Bitmap,
): String? {
    return runCatching {
        val dir = File(context.filesDir, "signatures").apply { mkdirs() }
        val file = File(dir, "checklist_${listId}_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        file.absolutePath
    }.getOrNull()
}

private fun lineStatusLabel(status: String): String {
    return when (status) {
        OutboundLineStatus.Pendiente -> "Pendiente"
        OutboundLineStatus.EnDeposito -> "En depósito"
        OutboundLineStatus.EnTransito -> "En tránsito"
        OutboundLineStatus.Entregado -> "Entregado"
        OutboundLineStatus.Devuelto -> "Devuelto"
        else -> status
    }
}

private fun listStatusLabel(status: String): String {
    return when (status) {
        OutboundListStatus.Abierta -> "Abierta"
        OutboundListStatus.Cerrada -> "Cerrada"
        else -> status
    }
}
