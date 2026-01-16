package com.jnkim.poschedule.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.width
import androidx.compose.ui.unit.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.jnkim.poschedule.R
import com.jnkim.poschedule.data.share.SharePayload
import com.jnkim.poschedule.ui.components.GlassBackground
import com.jnkim.poschedule.ui.components.GlassCard
import com.jnkim.poschedule.ui.components.GlassSegmentedControl
import com.jnkim.poschedule.ui.components.PlanReviewSheet
import com.jnkim.poschedule.ui.components.WittyLoadingIndicator
import com.jnkim.poschedule.ui.theme.ModeNormal
import com.jnkim.poschedule.ui.viewmodel.ImportDraftViewModel
import com.jnkim.poschedule.ui.viewmodel.ImportDraftUiState

/**
 * Screen for importing shared content from external apps.
 *
 * Flow:
 * 1. Preview - Show what was shared
 * 2. Analyze - Parse with LLM
 * 3. Review - Show PlanReviewSheet (reuse existing component)
 * 4. Save - Create plan and return to caller
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportDraftScreen(
    sharePayload: SharePayload,
    onSuccess: () -> Unit,
    onCancel: () -> Unit,
    viewModel: ImportDraftViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Screenshot deletion dialog state
    var showScreenshotDeletionDialog by remember { mutableStateOf(false) }

    // Activity result launcher for screenshot deletion (Android 11+)
    val deletionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        // Deletion complete (regardless of user choice)
        android.util.Log.d("ImportDraftScreen", "Screenshot deletion result: ${result.resultCode}")
        onSuccess()
    }

    // Auto-start analysis on first composition
    LaunchedEffect(sharePayload) {
        viewModel.analyzeShareContent(sharePayload)
    }

    // Screenshot deletion dialog
    if (showScreenshotDeletionDialog) {
        AlertDialog(
            onDismissRequest = {
                showScreenshotDeletionDialog = false
                onSuccess()
            },
            title = { Text("스크린샷 삭제") },
            text = { Text("이 스크린샷을 삭제할까요?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showScreenshotDeletionDialog = false
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                            val deletionIntent = viewModel.createScreenshotDeletionRequest()
                            if (deletionIntent != null) {
                                try {
                                    // Launch MediaStore deletion request
                                    val request = androidx.activity.result.IntentSenderRequest.Builder(deletionIntent).build()
                                    deletionLauncher.launch(request)
                                } catch (e: Exception) {
                                    android.util.Log.e("ImportDraftScreen", "Error launching deletion", e)
                                    onSuccess()
                                }
                            } else {
                                onSuccess()
                            }
                        } else {
                            onSuccess()
                        }
                    }
                ) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showScreenshotDeletionDialog = false
                        onSuccess()
                    }
                ) {
                    Text("유지")
                }
            }
        )
    }

    GlassBackground(accentColor = ModeNormal) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header
            TopAppBar(
                title = { Text(stringResource(R.string.import_draft_title)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ModeNormal.copy(alpha = 0.08f)  // UI refinement Phase 5: match main app tone
                )
            )

            // Content
            when (val state = uiState) {
                ImportDraftUiState.Idle -> {
                    // Should not reach here due to LaunchedEffect
                }

                is ImportDraftUiState.Preview -> {
                    PreviewContent(payload = sharePayload)
                }

                ImportDraftUiState.PerformingOCR -> {
                    LoadingContent(message = "이미지에서 텍스트 추출 중...")
                }

                is ImportDraftUiState.OCRComplete -> {
                    OCRResultContent(
                        textBlocks = state.textBlocks,
                        imageBitmap = state.imageBitmap,
                        selectionMode = state.selectionMode,
                        onBlockToggle = { index -> viewModel.toggleTextBlock(index) },
                        onYRangeSelect = { minY, maxY, select ->
                            viewModel.selectBlocksInYRange(minY, maxY, select)
                        },
                        onRectangleSelect = { left, top, right, bottom ->
                            viewModel.selectBlocksInRectangle(left, top, right, bottom)
                        },
                        onModeChange = { mode -> viewModel.switchSelectionMode(mode) },
                        onContinue = {
                            viewModel.continueWithLLMAnalysis(
                                selectedBlocks = state.textBlocks,
                                targetDate = state.selectedDate,
                                targetTime = state.selectedTime,
                                imageBitmap = state.imageBitmap
                            )
                        },
                        onCancel = onCancel
                    )
                }

                ImportDraftUiState.Analyzing -> {
                    LoadingContent(message = null)  // Use default witty messages
                }

                is ImportDraftUiState.AnalyzingWithDebug -> {
                    // Show OCR debug view with loading overlay (non-interactive)
                    OCRDebugWithLoading(
                        textBlocks = state.textBlocks,
                        imageBitmap = state.imageBitmap
                    )
                }

                is ImportDraftUiState.Ready -> {
                    // Reuse existing PlanReviewSheet
                    PlanReviewSheet(
                        normalizedPlan = state.normalizedPlan,
                        confidence = state.confidence,
                        alternatives = state.alternatives,
                        onConfirm = { modifiedPlan, selectedAlternatives ->
                            viewModel.savePlans(
                                mainPlan = modifiedPlan,
                                alternatives = selectedAlternatives
                            )

                            // Check if there's a screenshot to delete (Android 11+ only)
                            if (viewModel.hasScreenshotToDelete()) {
                                showScreenshotDeletionDialog = true
                            } else {
                                onSuccess()
                            }
                        },
                        onEdit = {
                            // Phase 2: Open manual editor
                            // For now, just show error toast
                        },
                        onCancel = onCancel
                    )
                }

                is ImportDraftUiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = { viewModel.analyzeShareContent(sharePayload) },
                        onCancel = onCancel
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewContent(payload: SharePayload) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (payload) {
            is SharePayload.SharedText -> {
                GlassCard {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Shared Text:",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = payload.text,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (payload.source != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "From: ${payload.source}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            is SharePayload.SharedImage -> {
                GlassCard {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Shared Image:",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Image(
                            painter = rememberAsyncImagePainter(payload.uri),
                            contentDescription = "Shared image preview",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp),
                            contentScale = ContentScale.Fit
                        )
                        if (payload.source != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "From: ${payload.source}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingContent(message: String? = null) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (message != null) {
            // Show specific message (e.g., for OCR)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(message, style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            // Use witty loading messages
            WittyLoadingIndicator()
        }
    }
}

@Composable
private fun OCRResultContent(
    textBlocks: List<com.jnkim.poschedule.ui.viewmodel.OCRTextBlock>,
    imageBitmap: android.graphics.Bitmap,
    selectionMode: com.jnkim.poschedule.ui.viewmodel.SelectionMode,
    onBlockToggle: (Int) -> Unit,
    onYRangeSelect: (Float, Float, Boolean) -> Unit,
    onRectangleSelect: (Float, Float, Float, Float) -> Unit,
    onModeChange: (com.jnkim.poschedule.ui.viewmodel.SelectionMode) -> Unit,
    onContinue: () -> Unit,
    onCancel: () -> Unit
) {
    val selectedCount = textBlocks.count { it.isSelected }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Title with selection count
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "추출된 텍스트",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = stringResource(R.string.import_draft_selected_count, selectedCount),  // UI refinement Phase 5: humanized text
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Selection mode toggle
        GlassSegmentedControl(
            options = listOf("탭 선택", "영역 선택"),
            selectedIndex = when (selectionMode) {
                com.jnkim.poschedule.ui.viewmodel.SelectionMode.TOGGLE -> 0
                com.jnkim.poschedule.ui.viewmodel.SelectionMode.CROP -> 1
            },
            onSelectionChange = { index ->
                val newMode = when (index) {
                    0 -> com.jnkim.poschedule.ui.viewmodel.SelectionMode.TOGGLE
                    1 -> com.jnkim.poschedule.ui.viewmodel.SelectionMode.CROP
                    else -> com.jnkim.poschedule.ui.viewmodel.SelectionMode.TOGGLE
                }
                onModeChange(newMode)
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Mode description
        Text(
            text = when (selectionMode) {
                com.jnkim.poschedule.ui.viewmodel.SelectionMode.TOGGLE -> "박스를 터치해서 선택/해제하세요"
                com.jnkim.poschedule.ui.viewmodel.SelectionMode.CROP -> "화면을 드래그해서 영역을 선택하세요"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Image with interactive bounding boxes (scaled to fit screen)
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            InteractiveOCRImage(
                bitmap = imageBitmap,
                textBlocks = textBlocks,
                selectionMode = selectionMode,
                onBlockToggle = onBlockToggle,
                onRectangleSelect = onRectangleSelect
            )
        }

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("취소")
            }
            Button(
                onClick = onContinue,
                enabled = selectedCount > 0,
                modifier = Modifier.weight(1f)
            ) {
                Text("계속하기 ($selectedCount)")
            }
        }
    }
}

/**
 * Interactive OCR image with touch-selectable text blocks.
 * Selected blocks are shown in normal brightness, unselected blocks are dimmed.
 *
 * TOGGLE mode: Tap to toggle individual blocks on/off.
 * CROP mode: Drag to draw rectangle, select blocks within.
 *
 * Scaled to fit within parent container without scrolling.
 */
