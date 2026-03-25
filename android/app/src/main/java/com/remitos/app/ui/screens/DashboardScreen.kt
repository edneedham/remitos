package com.remitos.app.ui.screens

import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.remitos.app.R
import com.remitos.app.RemitosApplication
import com.remitos.app.data.DatabaseManager
import com.remitos.app.ui.components.RemitosCard
import com.remitos.app.ui.theme.Spacing
import com.remitos.app.ui.components.BrandedBackground
import com.remitos.app.ui.theme.BrandBlue
import com.remitos.app.ui.theme.BrandRed
import com.remitos.app.ui.theme.DisabledButtonBackground
import com.remitos.app.ui.theme.DisabledButtonContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.collectAsState
import com.remitos.app.data.NetworkMonitor
import com.remitos.app.data.SyncManager
import com.remitos.app.data.SyncState
import com.remitos.app.ui.components.SyncModal

private const val AUTO_LOCK_TIMEOUT_MS = 5 * 60 * 1000L // 5 minutes

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onScan: () -> Unit,
    onInboundHistory: () -> Unit,
    onNewOutbound: () -> Unit,
    onOutboundHistory: () -> Unit,
    onActivity: () -> Unit,
    onUsers: () -> Unit,
    onTemplates: () -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit,
    onDeviceRevoked: () -> Unit,
) {
    val context = LocalContext.current
    
    var showUnlockDialog by remember { mutableStateOf(false) }
    var lastActivityTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var userName by remember { mutableStateOf("Usuario") }
    var unlockPassword by remember { mutableStateOf("") }
    var unlockError by remember { mutableStateOf<String?>(null) }
    
    // Load session info
    LaunchedEffect(Unit) {
        try {
            val db = DatabaseManager.getOfflineDatabase(context)
            val session = db.localSessionDao().getSession()
            session?.let {
                userName = it.userId
            }
        } catch (e: Exception) {
            // Ignore
        }
    }
    
    // Auto-lock timer
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000) // Check every 5 seconds
            if (System.currentTimeMillis() - lastActivityTime > AUTO_LOCK_TIMEOUT_MS) {
                showUnlockDialog = true
                break
            }
        }
    }
    
    // Reset activity on any interaction
    LaunchedEffect(Unit) {
        lastActivityTime = System.currentTimeMillis()
    }
    
    // Initialize sync manager
    val app = context.applicationContext as RemitosApplication
    val syncManager = remember {
        SyncManager(context, app.authManager, NetworkMonitor(context))
    }
    
    // Start network monitoring for sync
    LaunchedEffect(Unit) {
        syncManager.startMonitoring()
    }
    
    // Collect sync state
    val syncState by syncManager.syncState.collectAsState()
    val isSyncing by syncManager.isSyncing.collectAsState()
    val syncMessage by syncManager.syncMessage.collectAsState()
    
    val role = app.authManager.getCurrentUserRole() ?: "operator"
    
    // Handle sync state changes - force logout if suspended/revoked
    LaunchedEffect(syncState) {
        when (syncState) {
            is SyncState.UserSuspended -> {
                // Will show dialog below
            }
            is SyncState.DeviceRevoked -> {
                // Will show dialog below
            }
            else -> { }
        }
    }
    
    // Show sync modal while syncing
    if (isSyncing) {
        SyncModal(
            isVisible = true,
            message = syncMessage ?: "Sincronizando..."
        )
    }
    
    // Handle force logout for suspended/revoked
    when (syncState) {
        is SyncState.UserSuspended -> {
            AlertDialog(
                onDismissRequest = { },
                title = { Text(stringResource(R.string.cuenta_desactivada)) },
                text = { Text(stringResource(R.string.tu_cuenta_ha_sido_desactivada_por_favor_contacta_al_administrador)) },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        // Clear session and logout
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val db = DatabaseManager.getOfflineDatabase(context)
                                db.localSessionDao().clearSession()
                            } catch (e: Exception) { }
                        }
                        onLogout()
                    }) {
                        Text(stringResource(R.string.aceptar))
                    }
                }
            )
        }
        is SyncState.DeviceRevoked -> {
            AlertDialog(
                onDismissRequest = { },
                title = { Text(stringResource(R.string.dispositivo_revocado)) },
                text = { Text(stringResource(R.string.tu_dispositivo_ha_sido_revocado_por_favor_contacta_al_administrador)) },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val db = DatabaseManager.getOfflineDatabase(context)
                                // Clear session
                                db.localSessionDao().clearSession()
                                // Clear local users
                                db.localUserDao().deleteAll()
                            } catch (e: Exception) { }
                        }
                        onDeviceRevoked()
                    }) {
                        Text(stringResource(R.string.aceptar))
                    }
                }
            )
        }
        else -> { }
    }
    
    if (showUnlockDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(stringResource(R.string.sesi_n_bloqueada)) },
            containerColor = Color.White,
            tonalElevation = 0.dp,
            text = {
                Column {
                    Text("Usuario: $userName")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = unlockPassword,
                        onValueChange = { 
                            unlockPassword = it
                            unlockError = null
                        },
                        label = { Text(stringResource(R.string.contrase_a)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val db = DatabaseManager.getOfflineDatabase(context)
                                        val user = db.localUserDao().getById(userName)
                                        if (user != null && com.remitos.app.data.PasswordHasher.verify(unlockPassword, user.passwordHash ?: "")) {
                                            db.localSessionDao().updateLastActivity(System.currentTimeMillis())
                                            lastActivityTime = System.currentTimeMillis()
                                            showUnlockDialog = false
                                            unlockPassword = ""
                                        } else {
                                            unlockError = "Contraseña incorrecta"
                                        }
                                    } catch (e: Exception) {
                                        unlockError = "Error de verificación"
                                    }
                                }
                            }
                        ),
                        isError = unlockError != null,
                        supportingText = unlockError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandBlue,
                            unfocusedBorderColor = BrandBlue,
                            focusedLabelColor = BrandBlue,
                            unfocusedLabelColor = BrandBlue,
                        ),
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val db = DatabaseManager.getOfflineDatabase(context)
                                        db.localSessionDao().clearSession()
                                    } catch (e: Exception) { }
                                }
                                onLogout()
                            },
                            modifier = Modifier.weight(1f).height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandRed,
                                contentColor = Color.White,
                            )
                        ) {
                            Text(stringResource(R.string.cerrar_sesi_n), maxLines = 1)
                        }
                        Button(
                            onClick = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val db = DatabaseManager.getOfflineDatabase(context)
                                        val user = db.localUserDao().getById(userName)
                                        if (user != null && com.remitos.app.data.PasswordHasher.verify(unlockPassword, user.passwordHash ?: "")) {
                                            db.localSessionDao().updateLastActivity(System.currentTimeMillis())
                                            lastActivityTime = System.currentTimeMillis()
                                            // Restore AuthManager current user for role resolution
                                            app.authManager.setCurrentUser(user.id)
                                            showUnlockDialog = false
                                            unlockPassword = ""
                                        } else {
                                            unlockError = "Contraseña incorrecta"
                                        }
                                    } catch (e: Exception) {
                                        unlockError = "Error de verificación"
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).height(52.dp)
                        ) {
                            Text(stringResource(R.string.desbloquear), maxLines = 1)
                        }
                    }
                }
            },
            confirmButton = { },
            dismissButton = { }
        )
    }
    
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(top = 0.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.SectionSpacing),
        ) {
            DashboardHeader(onLogout = onLogout)

            Column(
                modifier = Modifier.padding(horizontal = Spacing.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(Spacing.SectionSpacing),
            ) {
                PrimaryActionCard(
                    icon = Icons.Outlined.CameraAlt,
                    title = stringResource(R.string.nuevo_ingreso),
                    subtitle = stringResource(R.string.escanear_remito),
                    onClick = onScan,
                    modifier = Modifier.fillMaxWidth(),
                )

                SectionLabel(text = stringResource(R.string.acciones))

                ActionGrid(
                    onInboundHistory = onInboundHistory,
                    onNewOutbound = onNewOutbound,
                    onOutboundHistory = onOutboundHistory,
                    onActivity = onActivity,
                    role = role,
                )

                if (role == "company_owner" || role == "warehouse_admin" || role == "admin") {
                    SectionLabel(text = "Administración")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ActionTile(
                            icon = Icons.Outlined.People,
                            title = "Usuarios",
                            subtitle = "Gestión de operadores",
                            onClick = onUsers,
                            modifier = Modifier.weight(1f),
                        )
                        ActionTile(
                            icon = Icons.Outlined.Description,
                            title = "Plantillas",
                            subtitle = "Formatos de impresión",
                            onClick = onTemplates,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                ActionTile(
                    icon = Icons.Outlined.Settings,
                    title = stringResource(R.string.ajustes),
                    subtitle = stringResource(R.string.preferencias_de_lectura),
                    onClick = onSettings,
                    modifier = Modifier.fillMaxWidth(),
                )

            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DashboardHeader(
    onLogout: () -> Unit,
) {
    BrandedBackground(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.width(48.dp))
                Image(
                    painter = painterResource(id = R.drawable.ic_logo_wordmark),
                    contentDescription = stringResource(R.string.en_punto),
                    modifier = Modifier.size(width = 160.dp, height = 42.dp),
                )
                IconButton(onClick = onLogout) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = stringResource(R.string.cerrar_sesi_n),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Text(
                text = stringResource(R.string.remitos),
                style = MaterialTheme.typography.bodyLarge,
                color = BrandBlue,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun PrimaryActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RemitosCard(
        onClick = onClick,
        modifier = modifier,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp).align(Alignment.CenterVertically),
        )
        Spacer(modifier = Modifier.width(Spacing.SectionSpacing))
        Column(modifier = Modifier.weight(1f).align(Alignment.CenterVertically), verticalArrangement = Arrangement.spacedBy(Spacing.ItemSpacing)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun ActionGrid(
    onInboundHistory: () -> Unit,
    onNewOutbound: () -> Unit,
    onOutboundHistory: () -> Unit,
    onActivity: () -> Unit,
    role: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionTile(
                icon = Icons.Outlined.History,
                title = stringResource(R.string.historial),
                subtitle = stringResource(R.string.ver_ingresos),
                onClick = onInboundHistory,
                modifier = Modifier.weight(1f),
            )
            ActionTile(
                icon = Icons.Outlined.LocalShipping,
                title = stringResource(R.string.reparto),
                subtitle = stringResource(R.string.nueva_lista),
                onClick = onNewOutbound,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionTile(
                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                title = stringResource(R.string.historial_de_reparto),
                subtitle = stringResource(R.string.reimprimir_listas),
                onClick = onOutboundHistory,
                modifier = Modifier.weight(1f),
            )
            if (role == "company_owner" || role == "warehouse_admin" || role == "admin") {
                ActionTile(
                    icon = Icons.Outlined.QueryStats,
                    title = stringResource(R.string.actividad),
                    subtitle = stringResource(R.string.ver_m_tricas),
                    onClick = onActivity,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RemitosCard(
        onClick = onClick,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.ItemSpacing),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
}
