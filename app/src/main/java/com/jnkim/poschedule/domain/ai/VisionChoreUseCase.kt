package com.jnkim.poschedule.domain.ai

import com.jnkim.poschedule.data.ai.GenAiClient
import com.jnkim.poschedule.data.local.entity.PlanItemWindow
import com.jnkim.poschedule.data.repo.PlanRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class MicroChore(
    val title: String,
    val etaMin: Int,
    val effort: String
)

@Singleton
class VisionChoreUseCase @Inject constructor(
    private val genAiClient: GenAiClient,
    private val planRepository: PlanRepository
) {
    suspend fun analyzeImage(imageFile: File, lang: String): List<MicroChore> {
        return genAiClient.analyzeChoreImage(imageFile, lang)
    }

    suspend fun injectTasks(tasks: List<MicroChore>, date: String, window: PlanItemWindow) {
        tasks.forEach { chore ->
            planRepository.addManualItem(
                date = date,
                title = chore.title,
                window = window
            )
        }
    }
}
