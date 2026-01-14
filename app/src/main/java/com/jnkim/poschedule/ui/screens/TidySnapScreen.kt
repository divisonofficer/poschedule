package com.jnkim.poschedule.ui.screens

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.jnkim.poschedule.ui.components.GlassCard
import com.jnkim.poschedule.ui.theme.ModeNormal
import com.jnkim.poschedule.ui.viewmodel.TidySnapUiState
import com.jnkim.poschedule.ui.viewmodel.TidySnapViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor

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
    
    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraExecutor = remember { ContextCompat.getMainExecutor(context) }

    GlassBackground(accentColor = ModeNormal) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Real Camera Preview
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageCapture
                            )
                        } catch (e: Exception) {
                            Log.e("TidySnap", "Use case binding failed", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

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
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
                
                Spacer(modifier = Modifier.weight(1f))

                if (uiState is TidySnapUiState.Idle) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = { 
                                takePhoto(context, imageCapture, cameraExecutor) { file ->
                                    viewModel.processImage(file, "ko")
                                }
                            },
                            modifier = Modifier.size(80.dp),
                            shape = androidx.compose.foundation.shape.CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ) { }
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

            // 4. Result Sheet
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

private fun takePhoto(
    context: android.content.Context,
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
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${task.title} (~${task.etaMin}m)", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                Text(stringResource(R.string.label_when_add), style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    PlanItemWindow.values().forEach { window ->
                        FilterChip(
                            selected = selectedWindow == window,
                            onClick = { selectedWindow = window },
                            label = { Text(window.name.lowercase().capitalize()) }
                        )
                    }
                }

                Button(
                    onClick = { onConfirm(selectedWindow) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.action_add_task))
                }
            }
        }
    }
}
