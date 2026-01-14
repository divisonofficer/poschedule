package com.jnkim.poschedule.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jnkim.poschedule.data.local.entity.PlanItemEntity
import com.jnkim.poschedule.data.local.entity.PlanItemWindow
import com.jnkim.poschedule.data.repo.PlanRepository
import com.jnkim.poschedule.domain.ai.GentleCopyRequest
import com.jnkim.poschedule.domain.ai.GentleCopyUseCase
import com.jnkim.poschedule.domain.engine.ModeStateMachine
import com.jnkim.poschedule.domain.model.Mode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class TodayUiState(
    val planItems: List<PlanItemEntity> = emptyList(),
    val weekDensity: Map<LocalDate, List<PlanItemEntity>> = emptyMap(),
    val monthDensity: Map<LocalDate, Int> = emptyMap(),
    val mode: Mode = Mode.NORMAL,
    val systemMessageTitle: String = "시스템이 안정적입니다",
    val systemMessageBody: String = "오늘도 당신의 속도대로 진행하세요",
    val isLoading: Boolean = true
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val planRepository: PlanRepository,
    private val gentleCopyUseCase: GentleCopyUseCase,
    private val modeStateMachine: ModeStateMachine
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
                flow { 
                    val planDay = planRepository.getPlanDay(date)
                    emit(planDay?.mode ?: Mode.NORMAL)
                },
                _systemMessage,
                // Simple week density aggregator
                combine(weekDates.map { planRepository.getPlanItems(it) }) { itemsArray ->
                    itemsArray.indices.associate { i -> 
                        LocalDate.parse(weekDates[i]) to itemsArray[i]
                    }
                }
            ) { items, mode, message, weekMap ->
                TodayUiState(
                    planItems = items,
                    weekDensity = weekMap,
                    monthDensity = weekMap.mapValues { it.value.size }, // Simplified for MVP
                    mode = mode,
                    systemMessageTitle = message.first,
                    systemMessageBody = message.second,
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
            val planDay = planRepository.getPlanDay(date)
            val mode = planDay?.mode ?: Mode.NORMAL
            val items = planRepository.getPlanItems(date).first()
            
            val response = gentleCopyUseCase.generateMessage(
                GentleCopyRequest(
                    mode = mode,
                    pendingItemsCount = items.count { it.status == "PENDING" },
                    completedTodayCount = items.count { it.status == "DONE" },
                    userLocale = "ko"
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
            planRepository.updateItemStatus(id, "SNOOZED")
            evaluateCurrentMode()
        }
    }

    fun skipItem(id: String) {
        viewModelScope.launch {
            planRepository.updateItemStatus(id, "SKIPPED")
            evaluateCurrentMode()
        }
    }

    fun addManualPlanItem(title: String, window: PlanItemWindow) {
        viewModelScope.launch {
            planRepository.addManualItem(_selectedDate.value, title, window)
            refreshSystemMessage()
        }
    }

    fun deletePlanItem(id: String) {
        viewModelScope.launch {
            planRepository.deleteItem(id)
            refreshSystemMessage()
        }
    }
}
