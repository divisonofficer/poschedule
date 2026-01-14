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

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val planRepository: PlanRepository,
    private val settingsRepository: SettingsRepository,
    private val gentleCopyUseCase: GentleCopyUseCase,
    private val modeStateMachine: ModeStateMachine,
    private val recurrenceEngine: RecurrenceEngine
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _systemMessage = MutableStateFlow(Pair("시스템이 안정적입니다", "오늘도 당신의 속도대로 진행하세요"))

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
            val response = gentleCopyUseCase.generateMessage(
                GentleCopyRequest(
                    mode = uiState.value.mode,
                    pendingItemsCount = items.count { it.status == "PENDING" },
                    completedTodayCount = items.count { it.status == "DONE" }
                )
            )
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
}
