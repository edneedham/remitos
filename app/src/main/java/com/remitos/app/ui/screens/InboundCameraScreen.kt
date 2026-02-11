package com.remitos.app.ui.screens

import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

@Composable
fun InboundCameraScreen(
    onBack: () -> Unit,
    onPhotoCaptured: (Uri) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isDocumentCovered by remember { mutableStateOf(false) }
    var lastCoverageRatio by remember { mutableStateOf(0f) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    val focusGate = remember { FocusGate() }
    var focusReady by remember { mutableStateOf(false) }
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    val allowCapture = isDocumentCovered && focusReady

    LaunchedEffect(cameraProviderFuture) {
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analyzer ->
                analyzer.setAnalyzer(analysisExecutor) { imageProxy ->
                    analyzeCoverage(imageProxy, recognizer) { ratio ->
                        mainExecutor.execute {
                            val covered = ratio >= 0.7f
                            if (covered != isDocumentCovered || ratio != lastCoverageRatio) {
                                isDocumentCovered = covered
                                lastCoverageRatio = ratio
                            }
                        }
                    }
                }
            }

        cameraProvider.unbindAll()
        val camera = cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            capture,
            analysis,
        )
        imageCapture = capture

        previewView.post {
            val width = previewView.width.toFloat().coerceAtLeast(1f)
            val height = previewView.height.toFloat().coerceAtLeast(1f)
            val meteringPoint = SurfaceOrientedMeteringPointFactory(width, height)
                .createPoint(width / 2f, height / 2f)
            focusGate.reset(System.currentTimeMillis())
            val future = camera.cameraControl.startFocusAndMetering(
                androidx.camera.core.FocusMeteringAction.Builder(meteringPoint).build()
            )
            future.addListener(
                {
                    val result = runCatching { future.get() }.getOrNull()
                    if (result != null) {
                        focusGate.onFocusResult(result.isFocusSuccessful)
                    }
                },
                mainExecutor,
            )
        }
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            runCatching { cameraProviderFuture.get().unbindAll() }
            analysisExecutor.shutdown()
            recognizer.close()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            focusReady = focusGate.isReady(nowMs)
            delay(200)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
        )

        // Dim overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.15f)),
        )

        // Document framing guide
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight(0.85f)
                .aspectRatio(3f / 4f)
                .padding(top = 8.dp, bottom = 72.dp)
                .border(
                    width = 2.dp,
                    color = if (allowCapture) {
                        Color.White.copy(alpha = 0.7f)
                    } else {
                        Color(0xFFFFC857).copy(alpha = 0.9f)
                    },
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = when {
                    !isDocumentCovered -> "Cubre al menos el 70% del marco"
                    !focusReady -> "Enfocando..."
                    else -> "Remito detectado"
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
            )
        }

        // Top bar with back button
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .systemBarsPadding()
                .padding(12.dp),
        ) {
            IconButton(
                onClick = onBack,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.4f),
                    contentColor = Color.White,
                ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                )
            }
        }

        // Bottom capture area
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .systemBarsPadding()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                when {
                    !isDocumentCovered -> "Asegura que el remito cubra al menos el 70%"
                    !focusReady -> "Enfocando..."
                    else -> "Listo para capturar"
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
            )

            // Shutter button
            IconButton(
                enabled = allowCapture,
                onClick = {
                    val capture = imageCapture ?: return@IconButton
                    val file = createImageFile(context.cacheDir)
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
                    capture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                onPhotoCaptured(Uri.fromFile(file))
                            }

                            override fun onError(exception: ImageCaptureException) {
                                Log.e("InboundCameraScreen", "Error al capturar imagen", exception)
                            }
                        },
                    )
                },
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        if (allowCapture) {
                            Color.White.copy(alpha = 0.2f)
                        } else {
                            Color.White.copy(alpha = 0.08f)
                        }
                    )
                    .border(
                        3.dp,
                        if (allowCapture) Color.White else Color.White.copy(alpha = 0.5f),
                        CircleShape
                    ),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = Color.White,
                ),
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (allowCapture) Color.White else Color.White.copy(alpha = 0.6f)),
                )
            }
        }
    }
}

private fun analyzeCoverage(
    imageProxy: ImageProxy,
    recognizer: com.google.mlkit.vision.text.TextRecognizer,
    onResult: (Float) -> Unit,
) {
    val image = imageProxy.image
    if (image == null) {
        imageProxy.close()
        return
    }

    val inputImage = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)
    recognizer.process(inputImage)
        .addOnSuccessListener { result ->
            val boxes = result.textBlocks.mapNotNull { it.boundingBox }
            if (boxes.isEmpty()) {
                onResult(0f)
            } else {
                val minLeft = boxes.minOf { it.left }
                val minTop = boxes.minOf { it.top }
                val maxRight = boxes.maxOf { it.right }
                val maxBottom = boxes.maxOf { it.bottom }
                val width = (maxRight - minLeft).coerceAtLeast(0)
                val height = (maxBottom - minTop).coerceAtLeast(0)
                val boxArea = width.toFloat() * height.toFloat()
                val frameArea = (imageProxy.width * imageProxy.height).toFloat().coerceAtLeast(1f)
                onResult(boxArea / frameArea)
            }
        }
        .addOnFailureListener {
            onResult(0f)
        }
        .addOnCompleteListener {
            imageProxy.close()
        }
}
