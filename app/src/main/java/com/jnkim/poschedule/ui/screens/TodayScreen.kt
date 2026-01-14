package com.jnkim.poschedule.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.jnkim.poschedule.R
import com.jnkim.poschedule.data.local.entity.*
import com.jnkim.poschedule.domain.model.Mode
import com.jnkim.poschedule.domain.model.RoutineType
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

@Composable
fun TodayScreen(
    viewModel: TodayViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToTidySnap: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDate>(LocalDate.now()) }
    var zoomLevel by remember { mutableStateOf<CalendarZoom>(CalendarZoom.DAY) }
    var selectedItemForActions by remember { mutableStateOf<PlanItemEntity?>(null) }

    LaunchedEffect(selectedDate) {
        viewModel.onDateSelected(selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
    }

    TodayContent(
        uiState = uiState,
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
            onSave = { title, planType, anchor, frequency, isCore, start, end, byDays ->
                viewModel.addPlanSeries(title, planType, anchor, frequency, isCore, start, end, byDays)
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

    // Track drag state for manual gestures
    var dragOffset by remember { mutableStateOf(0f) }
    val dragThreshold = 100f

    // NestedScrollConnection to intercept scroll gestures when Week/Month view is open
    val nestedScrollConnection = remember(zoomLevel) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Only intercept when Week/Month view is open
                if (zoomLevel == CalendarZoom.DAY) return Offset.Zero

                // Accumulate vertical scroll
                dragOffset += available.y

                // Check if threshold is reached
                when {
                    // Scrolling up (negative offset) - close the view
                    dragOffset < -dragThreshold -> {
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
        dragOffset += delta
    }

    // Gesture modifier for header (opens Week view from DAY mode)
    val headerGestureModifier = Modifier.draggable(
        state = draggableState,
        orientation = Orientation.Vertical,
        onDragStopped = {
            if (zoomLevel == CalendarZoom.DAY && dragOffset > dragThreshold) {
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
            when {
                // Swipe down - expand or do nothing
                dragOffset > dragThreshold -> {
                    when (zoomLevel) {
                        CalendarZoom.WEEK -> onZoomChange(CalendarZoom.MONTH)
                        else -> {}
                    }
                }
                // Swipe up - collapse
                dragOffset < -dragThreshold -> {
                    when (zoomLevel) {
                        CalendarZoom.MONTH -> onZoomChange(CalendarZoom.WEEK)
                        CalendarZoom.WEEK -> onZoomChange(CalendarZoom.DAY)
                        else -> {}
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
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
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
                        Column(modifier = overviewGestureModifier) {
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
                            Box(modifier = headerGestureModifier) {
                                HorizontalDateSelector(selectedDate, onDateSelected, accentColor)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(nestedScrollConnection),
                            contentPadding = PaddingValues(bottom = 120.dp)
                        ) {
                            item {
                                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                                    SystemStateBubble(uiState.mode, uiState.systemMessageTitle, uiState.systemMessageBody)
                                }
                                Spacer(modifier = Modifier.height(32.dp))
                            }
                            items(uiState.planItems, key = { it.id }) { item ->
                                val timeStr = if (item.startTimeMillis != null) {
                                    Instant.ofEpochMilli(item.startTimeMillis)
                                        .atZone(ZoneId.systemDefault())
                                        .format(DateTimeFormatter.ofPattern("HH:mm"))
                                } else item.window.name.lowercase().take(3).capitalize()

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
    onSave: (String, PlanType, TimeAnchor, RecurrenceFrequency, Boolean, Int, Int, String?) -> Unit
) {
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

    ModalBottomSheet(onDismissRequest = onDismiss) {
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

            // Anchor Type
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

            // Plan Type
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

            // Frequency
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

            // Weekly day picker
            if (frequency == RecurrenceFrequency.WEEKLY) {
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

            // Core toggle
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.label_mark_core), modifier = Modifier.weight(1f))
                    Switch(checked = isCore, onCheckedChange = { isCore = it })
                }
            }

            // Save button
            item {
                Button(
                    onClick = {
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
                        onSave(title, planType, anchor, frequency, isCore, finalStart, finalEnd, byDays)
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
        text = content
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
    val scale by animateFloatAsState(if (completed) 0.98f else 1f)
    val alpha by animateFloatAsState(if (completed) 0.6f else 1f)
    val haptics = LocalHapticFeedback.current

    val glowAlpha by animateFloatAsState(
        targetValue = if (completed) 0f else 0.3f,
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
                if (!completed && item.isCore) {
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
                Text(text = item.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            if (item.isCore) {
                Surface(color = accentColor.copy(alpha = 0.2f), shape = CircleShape) {
                    Text(text = stringResource(R.string.label_core), modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
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
