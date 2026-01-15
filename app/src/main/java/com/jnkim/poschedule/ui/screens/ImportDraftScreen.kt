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

    // Auto-start analysis on first composition
    LaunchedEffect(sharePayload) {
        viewModel.analyzeShareContent(sharePayload)
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
                    containerColor = Color.Transparent
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
                        onBlockToggle = { index -> viewModel.toggleTextBlock(index) },
                        onYRangeSelect = { minY, maxY, select ->
                            viewModel.selectBlocksInYRange(minY, maxY, select)
                        },
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
                            onSuccess()
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
    onBlockToggle: (Int) -> Unit,
    onYRangeSelect: (Float, Float, Boolean) -> Unit,
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
                text = "$selectedCount/${textBlocks.size} 선택됨",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Quick selection buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    // Select all blocks
                    textBlocks.forEachIndexed { index, block ->
                        if (!block.isSelected) {
                            onBlockToggle(index)
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("전체 선택")
            }
            OutlinedButton(
                onClick = {
                    // Deselect all blocks
                    textBlocks.forEachIndexed { index, block ->
                        if (block.isSelected) {
                            onBlockToggle(index)
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("전체 해제")
            }
        }

        Text(
            text = "박스를 터치해서 선택/해제하세요",
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
                onBlockToggle = onBlockToggle,
                onYRangeSelect = onYRangeSelect
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
 * Supports tap to toggle individual block and vertical drag to select/deselect range.
 * Scaled to fit within parent container without scrolling.
 */
@Composable
private fun InteractiveOCRImage(
    bitmap: android.graphics.Bitmap,
    textBlocks: List<com.jnkim.poschedule.ui.viewmodel.OCRTextBlock>,
    onBlockToggle: (Int) -> Unit,
    onYRangeSelect: (Float, Float, Boolean) -> Unit
) {
    var dragStartY by remember { mutableFloatStateOf(0f) }
    var dragEndY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(textBlocks) {
                    // Detect both tap and drag gestures
                    detectTapGestures { tapOffset ->
                        // Tap gesture - toggle individual block
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
                .pointerInput(textBlocks) {
                    // Detect vertical drag for range selection
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragStartY = offset.y
                            dragEndY = offset.y
                        },
                        onDrag = { change, _ ->
                            dragEndY = change.position.y
                        },
                        onDragEnd = {
                            isDragging = false

                            // Convert canvas Y to bitmap Y coordinates
                            val canvasWidth = size.width.toFloat()
                            val canvasHeight = size.height.toFloat()
                            val bitmapWidth = bitmap.width.toFloat()
                            val bitmapHeight = bitmap.height.toFloat()

                            val scale = minOf(
                                canvasWidth / bitmapWidth,
                                canvasHeight / bitmapHeight
                            )
                            val scaledHeight = bitmapHeight * scale
                            val offsetY = (canvasHeight - scaledHeight) / 2

                            val minY = kotlin.math.min(dragStartY, dragEndY)
                            val maxY = kotlin.math.max(dragStartY, dragEndY)

                            val bitmapMinY = (minY - offsetY) / scale
                            val bitmapMaxY = (maxY - offsetY) / scale

                            // Check if first block in range is selected or not
                            // to decide whether to select or deselect all in range
                            val firstBlockInRange = textBlocks.find { block ->
                                val centerY = block.boundingBox.centerY()
                                centerY >= bitmapMinY && centerY <= bitmapMaxY
                            }

                            val shouldSelect = firstBlockInRange?.isSelected == false

                            onYRangeSelect(bitmapMinY, bitmapMaxY, shouldSelect)
                        },
                        onDragCancel = {
                            isDragging = false
                        }
                    )
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
