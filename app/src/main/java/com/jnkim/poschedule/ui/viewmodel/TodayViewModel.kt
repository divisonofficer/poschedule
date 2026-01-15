package com.jnkim.poschedule.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jnkim.poschedule.data.local.entity.*
import com.jnkim.poschedule.data.repo.PlanRepository
import com.jnkim.poschedule.data.repo.SettingsRepository
import com.jnkim.poschedule.data.repo.UserSettings
import com.jnkim.poschedule.domain.ai.GentleCopyRequest
import com.jnkim.poschedule.domain.ai.GentleCopyUseCase
import com.jnkim.poschedule.domain.engine.ModeStateMachine
import com.jnkim.poschedule.domain.engine.RecurrenceEngine
import com.jnkim.poschedule.domain.model.Mode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

data class TodayUiState(
    val planItems: List<PlanItemEntity> = emptyList(),
    val weekDensity: Map<LocalDate, List<PlanItemEntity>> = emptyMap(),
    val monthDensity: Map<LocalDate, Int> = emptyMap(),
    val mode: Mode = Mode.NORMAL,
    val systemMessageTitle: String = "시스템이 안정적입니다",
    val systemMessageBody: String = "오늘도 당신의 속도대로 진행하세요",
    val userSettings: UserSettings? = null,
    val isLoading: Boolean = true
)

/**
 * State for LLM-based task normalization.
 * Tracks the lifecycle of converting natural language input to structured plan.
 */
