package com.jnkim.poschedule.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.jnkim.poschedule.R
import com.jnkim.poschedule.data.local.entity.PlanItemEntity
import com.jnkim.poschedule.data.local.entity.PlanItemSource
import com.jnkim.poschedule.data.local.entity.PlanItemWindow
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

enum class CalendarZoom { DAY, WEEK, MONTH }

@Composable
fun TodayScreen(
    viewModel: TodayViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToTidySnap: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var zoomLevel by remember { mutableStateOf(CalendarZoom.DAY) }
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
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToTidySnap = onNavigateToTidySnap,
        onAddClick = { showAddSheet = true }
    )

    if (showAddSheet) {
        PlanEditorSheet(
            onDismiss = { showAddSheet = false },
            onSave = { title, window ->
                viewModel.addManualPlanItem(title, window)
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
            onDelete = { id -> viewModel.deletePlanItem(id) }
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

    Scaffold(
        floatingActionButton = {
            if (zoomLevel == CalendarZoom.DAY) {
                Column(horizontalAlignment = Alignment.End) {
                    SmallFloatingActionButton(
                        onClick = onNavigateToTidySnap,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = androidx.compose.foundation.shape.CircleShape
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = stringResource(R.string.title_tidy_snap))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    FloatingActionButton(
                        onClick = onAddClick,
                        containerColor = accentColor.copy(alpha = 0.8f),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        shape = androidx.compose.foundation.shape.CircleShape
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.title_add_tiny_task))
                    }
                }
            }
        }
    ) { innerPadding ->
        GlassBackground(accentColor = accentColor) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            if (dragAmount.y > 50) {
                                if (zoomLevel == CalendarZoom.DAY) onZoomChange(CalendarZoom.WEEK)
                                else if (zoomLevel == CalendarZoom.WEEK) onZoomChange(CalendarZoom.MONTH)
                            }
                            if (dragAmount.y < -50) {
                                if (zoomLevel == CalendarZoom.MONTH) onZoomChange(CalendarZoom.WEEK)
                                else if (zoomLevel == CalendarZoom.WEEK) onZoomChange(CalendarZoom.DAY)
                            }
                        }
                    }
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (zoomLevel) {
                            CalendarZoom.DAY -> if (selectedDate == LocalDate.now()) stringResource(R.string.title_today) else selectedDate.format(DateTimeFormatter.ofPattern("MMM dd"))
                            CalendarZoom.WEEK -> "Week Overview"
                            CalendarZoom.MONTH -> "Month Heatmap"
                        },
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

                AnimatedContent(
                    targetState = zoomLevel,
                    transitionSpec = {
                        slideInVertically { if (targetState > initialState) -it else it } + fadeIn() togetherWith
                        slideOutVertically { if (targetState > initialState) it else -it } + fadeOut()
                    }
                ) { targetZoom ->
                    when (targetZoom) {
                        CalendarZoom.DAY -> {
                            Column {
                                HorizontalDateSelector(selectedDate, onDateSelected, accentColor)
                                Spacer(modifier = Modifier.height(16.dp))
                                LazyColumn(
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
                                        val timeStr = if (item.startTimeMillis != null) {
                                            Instant.ofEpochMilli(item.startTimeMillis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
                                        } else item.window.name.lowercase().take(3).capitalize()
                                        val isPast = item.endTimeMillis != null && System.currentTimeMillis() > item.endTimeMillis
                                        TimelineNode(time = timeStr, isPast = isPast, accentColor = accentColor) {
                                            PlanItemOrbCard(item, onPlanItemChecked, onPlanItemLongPress, accentColor)
                                        }
                                    }
                                }
                            }
                        }
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
fun PlanEditorSheet(onDismiss: () -> Unit, onSave: (String, PlanItemWindow) -> Unit) {
    var title by remember { mutableStateOf("") }
    var selectedWindow by remember { mutableStateOf(PlanItemWindow.ANYTIME) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(24.dp).fillMaxWidth().padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.title_add_tiny_task), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(stringResource(R.string.hint_tiny_task)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Text(stringResource(R.string.label_when), style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                PlanItemWindow.values().forEach { window ->
                    FilterChip(selected = selectedWindow == window, onClick = { selectedWindow = window }, label = { Text(window.name.lowercase().capitalize()) })
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { if (title.isNotBlank()) onSave(title, selectedWindow) }, modifier = Modifier.fillMaxWidth(), enabled = title.isNotBlank()) { Text(stringResource(R.string.action_add_task)) }
        }
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

    // Glow Animation for Batch 6
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
                Surface(color = accentColor.copy(alpha = 0.2f), shape = androidx.compose.foundation.shape.CircleShape) {
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
