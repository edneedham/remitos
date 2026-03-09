package com.remitos.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.remitos.app.BuildConfig
import com.remitos.app.RemitosApplication
import com.remitos.app.data.FeatureFlags
import com.remitos.app.data.UserInfo
import com.remitos.app.ui.components.AccountSwitcherCard
import com.remitos.app.ui.components.RemitosTopBar
import com.remitos.app.ui.theme.BrandBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenDebug: () -> Unit,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as RemitosApplication
    val scope = rememberCoroutineScope()
    val viewModel: SettingsViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(app.settingsStore) as T
            }
        }
    )

    val perspectiveEnabled by viewModel.perspectiveCorrectionEnabled.collectAsStateWithLifecycle()
    val storageInfo by produceState(initialValue = StorageInfo.empty(), context) {
        value = withContext(Dispatchers.IO) {
            loadStorageInfo(File(context.filesDir, "remitos"))
        }
    }

    // Account state
    var currentUser by remember { mutableStateOf<UserInfo?>(null) }
    var accounts by remember { mutableStateOf<List<UserInfo>>(emptyList()) }

    LaunchedEffect(Unit) {
        val currentUserId = app.authManager.getCurrentUser()
        currentUser = currentUserId?.let { app.authManager.getUserInfo(it) }
        accounts = app.authManager.listLoggedInUsers()
    }

    Scaffold(
        modifier = Modifier.background(Color.White),
        containerColor = Color.White,
        topBar = {
            RemitosTopBar(
                title = "Ajustes",
                onBack = onBack,
                showLogo = false,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .background(Color.White),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Account Section
            if (FeatureFlags.enableCloudSync) {
                AccountSwitcherCard(
                    currentUser = currentUser,
                    accounts = accounts,
                    onSwitchAccount = { userId ->
                        scope.launch {
                            app.switchUser(userId)
                            currentUser = app.authManager.getUserInfo(userId)
                            onBack() // Go back after switching
                        }
                    },
                    onLogout = { deleteData ->
                        scope.launch {
                            app.logoutCurrentUser(deleteData)
                            onLogout()
                        }
                    },
                )
            }

            SettingsCard(
                title = "Procesamiento",
                icon = Icons.Outlined.Tune,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Corrección de perspectiva",
                            style = MaterialTheme.typography.titleSmall,
                            color = BrandBlue,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "Mejora la lectura enderezando el remito. Puede usar más recursos.",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandBlue.copy(alpha = 0.7f),
                        )
                    }
                    Switch(
                        checked = perspectiveEnabled,
                        onCheckedChange = viewModel::setPerspectiveCorrectionEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = BrandBlue,
                            uncheckedThumbColor = BrandBlue,
                            uncheckedTrackColor = Color.White,
                        ),
                    )
                }
            }

            SettingsCard(
                title = "Almacenamiento",
                icon = Icons.Outlined.Tune,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Imágenes guardadas",
                            style = MaterialTheme.typography.titleSmall,
                            color = BrandBlue,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "${storageInfo.count} archivos · ${formatBytes(storageInfo.totalBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandBlue.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            // Demo Data Section - Only show if there are no remitos
            val hasRemitos by produceState(initialValue = true, context) {
                value = withContext(Dispatchers.IO) {
                    val notes = mutableListOf<com.remitos.app.data.db.entity.InboundNoteEntity>()
                    app.repository.observeInboundNotes().collect { notes.addAll(it) }
                    notes.isNotEmpty()
                }
            }
            
            if (!hasRemitos) {
                SettingsCard(
                    title = "Datos de demostración",
                    icon = Icons.Outlined.Info,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Generar remitos de ejemplo",
                            style = MaterialTheme.typography.titleSmall,
                            color = BrandBlue,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "Crea 6 remitos de prueba con datos realistas para explorar la aplicación sin necesidad de escanear documentos reales.",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandBlue.copy(alpha = 0.7f),
                        )
                        androidx.compose.material3.Button(
                            onClick = {
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        com.remitos.app.data.TestDataGenerator(app.repository).generateTestData()
                                    }
                                    onBack() // Go back to dashboard to see the data
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = BrandBlue,
                            ),
                        ) {
                            Text("Generar datos de demostración")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                        onLongClick = onOpenDebug,
                    ),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Versión ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandBlue.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                androidx.compose.material3.Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.padding(4.dp),
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

private data class StorageInfo(
    val count: Int,
    val totalBytes: Long,
) {
    companion object {
        fun empty() = StorageInfo(0, 0)
    }
}

private fun loadStorageInfo(directory: File): StorageInfo {
    if (!directory.exists() || !directory.isDirectory) return StorageInfo.empty()
    var count = 0
    var bytes = 0L
    directory.listFiles()?.forEach { file ->
        if (file.isFile) {
            count += 1
            bytes += file.length()
        }
    }
    return StorageInfo(count, bytes)
}

private fun formatBytes(bytes: Long): String {
    val units = arrayOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex += 1
    }
    val rounded = if (unitIndex == 0) value.toInt().toString() else String.format("%.1f", value)
    return "$rounded ${units[unitIndex]}"
}
