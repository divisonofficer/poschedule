package com.jnkim.poschedule.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
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
import com.jnkim.poschedule.ui.viewmodel.TodayUiState
import com.jnkim.poschedule.ui.viewmodel.TodayViewModel
import com.jnkim.poschedule.workers.DailyPlanWorker
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

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

@Composable
fun TodayScreen(
    viewModel: TodayViewModel,
    settingsRepository: SettingsRepository,
    onNavigateToSettings: () -> Unit,
    onNavigateToTidySnap: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by settingsRepository.settingsFlow.collectAsState(initial = null)
    var showAddSheet by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDate>(LocalDate.now()) }
    var zoomLevel by remember { mutableStateOf<CalendarZoom>(CalendarZoom.DAY) }
    var selectedItemForActions by remember { mutableStateOf<PlanItemEntity?>(null) }

    LaunchedEffect(selectedDate) {
        viewModel.onDateSelected(selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
    }

    TodayContent(
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
        onAddClick = { showAddSheet = true }
    )

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
}

@Composable
fun TodayContent(
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
    onAddClick: () -> Unit
) {
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

    // Track animation state to prevent gesture conflicts
    var isAnimating by remember { mutableStateOf(false) }

    // Track drag state for manual gestures
    var dragOffset by remember { mutableStateOf(0f) }
    val dragThreshold = 100f

    // NestedScrollConnection to intercept scroll gestures when Week/Month view is open
    val nestedScrollConnection = remember(zoomLevel, isAnimating) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Block gestures during animation or when in DAY mode
                if (zoomLevel == CalendarZoom.DAY || isAnimating) return Offset.Zero

                // Accumulate vertical scroll
                dragOffset += available.y

                // Check if threshold is reached
                when {
                    // Scrolling up (negative offset) - close the view
                    dragOffset < -dragThreshold -> {
                        isAnimating = true
                        when (zoomLevel) {
                            CalendarZoom.MONTH -> onZoomChange(CalendarZoom.WEEK)
                            CalendarZoom.WEEK -> onZoomChange(CalendarZoom.DAY)
                            else -> {}
                        }
                        dragOffset = 0f
                        return available // Consume the scroll
                    }
                    // Scrolling down (positive offset) - expand the view
                    dragOffset > dragThreshold -> {
                        isAnimating = true
                        when (zoomLevel) {
                            CalendarZoom.WEEK -> onZoomChange(CalendarZoom.MONTH)
                            else -> {}
                        }
                        dragOffset = 0f
                        return available // Consume the scroll
                    }
                }

                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // Reset offset when scrolling stops
                if (available == Offset.Zero && consumed == Offset.Zero) {
                    dragOffset = 0f
                }
                return Offset.Zero
            }
        }
    }

    // Draggable state for header (DAY mode) and Week/Month overview
    val draggableState = rememberDraggableState { delta ->
        if (!isAnimating) {
            dragOffset += delta
        }
    }

    // Gesture modifier for header (opens Week view from DAY mode)
    val headerGestureModifier = Modifier.draggable(
        state = draggableState,
        orientation = Orientation.Vertical,
        onDragStopped = {
            if (!isAnimating && zoomLevel == CalendarZoom.DAY && dragOffset > dragThreshold) {
                isAnimating = true
                onZoomChange(CalendarZoom.WEEK)
            }
            dragOffset = 0f
        }
    )

    // Gesture modifier for Week/Month overview (expand/collapse)
    val overviewGestureModifier = Modifier.draggable(
        state = draggableState,
        orientation = Orientation.Vertical,
        onDragStopped = {
            if (!isAnimating) {
                when {
                    // Swipe down - expand or do nothing
                    dragOffset > dragThreshold -> {
                        isAnimating = true
                        when (zoomLevel) {
                            CalendarZoom.WEEK -> onZoomChange(CalendarZoom.MONTH)
                            else -> {}
                        }
                    }
                    // Swipe up - collapse
                    dragOffset < -dragThreshold -> {
                        isAnimating = true
                        when (zoomLevel) {
                            CalendarZoom.MONTH -> onZoomChange(CalendarZoom.WEEK)
                            CalendarZoom.WEEK -> onZoomChange(CalendarZoom.DAY)
                            else -> {}
                        }
                    }
                }
            }
            dragOffset = 0f
        }
    )

    Scaffold(
        floatingActionButton = {
            if (zoomLevel == CalendarZoom.DAY) {
                Column(horizontalAlignment = Alignment.End) {
                    SmallFloatingActionButton(
                        onClick = onNavigateToTidySnap,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = stringResource(R.string.title_tidy_snap))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    FloatingActionButton(
                        onClick = onAddClick,
                        containerColor = accentColor.copy(alpha = 0.8f),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.title_create_routine))
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
                    .nestedScroll(nestedScrollConnection)
            ) {
                // Header (Today / Date Title)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(headerGestureModifier)
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
                        // Reset animation flag when animation completes
                        LaunchedEffect(transition.currentState, transition.targetState) {
                            if (transition.currentState == transition.targetState) {
                                isAnimating = false
                            }
                        }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            when (zoomLevel) {
                                CalendarZoom.WEEK -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .pointerInput(isAnimating) {
                                                if (isAnimating) return@pointerInput

                                                var totalDragOffset = 0f

                                                detectDragGestures(
                                                    onDragStart = { totalDragOffset = 0f },
                                                    onDragEnd = {
                                                        if (totalDragOffset > dragThreshold) {
                                                            // Swipe down - expand to Month
                                                            isAnimating = true
                                                            onZoomChange(CalendarZoom.MONTH)
                                                        } else if (totalDragOffset < -dragThreshold) {
                                                            // Swipe up - collapse to Day
                                                            isAnimating = true
                                                            onZoomChange(CalendarZoom.DAY)
                                                        }
                                                        totalDragOffset = 0f
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        totalDragOffset += dragAmount.y
                                                        change.consume()
                                                    }
                                                )
                                            }
                                    ) {
                                        WeekOverviewGrid(
                                            weekStart = selectedDate.minusDays(selectedDate.dayOfWeek.value.toLong() % 7),
                                            itemsByDay = uiState.weekDensity,
                                            onDaySelected = { onDateSelected(it); onZoomChange(CalendarZoom.DAY) },
                                            accentColor = accentColor
                                        )
                                    }
                                }
                                CalendarZoom.MONTH -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .pointerInput(isAnimating) {
                                                if (isAnimating) return@pointerInput

                                                var totalDragOffset = 0f

                                                detectDragGestures(
                                                    onDragStart = { totalDragOffset = 0f },
                                                    onDragEnd = {
                                                        if (totalDragOffset < -dragThreshold) {
                                                            // Swipe up - collapse to Week
                                                            isAnimating = true
                                                            onZoomChange(CalendarZoom.WEEK)
                                                        }
                                                        totalDragOffset = 0f
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        totalDragOffset += dragAmount.y
                                                        change.consume()
                                                    }
                                                )
                                            }
                                    ) {
                                        MonthViewHeatmap(
                                            currentMonth = YearMonth.from(selectedDate),
                                            itemsByDay = uiState.monthDensity,
                                            onDaySelected = { onDateSelected(it); onZoomChange(CalendarZoom.DAY) },
                                            accentColor = accentColor
                                        )
                                    }
                                }
                                else -> {}
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
                            // Reset animation flag when animation completes
                            LaunchedEffect(transition.currentState, transition.targetState) {
                                if (transition.currentState == transition.targetState) {
                                    isAnimating = false
                                }
                            }

                            Box(modifier = headerGestureModifier) {
                                HorizontalDateSelector(selectedDate, onDateSelected, accentColor)
                            }
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isRecurring,
                        onClick = { isRecurring = true },
                        label = { Text("Recurring") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !isRecurring,
                        onClick = { isRecurring = false },
                        label = { Text("One-time") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Title
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.label_what_is_it)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Date Selection (only for one-time events)
            if (!isRecurring) {
                item {
                    Text("Date", style = MaterialTheme.typography.titleMedium)
                    OutlinedCard(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
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
                OutlinedCard(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TimeAnchor.values().forEach { a ->
                            FilterChip(
                                selected = anchor == a,
                                onClick = { anchor = a },
                                label = { Text(a.name.lowercase().replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }

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
                            FilterChip(
                                selected = planType == type,
                                onClick = { planType = type },
                                label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                }
            }

            // Frequency (only for recurring events)
            if (isRecurring) {
                item {
                    Text(stringResource(R.string.label_frequency), style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RecurrenceFrequency.values().forEach { f ->
                            FilterChip(
                                selected = frequency == f,
                                onClick = { frequency = f },
                                label = { Text(f.name.lowercase().replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                }
            }

            // Weekly day picker (only for recurring weekly events)
            if (isRecurring && frequency == RecurrenceFrequency.WEEKLY) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        (1..7).forEach { day ->
                            val dayChar = listOf("M", "T", "W", "T", "F", "S", "S")[day - 1]
                            val isSelected = selectedDays.contains(day)
                            Surface(
                                onClick = {
                                    selectedDays = if (isSelected) selectedDays - day else selectedDays + day
                                },
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = dayChar,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Core toggle (only for recurring events)
            if (isRecurring) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.label_mark_core), modifier = Modifier.weight(1f))
                        Switch(checked = isCore, onCheckedChange = { isCore = it })
                    }
                }
            }

            // Save button
            item {
                Button(
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
                    modifier = Modifier.fillMaxWidth(),
                    enabled = title.isNotBlank()
                ) {
                    Text(stringResource(R.string.action_save_lifestyle))
                }
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

    val scale by animateFloatAsState(if (completed) 0.98f else 1f)
    val alpha by animateFloatAsState(
        targetValue = when {
            completed -> 0.6f
            skipped -> 0.5f
            else -> 1f
        }
    )
    val haptics = LocalHapticFeedback.current

    val glowAlpha by animateFloatAsState(
        targetValue = if (completed || skipped) 0f else 0.3f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse)
    )

    val icon = when (item.type) {
        RoutineType.MEDS_AM, RoutineType.MEDS_PM -> "💊"
        RoutineType.MEAL -> "🍽"
        RoutineType.WIND_DOWN -> "🌙"
        RoutineType.MOVEMENT -> "🏃"
        RoutineType.STUDY -> "📖"
        RoutineType.CHORE -> "🧹"
        null -> if (item.source == PlanItemSource.MANUAL) "✨" else "📝"
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .alpha(alpha)
            .drawBehind {
                if (!completed && !skipped && item.isCore) {
                    drawCircle(
                        color = accentColor.copy(alpha = glowAlpha),
                        radius = size.minDimension / 1.5f,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
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
