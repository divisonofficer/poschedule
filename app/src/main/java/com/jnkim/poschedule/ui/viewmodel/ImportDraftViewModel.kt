package com.jnkim.poschedule.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jnkim.poschedule.data.ai.AlternativePlan
import com.jnkim.poschedule.data.ai.NormalizedPlan
import com.jnkim.poschedule.data.local.entity.*
import com.jnkim.poschedule.data.repo.GenAiRepository
import com.jnkim.poschedule.data.repo.PlanRepository
import com.jnkim.poschedule.data.repo.SettingsRepository
import com.jnkim.poschedule.data.share.SharePayload
import com.jnkim.poschedule.domain.ai.LLMTaskNormalizerUseCase
import com.jnkim.poschedule.domain.engine.RecurrenceEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

/**
 * Represents a single text block extracted by OCR with its position.
 */
data class OCRTextBlock(
    val text: String,
    val boundingBox: android.graphics.Rect,
    val isSelected: Boolean = false  // Default: not selected (user selects center blocks)
)

sealed class ImportDraftUiState {
    object Idle : ImportDraftUiState()
    data class Preview(val payload: SharePayload) : ImportDraftUiState()
    object PerformingOCR : ImportDraftUiState()  // New: OCR in progress
    data class OCRComplete(  // New: OCR done, show results to user
        val textBlocks: List<OCRTextBlock>,
        val imageUri: android.net.Uri,
        val imageBitmap: android.graphics.Bitmap,  // For displaying with overlay
        val selectedDate: java.time.LocalDate = java.time.LocalDate.now(),  // Default to today
        val selectedTime: java.time.LocalTime? = null  // Null = let LLM decide
    ) : ImportDraftUiState()
    object Analyzing : ImportDraftUiState()  // LLM analysis in progress (text only)
    data class AnalyzingWithDebug(  // LLM analysis with OCR debug view visible
        val textBlocks: List<OCRTextBlock>,
        val imageBitmap: android.graphics.Bitmap
    ) : ImportDraftUiState()
    data class Ready(
        val normalizedPlan: NormalizedPlan,
        val confidence: Double,
        val alternatives: List<AlternativePlan>
    ) : ImportDraftUiState()
    data class Error(val message: String) : ImportDraftUiState()
}

