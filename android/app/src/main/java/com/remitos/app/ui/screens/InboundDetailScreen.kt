package com.remitos.app.ui.screens

import androidx.compose.ui.res.stringResource
import com.remitos.app.R
import android.graphics.Bitmap
import android.net.Uri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ImageNotSupported
import androidx.compose.material.icons.outlined.Save
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.remitos.app.RemitosApplication
import com.remitos.app.data.InboundNoteStatus
import com.remitos.app.ui.components.RemitosTextField
import com.remitos.app.ui.components.RemitosTopBar
import com.remitos.app.ui.theme.BrandBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.roundToInt



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboundDetailScreen(
    noteId: Long,
    onBack: () -> Unit,
    onScanBarcodes: (Long, Int) -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as RemitosApplication
    val viewModel: InboundDetailViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return InboundDetailViewModel(noteId, app.repository) as T
            }
        },
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    var showVoidDialog by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(saveState) {
        if (saveState is InboundDetailSaveState.Success) {
            viewModel.clearSaveState()
        }
    }

    Scaffold(
        modifier = Modifier,
        topBar = {
            RemitosTopBar(
                title = stringResource(R.string.detalle_de_ingreso),
                onBack = onBack,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when {
                uiState.isLoading -> {
                    Text(
                        text = stringResource(R.string.cargando_ingreso),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                uiState.errorMessage != null -> {
                    Text(
                        text = uiState.errorMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                else -> {
                    InboundDetailHeader(uiState)
                    InboundDetailImage(uiState.scanImagePath)
                    InboundDetailForm(
                        state = uiState,
                        onDraftChange = viewModel::updateDraft,
                    )
                    
                    // Barcode scan section
                    BarcodeScanSection(
                        scannedCount = uiState.scannedCount,
                        totalCount = uiState.draft.cantBultosTotal.toIntOrNull() ?: 0,
                        packages = uiState.packages,
                        onExportCsv = {
                            viewModel.exportToCsv(context)
                        },
                        onScanBarcodes = {
                            onScanBarcodes(noteId, uiState.draft.cantBultosTotal.toIntOrNull() ?: 0)
                        }
                    )
                    
                    if (saveState is InboundDetailSaveState.Error) {
                        Text(
                            text = (saveState as InboundDetailSaveState.Error).message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = viewModel::save,
                            enabled = !uiState.isSaving && !uiState.isVoiding && uiState.status != InboundNoteStatus.Anulada,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Save,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (uiState.isSaving) "Guardando…" else "Guardar cambios")
                        }
                        OutlinedButton(
                            onClick = { showVoidDialog = true },
                            enabled = !uiState.isSaving && !uiState.isVoiding && uiState.status != InboundNoteStatus.Anulada,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (uiState.isVoiding) "Anulando…" else "Anular ingreso")
                        }
                    }
                }
            }
        }
    }

    if (showVoidDialog) {
        AlertDialog(
            onDismissRequest = { showVoidDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showVoidDialog = false
                        viewModel.voidNote()
                    }
                ) {
                    Text(stringResource(R.string.anular))
                }
            },
            dismissButton = {
                TextButton(onClick = { showVoidDialog = false }) {
                    Text(stringResource(R.string.cancelar))
                }
            },
            title = { Text(stringResource(R.string.anular_ingreso)) },
            text = { Text(stringResource(R.string.este_ingreso_quedar_anulado_y_no_podr_asignarse_a_repartos)) },
        )
    }
}

