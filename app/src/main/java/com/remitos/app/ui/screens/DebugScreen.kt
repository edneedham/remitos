package com.remitos.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.remitos.app.RemitosApplication
import com.remitos.app.data.db.entity.DebugLogEntity
import com.remitos.app.ui.components.RemitosTopBar
import com.remitos.app.ui.components.SectionCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as RemitosApplication
    val viewModel: DebugViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DebugViewModel(app.repository) as T
            }
        },
    )

    val logs by viewModel.logs.collectAsStateWithLifecycle()
    var selectedLog by remember { mutableStateOf<DebugLogEntity?>(null) }

    Scaffold(
        topBar = {
            RemitosTopBar(
                title = "Debug",
                onBack = onBack,
                showLogo = false,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }
            items(logs) { log ->
                DebugLogCard(
                    log = log,
                    onClick = { selectedLog = log },
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    selectedLog?.let { log ->
        AlertDialog(
            onDismissRequest = { selectedLog = null },
            confirmButton = {
                TextButton(onClick = { selectedLog = null }) {
                    Text("Aceptar")
                }
            },
            title = { Text("Detalle de debug") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    DebugDetailRow(label = "Fecha", value = formatTimestamp(log.createdAt))
                    DebugDetailRow(label = "Modelo", value = log.deviceModel ?: "—")
                    DebugDetailRow(
                        label = "Resolución",
                        value = formatResolution(log.imageWidth, log.imageHeight),
                    )
                    DebugDetailRow(
                        label = "Preprocesamiento",
                        value = log.preprocessTimeMs?.let { "${it} ms" } ?: "—",
                    )
                    DebugDetailRow(
                        label = "Error",
                        value = log.failureReason ?: "—",
                    )
                    DebugDetailRow(
                        label = "Parsing",
                        value = log.parsingErrorSummary ?: "—",
                    )
                    DebugDetailRow(
                        label = "Confianza",
                        value = log.ocrConfidenceJson ?: "—",
                    )
                }
            },
        )
    }
}

@Composable
private fun DebugLogCard(log: DebugLogEntity, onClick: () -> Unit) {
    SectionCard(
        title = formatTimestamp(log.createdAt),
        icon = Icons.Outlined.BugReport,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            DebugDetailRow(
                label = "Estado",
                value = if (log.failureReason == null) "OK" else "Error",
            )
            DebugDetailRow(
                label = "Preprocesamiento",
                value = log.preprocessTimeMs?.let { "${it} ms" } ?: "—",
            )
            DebugDetailRow(
                label = "Resolución",
                value = formatResolution(log.imageWidth, log.imageHeight),
            )
            DebugDetailRow(
                label = "Parsing",
                value = log.parsingErrorSummary ?: "—",
            )
        }
    }
}

@Composable
private fun DebugDetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "AR"))
    return formatter.format(Date(timestamp))
}

private fun formatResolution(width: Int?, height: Int?): String {
    if (width == null || height == null) return "—"
    return "${width}x${height}"
}