@Composable
private fun InteractiveOCRImage(
    bitmap: android.graphics.Bitmap,
    textBlocks: List<com.jnkim.poschedule.ui.viewmodel.OCRTextBlock>,
    selectionMode: com.jnkim.poschedule.ui.viewmodel.SelectionMode,
    onBlockToggle: (Int) -> Unit,
    onRectangleSelect: (Float, Float, Float, Float) -> Unit
) {
    // Crop rectangle coordinates (always visible in CROP mode)
    var cropStartX by remember { mutableFloatStateOf(0f) }
    var cropStartY by remember { mutableFloatStateOf(0f) }
    var cropEndX by remember { mutableFloatStateOf(0f) }
    var cropEndY by remember { mutableFloatStateOf(0f) }
    var isCropInitialized by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }

    // Reset crop initialization when mode changes
    LaunchedEffect(selectionMode) {
        if (selectionMode != com.jnkim.poschedule.ui.viewmodel.SelectionMode.CROP) {
            isCropInitialized = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(textBlocks, selectionMode) {
                    when (selectionMode) {
                        com.jnkim.poschedule.ui.viewmodel.SelectionMode.TOGGLE -> {
                            // TOGGLE mode: Tap to toggle individual blocks
                            detectTapGestures { tapOffset ->
                                val canvasWidth = size.width.toFloat()
                                val canvasHeight = size.height.toFloat()
                                val bitmapWidth = bitmap.width.toFloat()
                                val bitmapHeight = bitmap.height.toFloat()

                                val scale = minOf(
                                    canvasWidth / bitmapWidth,
                                    canvasHeight / bitmapHeight
                                )
                                val scaledWidth = bitmapWidth * scale
                                val scaledHeight = bitmapHeight * scale
                                val offsetX = (canvasWidth - scaledWidth) / 2
                                val offsetY = (canvasHeight - scaledHeight) / 2

                                val bitmapX = (tapOffset.x - offsetX) / scale
                                val bitmapY = (tapOffset.y - offsetY) / scale

                                textBlocks.forEachIndexed { index, block ->
                                    val rect = block.boundingBox
                                    if (bitmapX >= rect.left && bitmapX <= rect.right &&
                                        bitmapY >= rect.top && bitmapY <= rect.bottom
                                    ) {
                                        onBlockToggle(index)
                                        return@detectTapGestures
                                    }
                                }
                            }
                        }
                        com.jnkim.poschedule.ui.viewmodel.SelectionMode.CROP -> {
                            // CROP mode: Drag to adjust crop rectangle
                            detectDragGestures(
                                onDragStart = { offset ->
                                    isDragging = true
                                    cropStartX = offset.x
                                    cropStartY = offset.y
                                    cropEndX = offset.x
                                    cropEndY = offset.y
                                },
                                onDrag = { change, _ ->
                                    cropEndX = change.position.x
                                    cropEndY = change.position.y
                                },
                                onDragEnd = {
                                    isDragging = false

                                    // Convert canvas coordinates to bitmap coordinates
                                    val canvasWidth = size.width.toFloat()
                                    val canvasHeight = size.height.toFloat()
                                    val bitmapWidth = bitmap.width.toFloat()
                                    val bitmapHeight = bitmap.height.toFloat()

                                    val scale = minOf(
                                        canvasWidth / bitmapWidth,
                                        canvasHeight / bitmapHeight
                                    )
                                    val scaledWidth = bitmapWidth * scale
                                    val scaledHeight = bitmapHeight * scale
                                    val offsetX = (canvasWidth - scaledWidth) / 2
                                    val offsetY = (canvasHeight - scaledHeight) / 2

                                    val minX = kotlin.math.min(cropStartX, cropEndX)
                                    val maxX = kotlin.math.max(cropStartX, cropEndX)
                                    val minY = kotlin.math.min(cropStartY, cropEndY)
                                    val maxY = kotlin.math.max(cropStartY, cropEndY)

                                    val bitmapLeft = (minX - offsetX) / scale
                                    val bitmapRight = (maxX - offsetX) / scale
                                    val bitmapTop = (minY - offsetY) / scale
                                    val bitmapBottom = (maxY - offsetY) / scale

                                    onRectangleSelect(bitmapLeft, bitmapTop, bitmapRight, bitmapBottom)
                                },
                                onDragCancel = {
                                    isDragging = false
                                }
                            )
                        }
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val bitmapWidth = bitmap.width.toFloat()
            val bitmapHeight = bitmap.height.toFloat()

            // Calculate scale to fit bitmap in canvas
            val scale = minOf(
                canvasWidth / bitmapWidth,
                canvasHeight / bitmapHeight
            )

            val scaledWidth = bitmapWidth * scale
            val scaledHeight = bitmapHeight * scale
            val offsetX = (canvasWidth - scaledWidth) / 2
            val offsetY = (canvasHeight - scaledHeight) / 2

            // Initialize crop rectangle in CROP mode (covers 90% of image with margin)
            if (selectionMode == com.jnkim.poschedule.ui.viewmodel.SelectionMode.CROP && !isCropInitialized) {
                val margin = 0.05f // 5% margin on each side
                cropStartX = offsetX + (scaledWidth * margin)
                cropStartY = offsetY + (scaledHeight * margin)
                cropEndX = offsetX + (scaledWidth * (1f - margin))
                cropEndY = offsetY + (scaledHeight * (1f - margin))
                isCropInitialized = true

                // Select blocks within initial crop rectangle
                val bitmapLeft = (cropStartX - offsetX) / scale
                val bitmapRight = (cropEndX - offsetX) / scale
                val bitmapTop = (cropStartY - offsetY) / scale
                val bitmapBottom = (cropEndY - offsetY) / scale
                onRectangleSelect(bitmapLeft, bitmapTop, bitmapRight, bitmapBottom)
            }

            // Draw the bitmap
            drawImage(
                image = bitmap.asImageBitmap(),
                dstOffset = androidx.compose.ui.unit.IntOffset(offsetX.toInt(), offsetY.toInt()),
                dstSize = androidx.compose.ui.unit.IntSize(scaledWidth.toInt(), scaledHeight.toInt())
            )

            // Draw full dimming overlay on entire image
            drawRect(
                color = Color.Black.copy(alpha = 0.7f),
                topLeft = Offset(offsetX, offsetY),
                size = Size(scaledWidth, scaledHeight)
            )

            // Redraw selected blocks with original brightness
            textBlocks.forEach { block ->
                if (block.isSelected) {
                    val rect = block.boundingBox

                    // Calculate source and destination rectangles
                    val srcLeft = rect.left
                    val srcTop = rect.top
                    val srcWidth = rect.width()
                    val srcHeight = rect.height()

                    val dstLeft = offsetX + (rect.left * scale)
                    val dstTop = offsetY + (rect.top * scale)
                    val dstWidth = rect.width() * scale
                    val dstHeight = rect.height() * scale

                    // Draw this portion of the original image to make it bright
                    drawImage(
                        image = bitmap.asImageBitmap(),
                        srcOffset = androidx.compose.ui.unit.IntOffset(srcLeft, srcTop),
                        srcSize = androidx.compose.ui.unit.IntSize(srcWidth, srcHeight),
                        dstOffset = androidx.compose.ui.unit.IntOffset(dstLeft.toInt(), dstTop.toInt()),
                        dstSize = androidx.compose.ui.unit.IntSize(dstWidth.toInt(), dstHeight.toInt())
                    )
                }
            }

            // Draw crop rectangle in CROP mode (always visible, like image crop UI)
            if (selectionMode == com.jnkim.poschedule.ui.viewmodel.SelectionMode.CROP) {
                val minX = kotlin.math.min(cropStartX, cropEndX)
                val maxX = kotlin.math.max(cropStartX, cropEndX)
                val minY = kotlin.math.min(cropStartY, cropEndY)
                val maxY = kotlin.math.max(cropStartY, cropEndY)

                val rectWidth = maxX - minX
                val rectHeight = maxY - minY

                // Draw semi-transparent white fill for selected area
                drawRect(
                    color = Color.White.copy(alpha = 0.15f),
                    topLeft = Offset(minX, minY),
                    size = Size(rectWidth, rectHeight)
                )

                // Draw outer shadow (black border for depth)
                drawRect(
                    color = Color.Black.copy(alpha = 0.3f),
                    topLeft = Offset(minX - 1.dp.toPx(), minY - 1.dp.toPx()),
                    size = Size(rectWidth + 2.dp.toPx(), rectHeight + 2.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                )

                // Draw main white border
                drawRect(
                    color = Color.White,
                    topLeft = Offset(minX, minY),
                    size = Size(rectWidth, rectHeight),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                )

                // Draw corner handles (8x8dp squares at each corner)
                val handleSize = 8.dp.toPx()
                val handleOffset = 1.5.dp.toPx() // Half of border width

                // Top-left handle
                drawRect(
                    color = Color.White,
                    topLeft = Offset(minX - handleOffset, minY - handleOffset),
                    size = Size(handleSize, handleSize)
                )
                drawRect(
                    color = Color.Black.copy(alpha = 0.3f),
                    topLeft = Offset(minX - handleOffset, minY - handleOffset),
                    size = Size(handleSize, handleSize),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                )

                // Top-right handle
                drawRect(
                    color = Color.White,
                    topLeft = Offset(maxX - handleSize + handleOffset, minY - handleOffset),
                    size = Size(handleSize, handleSize)
                )
                drawRect(
                    color = Color.Black.copy(alpha = 0.3f),
                    topLeft = Offset(maxX - handleSize + handleOffset, minY - handleOffset),
                    size = Size(handleSize, handleSize),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                )

                // Bottom-left handle
                drawRect(
                    color = Color.White,
                    topLeft = Offset(minX - handleOffset, maxY - handleSize + handleOffset),
                    size = Size(handleSize, handleSize)
                )
                drawRect(
                    color = Color.Black.copy(alpha = 0.3f),
                    topLeft = Offset(minX - handleOffset, maxY - handleSize + handleOffset),
                    size = Size(handleSize, handleSize),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                )

                // Bottom-right handle
                drawRect(
                    color = Color.White,
                    topLeft = Offset(maxX - handleSize + handleOffset, maxY - handleSize + handleOffset),
                    size = Size(handleSize, handleSize)
                )
                drawRect(
                    color = Color.Black.copy(alpha = 0.3f),
                    topLeft = Offset(maxX - handleSize + handleOffset, maxY - handleSize + handleOffset),
                    size = Size(handleSize, handleSize),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                )
            }
        }
    }
}

/**
 * Shows the image with dimming overlay on unselected areas.
 * Used in non-interactive debug view during LLM analysis.
 * Same visual style as InteractiveOCRImage but without touch interactions.
 */
@Composable
private fun ImageWithBoundingBoxes(
    bitmap: android.graphics.Bitmap,
    textBlocks: List<com.jnkim.poschedule.ui.viewmodel.OCRTextBlock>,
    onBlockTap: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val bitmapWidth = bitmap.width.toFloat()
            val bitmapHeight = bitmap.height.toFloat()

            // Calculate scale to fit bitmap in canvas
            val scale = minOf(
                canvasWidth / bitmapWidth,
                canvasHeight / bitmapHeight
            )

            val scaledWidth = bitmapWidth * scale
            val scaledHeight = bitmapHeight * scale
            val offsetX = (canvasWidth - scaledWidth) / 2
            val offsetY = (canvasHeight - scaledHeight) / 2

            // Draw the bitmap
            drawImage(
                image = bitmap.asImageBitmap(),
                dstOffset = androidx.compose.ui.unit.IntOffset(offsetX.toInt(), offsetY.toInt()),
                dstSize = androidx.compose.ui.unit.IntSize(scaledWidth.toInt(), scaledHeight.toInt())
            )

            // Draw full dimming overlay on entire image
            drawRect(
                color = Color.Black.copy(alpha = 0.7f),
                topLeft = Offset(offsetX, offsetY),
                size = Size(scaledWidth, scaledHeight)
            )

            // Redraw selected blocks with original brightness
            textBlocks.forEach { block ->
                if (block.isSelected) {
                    val rect = block.boundingBox

                    // Calculate source and destination rectangles
                    val srcLeft = rect.left
                    val srcTop = rect.top
                    val srcWidth = rect.width()
                    val srcHeight = rect.height()

                    val dstLeft = offsetX + (rect.left * scale)
                    val dstTop = offsetY + (rect.top * scale)
                    val dstWidth = rect.width() * scale
                    val dstHeight = rect.height() * scale

                    // Draw this portion of the original image to make it bright
                    drawImage(
                        image = bitmap.asImageBitmap(),
                        srcOffset = androidx.compose.ui.unit.IntOffset(srcLeft, srcTop),
                        srcSize = androidx.compose.ui.unit.IntSize(srcWidth, srcHeight),
                        dstOffset = androidx.compose.ui.unit.IntOffset(dstLeft.toInt(), dstTop.toInt()),
                        dstSize = androidx.compose.ui.unit.IntSize(dstWidth.toInt(), dstHeight.toInt())
                    )
                }
            }
        }
    }
}

/**
 * Shows OCR debug view (image + bounding boxes) with loading indicator overlay.
 * Non-interactive - used during LLM analysis.
 */
@Composable
private fun OCRDebugWithLoading(
    textBlocks: List<com.jnkim.poschedule.ui.viewmodel.OCRTextBlock>,
    imageBitmap: android.graphics.Bitmap
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Debug view in background
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "LLM 분석 중...",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            // Image with bounding boxes (non-interactive)
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                ImageWithBoundingBoxes(
                    bitmap = imageBitmap,
                    textBlocks = textBlocks,
                    onBlockTap = { /* non-interactive */ }
                )
            }
        }

        // Loading indicator overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 100.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            WittyLoadingIndicator()
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GlassCard {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.import_error_unknown),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onCancel) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    Button(onClick = onRetry) {
                        Text(stringResource(R.string.import_retry))
                    }
                }
            }
        }
    }
}