@Composable
private fun InboundDetailHeader(state: InboundDetailUiState) {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val createdAt = Instant.ofEpochMilli(state.createdAt)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    val updatedAt = Instant.ofEpochMilli(state.updatedAt)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Ingreso #${state.noteId}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (state.status == InboundNoteStatus.Anulada) "Anulado" else "Activo",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (state.status == InboundNoteStatus.Anulada) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.secondary
                    },
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = "Creado: ${formatter.format(createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Actualizado: ${formatter.format(updatedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InboundDetailImage(scanImagePath: String?) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        if (scanImagePath != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(Uri.parse(scanImagePath))
                    .crossfade(true)
                    .build(),
                contentDescription = stringResource(R.string.imagen_escaneada),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ImageNotSupported,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(36.dp),
                    )
                    Text(
                        text = stringResource(R.string.sin_imagen_asociada),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun InboundDetailForm(
    state: InboundDetailUiState,
    onDraftChange: (InboundDraftState) -> Unit,
) {
    val draft = state.draft
    val missing = if (state.showMissingErrors) draft.missingFields() else emptyList()
    val readOnly = state.status == InboundNoteStatus.Anulada

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RemitosTextField(
            value = draft.senderCuit,
            onValueChange = { onDraftChange(draft.copy(senderCuit = it)) },
            label = stringResource(R.string.cuit_remitente),
            isError = missing.contains(MissingField.Cuit),
            errorMessage = stringResource(R.string.ingres_un_cuit_v_lido),
            readOnly = readOnly,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RemitosTextField(
                value = draft.senderNombre,
                onValueChange = { onDraftChange(draft.copy(senderNombre = it)) },
                label = stringResource(R.string.nombre_remitente),
                isError = missing.contains(MissingField.SenderNombre),
                errorMessage = stringResource(R.string.campo_requerido),
                modifier = Modifier.weight(1f),
                readOnly = readOnly,
            )
            RemitosTextField(
                value = draft.senderApellido,
                onValueChange = { onDraftChange(draft.copy(senderApellido = it)) },
                label = stringResource(R.string.apellido_remitente),
                isError = missing.contains(MissingField.SenderApellido),
                errorMessage = stringResource(R.string.campo_requerido),
                modifier = Modifier.weight(1f),
                readOnly = readOnly,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RemitosTextField(
                value = draft.destNombre,
                onValueChange = { onDraftChange(draft.copy(destNombre = it)) },
                label = stringResource(R.string.nombre_destinatario),
                isError = missing.contains(MissingField.DestNombre),
                errorMessage = stringResource(R.string.campo_requerido),
                modifier = Modifier.weight(1f),
                readOnly = readOnly,
            )
            RemitosTextField(
                value = draft.destApellido,
                onValueChange = { onDraftChange(draft.copy(destApellido = it)) },
                label = stringResource(R.string.apellido_destinatario),
                isError = missing.contains(MissingField.DestApellido),
                errorMessage = stringResource(R.string.campo_requerido),
                modifier = Modifier.weight(1f),
                readOnly = readOnly,
            )
        }
        RemitosTextField(
            value = draft.destDireccion,
            onValueChange = { onDraftChange(draft.copy(destDireccion = it)) },
            label = stringResource(R.string.direcci_n_destinatario),
            isError = missing.contains(MissingField.DestDireccion),
            errorMessage = stringResource(R.string.campo_requerido),
            readOnly = readOnly,
        )
        RemitosTextField(
            value = draft.destTelefono,
            onValueChange = { onDraftChange(draft.copy(destTelefono = it)) },
            label = stringResource(R.string.tel_fono_destinatario),
            isError = missing.contains(MissingField.DestTelefono),
            errorMessage = stringResource(R.string.campo_requerido),
            readOnly = readOnly,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RemitosTextField(
                value = draft.cantBultosTotal,
                onValueChange = { onDraftChange(draft.copy(cantBultosTotal = it)) },
                label = stringResource(R.string.cantidad_de_bultos),
                isError = missing.contains(MissingField.CantBultos),
                errorMessage = stringResource(R.string.campo_requerido),
                modifier = Modifier.weight(1f),
                readOnly = readOnly,
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
            )
            RemitosTextField(
                value = draft.remitoNumCliente,
                onValueChange = { onDraftChange(draft.copy(remitoNumCliente = it)) },
                label = stringResource(R.string.n_mero_de_remito_de_cliente),
                isError = missing.contains(MissingField.RemitoCliente),
                errorMessage = stringResource(R.string.campo_requerido),
                modifier = Modifier.weight(1f),
                readOnly = readOnly,
            )
        }
    }
}



@Composable
private fun BarcodeScanSection(
    scannedCount: Int,
    totalCount: Int,
    packages: List<com.remitos.app.data.db.entity.InboundPackageEntity>,
    onExportCsv: () -> Unit,
    onScanBarcodes: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (scannedCount >= totalCount && totalCount > 0) 
                Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.escaneo_de_c_digos),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (scannedCount > 0) {
                    TextButton(onClick = onExportCsv) {
                        Text(stringResource(R.string.exportar_csv))
                    }
                }
                
                // Show scan button if not all packages are scanned
                if (scannedCount < totalCount && totalCount > 0) {
                    TextButton(onClick = onScanBarcodes) {
                        Text(stringResource(R.string.escanear))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.bultos_escaneados),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "$scannedCount / $totalCount",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        scannedCount >= totalCount && totalCount > 0 -> Color(0xFF2E7D32)
                        scannedCount > 0 -> BrandBlue
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            if (scannedCount >= totalCount && totalCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.escaneo_completo),
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            // Show list of scanned packages
            if (packages.any { it.barcodeRaw != null }) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.c_digos_escaneados),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                packages.filter { it.barcodeRaw != null }.take(5).forEach { pkg ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Bulto ${pkg.packageIndex}:",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = pkg.barcodeRaw?.take(25) ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                if (packages.count { it.barcodeRaw != null } > 5) {
                    Text(
                        text = "... y ${packages.count { it.barcodeRaw != null } - 5} más",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
