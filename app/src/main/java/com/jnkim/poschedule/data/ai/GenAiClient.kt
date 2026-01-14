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
            prompt = "Current State: ${request.completedTodayCount} tasks done, ${request.pendingItemsCount} remaining.",
            systemPrompt = systemPrompt
        )

        return try {
            val parsed = json.decodeFromString<GentleCopyResponseStub>(rawResponse ?: "{}")
            GentleCopyResponse(parsed.title, parsed.body, "calm")
        } catch (e: Exception) {
            GentleCopyResponse("Poschedule", "Taking it one step at a time.", "calm")
        }
    }

    /**
     * Calls the real Multi-modal pipeline to decompose a physical chore image.
     */
    suspend fun analyzeChoreImage(imageFile: File, lang: String = "en"): List<MicroChore> {
        // 1. Upload image to get URL
        val imageUrl = genAiRepository.uploadFile(imageFile) ?: return emptyList()

        // 2. Prepare Multi-modal Prompt
        val systemPrompt = promptManager.getTidySnapSystemPrompt(lang)
        val userPrompt = "Analyze this physical space: $imageUrl. Decompose the chores visible into 3 micro-tasks."

        // 3. Get LLM Decomposition
        val rawResponse = genAiRepository.getCompletion(
            prompt = userPrompt,
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