sealed class LLMNormalizerState {
    object Idle : LLMNormalizerState()
    object Loading : LLMNormalizerState()
    data class Success(val response: com.jnkim.poschedule.data.ai.LLMTaskResponse) : LLMNormalizerState()
    data class Clarification(val response: com.jnkim.poschedule.data.ai.LLMTaskResponse) : LLMNormalizerState()
    data class Error(val message: String) : LLMNormalizerState()
}

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val planRepository: PlanRepository,
    private val settingsRepository: SettingsRepository,
    private val gentleCopyUseCase: GentleCopyUseCase,
    private val modeStateMachine: ModeStateMachine,
    private val recurrenceEngine: RecurrenceEngine,
    private val notificationLogDao: com.jnkim.poschedule.data.local.dao.NotificationLogDao,
    private val llmTaskNormalizerUseCase: com.jnkim.poschedule.domain.ai.LLMTaskNormalizerUseCase
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _systemMessage = MutableStateFlow(Pair("시스템이 안정적입니다", "오늘도 당신의 속도대로 진행하세요"))

    // LLM task normalizer state
    private val _llmNormalizerState = MutableStateFlow<LLMNormalizerState>(LLMNormalizerState.Idle)
    val llmNormalizerState: StateFlow<LLMNormalizerState> = _llmNormalizerState.asStateFlow()

    // FAB menu expansion state
    private val _fabMenuExpanded = MutableStateFlow(false)
    val fabMenuExpanded: StateFlow<Boolean> = _fabMenuExpanded.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TodayUiState> = _selectedDate
        .flatMapLatest { date ->
            val startOfWeek = LocalDate.parse(date).minusDays(LocalDate.parse(date).dayOfWeek.value.toLong() % 7)
            val weekDates = (0..6).map { startOfWeek.plusDays(it.toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE) }
            
            combine(
                planRepository.getPlanItems(date),
                flow { emit(planRepository.getPlanDay(date)?.mode ?: Mode.NORMAL) },
                _systemMessage,
                settingsRepository.settingsFlow,
                combine(weekDates.map { planRepository.getPlanItems(it) }) { itemsArray ->
                    itemsArray.indices.associate { i -> LocalDate.parse(weekDates[i]) to itemsArray[i] }
                }
            ) { items, mode, message, settings, weekMap ->
                TodayUiState(
                    planItems = items,
                    weekDensity = weekMap,
                    monthDensity = weekMap.mapValues { it.value.size },
                    mode = mode,
                    systemMessageTitle = message.first,
                    systemMessageBody = message.second,
                    userSettings = settings,
                    isLoading = false
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TodayUiState()
        )

    init {
        refreshSystemMessage()
        evaluateCurrentMode()
    }

    fun onDateSelected(date: String) {
        _selectedDate.value = date
        refreshSystemMessage()
        evaluateCurrentMode()
    }

    private fun evaluateCurrentMode() {
        viewModelScope.launch {
            val date = _selectedDate.value
            val items = planRepository.getPlanItems(date).first()
            if (items.isEmpty()) return@launch

            val coreItems = items.filter { it.isCore }
            val missedCoreCount = coreItems.count { it.status == "PENDING" && isPastWindow(it) }
            val completedCount = items.count { it.status == "DONE" }
            val adherenceRate = if (items.isNotEmpty()) completedCount.toFloat() / items.size.toFloat() else 1.0f

            val newMode = modeStateMachine.calculateMode(
                adherenceRate = adherenceRate,
                consecutiveSnoozes = 0, 
                missedCoreRoutines = missedCoreCount,
                isCurrentlyBusy = false 
            )

            val currentDay = planRepository.getPlanDay(date)
            if (currentDay == null || currentDay.mode != newMode) {
                planRepository.insertPlanDay(date, newMode)
                refreshSystemMessage()
            }
        }
    }

    private fun isPastWindow(item: PlanItemEntity): Boolean {
        val end = item.endTimeMillis ?: return false
        return System.currentTimeMillis() > end
    }

    fun refreshSystemMessage() {
        viewModelScope.launch {
            val date = _selectedDate.value
            val items = planRepository.getPlanItems(date).first()
            val settings = settingsRepository.settingsFlow.first()

            // Gather notification budget
            val budgetUsed = notificationLogDao.countSentToday(date)
            val budgetTotal = settings.dailyBudget

            // Gather recent actions (last 7 days)
            val recentActions = mutableMapOf<String, Int>()
            val today = java.time.LocalDate.now()
            for (i in 0..6) {
                val checkDate = today.minusDays(i.toLong())
                    .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                val dayItems = planRepository.getPlanItems(checkDate).first()
                dayItems.forEach { item ->
                    val status = item.status ?: "PENDING"
                    recentActions[status] = recentActions.getOrDefault(status, 0) + 1
                }
            }

            // Build request with rich context
            val request = GentleCopyRequest(
                mode = uiState.value.mode,
                pendingItemsCount = items.count { it.status == "PENDING" },
                completedTodayCount = items.count { it.status == "DONE" },
                notificationBudgetUsed = budgetUsed,
                notificationBudgetTotal = budgetTotal,
                todayWindows = items.map { it.title ?: "Untitled" },
                recentActions = recentActions
            )

            val response = gentleCopyUseCase.generateMessage(request)
            _systemMessage.value = Pair(response.title, response.body)
        }
    }

    fun onPlanItemChecked(id: String, completed: Boolean) {
        viewModelScope.launch {
            val status = if (completed) "DONE" else "PENDING"
            planRepository.updateItemStatus(id, status)
            evaluateCurrentMode()
            refreshSystemMessage()
        }
    }

    fun snoozeItem(id: String) {
        viewModelScope.launch {
            // Snooze for 15 minutes from now
            val snoozeUntil = java.time.Instant.now()
                .plus(15, java.time.temporal.ChronoUnit.MINUTES)
                .toEpochMilli()
            planRepository.updateItemWithSnooze(id, "SNOOZED", snoozeUntil)
            evaluateCurrentMode()
        }
    }

    fun skipItem(id: String) {
        viewModelScope.launch {
            planRepository.updateItemStatus(id, "SKIPPED")
            evaluateCurrentMode()
        }
    }

    fun stopSeries(seriesId: String) {
        viewModelScope.launch {
            planRepository.stopRepeatingSeries(seriesId, _selectedDate.value)
            evaluateCurrentMode()
        }
    }

    fun removeOccurrence(seriesId: String) {
        viewModelScope.launch {
            planRepository.removeOccurrence(seriesId, _selectedDate.value)
            evaluateCurrentMode()
        }
    }

    fun addManualPlanItem(title: String, window: PlanItemWindow) {
        viewModelScope.launch {
            planRepository.addManualItem(_selectedDate.value, title, window)
            refreshSystemMessage()
        }
    }

    fun addPlanSeries(
        title: String,
        planType: PlanType,
        anchor: TimeAnchor,
        frequency: RecurrenceFrequency,
        isCore: Boolean,
        startOffset: Int,
        endOffset: Int,
        byDays: String? = null
    ) {
        viewModelScope.launch {
            val series = PlanSeriesEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                type = planType,
                routineType = null,
                isCore = isCore,
                anchor = anchor,
                startOffsetMin = startOffset,
                endOffsetMin = endOffset,
                frequency = frequency,
                interval = 1,
                byDays = byDays,
                isActive = true,
                archived = false
            )
            planRepository.addSeries(series)

            // Immediately expand the series for today and surrounding dates
            // This ensures the plan items appear in the UI right away
            expandSeriesForCurrentWeek(series)
            refreshSystemMessage()
        }
    }

    /**
     * Expands a newly created series for the current week to make it appear immediately.
     * Matches the expansion range used in DailyPlanWorker (-1 to +7 days).
     */
    private suspend fun expandSeriesForCurrentWeek(series: PlanSeriesEntity) {
        val today = LocalDate.now()
        for (offset in -1..7) {
            val targetDate = today.plusDays(offset.toLong())
            val dateStr = targetDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

            // Ensure PlanDay exists for the date
            if (planRepository.getPlanDay(dateStr) == null) {
                planRepository.insertPlanDay(dateStr, Mode.NORMAL)
            }

            // Expand the series and insert the plan item
            recurrenceEngine.expand(series, targetDate)?.let { instance ->
                planRepository.insertPlanItem(instance)
            }
        }
    }

    fun deletePlanItem(id: String) {
        viewModelScope.launch {
            planRepository.deleteItem(id)
            refreshSystemMessage()
        }
    }

    /**
     * Creates a one-time event (non-recurring plan) for a specific date and time.
     * Unlike recurring plans, this creates a single PlanItemEntity directly without a series.
     *
     * @param title Event title (e.g., "Dinner with John")
     * @param date Date in ISO format (yyyy-MM-dd)
     * @param startHour Hour of day (0-23)
     * @param startMinute Minute of hour (0-59)
     * @param durationMinutes Duration in minutes
     */
    fun addOneTimeEvent(title: String, date: String, startHour: Int, startMinute: Int, durationMinutes: Int) {
        viewModelScope.launch {
            planRepository.addOneTimeEvent(title, date, startHour, startMinute, durationMinutes)
            refreshSystemMessage()
        }
    }

    // ========== LLM-Based Task Creation ==========

    /**
     * Normalizes user's natural language input into structured plan using LLM.
     * Updates llmNormalizerState with the result.
     */
    fun normalizeTaskWithLLM(userInput: String) {
        viewModelScope.launch {
            _llmNormalizerState.value = LLMNormalizerState.Loading

            val settings = settingsRepository.settingsFlow.first()
            val locale = if (settings.language == "ko") "ko" else "en"

            val result = llmTaskNormalizerUseCase(
                userInput = userInput,
                locale = locale
            )

            result.fold(
                onSuccess = { response ->
                    when {
                        response.intent == "unsure" || response.confidence < 0.7 -> {
                            _llmNormalizerState.value = LLMNormalizerState.Clarification(response)
                        }
                        response.plan != null -> {
                            _llmNormalizerState.value = LLMNormalizerState.Success(response)
                        }
                        else -> {
                            _llmNormalizerState.value = LLMNormalizerState.Error("Could not understand input")
                        }
                    }
                },
                onFailure = { error ->
                    _llmNormalizerState.value = LLMNormalizerState.Error(
                        error.message ?: "Unknown error"
                    )
                }
            )
        }
    }

    /**
     * Confirms and saves the LLM-normalized plan to the database.
     * Maps NormalizedPlan to existing entity structure and creates a plan series.
     */
    fun confirmLLMPlanAndSave(normalizedPlan: com.jnkim.poschedule.data.ai.NormalizedPlan) {
        viewModelScope.launch {
            try {
                // Map to existing entity structure
                val anchor = when (normalizedPlan.time.anchor.uppercase()) {
                    "WAKE" -> TimeAnchor.WAKE
                    "BED" -> TimeAnchor.BED
                    "FIXED" -> TimeAnchor.FIXED
                    else -> TimeAnchor.FIXED // Default to FIXED for FLEX
                }

                val planType = try {
                    PlanType.valueOf(normalizedPlan.planType.uppercase())
                } catch (e: Exception) {
                    PlanType.TASK // Default to TASK if invalid
                }

                val routineType = normalizedPlan.routineType?.let {
                    try {
                        com.jnkim.poschedule.domain.model.RoutineType.valueOf(it.uppercase())
                    } catch (e: Exception) {
                        null
                    }
                }

                val frequency = when (normalizedPlan.recurrence.kind.uppercase()) {
                    "DAILY" -> RecurrenceFrequency.DAILY
                    "WEEKLY" -> RecurrenceFrequency.WEEKLY
                    "WEEKDAYS" -> RecurrenceFrequency.WEEKLY
                    "MONTHLY" -> RecurrenceFrequency.MONTHLY
                    else -> RecurrenceFrequency.DAILY // Default for NONE
                }

                // Convert weekdays list to byDays string (comma-separated)
                val byDays = when {
                    normalizedPlan.recurrence.kind.uppercase() == "WEEKDAYS" -> "1,2,3,4,5" // Mon-Fri
                    normalizedPlan.recurrence.weekdays != null ->
                        normalizedPlan.recurrence.weekdays.joinToString(",")
                    else -> null
                }

                // Calculate offsets based on anchor type
                val startOffset = when (anchor) {
                    TimeAnchor.FIXED -> {
                        // For FIXED anchor, we'll store hour/minute as offsets from midnight
                        val hour = normalizedPlan.time.fixedHour ?: 9
                        val minute = normalizedPlan.time.fixedMinute ?: 0
                        hour * 60 + minute
                    }
                    else -> normalizedPlan.time.offset ?: 0
                }

                val endOffset = startOffset + normalizedPlan.time.duration

                // Create plan series entity
                val series = PlanSeriesEntity(
                    id = UUID.randomUUID().toString(),
                    title = normalizedPlan.title,
                    type = planType,
                    routineType = routineType,
                    isCore = false, // LLM-created plans default to non-core
                    anchor = anchor,
                    startOffsetMin = startOffset,
                    endOffsetMin = endOffset,
                    frequency = frequency,
                    interval = 1,
                    byDays = byDays,
                    isActive = true,
                    archived = false
                )

                // Add series to database
                planRepository.addSeries(series)

                // Expand for current week
                expandSeriesForCurrentWeek(series)

                // Reset state
                _llmNormalizerState.value = LLMNormalizerState.Idle

                // Refresh UI
                refreshSystemMessage()
            } catch (e: Exception) {
                _llmNormalizerState.value = LLMNormalizerState.Error(
                    "Failed to save plan: ${e.message}"
                )
            }
        }
    }

    /**
     * Resets the LLM normalizer state to Idle.
     * Used when user cancels the LLM flow.
     */
    fun resetLLMState() {
        _llmNormalizerState.value = LLMNormalizerState.Idle
    }

    /**
     * Toggles the FAB menu expansion state.
     */
    fun toggleFabMenu() {
        _fabMenuExpanded.value = !_fabMenuExpanded.value
    }

    /**
     * Sets the FAB menu expansion state.
     */
    fun setFabMenuExpanded(expanded: Boolean) {
        _fabMenuExpanded.value = expanded
    }
}
