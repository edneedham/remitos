package com.remitos.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.remitos.app.RemitosApplication
import com.remitos.app.data.OutboundListStatus
import com.remitos.app.data.db.entity.OutboundListEntity
import com.remitos.app.print.OutboundListPrinter
import com.remitos.app.ui.components.RemitosTopBar
import com.remitos.app.ui.theme.BrandBlue
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutboundHistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as RemitosApplication
    val viewModel: OutboundHistoryViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return OutboundHistoryViewModel(app.repository) as T
            }
        },
    )

    val outboundLists by viewModel.outboundLists.collectAsStateWithLifecycle()
    val reprintState by viewModel.reprintState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            RemitosTopBar(
                title = "Historial de reparto",
                onBack = onBack,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        if (outboundLists.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Route,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Text(
                        text = "No se encontraron listas",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Las listas guardadas apareceran aqui",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(outboundLists) { list ->
                    OutboundHistoryCard(
                        list = list,
                        onReprint = { viewModel.requestReprint(list.id) },
                    )
                }
                item { Spacer(modifier = Modifier.size(16.dp)) }
            }
        }
    }

    when (val state = reprintState) {
        is OutboundReprintState.Ready -> {
            AlertDialog(
                onDismissRequest = { viewModel.clearReprintState() },
                confirmButton = {
                    TextButton(
                        onClick = {
                            OutboundListPrinter(context)
                                .print(state.payload.list, state.payload.lines)
                            viewModel.clearReprintState()
                        },
                    ) {
                        Icon(
                            Icons.Outlined.Print,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Reimprimir")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.clearReprintState() }) {
                        Text("Cerrar")
                    }
                },
                title = { Text("Lista para reimprimir") },
                text = { Text("Se preparo la lista de reparto para reimpresion.") },
            )
        }
        is OutboundReprintState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.clearReprintState() },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearReprintState() }) {
                        Text("Aceptar")
                    }
                },
                title = { Text("Error al reimprimir") },
                text = { Text(state.message) },
            )
        }
        null -> Unit
    }
}

@Composable
private fun OutboundHistoryCard(
    list: OutboundListEntity,
    onReprint: () -> Unit,
) {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val date = Instant.ofEpochMilli(list.issueDate)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Lista ${list.listNumber}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Chofer: ${list.driverApellido} ${list.driverNombre}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Estado: ${listStatusLabel(list.status)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatter.format(date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                Spacer(modifier = Modifier.size(6.dp))
                IconButton(onClick = onReprint) {
                    Icon(
                        imageVector = Icons.Outlined.Print,
                        contentDescription = "Reimprimir",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

private fun listStatusLabel(status: String): String {
    return when (status) {
        OutboundListStatus.Abierta -> "Abierta"
        OutboundListStatus.Cerrada -> "Cerrada"
        else -> status
    }
}
