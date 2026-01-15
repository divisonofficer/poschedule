package com.jnkim.poschedule.ui.screens

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.jnkim.poschedule.R
import com.jnkim.poschedule.data.local.entity.PlanItemWindow
import com.jnkim.poschedule.domain.ai.MicroChore
import com.jnkim.poschedule.ui.components.GlassBackground
import com.jnkim.poschedule.ui.components.GlassButton
import com.jnkim.poschedule.ui.components.GlassButtonStyle
import com.jnkim.poschedule.ui.components.GlassCard
import com.jnkim.poschedule.ui.components.GlassChip
import com.jnkim.poschedule.ui.theme.ModeNormal
import com.jnkim.poschedule.ui.viewmodel.TidySnapUiState
import com.jnkim.poschedule.ui.viewmodel.TidySnapViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor

// Lens types for camera selection
enum class LensType {
    ULTRA_WIDE,  // 광각 - 책상 전체 캡처
    WIDE,        // 표준 - 일반 촬영
    TELEPHOTO    // 망원 - 멀리서 확대
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TidySnapScreen(
    viewModel: TidySnapViewModel,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var currentLensType by remember { mutableStateOf(LensType.WIDE) }
    var availableLenses by remember { mutableStateOf(setOf(LensType.WIDE)) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraExecutor = remember { ContextCompat.getMainExecutor(context) }

    GlassBackground(accentColor = ModeNormal) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Real Camera Preview (only show during Capturing state)
            if (uiState is TidySnapUiState.Capturing) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()

                            // Debug: Log all available cameras
                            Log.d("TidySnap", "=== Available Cameras ===")
                            cameraProvider.availableCameraInfos.forEachIndexed { index, cameraInfo ->
                                val zoomState = cameraInfo.zoomState.value
                                val facing = if (cameraInfo.lensFacing == CameraSelector.LENS_FACING_BACK) "BACK" else "FRONT"
                                Log.d("TidySnap", "Camera $index: facing=$facing, minZoom=${zoomState?.minZoomRatio}, maxZoom=${zoomState?.maxZoomRatio}")
                            }

                            // Detect available lenses
                            val availableSet = mutableSetOf<LensType>()
                            availableSet.add(LensType.WIDE) // Always available

                            // Check for ultra-wide
                            val ultraWideCameras = cameraProvider.availableCameraInfos.filter { cameraInfo ->
                                cameraInfo.lensFacing == CameraSelector.LENS_FACING_BACK &&
                                hasUltraWideCapability(cameraInfo)
                            }
                            Log.d("TidySnap", "Ultra-wide cameras found: ${ultraWideCameras.size}")
                            if (ultraWideCameras.isNotEmpty()) {
                                availableSet.add(LensType.ULTRA_WIDE)
                            }

                            // Check for telephoto
                            val telephotoCameras = cameraProvider.availableCameraInfos.filter { cameraInfo ->
                                cameraInfo.lensFacing == CameraSelector.LENS_FACING_BACK &&
                                hasTelephotoCapability(cameraInfo)
                            }
                            Log.d("TidySnap", "Telephoto cameras found: ${telephotoCameras.size}")
                            if (telephotoCameras.isNotEmpty()) {
                                availableSet.add(LensType.TELEPHOTO)
                            }

                            availableLenses = availableSet
                            Log.d("TidySnap", "Available lens types: $availableSet")

                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            try {
                                cameraProvider.unbindAll()
                                val cameraSelector = getCameraSelectorForLens(currentLensType)
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageCapture
                                )
                            } catch (e: Exception) {
                                Log.e("TidySnap", "Use case binding failed", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    update = { previewView ->
                        // Rebind camera when lens type changes
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            try {
                                cameraProvider.unbindAll()
                                val cameraSelector = getCameraSelectorForLens(currentLensType)
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageCapture
                                )
                            } catch (e: Exception) {
                                Log.e("TidySnap", "Use case binding failed", e)
                            }
                        }, ContextCompat.getMainExecutor(context))
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 2. Overlay UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                TopAppBar(
                    title = { Text(stringResource(R.string.title_tidy_snap)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    },
                    actions = {
                        // Lens switcher (only show during Capturing)
                        if (uiState is TidySnapUiState.Capturing && availableLenses.size > 1) {
                            IconButton(
                                onClick = {
                                    // Cycle through available lenses
                                    currentLensType = when (currentLensType) {
                                        LensType.WIDE -> {
                                            if (LensType.ULTRA_WIDE in availableLenses) LensType.ULTRA_WIDE
                                            else if (LensType.TELEPHOTO in availableLenses) LensType.TELEPHOTO
                                            else LensType.WIDE
                                        }
                                        LensType.ULTRA_WIDE -> {
                                            if (LensType.TELEPHOTO in availableLenses) LensType.TELEPHOTO
                                            else LensType.WIDE
                                        }
                                        LensType.TELEPHOTO -> LensType.WIDE
                                    }
                                }
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = "Switch lens",
                                        tint = Color.White
                                    )
                                    Text(
                                        text = when (currentLensType) {
                                            LensType.ULTRA_WIDE -> "광각"
                                            LensType.WIDE -> "표준"
                                            LensType.TELEPHOTO -> "망원"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Capture Mode UI
                if (uiState is TidySnapUiState.Capturing) {
                    val capturingState = uiState as TidySnapUiState.Capturing
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp, start = 16.dp, end = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Thumbnails of captured images
                        if (capturingState.capturedImages.isNotEmpty()) {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(capturingState.capturedImages) { imageFile ->
                                    ImageThumbnail(
                                        imageFile = imageFile,
                                        onRemove = { viewModel.removeCapturedImage(imageFile) }
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Analyze button (only show when images are captured)
                            if (capturingState.capturedImages.isNotEmpty()) {
                                GlassButton(
                                    text = "Analyze (${capturingState.capturedImages.size})",
                                    onClick = { viewModel.analyzeImages("ko") },
                                    style = GlassButtonStyle.PRIMARY,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Capture button
                            Surface(
                                onClick = {
                                    takePhoto(context, imageCapture, cameraExecutor) { file ->
                                        viewModel.addCapturedImage(file)
                                    }
                                },
                                modifier = Modifier.size(if (capturingState.capturedImages.isNotEmpty()) 64.dp else 80.dp),
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = Color.White.copy(alpha = 0.9f),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 3.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                )
                            ) { }
                        }
                    }
                }
            }

            // 3. Processing Overlay
            AnimatedVisibility(
                visible = uiState is TidySnapUiState.Processing,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    GlassCard(modifier = Modifier.padding(32.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(stringResource(R.string.processing_vision), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            // 4. Error Overlay
            if (uiState is TidySnapUiState.Error) {
                val errorState = uiState as TidySnapUiState.Error
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    GlassCard(modifier = Modifier.padding(32.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Error",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorState.message,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            GlassButton(
                                text = "OK",
                                onClick = { viewModel.reset() },
                                style = GlassButtonStyle.PRIMARY
                            )
                        }
                    }
                }
            }

            // 5. Result Sheet
            if (uiState is TidySnapUiState.Success) {
                val state = uiState as TidySnapUiState.Success
                TidySnapResultOverlay(
                    tasks = state.tasks,
                    onDismiss = { viewModel.reset() },
                    onConfirm = { window ->
                        viewModel.injectTasks(state.tasks, window)
                        onSuccess()
                    }
                )
            }
        }
    }
}

@Composable
fun ImageThumbnail(
    imageFile: File,
    onRemove: () -> Unit
) {
    Box {
        val bitmap = remember(imageFile) {
            BitmapFactory.decodeFile(imageFile.absolutePath)
        }

        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Captured image",
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        // Remove button
        Surface(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(24.dp),
            shape = androidx.compose.foundation.shape.CircleShape,
            color = Color.Black.copy(alpha = 0.6f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Get CameraSelector for the specified lens type.
 */
private fun getCameraSelectorForLens(lensType: LensType): CameraSelector {
    return when (lensType) {
        LensType.ULTRA_WIDE -> {
            CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .addCameraFilter { cameraInfos ->
                    cameraInfos.filter { hasUltraWideCapability(it) }
                }
                .build()
        }
        LensType.TELEPHOTO -> {
            CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .addCameraFilter { cameraInfos ->
                    cameraInfos.filter { hasTelephotoCapability(it) }
                }
                .build()
        }
        LensType.WIDE -> CameraSelector.DEFAULT_BACK_CAMERA
    }
}

/**
 * Check if camera has ultra-wide capability.
 * For Xiaomi devices, check physical camera IDs or logical multi-camera setup.
 */
private fun hasUltraWideCapability(cameraInfo: CameraInfo): Boolean {
    return try {
        // Method 1: Check zoom ratio (works on some devices)
        val zoomState = cameraInfo.zoomState.value
        val hasLowZoomRatio = zoomState?.minZoomRatio ?: 1f < 0.7f

        // Method 2: Check if this is a logical camera with physical sub-cameras
        // Ultra-wide cameras usually have camera ID 2 on Xiaomi devices
        val cameraId = (cameraInfo as? androidx.camera.camera2.interop.Camera2CameraInfo)
            ?.getCameraCharacteristic(android.hardware.camera2.CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
            ?.firstOrNull()

        Log.d("TidySnap", "Camera info - minZoomRatio: ${zoomState?.minZoomRatio}, focalLength: $cameraId")

        hasLowZoomRatio
    } catch (e: Exception) {
        Log.e("TidySnap", "Error checking ultra-wide: ${e.message}")
        false
    }
}

/**
 * Check if camera has telephoto capability.
 * For Xiaomi devices, telephoto is usually camera ID 3 or 4.
 */
private fun hasTelephotoCapability(cameraInfo: CameraInfo): Boolean {
    return try {
        // Method 1: Check zoom ratio
        val zoomState = cameraInfo.zoomState.value
        val hasHighZoomRatio = zoomState?.minZoomRatio ?: 1f > 1.3f

        Log.d("TidySnap", "Camera info - minZoomRatio: ${zoomState?.minZoomRatio}")

        hasHighZoomRatio
    } catch (e: Exception) {
        Log.e("TidySnap", "Error checking telephoto: ${e.message}")
        false
    }
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    executor: Executor,
    onImageCaptured: (File) -> Unit
) {
    val photoFile = File(
        context.cacheDir,
        SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis()) + ".jpg"
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                onImageCaptured(photoFile)
            }

            override fun onError(exc: ImageCaptureException) {
                Log.e("TidySnap", "Photo capture failed: ${exc.message}", exc)
            }
        }
    )
}

@Composable
fun TidySnapResultOverlay(
    tasks: List<MicroChore>,
    onDismiss: () -> Unit,
    onConfirm: (PlanItemWindow) -> Unit
) {
    var selectedWindow by remember { mutableStateOf(PlanItemWindow.ANYTIME) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.title_decomposed_tasks), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_back))
                    }
                }

                Text(stringResource(R.string.desc_decomposed_tasks), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))

                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tasks) { task ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Show emoji if available, otherwise checkmark
                            if (!task.iconEmoji.isNullOrBlank()) {
                                Text(
                                    text = task.iconEmoji,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${task.title} (~${task.etaMin}m)", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                Text(stringResource(R.string.label_when_add), style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlanItemWindow.values().forEach { window ->
                        GlassChip(
                            text = window.name.lowercase().replaceFirstChar { it.uppercase() },
                            isSelected = selectedWindow == window,
                            onClick = { selectedWindow = window },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                GlassButton(
                    text = stringResource(R.string.action_add_task),
                    onClick = { onConfirm(selectedWindow) },
                    style = GlassButtonStyle.PRIMARY,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
