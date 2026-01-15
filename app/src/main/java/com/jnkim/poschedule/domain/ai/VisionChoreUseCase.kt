package com.jnkim.poschedule.domain.ai

import com.jnkim.poschedule.data.ai.GenAiClient
import com.jnkim.poschedule.data.local.entity.PlanItemWindow
import com.jnkim.poschedule.data.repo.PlanRepository
import kotlinx.serialization.Serializable
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class MicroChore(
    val title: String,
    val etaMin: Int,
    val effort: String,
    val iconEmoji: String? = null  // LLM-generated emoji icon
)

@Singleton
class VisionChoreUseCase @Inject constructor(
    private val genAiClient: GenAiClient,
    private val planRepository: PlanRepository
) {
    /**
     * Analyze a single image (deprecated, use analyzeImages instead).
     */
    @Deprecated("Use analyzeImages for better multi-image support")
    suspend fun analyzeImage(imageFile: File, lang: String): List<MicroChore> {
        return genAiClient.analyzeChoreImages(listOf(imageFile), lang)
    }

    /**
     * Analyze multiple images from different angles.
     */
    suspend fun analyzeImages(imageFiles: List<File>, lang: String): List<MicroChore> {
        return genAiClient.analyzeChoreImages(imageFiles, lang)
    }

    /**
     * Inject micro-chores as one-time events with specific times.
     * Schedules tasks sequentially starting from current time + 5 minutes.
     */
    suspend fun injectTasks(tasks: List<MicroChore>, date: String, window: PlanItemWindow) {
        val now = java.time.LocalDateTime.now()
        var currentStartTime = now.plusMinutes(5) // Start 5 minutes from now

        tasks.forEach { chore ->
            val startHour = currentStartTime.hour
            val startMinute = currentStartTime.minute
            val durationMinutes = chore.etaMin

            planRepository.addOneTimeEvent(
                title = chore.title,
                date = date,
                startHour = startHour,
                startMinute = startMinute,
                durationMinutes = durationMinutes,
                iconEmoji = chore.iconEmoji
            )

            // Next task starts after current task + 5 minute buffer
            currentStartTime = currentStartTime.plusMinutes(durationMinutes.toLong() + 5)
        }
    }
}
