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
    data class Capturing(val capturedImages: List<File> = emptyList()) : TidySnapUiState()
    object Processing : TidySnapUiState()
    data class Success(val tasks: List<MicroChore>) : TidySnapUiState()
    data class Error(val message: String) : TidySnapUiState()
}

@HiltViewModel
class TidySnapViewModel @Inject constructor(
    private val visionChoreUseCase: VisionChoreUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<TidySnapUiState>(TidySnapUiState.Capturing())
    val uiState: StateFlow<TidySnapUiState> = _uiState.asStateFlow()

    private val _capturedImages = mutableListOf<File>()

    /**
     * Add a captured image to the list.
     */
    fun addCapturedImage(imageFile: File) {
        _capturedImages.add(imageFile)
        _uiState.value = TidySnapUiState.Capturing(_capturedImages.toList())
    }

    /**
     * Remove an image from the captured list.
     */
    fun removeCapturedImage(imageFile: File) {
        _capturedImages.remove(imageFile)
        _uiState.value = TidySnapUiState.Capturing(_capturedImages.toList())
    }

    /**
     * Analyze all captured images.
     */
    fun analyzeImages(lang: String) {
        if (_capturedImages.isEmpty()) {
            _uiState.value = TidySnapUiState.Error("No images to analyze")
            return
        }

        viewModelScope.launch {
            _uiState.value = TidySnapUiState.Processing
            try {
                val tasks = visionChoreUseCase.analyzeImages(_capturedImages, lang)
                _uiState.value = TidySnapUiState.Success(tasks)
            } catch (e: Exception) {
                _uiState.value = TidySnapUiState.Error(e.message ?: "Failed to analyze images")
            }
        }
    }

    fun injectTasks(tasks: List<MicroChore>, window: PlanItemWindow) {
        viewModelScope.launch {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            visionChoreUseCase.injectTasks(tasks, today, window)
            reset()
        }
    }

    fun reset() {
        _capturedImages.clear()
        _uiState.value = TidySnapUiState.Capturing()
    }
}
