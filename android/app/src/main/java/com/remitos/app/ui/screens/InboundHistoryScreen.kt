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
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sync
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.remitos.app.data.InboundNoteStatus
import com.remitos.app.data.db.entity.InboundNoteEntity
import com.remitos.app.ui.components.DateUtils
import com.remitos.app.ui.components.EmptyState
import com.remitos.app.ui.components.RemitosTextField
import com.remitos.app.ui.components.RemitosTextFieldVariant
import com.remitos.app.ui.components.RemitosTopBar
import com.remitos.app.ui.components.RemitosCard
import com.remitos.app.ui.theme.BrandBlue
import com.remitos.app.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboundHistoryScreen(
    onBack: () -> Unit,
    onOpenDetail: (Long) -> Unit,
) {
    val context = LocalContext.current
    val viewModel: InboundHistoryViewModel = hiltViewModel()

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
                            tint = MaterialTheme.colorScheme.primary,
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
                modifier = Modifier.padding(horizontal = Spacing.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(Spacing.ItemSpacing),
            ) {
                RemitosTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    label = "Buscar por CUIT, nombre o remito",
                    leadingIcon = Icons.Outlined.Search,
                    variant = RemitosTextFieldVariant.Reversed,
                )

                AnimatedVisibility(
                    visible = showFilters,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.ItemSpacing),
                    ) {
                        RemitosTextField(
                            value = fromDate,
                            onValueChange = { viewModel.updateFromDate(it) },
                            label = "Desde (YYYY-MM-DD)",
                            leadingIcon = Icons.Outlined.CalendarMonth,
                            modifier = Modifier.weight(1f),
                            variant = RemitosTextFieldVariant.Reversed,
                        )
                        RemitosTextField(
                            value = toDate,
                            onValueChange = { viewModel.updateToDate(it) },
                            label = "Hasta (YYYY-MM-DD)",
                            leadingIcon = Icons.Outlined.CalendarMonth,
                            modifier = Modifier.weight(1f),
                            variant = RemitosTextFieldVariant.Reversed,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.SectionSpacing))

            if (notes.isEmpty()) {
                // Empty state
                EmptyState(
                    icon = Icons.Outlined.Inbox,
                    title = "No se encontraron ingresos",
                    subtitle = "Los ingresos escaneados apareceran aqui",
                )
            } else {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = Spacing.ScreenPadding),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(Spacing.ItemSpacing),
                ) {
                    items(notes, key = { it.id }) { note ->
                        InboundHistoryCard(
                            note = note,
                            onOpenDetail = { onOpenDetail(note.id) },
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
                    item { Spacer(modifier = Modifier.height(Spacing.SectionSpacing)) }
                }
            }
        }
    }
}

@Composable
private fun InboundHistoryCard(
    note: InboundNoteEntity,
    onOpenDetail: () -> Unit,
) {
    val dateStr = DateUtils.formatDate(note.createdAt)

    RemitosCard(
        onClick = onOpenDetail,
        modifier = Modifier.fillMaxWidth(),
        elevation = 2,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.SectionSpacing),
            verticalAlignment = Alignment.Top,
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Inventory2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
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
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (note.status == InboundNoteStatus.Anulada) {
                        Spacer(modifier = Modifier.width(Spacing.ItemSpacing))
                        Text(
                            text = "Anulado",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Text(
                    text = note.senderCuit,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Dest: ${note.destApellido} ${note.destNombre}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Remito cliente: ${note.remitoNumCliente}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(Spacing.ItemSpacing))
                    Text(
                        text = "${note.cantBultosTotal} bultos",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                }
            }

            // Date badge, image, and upload status
            Spacer(modifier = Modifier.width(Spacing.ItemSpacing))
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Upload status indicator
                UploadStatusIndicator(note.uploadStatus)

                // Image thumbnail (if available)
                if (note.imageUrl != null || note.scanImagePath != null) {
                    InboundHistoryImage(
                        imageUrl = note.imageUrl,
                        localPath = note.scanImagePath,
                        modifier = Modifier.size(60.dp)
                    )
                }

                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun UploadStatusIndicator(uploadStatus: String) {
    val (icon, tint) = when (uploadStatus) {
        "uploaded" -> Pair(Icons.Outlined.CheckCircle, MaterialTheme.colorScheme.primary)
        "pending" -> Pair(Icons.Outlined.CloudQueue, MaterialTheme.colorScheme.tertiary)
        "uploading" -> Pair(Icons.Outlined.Sync, MaterialTheme.colorScheme.primary)
        "failed" -> Pair(Icons.Outlined.CloudOff, MaterialTheme.colorScheme.error)
        else -> Pair(Icons.Outlined.CloudQueue, MaterialTheme.colorScheme.outline)
    }

    val description = when (uploadStatus) {
        "uploaded" -> "Imagen subida"
        "pending" -> "Pendiente de subir"
        "uploading" -> "Subiendo..."
        "failed" -> "Error al subir"
        else -> "Sin imagen"
    }

    Icon(
        imageVector = icon,
        contentDescription = description,
        tint = tint,
        modifier = Modifier.size(16.dp)
    )
}

@Composable
private fun InboundHistoryImage(
    imageUrl: String?,
    localPath: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Determine the image source
    val imageModel = when {
        imageUrl != null -> {
            // Use the signed URL from GCS
            ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(true)
                .build()
        }
        localPath != null && File(localPath).exists() -> {
            // Fallback to local file
            File(localPath)
        }
        else -> null
    }

    if (imageModel != null) {
        Box(
            modifier = modifier
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = imageModel,
                contentDescription = "Imagen del remito",
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
