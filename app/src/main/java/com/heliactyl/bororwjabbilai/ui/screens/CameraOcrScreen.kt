package com.heliactyl.bororwjabbilai.ui.screens

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.heliactyl.bororwjabbilai.OcrManager
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraOcrScreen(
    onTextRecognized: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val imageCapture: ImageCapture = remember {
        ImageCapture.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .build()
    }
    val ocrManager = remember { OcrManager() }

    var isProcessing by remember { mutableStateOf(false) }

    // Track the actual pixel size of the composable so we can map frame → bitmap coords
    var viewSize by remember { mutableStateOf(IntSize.Zero) }

    // Frame occupies 80% width and 60% height of the view, centered
    val frameWidthFraction = 0.8f
    val frameHeightFraction = 0.6f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { viewSize = it.size }
    ) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                // FILL CENTER so the preview scales uniformly — keeps frame alignment honest
                previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .build()
                        .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture
                        )
                    } catch (e: Exception) {
                        Log.e("CameraOcrScreen", "Use case binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Dimming overlay with transparent hole cut out for the frame
        Canvas(modifier = Modifier.fillMaxSize()) {
            val frameW = size.width * frameWidthFraction
            val frameH = size.height * frameHeightFraction
            val left = (size.width - frameW) / 2f
            val top = (size.height - frameH) / 2f

            val framePath = Path().apply {
                addRoundRect(
                    RoundRect(
                        Rect(left, top, left + frameW, top + frameH),
                        CornerRadius(24.dp.toPx())
                    )
                )
            }
            clipPath(framePath, clipOp = ClipOp.Difference) {
                drawRect(Color.Black.copy(alpha = 0.55f))
            }
        }

        // Frame border
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(frameWidthFraction)
                    .fillMaxHeight(frameHeightFraction)
                    .border(2.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(24.dp))
            )
        }

        // Back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        }

        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Hold phone upright in portrait mode",
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Align lyrics within the frame",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                FloatingActionButton(
                    onClick = {
                        if (viewSize == IntSize.Zero) return@FloatingActionButton
                        isProcessing = true
                        imageCapture.takePicture(
                            cameraExecutor,
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    val rawBitmap = image.toBitmap()
                                    val rotationDegrees = image.imageInfo.rotationDegrees
                                    image.close()

                                    // 1. Rotate the bitmap to match screen orientation
                                    val bitmap = if (rotationDegrees != 0) {
                                        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                                        Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                                    } else {
                                        rawBitmap
                                    }

                                    // 2. The preview uses FILL_CENTER with a 4:3 aspect ratio.
                                    //    Work out how the bitmap is scaled/offset onto the view.
                                    val bmpW = bitmap.width.toFloat()
                                    val bmpH = bitmap.height.toFloat()
                                    val viewW = viewSize.width.toFloat()
                                    val viewH = viewSize.height.toFloat()

                                    // Scale that makes the bitmap fill the view (FILL_CENTER = cover)
                                    val scale = maxOf(viewW / bmpW, viewH / bmpH)

                                    // How much of the bitmap is visible (in bitmap pixels)
                                    val visibleBmpW = viewW / scale
                                    val visibleBmpH = viewH / scale

                                    // Top-left corner of the visible region in bitmap coords
                                    val bmpOffsetX = (bmpW - visibleBmpW) / 2f
                                    val bmpOffsetY = (bmpH - visibleBmpH) / 2f

                                    // 3. Frame in view coords → frame in bitmap coords
                                    val frameViewW = viewW * frameWidthFraction
                                    val frameViewH = viewH * frameHeightFraction
                                    val frameViewLeft = (viewW - frameViewW) / 2f
                                    val frameViewTop = (viewH - frameViewH) / 2f

                                    val cropLeft   = (bmpOffsetX + frameViewLeft  / scale).toInt().coerceAtLeast(0)
                                    val cropTop    = (bmpOffsetY + frameViewTop   / scale).toInt().coerceAtLeast(0)
                                    val cropWidth  = (frameViewW / scale).toInt().coerceAtMost(bitmap.width  - cropLeft)
                                    val cropHeight = (frameViewH / scale).toInt().coerceAtMost(bitmap.height - cropTop)

                                    val croppedBitmap = Bitmap.createBitmap(
                                        bitmap, cropLeft, cropTop, cropWidth, cropHeight
                                    )

                                    coroutineScope.launch {
                                        val recognizedText = ocrManager.recognizeText(croppedBitmap)
                                        isProcessing = false
                                        if (!recognizedText.isNullOrBlank()) {
                                            onTextRecognized(recognizedText)
                                        }
                                    }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    Log.e("CameraOcrScreen", "Capture failed", exception)
                                    isProcessing = false
                                }
                            }
                        )
                    },
                    modifier = Modifier.size(80.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Camera, contentDescription = "Capture", modifier = Modifier.size(40.dp))
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }
}
