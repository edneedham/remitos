package com.remitos.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.remitos.app.RemitosApplication
import com.remitos.app.data.InboundNoteStatus
import com.remitos.app.data.db.entity.InboundNoteEntity
import com.remitos.app.ui.components.DateUtils
import com.remitos.app.ui.components.EmptyState
import com.remitos.app.ui.components.RemitosTextField
import com.remitos.app.ui.components.RemitosTopBar
import com.remitos.app.ui.theme.BrandBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboundHistoryScreen(
    onBack: () -> Unit,
    onOpenDetail: (Long) -> Unit,
    onScanBarcodes: (Long, Int) -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as RemitosApplication
    val viewModel: InboundHistoryViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return InboundHistoryViewModel(app.repository) as T
            }
        },
    )

    val notes by viewModel.filteredNotes.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQueryState.collectAsStateWithLifecycle()
    val fromDate by viewModel.fromDateState.collectAsStateWithLifecycle()
    val toDate by viewModel.toDateState.collectAsStateWithLifecycle()
    val canLoadMore by viewModel.canLoadMoreState.collectAsStateWithLifecycle()
    var showFilters by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()
    LaunchedEffect(notes.size) {
        if (listState.firstVisibleItemIndex > 0 && notes.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            RemitosTopBar(
                title = "Historial de ingresos",
                onBack = onBack,
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(
                            Icons.Outlined.FilterList,
                            contentDescription = "Filtros",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Search and filters
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RemitosTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    label = "Buscar por CUIT, nombre o remito",
                    leadingIcon = Icons.Outlined.Search,
                )

                AnimatedVisibility(
                    visible = showFilters,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RemitosTextField(
                            value = fromDate,
                            onValueChange = { viewModel.updateFromDate(it) },
                            label = "Desde (YYYY-MM-DD)",
                            leadingIcon = Icons.Outlined.CalendarMonth,
                            modifier = Modifier.weight(1f),
                        )
                        RemitosTextField(
                            value = toDate,
                            onValueChange = { viewModel.updateToDate(it) },
                            label = "Hasta (YYYY-MM-DD)",
                            leadingIcon = Icons.Outlined.CalendarMonth,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (notes.isEmpty()) {
                // Empty state
                EmptyState(
                    icon = Icons.Outlined.Inbox,
                    title = "No se encontraron ingresos",
                    subtitle = "Los ingresos escaneados apareceran aqui",
                )
            } else {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(notes, key = { it.id }) { note ->
                        InboundHistoryCard(
                            note = note,
                            onOpenDetail = { onOpenDetail(note.id) },
                            onScanBarcodes = { onScanBarcodes(note.id, note.cantBultosTotal) },
                        )
                    }
                    if (canLoadMore) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                TextButton(onClick = { viewModel.loadMore() }) {
                                    Text("Cargar más")
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun InboundHistoryCard(
    note: InboundNoteEntity,
    onOpenDetail: () -> Unit,
    onScanBarcodes: () -> Unit,
) {
    val dateStr = DateUtils.formatDate(note.createdAt)

    Card(
        onClick = onOpenDetail,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = BrandBlue,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Inventory2,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${note.senderApellido} ${note.senderNombre}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        color = Color.White,
                    )
                    if (note.status == InboundNoteStatus.Anulada) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Anulado",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFFCDD2),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Text(
                    text = note.senderCuit,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                )
                Text(
                    text = "Dest: ${note.destApellido} ${note.destNombre}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Remito cliente: ${note.remitoNumCliente}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "${note.cantBultosTotal} bultos",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                    )
                }
            }

            // Date badge
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = dateStr,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
            )
            
            // Scan barcode button
            if (note.status != InboundNoteStatus.Anulada) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onScanBarcodes,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Escanear códigos",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