@HiltViewModel
class ImportDraftViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val llmTaskNormalizerUseCase: LLMTaskNormalizerUseCase,
    private val planRepository: PlanRepository,
    private val settingsRepository: SettingsRepository,
    private val recurrenceEngine: RecurrenceEngine,
    private val genAiRepository: GenAiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ImportDraftUiState>(ImportDraftUiState.Idle)
    val uiState: StateFlow<ImportDraftUiState> = _uiState.asStateFlow()

    /**
     * Analyzes shared content.
     * For text: directly analyze with LLM
     * For images: perform MLKit OCR first, show results to user
     */
    fun analyzeShareContent(payload: SharePayload) {
        viewModelScope.launch {
            try {
                when (payload) {
                    is SharePayload.SharedText -> {
                        // Text: directly analyze with LLM
                        _uiState.value = ImportDraftUiState.Analyzing
                        val sanitized = sanitizeText(payload.text)
                        performLLMAnalysis(sanitized)
                    }
                    is SharePayload.SharedImage -> {
                        // Image: perform MLKit OCR first
                        _uiState.value = ImportDraftUiState.PerformingOCR
                        val (textBlocks, bitmap) = performMLKitOCR(payload.uri)

                        if (textBlocks.isEmpty()) {
                            _uiState.value = ImportDraftUiState.Error(
                                "이미지에서 텍스트를 찾을 수 없습니다"
                            )
                        } else {
                            // Show OCR results to user with text block overlay
                            _uiState.value = ImportDraftUiState.OCRComplete(
                                textBlocks = textBlocks,
                                imageUri = payload.uri,
                                imageBitmap = bitmap
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ImportDraftVM", "Error analyzing content", e)
                _uiState.value = ImportDraftUiState.Error(
                    "Failed to analyze content: ${e.message}"
                )
            }
        }
    }

    /**
     * Toggles selection state of a text block.
     * Called when user taps on a text block overlay.
     */
    fun toggleTextBlock(index: Int) {
        val currentState = _uiState.value
        if (currentState is ImportDraftUiState.OCRComplete) {
            val updatedBlocks = currentState.textBlocks.toMutableList()
            updatedBlocks[index] = updatedBlocks[index].copy(
                isSelected = !updatedBlocks[index].isSelected
            )
            _uiState.value = currentState.copy(textBlocks = updatedBlocks)
        }
    }

    /**
     * Selects or deselects all text blocks within a Y-coordinate range.
     * Used for vertical drag gesture to select multiple blocks at once.
     *
     * @param minY Minimum Y coordinate in bitmap space
     * @param maxY Maximum Y coordinate in bitmap space
     * @param select true to select blocks, false to deselect
     */
    fun selectBlocksInYRange(minY: Float, maxY: Float, select: Boolean) {
        val currentState = _uiState.value
        if (currentState is ImportDraftUiState.OCRComplete) {
            val updatedBlocks = currentState.textBlocks.map { block ->
                val blockCenterY = block.boundingBox.centerY()
                // Check if block center is within Y range
                if (blockCenterY >= minY && blockCenterY <= maxY) {
                    block.copy(isSelected = select)
                } else {
                    block  // Keep existing state
                }
            }
            _uiState.value = currentState.copy(textBlocks = updatedBlocks)
        }
    }

    /**
     * Updates the selected date for the schedule.
     */
    fun updateSelectedDate(date: java.time.LocalDate) {
        val currentState = _uiState.value
        if (currentState is ImportDraftUiState.OCRComplete) {
            _uiState.value = currentState.copy(selectedDate = date)
        }
    }

    /**
     * Updates the selected time for the schedule.
     */
    fun updateSelectedTime(time: java.time.LocalTime?) {
        val currentState = _uiState.value
        if (currentState is ImportDraftUiState.OCRComplete) {
            _uiState.value = currentState.copy(selectedTime = time)
        }
    }

    /**
     * Continue with LLM analysis after user confirms OCR results.
     * Called when user clicks "Continue" button after seeing OCR text.
     * @param selectedBlocks Only the selected text blocks will be sent to LLM
     * @param targetDate The date when this schedule should occur
     * @param targetTime Optional specific time for the schedule
     */
    fun continueWithLLMAnalysis(
        selectedBlocks: List<OCRTextBlock>,
        targetDate: java.time.LocalDate,
        targetTime: java.time.LocalTime?,
        imageBitmap: android.graphics.Bitmap  // For showing debug view during analysis
    ) {
        viewModelScope.launch {
            // Show analyzing state with debug view
            _uiState.value = ImportDraftUiState.AnalyzingWithDebug(
                textBlocks = selectedBlocks,
                imageBitmap = imageBitmap
            )
            try {
                // Combine selected blocks into single text
                val combinedText = selectedBlocks
                    .filter { it.isSelected }
                    .joinToString("\n") { it.text }

                // Add date/time context to help LLM
                val dateHint = if (targetDate == java.time.LocalDate.now()) {
                    "오늘"
                } else {
                    targetDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E)", java.util.Locale.KOREAN))
                }

                val timeHint = targetTime?.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))

                val contextualText = buildString {
                    append(combinedText)
                    append("\n\n")
                    append("이 일정은 $dateHint")
                    if (timeHint != null) {
                        append(" $timeHint")
                    }
                    append("에 하는 일정입니다.")
                }

                performLLMAnalysis(sanitizeText(contextualText))
            } catch (e: Exception) {
                android.util.Log.e("ImportDraftVM", "Error in LLM analysis", e)
                _uiState.value = ImportDraftUiState.Error(
                    "Failed to analyze: ${e.message}"
                )
            }
        }
    }

    /**
     * Performs LLM analysis on the given text input.
     */
    private suspend fun performLLMAnalysis(inputText: String) {
        val settings = settingsRepository.settingsFlow.first()
        val locale = if (settings.language == "ko") "ko" else "en"

        val result = llmTaskNormalizerUseCase(
            userInput = inputText,
            locale = locale
        )

        result.fold(
            onSuccess = { response ->
                when {
                    response.intent == "unsure" || response.confidence < 0.7 -> {
                        _uiState.value = ImportDraftUiState.Error(
                            "Could not understand the shared content. Please try manual entry."
                        )
                    }
                    response.plan != null -> {
                        _uiState.value = ImportDraftUiState.Ready(
                            normalizedPlan = response.plan,
                            confidence = response.confidence,
                            alternatives = response.alternatives
                        )
                    }
                    else -> {
                        _uiState.value = ImportDraftUiState.Error("No plan found in response")
                    }
                }
            },
            onFailure = { error ->
                _uiState.value = ImportDraftUiState.Error(
                    error.message ?: "Unknown error occurred"
                )
            }
        )
    }

    /**
     * Sanitizes text input by removing problematic characters and limiting length.
     * Prevents JSON parsing errors from special characters and overly long input.
     */
    private fun sanitizeText(input: String): String {
        var sanitized = input.trim()

        // Limit length to 3000 characters to prevent API overload
        if (sanitized.length > 3000) {
            sanitized = sanitized.substring(0, 3000) + "..."
        }

        // Remove or escape problematic characters that break JSON parsing
        sanitized = sanitized
            .replace("\u0000", "") // Null characters
            .replace("\r", " ") // Carriage returns
            .replace("\t", " ") // Tabs
            .replace("\\", "\\\\") // Escape backslashes
            .replace("\"", "\\\"") // Escape quotes

        // Remove excessive whitespace
        sanitized = sanitized.replace(Regex("\\s+"), " ")

        android.util.Log.d("ImportDraftVM", "Sanitized text length: ${sanitized.length}")
        return sanitized
    }

    /**
     * Performs on-device OCR using MLKit Text Recognition.
     * Fast, private, and supports Korean + English.
     * @return Pair of text blocks and the original bitmap
     */
    private suspend fun performMLKitOCR(uri: Uri): Pair<List<OCRTextBlock>, android.graphics.Bitmap> {
        return kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
            try {
                // Load image from URI
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap == null) {
                    continuation.resumeWith(Result.failure(IllegalStateException("Failed to load image")))
                    return@suspendCancellableCoroutine
                }

                // Create InputImage from bitmap
                val image = com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap, 0)

                // Use Korean script recognizer (also handles English)
                val recognizer = com.google.mlkit.vision.text.TextRecognition.getClient(
                    com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions.Builder().build()
                )

                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val allBlocks = mutableListOf<OCRTextBlock>()

                        // Extract text blocks with bounding boxes
                        for (block in visionText.textBlocks) {
                            val text = block.text.trim()
                            val boundingBox = block.boundingBox ?: continue

                            // Filter out noise:
                            // - Very short text (< 2 chars)
                            // - Only special characters
                            // - Common OCR artifacts
                            if (text.length < 2) continue
                            if (text.matches(Regex("[^\\w\\s가-힣]+"))) continue // Only special chars
                            if (text in listOf("Translate", "←", "→", "↑", "↓")) continue

                            allBlocks.add(
                                OCRTextBlock(
                                    text = text,
                                    boundingBox = boundingBox,
                                    isSelected = false  // Start with all unselected
                                )
                            )
                        }

                        // Auto-select center blocks only
                        val imageCenterX = bitmap.width / 2f
                        val imageCenterY = bitmap.height / 2f

                        // Calculate distance from center for each block
                        val blocksWithDistance = allBlocks.map { block ->
                            val blockCenterX = block.boundingBox.centerX()
                            val blockCenterY = block.boundingBox.centerY()
                            val distance = kotlin.math.sqrt(
                                (blockCenterX - imageCenterX) * (blockCenterX - imageCenterX) +
                                (blockCenterY - imageCenterY) * (blockCenterY - imageCenterY)
                            )
                            block to distance
                        }

                        // Select blocks in center 60% region or top 3-5 closest blocks
                        val maxSelectCount = kotlin.math.min(5, allBlocks.size)
                        val selectedIndices = blocksWithDistance
                            .sortedBy { it.second }  // Sort by distance from center
                            .take(maxSelectCount)
                            .map { allBlocks.indexOf(it.first) }
                            .toSet()

                        val textBlocks = allBlocks.mapIndexed { index, block ->
                            if (index in selectedIndices) {
                                block.copy(isSelected = true)
                            } else {
                                block
                            }
                        }

                        android.util.Log.d("ImportDraftVM", "MLKit OCR extracted ${textBlocks.size} blocks, ${selectedIndices.size} auto-selected")
                        continuation.resumeWith(Result.success(Pair(textBlocks, bitmap)))
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("ImportDraftVM", "MLKit OCR failed", e)
                        continuation.resumeWith(Result.failure(e))
                    }
            } catch (e: Exception) {
                android.util.Log.e("ImportDraftVM", "Error loading image for OCR", e)
                continuation.resumeWith(Result.failure(e))
            }
        }
    }

    /**
     * Extracts text from image using Vision API.
     * Uploads image and uses OCR to extract schedule-related text.
     * @deprecated Use performMLKitOCR instead for better UX
     */
    @Deprecated("Use performMLKitOCR for faster on-device processing")
    private suspend fun extractTextFromImage(uri: Uri): String {
        android.util.Log.d("ImportDraftVM", "Extracting text from image: $uri")

        // 1. Convert Uri to File
        val imageFile = uriToFile(uri)

        // 2. Upload image to GenAI
        val genAiFile = genAiRepository.uploadFile(imageFile)
        if (genAiFile == null) {
            throw IllegalStateException("Failed to upload image")
        }

        // 3. Use Vision API to extract schedule-related text
        val ocrPrompt = """
            이미지에서 일정, 할일, 이벤트 관련 정보를 추출해주세요.

            추출할 내용:
            - 날짜와 시간 (예: "토요일", "오후 3시", "내일")
            - 할 일 제목이나 설명
            - 반복 패턴 (매일, 매주, 매월 등)
            - 일정과 관련된 모든 텍스트

            **중요**: 이미지의 모든 텍스트를 그대로 추출해서 반환하세요.
            분석하거나 해석하지 말고, 보이는 텍스트를 모두 복사해주세요.

            일정 정보가 전혀 없으면 "EMPTY"라고만 응답하세요.
        """.trimIndent()

        val systemPrompt = "You are an OCR assistant. Extract all text from images exactly as shown."

        val rawResponse = genAiRepository.getCompletion(
            prompt = ocrPrompt,
            systemPrompt = systemPrompt,
            files = listOf(genAiFile)
        )

        // 4. Clean up temporary file
        imageFile.delete()

        if (rawResponse == null || rawResponse.trim() == "EMPTY") {
            throw IllegalStateException("Could not extract schedule information from image")
        }

        // Remove common LLM artifacts from the response
        var cleanedText = rawResponse
            .replace("<!-- tools: image_gen -->", "")
            .replace(Regex("```.*?```", RegexOption.DOT_MATCHES_ALL), "")
            .trim()

        // If the text is too short or looks like an error message, reject it
        if (cleanedText.length < 5) {
            throw IllegalStateException("Extracted text too short: $cleanedText")
        }

        android.util.Log.d("ImportDraftVM", "Extracted text: $cleanedText")
        return sanitizeText(cleanedText)
    }

    /**
     * Converts content:// Uri to temporary File for upload.
     */
    private fun uriToFile(uri: Uri): File {
        val contentResolver = context.contentResolver
        val tempFile = File(context.cacheDir, "share_${System.currentTimeMillis()}.jpg")

        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Failed to read image from Uri")

        return tempFile
    }

    /**
     * Saves the main plan and selected alternatives.
     * Reuses logic from TodayViewModel.confirmLLMPlanAndSave().
     * Alternatives are now full NormalizedPlan objects with potentially edited date/time.
     */
    fun savePlans(mainPlan: NormalizedPlan, alternatives: List<NormalizedPlan>) {
        viewModelScope.launch {
            try {
                // Check if this is a one-time event
                val isOneTime = mainPlan.recurrence.kind.uppercase() == "NONE" && mainPlan.specificDate != null

                // Save main plan
                if (isOneTime) {
                    // One-time event: create a single PlanItemEntity directly
                    val hour = mainPlan.time.fixedHour ?: 9
                    val minute = mainPlan.time.fixedMinute ?: 0
                    val durationMinutes = mainPlan.time.duration

                    planRepository.addOneTimeEvent(
                        title = mainPlan.title,
                        date = mainPlan.specificDate!!,
                        startHour = hour,
                        startMinute = minute,
                        durationMinutes = durationMinutes,
                        iconEmoji = mainPlan.iconEmoji
                    )

                    android.util.Log.d("ImportDraftVM", "Created one-time event: ${mainPlan.title} on ${mainPlan.specificDate} at $hour:$minute")
                } else {
                    // Recurring plan: create a series
                    val mainSeries = convertToPlanSeries(mainPlan, false)
                    planRepository.addSeries(mainSeries)
                    expandSeriesForCurrentWeek(mainSeries)

                    android.util.Log.d("ImportDraftVM", "Created recurring plan series: ${mainPlan.title}")
                }

                // Save alternatives - now using full NormalizedPlan (may have edited date/time)
                alternatives.forEach { altPlan ->
                    val altIsOneTime = altPlan.recurrence.kind.uppercase() == "NONE" && altPlan.specificDate != null

                    if (altIsOneTime) {
                        // One-time event for alternative
                        val hour = altPlan.time.fixedHour ?: 9
                        val minute = altPlan.time.fixedMinute ?: 0
                        val durationMinutes = altPlan.time.duration

                        planRepository.addOneTimeEvent(
                            title = altPlan.title,
                            date = altPlan.specificDate!!,
                            startHour = hour,
                            startMinute = minute,
                            durationMinutes = durationMinutes,
                            iconEmoji = altPlan.iconEmoji
                        )

                        android.util.Log.d("ImportDraftVM", "Created alternative one-time event: ${altPlan.title} on ${altPlan.specificDate} at $hour:$minute")
                    } else {
                        // Recurring plan for alternative
                        val altSeries = convertToPlanSeries(altPlan, false)
                        planRepository.addSeries(altSeries)
                        expandSeriesForCurrentWeek(altSeries)

                        android.util.Log.d("ImportDraftVM", "Created alternative recurring plan: ${altPlan.title}")
                    }
                }

                // Success handled by caller (onSuccess callback)
            } catch (e: Exception) {
                android.util.Log.e("ImportDraftVM", "Error saving plan", e)
                _uiState.value = ImportDraftUiState.Error(
                    "Failed to save plan: ${e.message}"
                )
            }
        }
    }

    /**
     * Converts NormalizedPlan to PlanSeriesEntity.
     * Copied from TodayViewModel.confirmLLMPlanAndSave().
     * @param isOneTime If true, the series will be marked as archived (for one-time tasks)
     */
    private fun convertToPlanSeries(plan: NormalizedPlan, isOneTime: Boolean = false): PlanSeriesEntity {
        val anchor = when (plan.time.anchor.uppercase()) {
            "WAKE" -> TimeAnchor.WAKE
            "BED" -> TimeAnchor.BED
            "FIXED" -> TimeAnchor.FIXED
            else -> TimeAnchor.FIXED
        }

        val planType = try {
            PlanType.valueOf(plan.planType.uppercase())
        } catch (e: Exception) {
            PlanType.TASK
        }

        val routineType = plan.routineType?.let {
            try {
                com.jnkim.poschedule.domain.model.RoutineType.valueOf(it.uppercase())
            } catch (e: Exception) {
                null
            }
        }

        val frequency = when (plan.recurrence.kind.uppercase()) {
            "DAILY" -> RecurrenceFrequency.DAILY
            "WEEKLY" -> RecurrenceFrequency.WEEKLY
            "WEEKDAYS" -> RecurrenceFrequency.WEEKLY
            "MONTHLY" -> RecurrenceFrequency.MONTHLY
            else -> RecurrenceFrequency.DAILY
        }

        val byDays = when {
            plan.recurrence.kind.uppercase() == "WEEKDAYS" -> "1,2,3,4,5"
            plan.recurrence.weekdays != null ->
                plan.recurrence.weekdays.joinToString(",")
            else -> null
        }

        val startOffset = when (anchor) {
            TimeAnchor.FIXED -> {
                val hour = plan.time.fixedHour ?: 9
                val minute = plan.time.fixedMinute ?: 0
                hour * 60 + minute
            }
            else -> plan.time.offset ?: 0
        }

        val endOffset = startOffset + plan.time.duration

        return PlanSeriesEntity(
            id = UUID.randomUUID().toString(),
            title = plan.title,
            type = planType,
            routineType = routineType,
            iconEmoji = plan.iconEmoji,
            isCore = false,
            anchor = anchor,
            startOffsetMin = startOffset,
            endOffsetMin = endOffset,
            frequency = frequency,
            interval = 1,
            byDays = byDays,
            isActive = true,
            archived = isOneTime  // Archive one-time tasks immediately
        )
    }

    /**
     * Expands series for current week.
     * Copied from TodayViewModel.expandSeriesForCurrentWeek().
     */
    private suspend fun expandSeriesForCurrentWeek(series: PlanSeriesEntity) {
        val today = LocalDate.now()
        for (offset in -1..7) {
            val targetDate = today.plusDays(offset.toLong())
            val dateStr = targetDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

            if (planRepository.getPlanDay(dateStr) == null) {
                planRepository.insertPlanDay(dateStr, com.jnkim.poschedule.domain.model.Mode.NORMAL)
            }

            recurrenceEngine.expand(series, targetDate)?.let { instance ->
                planRepository.insertPlanItem(instance)
            }
        }
    }

}
