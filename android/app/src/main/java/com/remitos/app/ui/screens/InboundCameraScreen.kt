package com.remitos.app.ui.screens

import android.net.Uri
import android.util.Log
import java.io.File
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
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
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

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
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    val focusGate = remember { FocusGate() }
    var focusReady by remember { mutableStateOf(false) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var cameraControl by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }

    LaunchedEffect(cameraProviderFuture) {
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder()
            .setTargetAspectRatio(androidx.camera.core.AspectRatio.RATIO_4_3)
            .build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetAspectRatio(androidx.camera.core.AspectRatio.RATIO_4_3)
            .build()

        cameraProvider.unbindAll()
        val camera = cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            capture,
        )
        cameraControl = camera.cameraControl
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
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val control = cameraControl ?: return@detectTapGestures
                        val width = previewView.width.toFloat().coerceAtLeast(1f)
                        val height = previewView.height.toFloat().coerceAtLeast(1f)
                        val meteringPoint = SurfaceOrientedMeteringPointFactory(width, height)
                            .createPoint(offset.x, offset.y)
                        focusGate.reset(System.currentTimeMillis())
                        val future = control.startFocusAndMetering(
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
                },
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
                .fillMaxHeight(0.65f)
                .aspectRatio(3f / 4f)
                .padding(top = 8.dp, bottom = 72.dp)
                .border(
                    width = 2.dp,
                    color = Color.White.copy(alpha = 0.7f),
                    shape = MaterialTheme.shapes.medium,
                )
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = when {
                    !focusReady -> "Enfocando..."
                    else -> "Listo para capturar"
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
                    !focusReady -> "Enfocando..."
                    else -> "Toca para capturar"
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
            )

            // Shutter button
            IconButton(
                enabled = true,
                onClick = {
                    val capture = imageCapture ?: return@IconButton
                    val file = createImageFile(File(context.filesDir, "remitos"))
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
                    .background(Color.White.copy(alpha = 0.2f))
                    .border(3.dp, Color.White, CircleShape),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = Color.White,
                ),
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                )
            }
        }
    }
}