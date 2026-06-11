package com.heliactyl.bororwjabbilai.ui.screens

import android.graphics.Bitmap
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
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
    val imageCapture: ImageCapture = remember { ImageCapture.Builder().build() }
    val ocrManager = remember { OcrManager() }
    
    var isProcessing by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
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

        // Scanning Frame Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            
            // Define the frame size (80% width, 60% height)
            val frameWidth = canvasWidth * 0.8f
            val frameHeight = canvasHeight * 0.6f
            val left = (canvasWidth - frameWidth) / 2
            val top = (canvasHeight - frameHeight) / 2
            
            val frameRect = Rect(left, top, left + frameWidth, top + frameHeight)
            val framePath = Path().apply {
                addRoundRect(RoundRect(frameRect, CornerRadius(24.dp.toPx(), 24.dp.toPx())))
            }

            // Draw semi-transparent background with a hole for the frame
            clipPath(framePath, clipOp = ClipOp.Difference) {
                drawRect(Color.Black.copy(alpha = 0.5f))
            }
        }
        
        // Frame Border
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .fillMaxHeight(0.6f)
                    .border(2.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
            )
        }

        // UI Controls
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
                        isProcessing = true
                        imageCapture.takePicture(
                            cameraExecutor,
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    val fullBitmap = image.toBitmap()
                                    image.close()

                                    // Crop bitmap to match the visual frame (80% width, 60% height)
                                    val width = fullBitmap.width
                                    val height = fullBitmap.height
                                    
                                    val cropWidth = (width * 0.8f).toInt()
                                    val cropHeight = (height * 0.6f).toInt()
                                    val left = (width - cropWidth) / 2
                                    val top = (height - cropHeight) / 2
                                    
                                    val croppedBitmap = Bitmap.createBitmap(
                                        fullBitmap,
                                        left,
                                        top,
                                        cropWidth,
                                        cropHeight
                                    )

                                    coroutineScope.launch {
                                        val recognizedText = ocrManager.recognizeText(croppedBitmap)
                                        isProcessing = false
                                        if (recognizedText != null) {
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
        onDispose {
            cameraExecutor.shutdown()
        }
    }
}
