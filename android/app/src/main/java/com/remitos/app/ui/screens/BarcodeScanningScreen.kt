package com.remitos.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.remitos.app.barcode.Gs1Parser
import com.remitos.app.ui.theme.BrandBlue
import java.util.concurrent.Executors

/**
 * Data class representing a scanned barcode item.
 */
data class ScannedBarcodeItem(
    val id: String,
    val rawValue: String,
    val gtin: String? = null,
    val batchLot: String? = null,
    val expiryDate: String? = null,
    val sscc: String? = null,
    val scannedAt: Long = System.currentTimeMillis(),
    val isManual: Boolean = false
)

/**
 * Screen for continuous barcode scanning with tally tracking.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScanningScreen(
    inboundNoteId: Long,
    expectedCount: Int,
    onBack: () -> Unit,
    onComplete: () -> Unit,
    viewModel: BarcodeScanningViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var showManualEntry by remember { mutableStateOf(false) }
    var showDuplicateWarning by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
        viewModel.loadNote(inboundNoteId, expectedCount)
    }

    val scanState = viewModel.scanState.value
    val scannedItems = viewModel.scannedItems
    val progress = viewModel.progress.value

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Escanear códigos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showManualEntry = true },
                containerColor = BrandBlue
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ingreso manual")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Progress section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (progress >= 1f) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Escaneados",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${scannedItems.size} / $expectedCount",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (progress >= 1f) Color(0xFF2E7D32) else BrandBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (progress >= 1f) Color(0xFF4CAF50) else BrandBlue,
                    )

                    if (progress >= 1f) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50)
                            )
                            Text(
                                text = "¡Completado!",
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Camera preview or scanned items list
            if (hasCameraPermission && scannedItems.size < expectedCount) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    CameraPreview(
                        onBarcodeScanned = { barcode ->
                            val isDuplicate = scannedItems.any { it.rawValue == barcode }
                            if (isDuplicate) {
                                showDuplicateWarning = barcode
                            } else {
                                viewModel.processBarcode(barcode)
                            }
                        }
                    )

                    // Scanning overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(280.dp, 160.dp)
                                .background(Color.Transparent)
                        ) {
                            // Scanner frame corners
                            ScannerOverlay()
                        }
                    }

                    // Scanning hint
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp)
                    ) {
                        Text(
                            text = "Alineá el código de barras",
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(12.dp),
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                // Show scanned items list when camera not needed
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    items(
                        items = scannedItems,
                        key = { it.id }
                    ) { item ->
                        ScannedItemCard(item = item)
                    }
                }
            }

            // Bottom action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar")
                }

                if (scannedItems.isNotEmpty()) {
                    TextButton(
                        onClick = { viewModel.exportToCsv(context) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Exportar CSV")
                    }
                }

                TextButton(
                    onClick = {
                        viewModel.saveScans(context)
                        onComplete()
                    },
                    enabled = scannedItems.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Finalizar")
                }
            }
        }
    }

    // Manual entry dialog
    if (showManualEntry) {
        ManualBarcodeDialog(
            onDismiss = { showManualEntry = false },
            onConfirm = { barcode ->
                viewModel.processBarcode(barcode, isManual = true)
                showManualEntry = false
            }
        )
    }

    // Duplicate warning dialog
    showDuplicateWarning?.let { barcode ->
        AlertDialog(
            onDismissRequest = { showDuplicateWarning = null },
            confirmButton = {
                TextButton(onClick = { showDuplicateWarning = null }) {
                    Text("Aceptar")
                }
            },
            title = { Text("Código duplicado") },
            text = { Text("El código $barcode ya fue escaneado.") }
        )
    }
}

/**
 * Camera preview with barcode scanning.
 */
@Composable
private fun CameraPreview(
    onBarcodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }

    var lastScanTime by remember { mutableStateOf(0L) }
    val scanCooldownMs = 1500L  // Prevent duplicate scans

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            previewView.scaleType = PreviewView.ScaleType.FILL_CENTER

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()

                // Set surface provider after building
                preview.setSurfaceProvider(previewView.surfaceProvider)

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                    processImage(
                        imageProxy = imageProxy,
                        onBarcodeDetected = { barcode ->
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastScanTime > scanCooldownMs) {
                                lastScanTime = currentTime
                                onBarcodeScanned(barcode)
                            }
                        }
                    )
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    // Handle camera binding error
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * Process camera image for barcode detection.
 */
private fun processImage(
    imageProxy: ImageProxy,
    onBarcodeDetected: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        val scanner = BarcodeScanning.getClient()

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    barcode.rawValue?.let { value ->
                        onBarcodeDetected(value)
                    }
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

/**
 * Scanner overlay with corner brackets.
 */
@Composable
private fun ScannerOverlay() {
    val strokeWidth = 4.dp
    val cornerSize = 32.dp
    val color = Color.White

    Box(modifier = Modifier.fillMaxSize()) {
        // Top-left corner
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(cornerSize)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(strokeWidth)
                    .background(color)
                    .align(Alignment.TopStart)
            )
            Box(
                modifier = Modifier
                    .width(strokeWidth)
                    .fillMaxHeight()
                    .background(color)
                    .align(Alignment.TopStart)
            )
        }

        // Top-right corner
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(cornerSize)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(strokeWidth)
                    .background(color)
                    .align(Alignment.TopStart)
            )
            Box(
                modifier = Modifier
                    .width(strokeWidth)
                    .fillMaxHeight()
                    .background(color)
                    .align(Alignment.TopEnd)
            )
        }

        // Bottom-left corner
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(cornerSize)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(strokeWidth)
                    .background(color)
                    .align(Alignment.BottomStart)
            )
            Box(
                modifier = Modifier
                    .width(strokeWidth)
                    .fillMaxHeight()
                    .background(color)
                    .align(Alignment.TopStart)
            )
        }

        // Bottom-right corner
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(cornerSize)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(strokeWidth)
                    .background(color)
                    .align(Alignment.BottomStart)
            )
            Box(
                modifier = Modifier
                    .width(strokeWidth)
                    .fillMaxHeight()
                    .background(color)
                    .align(Alignment.TopEnd)
            )
        }
    }
}

/**
 * Card displaying a scanned item.
 */
@Composable
private fun ScannedItemCard(item: ScannedBarcodeItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isManual) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.rawValue.take(30) + if (item.rawValue.length > 30) "..." else "",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (item.isManual) {
                    Text(
                        text = "Manual",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFE65100)
                    )
                }
            }

            if (item.gtin != null || item.batchLot != null || item.expiryDate != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item.gtin?.let {
                        Text(
                            text = "GTIN: $it",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    item.batchLot?.let {
                        Text(
                            text = "Lote: $it",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    item.expiryDate?.let {
                        Text(
                            text = "Venc: $it",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
