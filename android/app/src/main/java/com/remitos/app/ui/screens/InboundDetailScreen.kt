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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ImageNotSupported
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.remitos.app.RemitosApplication
import com.remitos.app.data.InboundNoteStatus
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Download
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.remitos.app.drive.GoogleDriveManager
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import com.remitos.app.ui.components.DetailSectionCard
import com.remitos.app.ui.components.RemitosTopBar
import com.remitos.app.ui.theme.BrandBlue
import com.remitos.app.ui.theme.Spacing
import com.remitos.app.ui.theme.Success100
import com.remitos.app.ui.theme.Success500
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
    viewModel: InboundDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val app = context.applicationContext as RemitosApplication

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    var showVoidDialog by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    val role = app.authManager.getCurrentUserRole() ?: "operator"

    LaunchedEffect(saveState) {
        if (saveState is InboundDetailSaveState.Success) {
            viewModel.clearSaveState()
        }
    }
    
    // Check Google Sign-In status when screen loads
    LaunchedEffect(Unit) {
        val driveManager = GoogleDriveManager(context)
        viewModel.checkGoogleSignInStatus(driveManager)
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
        floatingActionButton = {
            ExportFab(
                onClick = { viewModel.showExportDialog() },
                enabled = !uiState.isLoading && uiState.errorMessage == null
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.SectionSpacing),
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
                            viewModel.showExportDialog()
                        },
                        onScanBarcodes = {
                            onScanBarcodes(noteId, uiState.draft.cantBultosTotal.toIntOrNull() ?: 0)
                        }
                    )
                    
                    // Google Drive Manager
                    val driveManager = remember { GoogleDriveManager(context) }
                    
                    // Google Sign-In launcher
                    val signInLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        viewModel.handleGoogleSignInResult(result, driveManager)
                    }
                    
                    // Export filename dialog
                    if (uiState.showExportDialog) {
                        ExportFilenameDialog(
                            filename = uiState.suggestedExportFilename,
                            isSignedIn = uiState.isGoogleSignedIn,
                            isUploading = uiState.isUploadingToDrive,
                            uploadError = uiState.driveUploadError,
                            uploadSuccess = uiState.driveUploadSuccess,
                            lastUploadedFileId = uiState.lastUploadedFileId,
                            onDismiss = { viewModel.hideExportDialog() },
                            onExport = { customName, uploadToDrive ->
                                viewModel.exportToCsv(context, customName, uploadToDrive, driveManager)
                            },
                            onSignIn = {
                                val signInIntent = driveManager.getGoogleSignInClient().signInIntent
                                signInLauncher.launch(signInIntent)
                            },
                            onViewInDrive = { fileId ->
                                driveManager.openDriveFile(fileId)
                            }
                        )
                    }
                    
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
                        if (role == "company_owner" || role == "warehouse_admin" || role == "admin") {
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
                    .clip(MaterialTheme.shapes.medium),
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
    
    // Render dynamic sections from fieldSections
    state.fieldSections.forEach { (sectionName, fields) ->
        if (fields.isNotEmpty()) {
            DetailSectionCard(
                title = sectionName,
                fields = fields,
            )
        }
    }
}

@Composable
private fun ExportFab(
    onClick: () -> Unit,
    enabled: Boolean,
) {
    if (enabled) {
        FloatingActionButton(
            onClick = onClick,
            containerColor = BrandBlue,
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 12.dp,
            ),
            modifier = Modifier.padding(16.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Download,
                contentDescription = stringResource(R.string.exportar_csv),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun ExportFilenameDialog(
    filename: String,
    isSignedIn: Boolean,
    isUploading: Boolean,
    uploadError: String?,
    uploadSuccess: Boolean,
    lastUploadedFileId: String?,
    onDismiss: () -> Unit,
    onExport: (String, Boolean) -> Unit,
    onSignIn: () -> Unit,
    onViewInDrive: (String) -> Unit,
) {
    var editedName by remember { mutableStateOf(filename) }
    var uploadToDrive by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.exportar_csv)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Filename field
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { newValue -> editedName = newValue },
                    label = { Text(stringResource(R.string.nombre)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Google Drive section
                if (isSignedIn) {
                    // User is signed in - show upload option
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = uploadToDrive,
                            onCheckedChange = { uploadToDrive = it },
                            enabled = !isUploading
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.subir_csv_a_google_drive),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = stringResource(R.string.carpeta_por_defecto),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Show success/error states
                    if (uploadSuccess && lastUploadedFileId != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Success100
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.exportado_y_subido_a_drive),
                                    color = Success500,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                TextButton(
                                    onClick = { onViewInDrive(lastUploadedFileId) },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text(stringResource(R.string.ver_en_drive))
                                }
                            }
                        }
                    }
                    
                    uploadError?.let { error ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (error.contains(stringResource(R.string.error_de_conexi_n_no_se_pudo_subir_a_drive))) {
                                    Text(
                                        text = stringResource(R.string.guardado_localmente_sin_conexi_n),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // User not signed in - show sign in button
                    OutlinedButton(
                        onClick = onSignIn,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.iniciar_sesion_con_google))
                    }
                    Text(
                        text = stringResource(R.string.debes_iniciar_sesion_para_subir_a_drive),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (isUploading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Subiendo a Google Drive...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onExport(editedName, uploadToDrive) },
                enabled = !isUploading && editedName.isNotBlank()
            ) {
                if (uploadToDrive && isSignedIn) {
                    Text(stringResource(R.string.exportar_y_subir))
                } else {
                    Text(stringResource(R.string.solo_guardar_locamente))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancelar))
            }
        }
    )
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
                Success100 else MaterialTheme.colorScheme.surface
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
                        scannedCount >= totalCount && totalCount > 0 -> Success500
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
                        tint = Success500,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.escaneo_completo),
                        color = Success500,
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
