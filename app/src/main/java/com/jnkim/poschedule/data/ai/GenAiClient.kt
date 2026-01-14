package com.jnkim.poschedule.data.ai

import com.jnkim.poschedule.data.repo.GenAiRepository
import com.jnkim.poschedule.domain.ai.GentleCopyRequest
import com.jnkim.poschedule.domain.ai.GentleCopyResponse
import com.jnkim.poschedule.domain.ai.MicroChore
import com.jnkim.poschedule.domain.ai.PromptManager
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GenAiClient @Inject constructor(
    private val genAiRepository: GenAiRepository,
    private val promptManager: PromptManager
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Calls the real GenAI API to generate supportive copy using PromptManager templates.
     */
    suspend fun generateGentleCopy(request: GentleCopyRequest): GentleCopyResponse {
        val systemPrompt = promptManager.getGentleCopySystemPrompt(request.mode, request.userLocale)

        val rawResponse = genAiRepository.getCompletion(
            prompt = "Generate a supportive message based on: ${request.completedTodayCount} done, ${request.pendingItemsCount} left.",
            systemPrompt = systemPrompt
        )

        return try {
            val parsed = json.decodeFromString<GentleCopyResponseStub>(rawResponse ?: "{}")
            GentleCopyResponse(parsed.title, parsed.body, "calm")
        } catch (e: Exception) {
            // Return a default/fallback object that the UseCase will handle
            GentleCopyResponse("Poschedule", "Just taking it one step at a time.", "calm")
        }
    }

    /**
     * Calls the real GenAI API to decompose a chore into micro-tasks.
     */
    suspend fun analyzeChoreImage(imageFile: File, lang: String = "en"): List<MicroChore> {
        val systemPrompt = promptManager.getTidySnapSystemPrompt(lang)

        val rawResponse = genAiRepository.getCompletion(
            prompt = "Analyze chore from image (placeholder: messy desk).", 
            systemPrompt = systemPrompt
        )

        return try {
            json.decodeFromString<List<MicroChore>>(rawResponse ?: "[]")
        } catch (e: Exception) {
            emptyList()
        }
    }
}

@kotlinx.serialization.Serializable
private data class GentleCopyResponseStub(val title: String, val body: String)
