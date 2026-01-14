package com.jnkim.poschedule.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jnkim.poschedule.data.local.AuthTokenManager
import com.jnkim.poschedule.data.local.entity.PlanItemSource
import com.jnkim.poschedule.data.local.entity.PlanItemWindow
import com.jnkim.poschedule.data.repo.PlanRepository
import com.jnkim.poschedule.data.repo.SettingsRepository
import com.jnkim.poschedule.data.repo.UserSettings
import com.jnkim.poschedule.domain.model.Mode
import com.jnkim.poschedule.domain.model.RoutineType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val tokenManager: AuthTokenManager,
    private val planRepository: PlanRepository
) : ViewModel() {

    val settings: StateFlow<UserSettings?> = repository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _isApiKeyPresent = MutableStateFlow(tokenManager.getApiKey() != null)
    val isApiKeyPresent: StateFlow<Boolean> = _isApiKeyPresent.asStateFlow()

    fun toggleAiEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.updateAiEnabled(enabled) }
    }

    fun toggleVisionConsent(consent: Boolean) {
        viewModelScope.launch { repository.updateVisionConsent(consent) }
    }

    fun updateDailyBudget(budget: Int) {
        viewModelScope.launch { repository.updateDailyBudget(budget) }
    }

    fun updateLanguage(lang: String) {
        viewModelScope.launch { repository.updateLanguage(lang) }
    }

    fun updateSiteName(name: String) {
        viewModelScope.launch { repository.updateSiteName(name) }
    }

    fun fetchApiKey() {
        viewModelScope.launch {
            val token = tokenManager.getAccessToken()
            if (token != null) {
                tokenManager.saveApiKey("sk-simulated-${System.currentTimeMillis()}")
                _isApiKeyPresent.value = true
            }
        }
    }

    /**
     * Simulation tool for Milestone 2.4
     * Injects a day with missed core tasks to test RECOVERY mode triggers.
     */
    fun simulateRecoveryScenario() {
        viewModelScope.launch {
            val date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            
            // 1. Setup a day in NORMAL mode
            planRepository.insertPlanDay(date, Mode.NORMAL)
            
            // 2. Add some deterministic tasks that are 'past' their window
            val pastTime = System.currentTimeMillis() - 3600000 // 1 hour ago
            
            // Inject two missed core tasks
            planRepository.insertPlanItem(
                com.jnkim.poschedule.data.local.entity.PlanItemEntity(
                    id = UUID.randomUUID().toString(),
                    date = date,
                    title = "Missed Meds",
                    type = RoutineType.MEDS_AM,
                    source = PlanItemSource.DETERMINISTIC,
                    window = PlanItemWindow.MORNING,
                    status = "PENDING",
                    isCore = true,
                    startTimeMillis = pastTime - 1000,
                    endTimeMillis = pastTime
                )
            )
            
            planRepository.insertPlanItem(
                com.jnkim.poschedule.data.local.entity.PlanItemEntity(
                    id = UUID.randomUUID().toString(),
                    date = date,
                    title = "Missed Breakfast",
                    type = RoutineType.MEAL,
                    source = PlanItemSource.DETERMINISTIC,
                    window = PlanItemWindow.MORNING,
                    status = "PENDING",
                    isCore = true,
                    startTimeMillis = pastTime - 1000,
                    endTimeMillis = pastTime
                )
            )
            
            // Navigation or UI refresh will trigger the ModeStateMachine re-calc in TodayViewModel
        }
    }
}
