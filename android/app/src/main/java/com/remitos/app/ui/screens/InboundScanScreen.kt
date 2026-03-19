package com.remitos.app.ui.screens

import androidx.compose.ui.res.stringResource
import com.remitos.app.R
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.remitos.app.RemitosApplication
import com.remitos.app.network.NetworkChecker
import com.remitos.app.ocr.OcrProcessor
import com.remitos.app.ui.components.RemitosTextField
import com.remitos.app.ui.components.RemitosTopBar
import com.remitos.app.ui.components.SectionCard
import com.remitos.app.ui.theme.BrandBlue
import com.remitos.app.ui.theme.Spacing
import com.remitos.app.ui.theme.DisabledButtonBackground
import com.remitos.app.ui.theme.DisabledButtonContent
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import com.remitos.app.ui.components.RemitosTextFieldVariant
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboundScanScreen(
    onBack: () -> Unit,
    onOpenCamera: () -> Unit,
    onNavigateToBarcodeScanning: (Long, Int) -> Unit,
    capturedUri: Uri? = null,
    onCapturedUriHandled: () -> Unit = {},
) {
    val context = LocalContext.current
    val app = context.applicationContext as RemitosApplication
    val viewModel: InboundViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return InboundViewModel(app.repository, app.settingsStore, ocrProcessor = OcrProcessor(), authManager = app.authManager) as T
            }
        },
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val draft = uiState.draft
    var showMissingDialog by remember { mutableStateOf(false) }
    var showCameraPermissionDialog by remember { mutableStateOf(false) }
    var pendingScanInfo by remember { mutableStateOf<Pair<Long, Int>?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        viewModel.updateImageUri(uri)
        if (uri != null) {
            viewModel.processImage(context)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            onOpenCamera()
        } else {
            showCameraPermissionDialog = true
        }
    }

    LaunchedEffect(capturedUri) {
        if (capturedUri != null) {
            viewModel.updateImageUri(capturedUri)
            viewModel.processImage(context)
            onCapturedUriHandled()
        }
    }

    val missing = draft.missingFields()
    val missingForDisplay = if (uiState.showMissingErrors) missing else emptyList()

    fun errorMessage(field: MissingField): String? {
        if (!missingForDisplay.contains(field)) return null
        return when (field) {
            MissingField.Cuit -> "Ingresá un CUIT válido (NN-NNNNNNNN-N)."
            MissingField.CantBultos -> "Ingresá una cantidad mayor a cero."
            else -> "Completar ${field.label}."
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            RemitosTopBar(
                title = stringResource(R.string.ingreso_por_ocr),
                onBack = onBack,
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = Spacing.ScreenPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.SectionSpacing),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Scan actions section
            SectionCard(
                title = stringResource(R.string.escanear_documento),
                icon = Icons.Outlined.DocumentScanner,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.ItemSpacing),
                ) {
                    FilledTonalButton(
                        onClick = { imagePicker.launch("image/*") },
                        enabled = !uiState.isProcessing,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = BrandBlue,
                            disabledContainerColor = DisabledButtonBackground,
                            contentColor = Color.White,
                            disabledContentColor = DisabledButtonContent
                        )
                    ) {
                        Icon(
                            Icons.Outlined.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (uiState.isProcessing) DisabledButtonContent else Color.White,
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(stringResource(R.string.galeria), color = if (uiState.isProcessing) DisabledButtonContent else Color.White)
                    }
                    FilledTonalButton(
                        onClick = {
                            val permission = Manifest.permission.CAMERA
                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    permission,
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                onOpenCamera()
                            } else {
                                cameraPermissionLauncher.launch(permission)
                            }
                        },
                        enabled = !uiState.isProcessing,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = BrandBlue,
                            disabledContainerColor = DisabledButtonBackground,
                            contentColor = Color.White,
                            disabledContentColor = DisabledButtonContent
                        )
                    ) {
                        Icon(
                            Icons.Outlined.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (uiState.isProcessing) DisabledButtonContent else Color.White,
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(stringResource(R.string.camara), color = if (uiState.isProcessing) DisabledButtonContent else Color.White)
                    }
                }
                AnimatedVisibility(
                    visible = uiState.isProcessing,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White,
                        trackColor = Color.White,
                    )
                }
                
                AnimatedVisibility(
                    visible = uiState.showOfflineModeMessage,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically(),
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WifiOff,
                                contentDescription = null,
                                tint = BrandBlue,
                            )
                            Spacer(modifier = Modifier.width(Spacing.ItemSpacing))
                            Text(
                                text = stringResource(R.string.red_inestable_procesando_localmente),
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandBlue,
                            )
                        }
                    }
                }
            }

            // Sender section
            SectionCard(
                title = stringResource(R.string.remitente),
                icon = Icons.Outlined.Person,
            ) {
                RemitosTextField(
                    value = draft.senderCuit,
                    onValueChange = { viewModel.updateDraft(draft.copy(senderCuit = it)) },
                    label = stringResource(R.string.cuit_remitente),
                    leadingIcon = Icons.Outlined.Badge,
                    isError = errorMessage(MissingField.Cuit) != null,
                    errorMessage = errorMessage(MissingField.Cuit),
                    variant = RemitosTextFieldVariant.Reversed
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.ItemSpacing)) {
                    RemitosTextField(
                        value = draft.senderNombre,
                        onValueChange = { viewModel.updateDraft(draft.copy(senderNombre = it)) },
                        label = stringResource(R.string.nombre),
                        leadingIcon = Icons.Outlined.Person,
                        modifier = Modifier.weight(1f),
                        isError = errorMessage(MissingField.SenderNombre) != null,
                        errorMessage = errorMessage(MissingField.SenderNombre),
                        variant = RemitosTextFieldVariant.Reversed
                    )
                    RemitosTextField(
                        value = draft.senderApellido,
                        onValueChange = { viewModel.updateDraft(draft.copy(senderApellido = it)) },
                        label = stringResource(R.string.apellido),
                        modifier = Modifier.weight(1f),
                        isError = errorMessage(MissingField.SenderApellido) != null,
                        errorMessage = errorMessage(MissingField.SenderApellido),
                        variant = RemitosTextFieldVariant.Reversed
                    )
                }
            }

            // Destination section
            SectionCard(
                title = stringResource(R.string.destinatario),
                icon = Icons.Outlined.Home,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.ItemSpacing)) {
                    RemitosTextField(
                        value = draft.destNombre,
                        onValueChange = { viewModel.updateDraft(draft.copy(destNombre = it)) },
                        label = stringResource(R.string.nombre),
                        leadingIcon = Icons.Outlined.Person,
                        modifier = Modifier.weight(1f),
                        isError = errorMessage(MissingField.DestNombre) != null,
                        errorMessage = errorMessage(MissingField.DestNombre),
                        variant = RemitosTextFieldVariant.Reversed
                    )
                    RemitosTextField(
                        value = draft.destApellido,
                        onValueChange = { viewModel.updateDraft(draft.copy(destApellido = it)) },
                        label = stringResource(R.string.apellido),
                        modifier = Modifier.weight(1f),
                        isError = errorMessage(MissingField.DestApellido) != null,
                        errorMessage = errorMessage(MissingField.DestApellido),
                        variant = RemitosTextFieldVariant.Reversed
                    )
                }
                RemitosTextField(
                    value = draft.destDireccion,
                    onValueChange = { viewModel.updateDraft(draft.copy(destDireccion = it)) },
                    label = stringResource(R.string.direccion),
                    leadingIcon = Icons.Outlined.Home,
                    isError = errorMessage(MissingField.DestDireccion) != null,
                    errorMessage = errorMessage(MissingField.DestDireccion),
                    variant = RemitosTextFieldVariant.Reversed
                )
                RemitosTextField(
                    value = draft.destTelefono,
                    onValueChange = { viewModel.updateDraft(draft.copy(destTelefono = it)) },
                    label = stringResource(R.string.telefono),
                    leadingIcon = Icons.Outlined.Phone,
                    keyboardType = KeyboardType.Phone,
                    isError = errorMessage(MissingField.DestTelefono) != null,
                    errorMessage = errorMessage(MissingField.DestTelefono),
                    variant = RemitosTextFieldVariant.Reversed
                )
            }

            // Package & remito section
            SectionCard(
                title = stringResource(R.string.datos_del_remito),
                icon = Icons.Outlined.Description,
            ) {
                RemitosTextField(
                    value = draft.cantBultosTotal,
                    onValueChange = { viewModel.updateDraft(draft.copy(cantBultosTotal = it)) },
                    label = stringResource(R.string.cantidad_de_bultos),
                    leadingIcon = Icons.Outlined.Inventory2,
                    keyboardType = KeyboardType.Number,
                    isError = errorMessage(MissingField.CantBultos) != null,
                    errorMessage = errorMessage(MissingField.CantBultos),
                    variant = RemitosTextFieldVariant.Reversed
                )
                RemitosTextField(
                    value = draft.remitoNumCliente,
                    onValueChange = { viewModel.updateDraft(draft.copy(remitoNumCliente = it)) },
                    label = stringResource(R.string.n_mero_de_remito_de_cliente),
                    leadingIcon = Icons.Outlined.Numbers,
                    isError = errorMessage(MissingField.RemitoCliente) != null,
                    errorMessage = errorMessage(MissingField.RemitoCliente),
                    variant = RemitosTextFieldVariant.Reversed
                )
            }

            // Save button
            Button(
                onClick = {
                    if (missing.isNotEmpty()) {
                        showMissingDialog = true
                    } else {
                        viewModel.save()
                    }
                },
                enabled = !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandBlue,
                    disabledContainerColor = DisabledButtonBackground,
                    contentColor = Color.White,
                    disabledContentColor = DisabledButtonContent
                )
            ) {
                Icon(
                    Icons.Outlined.Save,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (uiState.isSaving) DisabledButtonContent else Color.White,
                )
                Spacer(modifier = Modifier.size(Spacing.ItemSpacing))
                Text(
                    stringResource(R.string.guardar_ingreso),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (uiState.isSaving) DisabledButtonContent else Color.White,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.SectionSpacing))
        }
    }

    // Dialogs
    if (showMissingDialog) {
        MissingFieldsDialog(
            missing = missing,
            onDismiss = { showMissingDialog = false },
            onConfirm = { showMissingDialog = false },
        )
    }

    if (showCameraPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showCameraPermissionDialog = false },
            confirmButton = {
                TextButton(onClick = { showCameraPermissionDialog = false }) { Text(stringResource(R.string.aceptar)) }
            },
            title = { Text(stringResource(R.string.permiso_de_camara)) },
            text = { Text(stringResource(R.string.se_necesita_acceso_a_la_camara_para_tomar_la_foto)) },
        )
    }

    if (uiState.showManualEntryPrompt) {
        AlertDialog(
            onDismissRequest = { viewModel.clearManualEntryPrompt() },
            confirmButton = {
                TextButton(onClick = { viewModel.clearManualEntryPrompt() }) { Text(stringResource(R.string.aceptar)) }
            },
            title = { Text(stringResource(R.string.no_se_pudo_leer_el_documento)) },
            text = { Text(stringResource(R.string.complet_los_datos_manualmente)) },
        )
    }

    when (val state = uiState.saveState) {
        is SaveState.Success -> {
            LaunchedEffect(state) {
                scope.launch {
                    snackbarHostState.showSnackbar("Ingreso guardado correctamente")
                }
                viewModel.clearSaveState()
                pendingScanInfo = Pair(state.noteId, state.packageCount)
            }
        }
        is SaveState.Error -> {
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

    if (pendingScanInfo != null) {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = {
                TextButton(onClick = {
                    val info = pendingScanInfo!!
                    pendingScanInfo = null
                    onNavigateToBarcodeScanning(info.first, info.second)
                }) { Text(stringResource(R.string.ahora)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingScanInfo = null
                    onBack()
                }) { Text(stringResource(R.string.mas_tarde)) }
            },
            title = { Text(stringResource(R.string.escanear_bultos_titulo)) },
            text = { Text(stringResource(R.string.escanear_bultos_mensaje)) },
        )
    }
}

@Composable
private fun MissingFieldsDialog(
    missing: List<MissingField>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.aceptar)) }
        },
        title = { Text(stringResource(R.string.completar_datos_faltantes)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.ItemSpacing)) {
                missing.forEach { field ->
                    Text("• ${field.label}")
                }
            }
        },
    )
}
