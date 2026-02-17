package com.remitos.app.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.RotateRight
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.remitos.app.ui.components.RemitosTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

private const val PREVIEW_ASPECT = 3f / 4f
private const val PREVIEW_MAX_EDGE_PX = 2000

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboundPreviewScreen(
    photoUri: Uri?,
    onBack: () -> Unit,
    onRetake: () -> Unit,
    onConfirm: (Uri) -> Unit,
    onPhotoUriHandled: () -> Unit = {},
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(photoUri) {
        if (photoUri == null) return@LaunchedEffect
        isLoading = true
        errorMessage = null
        bitmap = withContext(Dispatchers.IO) { loadPreviewBitmap(context, photoUri) }
        if (bitmap == null) {
            errorMessage = "No se pudo cargar la imagen."
        }
        isLoading = false
        onPhotoUriHandled()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            RemitosTopBar(
                title = "Previsualizacion",
                onBack = onBack,
                scrollBehavior = scrollBehavior,
                showLogo = false,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.secondaryContainer,
                )
            }

            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "Previsualizacion de remito",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Fit,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilledTonalButton(
                        onClick = {
                            bitmap = bitmap?.let { rotateBitmap(it, 90f) }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f),
                    ) {
                        androidx.compose.material3.Icon(
                            Icons.AutoMirrored.Outlined.RotateRight,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Rotar")
                    }
                    FilledTonalButton(
                        onClick = {
                            bitmap = bitmap?.let { centerCropToAspect(it, PREVIEW_ASPECT) }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f),
                    ) {
                        androidx.compose.material3.Icon(
                            Icons.Outlined.Crop,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Recortar")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilledTonalButton(
                        onClick = onRetake,
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f),
                    ) {
                        androidx.compose.material3.Icon(
                            Icons.Outlined.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Repetir")
                    }
                    Button(
                        onClick = {
                            val current = bitmap
                            if (current == null) return@Button
                            scope.launch {
                                val uri = withContext(Dispatchers.IO) {
                                    val file = saveBitmapToFile(current, File(context.filesDir, "remitos"))
                                    Uri.fromFile(file)
                                }
                                onConfirm(uri)
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f),
                    ) {
                        androidx.compose.material3.Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Usar foto")
                    }
                }
            }

            if (bitmap == null && !isLoading) {
                Text(
                    text = "No se encontro la imagen para previsualizar.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                FilledTonalButton(onClick = onRetake) { Text("Volver a la camara") }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) { Text("Aceptar") }
            },
            title = { Text("Error al cargar") },
            text = { Text(errorMessage ?: "") },
        )
    }
}

private fun loadPreviewBitmap(context: Context, uri: Uri): Bitmap? {
    return runCatching {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.isMutableRequired = false
            val size = info.size
            val maxEdge = max(size.width, size.height).coerceAtLeast(1)
            if (maxEdge > PREVIEW_MAX_EDGE_PX) {
                val scale = PREVIEW_MAX_EDGE_PX.toFloat() / maxEdge.toFloat()
                val targetWidth = (size.width * scale).roundToInt().coerceAtLeast(1)
                val targetHeight = (size.height * scale).roundToInt().coerceAtLeast(1)
                decoder.setTargetSize(targetWidth, targetHeight)
            }
        }
    }.getOrNull()
}

private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
    val matrix = android.graphics.Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private fun centerCropToAspect(bitmap: Bitmap, aspect: Float): Bitmap {
    if (bitmap.width == 0 || bitmap.height == 0) return bitmap
    val current = bitmap.width.toFloat() / bitmap.height.toFloat()
    val targetWidth: Int
    val targetHeight: Int
    if (current > aspect) {
        targetHeight = bitmap.height
        targetWidth = (bitmap.height * aspect).roundToInt().coerceAtLeast(1)
    } else {
        targetWidth = bitmap.width
        targetHeight = (bitmap.width / aspect).roundToInt().coerceAtLeast(1)
    }
    val left = ((bitmap.width - targetWidth) / 2f).roundToInt().coerceAtLeast(0)
    val top = ((bitmap.height - targetHeight) / 2f).roundToInt().coerceAtLeast(0)
    return Bitmap.createBitmap(bitmap, left, top, targetWidth, targetHeight)
}
