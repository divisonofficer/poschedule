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
import kotlinx.coroutines.flow.first
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
    private val planRepository: PlanRepository,
    private val genAiRepository: com.jnkim.poschedule.data.repo.GenAiRepository,
    private val geminiClient: com.jnkim.poschedule.data.ai.GeminiClient
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
            android.util.Log.d("SettingsViewModel", "Fetching API key from server...")
            val apiKey = genAiRepository.fetchAndCacheApiKey()
            if (apiKey != null) {
                android.util.Log.d("SettingsViewModel", "API key fetched successfully")
                _isApiKeyPresent.value = true
            } else {
                android.util.Log.e("SettingsViewModel", "Failed to fetch API key. Check logs for details.")
                _isApiKeyPresent.value = false
            }
        }
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch { repository.updateThemeMode(mode) }
    }

    fun updateWeatherEffectsEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.updateWeatherEffectsEnabled(enabled) }
    }

    fun updateManualWeatherState(state: String) {
        viewModelScope.launch { repository.updateManualWeatherState(state) }
    }

    fun updateStatusCompanionEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.updateStatusCompanionEnabled(enabled) }
    }

    fun updateLockscreenDetailsEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.updateLockscreenDetailsEnabled(enabled) }
    }

    fun updateApiProvider(provider: String) {
        viewModelScope.launch { repository.updateApiProvider(provider) }
    }

    fun updatePostechModel(model: String) {
        viewModelScope.launch { repository.updatePostechModel(model) }
    }

    fun updateGeminiModel(model: String) {
        viewModelScope.launch { repository.updateGeminiModel(model) }
    }

    /**
     * Validates and saves Gemini API key using the currently selected Gemini model.
     * Returns true if validation successful, false otherwise.
     */
    suspend fun validateAndSaveGeminiKey(apiKey: String): Boolean {
        return try {
            val currentSettings = repository.settingsFlow.first()
            val modelToUse = currentSettings.geminiModel

            android.util.Log.d("SettingsViewModel", "Validating Gemini API key with model: $modelToUse")
            val isValid = geminiClient.validateApiKey(apiKey, modelToUse)

            if (isValid) {
                tokenManager.saveGeminiApiKey(apiKey)
                android.util.Log.d("SettingsViewModel", "Gemini API key saved successfully")
                true
            } else {
                android.util.Log.e("SettingsViewModel", "Gemini API key validation failed")
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("SettingsViewModel", "Error validating Gemini key: ${e.message}", e)
            false
        }
    }

    fun isGeminiKeyConfigured(): Boolean {
        return tokenManager.getGeminiApiKey() != null
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
