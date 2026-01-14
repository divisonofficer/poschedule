package com.jnkim.poschedule.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jnkim.poschedule.data.local.entity.PlanItemWindow
import com.jnkim.poschedule.domain.ai.MicroChore
import com.jnkim.poschedule.domain.ai.VisionChoreUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

sealed class TidySnapUiState {
    object Idle : TidySnapUiState()
    object Processing : TidySnapUiState()
    data class Success(val tasks: List<MicroChore>) : TidySnapUiState()
    data class Error(val message: String) : TidySnapUiState()
}

@HiltViewModel
class TidySnapViewModel @Inject constructor(
    private val visionChoreUseCase: VisionChoreUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<TidySnapUiState>(TidySnapUiState.Idle)
    val uiState: StateFlow<TidySnapUiState> = _uiState.asStateFlow()

    fun processImage(imageFile: File, lang: String) {
        viewModelScope.launch {
            _uiState.value = TidySnapUiState.Processing
            try {
                val tasks = visionChoreUseCase.analyzeImage(imageFile, lang)
                _uiState.value = TidySnapUiState.Success(tasks)
            } catch (e: Exception) {
                _uiState.value = TidySnapUiState.Error(e.message ?: "Failed to analyze image")
            }
        }
    }

    fun injectTasks(tasks: List<MicroChore>, window: PlanItemWindow) {
        viewModelScope.launch {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            visionChoreUseCase.injectTasks(tasks, today, window)
            _uiState.value = TidySnapUiState.Idle
        }
    }
    
    fun reset() {
        _uiState.value = TidySnapUiState.Idle
    }
}
