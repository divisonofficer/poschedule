package com.jnkim.poschedule.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.*
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.jnkim.poschedule.R
import com.jnkim.poschedule.data.local.entity.*
import com.jnkim.poschedule.data.repo.SettingsRepository
import com.jnkim.poschedule.domain.model.DayPhase
import com.jnkim.poschedule.domain.model.Mode
import com.jnkim.poschedule.domain.model.RoutineType
import com.jnkim.poschedule.domain.model.WeatherState
import com.jnkim.poschedule.ui.components.*
import com.jnkim.poschedule.ui.theme.*
import com.jnkim.poschedule.ui.viewmodel.LLMNormalizerState
import com.jnkim.poschedule.ui.viewmodel.TodayUiState
import com.jnkim.poschedule.ui.viewmodel.TodayViewModel
import android.widget.Toast
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.jnkim.poschedule.workers.DailyPlanWorker
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*
import kotlin.math.abs

enum class CalendarZoom {
    DAY, WEEK, MONTH
}

/**
 * Returns an icon representing the current day phase.
 */
private fun getDayPhaseIcon(phase: DayPhase): androidx.compose.ui.graphics.vector.ImageVector {
    return when (phase) {
        DayPhase.DAWN -> Icons.Default.WbTwilight      // 새벽 - 황혼 아이콘
        DayPhase.MORNING -> Icons.Default.WbSunny      // 아침 - 해
        DayPhase.NOON -> Icons.Default.LightMode       // 낮 - 밝은 태양
        DayPhase.AFTERNOON -> Icons.Default.WbSunny    // 오후 - 해
        DayPhase.EVENING -> Icons.Default.WbTwilight   // 저녁 - 황혼
        DayPhase.NIGHT -> Icons.Default.Nightlight     // 밤 - 달
    }
}

/**
 * Returns an icon representing the weather state.
 */
