package com.remitos.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.remitos.app.RemitosApplication
import com.remitos.app.ui.components.RemitosTopBar
import com.remitos.app.ui.components.RemitosCard
import com.remitos.app.ui.theme.BrandBlue
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as RemitosApplication
    val viewModel: ActivityViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ActivityViewModel(app.settingsStore, app.repository) as T
            }
        },
    )

    val stats by viewModel.usageStats.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()

    val averageTime = if (stats.totalScans > 0L) {
        stats.totalScanTimeMs / stats.totalScans
    } else {
        null
    }

    LaunchedEffect(exportState) {
        if (exportState is ExportState.Success) {
            val uri = (exportState as ExportState.Success).uri
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Exportar Actividad"))
            viewModel.clearExportState()
        }
    }

    Scaffold(
        topBar = {
            RemitosTopBar(
                title = "Actividad",
                onBack = onBack,
                showLogo = false,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RemitosCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 2,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.QueryStats,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "Uso",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    MetricRow(label = "Escaneos totales", value = stats.totalScans.toString(), lightText = false)
                    MetricRow(label = "Lecturas exitosas", value = stats.successfulParses.toString(), lightText = false)
                    MetricRow(label = "Correcciones manuales", value = stats.manualCorrections.toString(), lightText = false)
                    MetricRow(
                        label = "Tiempo promedio por escaneo",
                        value = averageTime?.let { formatSeconds(it) } ?: "—",
                        lightText = false,
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (exportState is ExportState.Error) {
                Text(
                    text = (exportState as ExportState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = { viewModel.exportData(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = exportState !is ExportState.Exporting,
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
            ) {
                if (exportState is ExportState.Exporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Outlined.FileDownload, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Exportar a CSV")
                }
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String, lightText: Boolean = false) {
    val textColor = if (lightText) Color.White else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = textColor,
        )
    }
}

private fun formatSeconds(durationMs: Long): String {
    val seconds = durationMs / 1000.0
    val locale = Locale("es", "AR")
    return String.format(locale, "%.1f s", seconds)
}
