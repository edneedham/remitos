package com.remitos.app.ui.screens

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
import com.remitos.app.ui.theme.BrandBlue
import com.remitos.app.ui.theme.BrandRed
import com.remitos.app.ui.theme.Blue100
import com.remitos.app.ui.theme.Blue50
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.OutlinedTextField
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
    onSettings: () -> Unit,
    onLogout: () -> Unit,
    onDeviceRevoked: () -> Unit,
) {
    val context = LocalContext.current
    
    var showUnlockDialog by remember { mutableStateOf(false) }
    var lastActivityTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var userName by remember { mutableStateOf("Usuario") }
    var unlockPin by remember { mutableStateOf("") }
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
            message = "Sincronizando..."
        )
    }
    
    // Handle force logout for suspended/revoked
    when (syncState) {
        is SyncState.UserSuspended -> {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("Cuenta desactivada") },
                text = { Text("Tu cuenta ha sido desactivada. Por favor contacta al administrador.") },
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
                        Text("Aceptar")
                    }
                }
            )
        }
        is SyncState.DeviceRevoked -> {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("Dispositivo revocado") },
                text = { Text("Tu dispositivo ha sido revocado. Por favor contacta al administrador.") },
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
                        Text("Aceptar")
                    }
                }
            )
        }
        else -> { }
    }
    
    if (showUnlockDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Sesión bloqueada") },
            text = {
                Column {
                    Text("Usuario: $userName")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = unlockPin,
                        onValueChange = { 
                            unlockPin = it
                            unlockError = null
                        },
                        label = { Text("PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val db = DatabaseManager.getOfflineDatabase(context)
                                        val user = db.localUserDao().getById(userName)
                                        if (user?.pinHash == unlockPin) {
                                            db.localSessionDao().updateLastActivity(System.currentTimeMillis())
                                            lastActivityTime = System.currentTimeMillis()
                                            showUnlockDialog = false
                                            unlockPin = ""
                                        } else {
                                            unlockError = "PIN incorrecto"
                                        }
                                    } catch (e: Exception) {
                                        unlockError = "Error de verificación"
                                    }
                                }
                            }
                        ),
                        isError = unlockError != null,
                        supportingText = unlockError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth()
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
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandRed,
                                contentColor = Color.White,
                            )
                        ) {
                            Text("Cerrar sesión")
                        }
                        Button(
                            onClick = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val db = DatabaseManager.getOfflineDatabase(context)
                                        val user = db.localUserDao().getById(userName)
                                        if (user?.pinHash == unlockPin) {
                                            db.localSessionDao().updateLastActivity(System.currentTimeMillis())
                                            lastActivityTime = System.currentTimeMillis()
                                            showUnlockDialog = false
                                            unlockPin = ""
                                        } else {
                                            unlockError = "PIN incorrecto"
                                        }
                                    } catch (e: Exception) {
                                        unlockError = "Error de verificación"
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Desbloquear")
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DashboardHeader(onLogout = onLogout)

            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PrimaryActionCard(
                    icon = Icons.Outlined.CameraAlt,
                    title = "Nuevo ingreso",
                    subtitle = "Escanear remito",
                    onClick = onScan,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(116.dp),
                )

                SectionLabel(text = "Acciones")

                ActionGrid(
                    onInboundHistory = onInboundHistory,
                    onNewOutbound = onNewOutbound,
                    onOutboundHistory = onOutboundHistory,
                    onActivity = onActivity,
                )

                ActionTile(
                    icon = Icons.Outlined.Settings,
                    title = "Ajustes",
                    subtitle = "Preferencias de lectura",
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Blue50,
                        Color(0xFFF6F7F9),
                    ),
                ),
            )
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.width(48.dp))
                Image(
                    painter = painterResource(id = R.drawable.ic_logo_wordmark),
                    contentDescription = "en punto",
                    modifier = Modifier.size(width = 160.dp, height = 42.dp),
                )
                IconButton(onClick = onLogout) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = "Cerrar sesión",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Text(
                text = "Remitos",
                style = MaterialTheme.typography.bodyMedium,
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
    Card(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(Blue100),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.size(26.dp),
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionTile(
                icon = Icons.Outlined.History,
                title = "Historial",
                subtitle = "Ver ingresos",
                onClick = onInboundHistory,
                modifier = Modifier.weight(1f),
            )
            ActionTile(
                icon = Icons.Outlined.LocalShipping,
                title = "Reparto",
                subtitle = "Nueva lista",
                onClick = onNewOutbound,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionTile(
                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                title = "Historial de reparto",
                subtitle = "Reimprimir listas",
                onClick = onOutboundHistory,
                modifier = Modifier.weight(1f),
            )
            ActionTile(
                icon = Icons.Outlined.QueryStats,
                title = "Actividad",
                subtitle = "Ver métricas",
                onClick = onActivity,
                modifier = Modifier.weight(1f),
            )
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
    Card(
        modifier = modifier
            .height(104.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.size(16.dp),
                )
            }
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