private fun getWeatherIcon(weather: WeatherState): androidx.compose.ui.graphics.vector.ImageVector {
    return when (weather) {
        WeatherState.CLEAR -> Icons.Default.WbSunny       // 맑음
        WeatherState.CLOUDY -> Icons.Default.Cloud        // 흐림
        WeatherState.RAIN -> Icons.Default.WaterDrop      // 비
        WeatherState.SNOW -> Icons.Default.AcUnit         // 눈
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    viewModel: TodayViewModel,
    settingsRepository: SettingsRepository,
    onNavigateToSettings: () -> Unit,
    onNavigateToTidySnap: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by settingsRepository.settingsFlow.collectAsState(initial = null)
    val llmState by viewModel.llmNormalizerState.collectAsState()
    val fabMenuExpanded by viewModel.fabMenuExpanded.collectAsState()

    var showAddSheet by remember { mutableStateOf(false) }
    var showLLMInput by remember { mutableStateOf(false) }
    var showFabMenu by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDate>(LocalDate.now()) }
    var zoomLevel by remember { mutableStateOf<CalendarZoom>(CalendarZoom.DAY) }
    var selectedItemForActions by remember { mutableStateOf<PlanItemEntity?>(null) }

    var showClipboardPrompt by remember { mutableStateOf(false) }
    var clipboardText by remember { mutableStateOf("") }
    var clipboardChecked by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Check clipboard on first composition
    LaunchedEffect(Unit) {
        if (!clipboardChecked) {
            clipboardChecked = true
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clipData = clipboard?.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val text = clipData.getItemAt(0).text?.toString()
                if (!text.isNullOrBlank() && text.length > 10 && text.length < 1000) {
                    // Only prompt for meaningful text (10-1000 chars)
                    clipboardText = text
                    showClipboardPrompt = true
                }
            }
        }
    }

    LaunchedEffect(selectedDate) {
        viewModel.onDateSelected(selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
    }

    TodayContent(
        viewModel = viewModel,
        uiState = uiState,
        settings = settings,
        selectedDate = selectedDate,
        zoomLevel = zoomLevel,
        onZoomChange = { zoomLevel = it },
        onDateSelected = { selectedDate = it },
        onPlanItemChecked = { id, checked -> viewModel.onPlanItemChecked(id, checked) },
        onPlanItemLongPress = { item -> selectedItemForActions = item },
        onPlanItemSnooze = { id -> viewModel.snoozeItem(id) },
        onPlanItemSkip = { id -> viewModel.skipItem(id) },
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToTidySnap = onNavigateToTidySnap,
        onNaturalLanguageClick = { showLLMInput = true },
        onClassicFormClick = { showAddSheet = true }
    )

    // FAB Menu (Natural Language vs Classic Form)
    // Scrim for FAB menu
    if (showFabMenu) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    showFabMenu = false
                }
        )
    }

    if (showAddSheet) {
        PlanEditorSheet(
            onDismiss = { showAddSheet = false },
            onSaveRecurring = { title, planType, anchor, frequency, isCore, start, end, byDays ->
                viewModel.addPlanSeries(title, planType, anchor, frequency, isCore, start, end, byDays)
                showAddSheet = false
            },
            onSaveOneTime = { title, date, startHour, startMinute, durationMinutes ->
                viewModel.addOneTimeEvent(title, date, startHour, startMinute, durationMinutes)
                showAddSheet = false
            }
        )
    }

    selectedItemForActions?.let { item ->
        PlanActionBottomSheet(
            item = item,
            onDismiss = { selectedItemForActions = null },
            onSnooze = { id -> viewModel.snoozeItem(id) },
            onSkip = { id -> viewModel.skipItem(id) },
            onDelete = { id ->
                if (item.seriesId != null) {
                    viewModel.removeOccurrence(item.seriesId!!)
                } else {
                    viewModel.deletePlanItem(id)
                }
            },
            onStopSeries = { seriesId -> viewModel.stopSeries(seriesId) }
        )
    }

    // Clipboard Prompt Dialog
    if (showClipboardPrompt) {
        AlertDialog(
            onDismissRequest = { showClipboardPrompt = false },
            title = { Text("클립보드에서 만들까요?") },
            text = {
                Column {
                    Text(
                        text = "클립보드에 다음 내용이 있습니다:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    GlassCard(
                        modifier = Modifier.heightIn(max = 150.dp)
                    ) {
                        Text(
                            text = clipboardText.take(200) + if (clipboardText.length > 200) "..." else "",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClipboardPrompt = false
                        viewModel.normalizeTaskWithLLM(clipboardText)
                        showLLMInput = true
                    }
                ) {
                    Text("만들기")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClipboardPrompt = false }) {
                    Text("취소")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }

    // LLM Input Flow
    if (showLLMInput) {
        GlassBottomSheet(
            onDismissRequest = {
                showLLMInput = false
                viewModel.resetLLMState()
            }
        ) {
            when (llmState) {
                is LLMNormalizerState.Idle -> {
                    LLMTaskAddSheet(
                        onDismiss = {
                            showLLMInput = false
                            viewModel.resetLLMState()
                        },
                        onSubmit = { userInput ->
                            viewModel.normalizeTaskWithLLM(userInput)
                        },
                        isLoading = false
                    )
                }
                is LLMNormalizerState.Loading -> {
                    LLMTaskAddSheet(
                        onDismiss = { /* Block dismissal during loading */ },
                        onSubmit = { /* No-op during loading */ },
                        isLoading = true
                    )
                }
                is LLMNormalizerState.Success -> {
                    val response = (llmState as LLMNormalizerState.Success).response
                    response.plan?.let { plan ->
                        PlanReviewSheet(
                            normalizedPlan = plan,
                            confidence = response.confidence,
                            alternatives = response.alternatives,
                            onConfirm = { modifiedPlan, selectedAlternatives ->
                                // Save main plan (with possible date/time modifications)
                                viewModel.confirmLLMPlanAndSave(modifiedPlan)

                                // Save selected alternatives (now full NormalizedPlan with edited date/time)
                                selectedAlternatives.forEach { altPlan ->
                                    viewModel.confirmLLMPlanAndSave(altPlan)
                                }

                                showLLMInput = false
                            },
                            onEdit = {
                                showLLMInput = false
                                viewModel.resetLLMState()
                                showAddSheet = true
                            },
                            onCancel = {
                                showLLMInput = false
                                viewModel.resetLLMState()
                            }
                        )
                    }
                }
                is LLMNormalizerState.Clarification -> {
                    // For MVP: Fall back to classic form with toast message
                    LaunchedEffect(Unit) {
                        Toast.makeText(
                            context,
                            "Could not understand input clearly. Please try the classic form.",
                            Toast.LENGTH_SHORT
                        ).show()
                        showLLMInput = false
                        viewModel.resetLLMState()
                        showAddSheet = true
                    }
                }
                is LLMNormalizerState.Error -> {
                    val errorMessage = (llmState as LLMNormalizerState.Error).message
                    LaunchedEffect(Unit) {
                        Toast.makeText(
                            context,
                            "Error: $errorMessage",
                            Toast.LENGTH_SHORT
                        ).show()
                        showLLMInput = false
                        viewModel.resetLLMState()
                        showAddSheet = true
                    }
                }
            }
        }
    }
}

@Composable
fun TodayContent(
    viewModel: TodayViewModel,
    uiState: TodayUiState,
    settings: com.jnkim.poschedule.data.repo.UserSettings?,
    selectedDate: LocalDate,
    zoomLevel: CalendarZoom,
    onZoomChange: (CalendarZoom) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onPlanItemChecked: (String, Boolean) -> Unit,
    onPlanItemLongPress: (PlanItemEntity) -> Unit,
    onPlanItemSnooze: (String) -> Unit,
    onPlanItemSkip: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTidySnap: () -> Unit,
    onNaturalLanguageClick: () -> Unit,
    onClassicFormClick: () -> Unit
) {
    val fabMenuExpanded by viewModel.fabMenuExpanded.collectAsState()
    val context = LocalContext.current
    val accentColor by animateColorAsState(
        targetValue = when (uiState.mode) {
            Mode.NORMAL -> ModeNormal
            Mode.RECOVERY -> ModeRecovery
            Mode.LOW_MOOD -> ModeLowMood
            Mode.BUSY -> ModeBusy
        },
        animationSpec = tween(durationMillis = 400)
    )

    val listState = rememberLazyListState()

    Scaffold(
        floatingActionButton = {
            if (zoomLevel == CalendarZoom.DAY) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Expandable menu items
                    androidx.compose.animation.AnimatedVisibility(
                        visible = fabMenuExpanded,
                        enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Natural Language FAB
                            Row(
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                androidx.compose.material3.Surface(
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(DesignTokens.Radius.md),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = DesignTokens.Alpha.glassDefault),
                                    tonalElevation = DesignTokens.Layer.surfaceElevation,
                                    modifier = Modifier.padding(end = 8.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = DesignTokens.Alpha.borderDefault))
                                ) {
                                    Text(
                                        text = "Natural Language",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                SmallFloatingActionButton(
                                    onClick = {
                                        viewModel.setFabMenuExpanded(false)
                                        onNaturalLanguageClick()
                                    },
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    shape = CircleShape,
                                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
                                ) {
                                    Icon(Icons.Default.ChatBubble, contentDescription = "Natural Language")
                                }
                            }

                            // Classic Form FAB
                            Row(
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                androidx.compose.material3.Surface(
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(DesignTokens.Radius.md),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = DesignTokens.Alpha.glassDefault),
                                    tonalElevation = DesignTokens.Layer.surfaceElevation,
                                    modifier = Modifier.padding(end = 8.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = DesignTokens.Alpha.borderDefault))
                                ) {
                                    Text(
                                        text = "Classic Form",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                SmallFloatingActionButton(
                                    onClick = {
                                        viewModel.setFabMenuExpanded(false)
                                        onClassicFormClick()
                                    },
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    shape = CircleShape,
                                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Classic Form")
                                }
                            }

                            // Tidy Camera FAB
                            Row(
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                androidx.compose.material3.Surface(
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(DesignTokens.Radius.md),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = DesignTokens.Alpha.glassDefault),
                                    tonalElevation = DesignTokens.Layer.surfaceElevation,
                                    modifier = Modifier.padding(end = 8.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = DesignTokens.Alpha.borderDefault))
                                ) {
                                    Text(
                                        text = "Tidy Camera",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                SmallFloatingActionButton(
                                    onClick = {
                                        viewModel.setFabMenuExpanded(false)
                                        onNavigateToTidySnap()
                                    },
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    shape = CircleShape,
                                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = stringResource(R.string.title_tidy_snap))
                                }
                            }
                        }
                    }

                    // Main FAB
                    FloatingActionButton(
                        onClick = { viewModel.toggleFabMenu() },
                        containerColor = accentColor.copy(alpha = 0.8f),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                            focusedElevation = 0.dp,
                            hoveredElevation = 0.dp
                        )
                    ) {
                        androidx.compose.animation.AnimatedContent(
                            targetState = fabMenuExpanded,
                            label = "FAB icon animation"
                        ) { expanded ->
                            Icon(
                                imageVector = if (expanded) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = if (expanded) "Close menu" else stringResource(R.string.title_create_routine)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        GlassBackground(accentColor = accentColor) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .statusBarsPadding()
                    .pointerInput(zoomLevel) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var totalDragY = 0f

                            Log.d("TodayGesture", "Touch down at ${down.position}, zoomLevel=$zoomLevel")

                            do {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break

                                if (change.pressed) {
                                    val deltaY = change.position.y - change.previousPosition.y
                                    totalDragY += deltaY
                                }
                            } while (change.pressed)

                            Log.d("TodayGesture", "Touch up, totalDragY=$totalDragY, zoomLevel=$zoomLevel")

                            // Threshold: 120px
                            when {
                                totalDragY < -120f -> {
                                    Log.d("TodayGesture", "Swipe up detected")
                                    when (zoomLevel) {
                                        CalendarZoom.MONTH -> onZoomChange(CalendarZoom.WEEK)
                                        CalendarZoom.WEEK -> onZoomChange(CalendarZoom.DAY)
                                        else -> {}
                                    }
                                }
                                totalDragY > 120f -> {
                                    Log.d("TodayGesture", "Swipe down detected")
                                    when (zoomLevel) {
                                        CalendarZoom.DAY -> onZoomChange(CalendarZoom.WEEK)
                                        CalendarZoom.WEEK -> onZoomChange(CalendarZoom.MONTH)
                                        else -> {}
                                    }
                                }
                            }
                        }
                    }
            ) {
                // Header (Today / Date Title)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val titleText = when (zoomLevel) {
                        CalendarZoom.DAY -> {
                            if (selectedDate == LocalDate.now()) {
                                stringResource(R.string.title_today)
                            } else {
                                val locale = Locale.getDefault()
                                val pattern = if (locale.language == "ko") "M월 d일" else "MMM dd"
                                selectedDate.format(DateTimeFormatter.ofPattern(pattern, locale))
                            }
                        }
                        CalendarZoom.WEEK -> "Week Overview"
                        CalendarZoom.MONTH -> "Month Heatmap"
                    }

                    // Show time & weather icons when viewing today in TIME_ADAPTIVE mode
                    val showIcons = zoomLevel == CalendarZoom.DAY &&
                                   selectedDate == LocalDate.now() &&
                                   settings?.themeMode == "TIME_ADAPTIVE"

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (showIcons) {
                            // Current day phase icon
                            val currentPhase = DayPhase.fromCurrentTime()
                            Icon(
                                imageVector = getDayPhaseIcon(currentPhase),
                                contentDescription = "Current time: ${currentPhase.name}",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )

                            // Weather icon (if enabled)
                            if (settings?.weatherEffectsEnabled == true) {
                                val currentWeather = try {
                                    WeatherState.valueOf(settings.manualWeatherState)
                                } catch (e: IllegalArgumentException) {
                                    WeatherState.CLEAR
                                }
                                Icon(
                                    imageVector = getWeatherIcon(currentWeather),
                                    contentDescription = "Weather: ${currentWeather.name}",
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    Row {
                        IconButton(onClick = {
                            val workRequest = OneTimeWorkRequestBuilder<DailyPlanWorker>().build()
                            WorkManager.getInstance(context).enqueue(workRequest)
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.action_refresh), tint = MaterialTheme.colorScheme.onSurface)
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.title_settings), tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Week/Month Overview (collapsible at top)
                    AnimatedVisibility(
                        visible = zoomLevel == CalendarZoom.WEEK || zoomLevel == CalendarZoom.MONTH,
                        enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeIn(),
                        exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeOut()
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                when (zoomLevel) {
                                    CalendarZoom.WEEK -> {
                                        WeekOverviewGrid(
                                            weekStart = selectedDate.minusDays(selectedDate.dayOfWeek.value.toLong() % 7),
                                            itemsByDay = uiState.weekDensity,
                                            onDaySelected = { onDateSelected(it); onZoomChange(CalendarZoom.DAY) },
                                            accentColor = accentColor
                                        )
                                    }
                                    CalendarZoom.MONTH -> {
                                        MonthViewHeatmap(
                                            currentMonth = YearMonth.from(selectedDate),
                                            itemsByDay = uiState.monthDensity,
                                            onDaySelected = { onDateSelected(it); onZoomChange(CalendarZoom.DAY) },
                                            accentColor = accentColor
                                        )
                                    }
                                    else -> {}
                                }
                            }
                        }
                    }

                    // Day View
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Only show date selector when in DAY mode
                        AnimatedVisibility(
                            visible = zoomLevel == CalendarZoom.DAY,
                            enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeIn(),
                            exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeOut()
                        ) {
                            HorizontalDateSelector(selectedDate, onDateSelected, accentColor)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 120.dp)
                        ) {
                            item {
                                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                                    SystemStateBubble(uiState.mode, uiState.systemMessageTitle, uiState.systemMessageBody)
                                }
                                Spacer(modifier = Modifier.height(32.dp))
                            }
                            items(uiState.planItems, key = { it.id }) { item ->
                                // Show adjusted time for snoozed items
                                val timeStr = when {
                                    item.status == "SNOOZED" && item.snoozeUntil != null -> {
                                        Instant.ofEpochMilli(item.snoozeUntil)
                                            .atZone(ZoneId.systemDefault())
                                            .format(DateTimeFormatter.ofPattern("HH:mm"))
                                    }
                                    item.startTimeMillis != null -> {
                                        Instant.ofEpochMilli(item.startTimeMillis)
                                            .atZone(ZoneId.systemDefault())
                                            .format(DateTimeFormatter.ofPattern("HH:mm"))
                                    }
                                    else -> item.window.name.lowercase().take(3).capitalize()
                                }

                                val isPast = remember(item, selectedDate) {
                                    val nowLocalDate = LocalDate.now()
                                    val currentTimeMillis = System.currentTimeMillis()

                                    when {
                                        selectedDate.isBefore(nowLocalDate) -> true
                                        selectedDate.isAfter(nowLocalDate) -> false
                                        else -> {
                                            val result = item.endTimeMillis != null && currentTimeMillis > item.endTimeMillis

                                            // Debug logging for timezone issue diagnosis
                                            if (item.startTimeMillis != null && item.endTimeMillis != null) {
                                                val startTime = Instant.ofEpochMilli(item.startTimeMillis)
                                                    .atZone(ZoneId.systemDefault())
                                                    .format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                                                val endTime = Instant.ofEpochMilli(item.endTimeMillis)
                                                    .atZone(ZoneId.systemDefault())
                                                    .format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                                                val now = Instant.ofEpochMilli(currentTimeMillis)
                                                    .atZone(ZoneId.systemDefault())
                                                    .format(DateTimeFormatter.ofPattern("HH:mm:ss"))

                                                android.util.Log.d("TodayScreen",
                                                    "isPast check: ${item.title} | start=$startTime, end=$endTime, now=$now | " +
                                                    "currentMillis=$currentTimeMillis, endMillis=${item.endTimeMillis} | " +
                                                    "isPast=$result (now > end = ${currentTimeMillis > item.endTimeMillis})")
                                            }
                                            result
                                        }
                                    }
                                }

                                TimelineNode(time = timeStr, isPast = isPast, accentColor = accentColor) {
                                    SwipeablePlanItem(
                                        item = item,
                                        onCheckedChange = onPlanItemChecked,
                                        onLongPress = onPlanItemLongPress,
                                        onSnooze = onPlanItemSnooze,
                                        onSkip = onPlanItemSkip,
                                        accentColor = accentColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HorizontalDateSelector(selectedDate: LocalDate, onDateSelected: (LocalDate) -> Unit, accentColor: Color) {
    val dates = remember { (-7..14).map { LocalDate.now().plusDays(it.toLong()) } }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = 4)
    LazyRow(state = listState, contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        items(dates) { date ->
            val isSelected = date == selectedDate
            val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            val dayNumber = date.dayOfMonth.toString()
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(MaterialTheme.shapes.medium).background(if (isSelected) accentColor.copy(alpha = 0.2f) else Color.Transparent).clickable { onDateSelected(date) }.padding(horizontal = 12.dp, vertical = 8.dp).width(40.dp)) {
                Text(text = dayName, style = MaterialTheme.typography.labelSmall, color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Text(text = dayNumber, style = MaterialTheme.typography.titleMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanEditorSheet(
    onDismiss: () -> Unit,
    onSaveRecurring: (String, PlanType, TimeAnchor, RecurrenceFrequency, Boolean, Int, Int, String?) -> Unit,
    onSaveOneTime: (String, String, Int, Int, Int) -> Unit
) {
    // Event type toggle
    var isRecurring by remember { mutableStateOf(true) }

    var title by remember { mutableStateOf("") }
    var planType by remember { mutableStateOf(PlanType.ROUTINE) }
    var anchor by remember { mutableStateOf(TimeAnchor.FIXED) }
    var frequency by remember { mutableStateOf(RecurrenceFrequency.DAILY) }
    var isCore by remember { mutableStateOf(true) }

    // Day Picker State (Mon=1 ... Sun=7)
    var selectedDays by remember { mutableStateOf(setOf(1, 2, 3, 4, 5, 6, 7)) }

    // Time Picker State
    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(initialHour = 9, initialMinute = 0)
    var durationMin by remember { mutableStateOf(30) }

    // Date picker for one-time events
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    )

    GlassBottomSheet(
        onDismissRequest = onDismiss
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    stringResource(R.string.title_create_routine),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Event Type Toggle
            item {
                GlassSegmentedControl(
                    options = listOf("Recurring", "One-time"),
                    selectedIndex = if (isRecurring) 0 else 1,
                    onSelectionChange = { index ->
                        isRecurring = (index == 0)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Title
            item {
                GlassTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = stringResource(R.string.label_what_is_it),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Date Selection (only for one-time events)
            if (!isRecurring) {
                item {
                    Text("Date", style = MaterialTheme.typography.titleMedium)
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null)
                            Spacer(Modifier.width(16.dp))
                            Text(
                                text = selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }

            // Time Selection
            item {
                Text("Time", style = MaterialTheme.typography.titleMedium)
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTimePicker = true }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null)
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // Anchor Type (only for recurring events)
            if (isRecurring) {
                item {
                    Text(stringResource(R.string.label_anchor), style = MaterialTheme.typography.titleMedium)
                    GlassSegmentedControl(
                        options = TimeAnchor.values().map { it.name.lowercase().replaceFirstChar { it.uppercase() } },
                        selectedIndex = TimeAnchor.values().indexOf(anchor),
                        onSelectionChange = { index ->
                            anchor = TimeAnchor.values()[index]
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Descriptive text for selected anchor
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = when (anchor) {
                            TimeAnchor.FIXED -> "Absolute time every day (e.g., 09:00 means exactly 09:00)"
                            TimeAnchor.WAKE -> "Relative to wake time (default 08:00). Time shown is final time."
                            TimeAnchor.BED -> "Relative to bed time (default 23:00). Time shown is final time."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Plan Type (only for recurring events)
            if (isRecurring) {
                item {
                    Text(stringResource(R.string.label_plan_type), style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PlanType.values().forEach { type ->
                            GlassChip(
                                text = type.name.lowercase().replaceFirstChar { it.uppercase() },
                                isSelected = planType == type,
                                onClick = { planType = type }
                            )
                        }
                    }
                }
            }

            // Frequency (only for recurring events)
            if (isRecurring) {
                item {
                    Text(stringResource(R.string.label_frequency), style = MaterialTheme.typography.titleMedium)
                    GlassSegmentedControl(
                        options = RecurrenceFrequency.values().map { it.name.lowercase().replaceFirstChar { it.uppercase() } },
                        selectedIndex = RecurrenceFrequency.values().indexOf(frequency),
                        onSelectionChange = { index ->
                            frequency = RecurrenceFrequency.values()[index]
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Weekly day picker (only for recurring weekly events)
            if (isRecurring && frequency == RecurrenceFrequency.WEEKLY) {
                item {
                    DayChipRow(
                        selectedDays = selectedDays,
                        onDayToggle = { day ->
                            selectedDays = if (selectedDays.contains(day)) {
                                selectedDays - day
                            } else {
                                selectedDays + day
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Core toggle (only for recurring events)
            if (isRecurring) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.label_mark_core), modifier = Modifier.weight(1f))
                        GlassToggle(checked = isCore, onCheckedChange = { isCore = it })
                    }
                }
            }

            // Save button
            item {
                GlassButton(
                    text = stringResource(R.string.action_save_lifestyle),
                    onClick = {
                        if (isRecurring) {
                            // Calculate offset from anchor time
                            val selectedMinutes = timePickerState.hour * 60 + timePickerState.minute

                            // Convert absolute time to offset based on anchor type
                            // These values match the defaults in RecurrenceEngine
                            val finalStart = when (anchor) {
                                TimeAnchor.FIXED -> selectedMinutes // Offset from midnight (00:00)
                                TimeAnchor.WAKE -> selectedMinutes - (8 * 60) // Offset from wake (08:00)
                                TimeAnchor.BED -> selectedMinutes - (23 * 60) // Offset from bed (23:00)
                            }

                            val finalEnd = finalStart + durationMin
                            val byDays = if (frequency == RecurrenceFrequency.WEEKLY) selectedDays.sorted().joinToString(",") else null
                            onSaveRecurring(title, planType, anchor, frequency, isCore, finalStart, finalEnd, byDays)
                        } else {
                            // One-time event
                            val dateStr = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                            onSaveOneTime(title, dateStr, timePickerState.hour, timePickerState.minute, durationMin)
                        }
                    },
                    style = GlassButtonStyle.PRIMARY,
                    enabled = title.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = { TextButton(onClick = { showTimePicker = false }) { Text("OK") } }
        ) {
            TimePicker(state = timePickerState)
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        text = content,
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeablePlanItem(
    item: PlanItemEntity,
    onCheckedChange: (String, Boolean) -> Unit,
    onLongPress: (PlanItemEntity) -> Unit,
    onSnooze: (String) -> Unit,
    onSkip: (String) -> Unit,
    accentColor: Color
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onSnooze(item.id)
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onSkip(item.id)
                    false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            dismissState.reset()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val isDark = isSystemInDarkTheme()
            val direction = dismissState.dismissDirection

            val (bgColor, borderColor) = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (isDark) {
                        Color(0xFF1B3A2F).copy(alpha = 0.9f) to Color(0x3F4CAF50)
                    } else {
                        Color(0xFFE8F5E9).copy(alpha = 0.95f) to Color(0x594CAF50)
                    }
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    if (isDark) {
                        Color(0xFF3A2D1B).copy(alpha = 0.9f) to Color(0x3FFF9800)
                    } else {
                        Color(0xFFFFF3E0).copy(alpha = 0.95f) to Color(0x59FF9800)
                    }
                }
                else -> Color.Transparent to Color.Transparent
            }

            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }

            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.AccessTime
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.SkipNext
                else -> Icons.Default.Add
            }

            val iconColor = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Color(0xFF4CAF50)
                SwipeToDismissBoxValue.EndToStart -> Color(0xFFFF9800)
                else -> Color.Gray
            }

            val text = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> "Snooze"
                SwipeToDismissBoxValue.EndToStart -> "Skip"
                else -> ""
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = bgColor,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, borderColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentAlignment = alignment
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (direction == SwipeToDismissBoxValue.StartToEnd) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = text,
                                color = iconColor,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (direction == SwipeToDismissBoxValue.EndToStart) {
                            Text(
                                text = text,
                                color = iconColor,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    ) {
        PlanItemOrbCard(item, onCheckedChange, onLongPress, accentColor)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlanItemOrbCard(
    item: PlanItemEntity,
    onCheckedChange: (String, Boolean) -> Unit,
    onLongPress: (PlanItemEntity) -> Unit,
    accentColor: Color
) {
    val completed = item.status == "DONE"
    val skipped = item.status == "SKIPPED"
    val snoozed = item.status == "SNOOZED"

    // Check if item is overdue (past end time but not done)
    val isOverdue = !completed && !skipped && item.endTimeMillis != null &&
                    System.currentTimeMillis() > item.endTimeMillis

    val scale by animateFloatAsState(if (completed) 0.98f else 1f)
    val alpha by animateFloatAsState(
        targetValue = when {
            completed -> 0.6f
            skipped -> 0.5f
            else -> 1f
        }
    )
    val haptics = LocalHapticFeedback.current

    // Priority: LLM-generated emoji > RoutineType defaults > fallback
    val icon = if (!item.iconEmoji.isNullOrBlank()) {
        item.iconEmoji
    } else {
        when (item.type) {
            RoutineType.MEDS_AM, RoutineType.MEDS_PM -> "💊"
            RoutineType.MEAL -> "🍽"
            RoutineType.WIND_DOWN -> "🌙"
            RoutineType.MOVEMENT -> "🏃"
            RoutineType.STUDY -> "📖"
            RoutineType.CHORE -> "🧹"
            null -> if (item.source == PlanItemSource.MANUAL) "✨" else "📝"
        }
    }

    // Apply very subtle orange tint for overdue items
    val cardModifier = if (isOverdue) {
        Modifier
            .fillMaxWidth()
            .scale(scale)
            .alpha(alpha)
            .background(
                color = Color(0xFFFF9800).copy(alpha = 0.03f),
                shape = RoundedCornerShape(24.dp)
            )
    } else {
        Modifier
            .fillMaxWidth()
            .scale(scale)
            .alpha(alpha)
    }

    GlassCard(
        modifier = cardModifier
            .combinedClickable(
                onClick = { onCheckedChange(item.id, !completed) },
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPress(item)
                }
            )
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(text = icon, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (skipped) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                )
            }
            // Status badges
            when {
                completed -> {
                    Surface(
                        color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                        shape = CircleShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Done",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
                skipped -> {
                    Surface(
                        color = Color(0xFFFF9800).copy(alpha = 0.2f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = "Skipped",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFF9800)
                        )
                    }
                }
                snoozed -> {
                    Surface(
                        color = Color(0xFF2196F3).copy(alpha = 0.2f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = "+15min",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2196F3)
                        )
                    }
                }
                item.isCore -> {
                    Surface(color = accentColor.copy(alpha = 0.2f), shape = CircleShape) {
                        Text(
                            text = stringResource(R.string.label_core),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SystemStateBubble(mode: Mode, title: String, body: String) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val emoji = when (mode) {
                Mode.NORMAL -> "🌿"
                Mode.RECOVERY -> "🌊"
                Mode.LOW_MOOD -> "🍑"
                Mode.BUSY -> "🐝"
            }
            Text(text = emoji, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(text = body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
    }
}
