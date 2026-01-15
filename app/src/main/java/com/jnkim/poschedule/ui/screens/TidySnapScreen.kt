package com.jnkim.poschedule.ui.screens

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.ZoomState
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

/**
 * Semantic zoom profiles for multi-camera control.
 * Uses zoom ratios instead of physical lens detection to support
 * both Samsung (multiple CameraInfos) and Xiaomi (logical camera) devices.
 */
enum class ZoomProfile(
    val targetZoomRatio: Float,
    val labelKr: String,
    val labelEn: String
) {
    ULTRA_WIDE(0.6f, "광각", "Ultra-Wide"),  // Wide field of view
    STANDARD(1.0f, "표준", "Standard"),        // Default 1x zoom
    TELEPHOTO(2.0f, "망원", "Telephoto")      // 2x zoom for distant objects
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

    var currentZoomProfile by remember { mutableStateOf(ZoomProfile.STANDARD) }
    var availableZoomProfiles by remember { mutableStateOf(setOf(ZoomProfile.STANDARD)) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY) // Use maximum resolution
            .build()
    }
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

                            // Single camera selector - no filtering needed
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            // Build preview use case
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            try {
                                // Unbind previous camera
                                cameraProvider.unbindAll()

                                // Bind to single logical camera (works on all vendors)
                                val camera = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageCapture
                                )

                                // Store camera instance for zoom control
                                boundCamera = camera

                                // Detect zoom capabilities
                                val initialZoomState = camera.cameraInfo.zoomState.value
                                val detected = detectAvailableZoomProfiles(initialZoomState)
                                availableZoomProfiles = detected

                                Log.d("TidySnap", "Camera bound. Available profiles: $detected")

                                // Set initial zoom ratio
                                setZoomRatioSafely(camera, currentZoomProfile.targetZoomRatio)

                            } catch (e: Exception) {
                                Log.e("TidySnap", "Camera binding failed", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    update = { previewView ->
                        // No rebinding needed - zoom changes handled via setZoomRatio
                        // This lambda is called on recomposition but we don't rebind camera
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
                    actions = { },
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

                        // Zoom Profile Selector (show all available profiles)
                        if (availableZoomProfiles.size > 1) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                availableZoomProfiles.toList()
                                    .sortedBy { it.targetZoomRatio }
                                    .forEach { profile ->
                                        FilterChip(
                                            selected = currentZoomProfile == profile,
                                            onClick = {
                                                currentZoomProfile = profile
                                                setZoomRatioSafely(boundCamera, profile.targetZoomRatio)
                                                Log.d("TidySnap", "Switched to $profile (${profile.targetZoomRatio}x)")
                                            },
                                            label = {
                                                Text(
                                                    text = "${profile.labelKr} ${profile.targetZoomRatio}x",
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            },
                                            modifier = Modifier.padding(horizontal = 4.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                containerColor = Color.Black.copy(alpha = 0.3f),
                                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                labelColor = Color.White,
                                                selectedLabelColor = Color.White
                                            ),
                                            border = FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = currentZoomProfile == profile,
                                                borderColor = Color.White.copy(alpha = 0.3f),
                                                selectedBorderColor = MaterialTheme.colorScheme.primary
                                            )
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
 * Detect available zoom profiles based on camera's zoom range.
 * Works on all vendors (Xiaomi, Samsung, Pixel) via unified ZoomState API.
 *
 * @param zoomState Camera zoom capabilities
 * @return Set of supported zoom profiles
 */
private fun detectAvailableZoomProfiles(zoomState: ZoomState?): Set<ZoomProfile> {
    if (zoomState == null) {
        Log.w("TidySnap", "ZoomState is null, only STANDARD available")
        return setOf(ZoomProfile.STANDARD)
    }

    val minZoom = zoomState.minZoomRatio
    val maxZoom = zoomState.maxZoomRatio

    Log.d("TidySnap", "Camera zoom range: $minZoom - $maxZoom")

    val profiles = mutableSetOf<ZoomProfile>()

    // Standard 1.0x is always available
    profiles.add(ZoomProfile.STANDARD)

    // Ultra-wide: Check if camera supports 0.6x zoom (with 0.05 tolerance)
    if (minZoom <= 0.65f) {
        profiles.add(ZoomProfile.ULTRA_WIDE)
        Log.d("TidySnap", "Ultra-wide available (minZoom=$minZoom)")
    }

    // Telephoto: Check if camera supports 2.0x zoom (with 0.1 tolerance)
    if (maxZoom >= 1.9f) {
        profiles.add(ZoomProfile.TELEPHOTO)
        Log.d("TidySnap", "Telephoto available (maxZoom=$maxZoom)")
    }

    return profiles
}

/**
 * Safely set zoom ratio with bounds checking and error handling.
 * Replaces expensive camera rebinding with lightweight zoom control.
 *
 * @param camera Bound camera instance
 * @param targetZoomRatio Desired zoom ratio (0.6, 1.0, 2.0, etc.)
 */
private fun setZoomRatioSafely(camera: Camera?, targetZoomRatio: Float) {
    if (camera == null) {
        Log.e("TidySnap", "Cannot set zoom: camera not bound")
        return
    }

    val zoomState = camera.cameraInfo.zoomState.value
    if (zoomState == null) {
        Log.e("TidySnap", "Cannot set zoom: zoomState unavailable")
        return
    }

    // Clamp zoom ratio to valid range
    val minZoom = zoomState.minZoomRatio
    val maxZoom = zoomState.maxZoomRatio
    val clampedZoom = targetZoomRatio.coerceIn(minZoom, maxZoom)

    if (clampedZoom != targetZoomRatio) {
        Log.w("TidySnap", "Zoom ratio $targetZoomRatio clamped to $clampedZoom (range: $minZoom-$maxZoom)")
    }

    // Apply zoom asynchronously
    camera.cameraControl.setZoomRatio(clampedZoom)
    Log.d("TidySnap", "Zoom set to ${clampedZoom}x")
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
