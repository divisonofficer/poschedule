package com.jnkim.poschedule.data.ai

import com.jnkim.poschedule.data.repo.GenAiRepository
import com.jnkim.poschedule.data.repo.SettingsRepository
import com.jnkim.poschedule.domain.ai.GentleCopyRequest
import com.jnkim.poschedule.domain.ai.GentleCopyResponse
import com.jnkim.poschedule.domain.ai.MicroChore
import com.jnkim.poschedule.domain.ai.PromptManager
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GenAiClient @Inject constructor(
    private val genAiRepository: GenAiRepository,
    private val settingsRepository: SettingsRepository,
    private val promptManager: PromptManager
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Calls the real GenAI API to generate supportive copy with rich context.
     */
    suspend fun generateGentleCopy(request: GentleCopyRequest): GentleCopyResponse {
        val language = settingsRepository.settingsFlow.first().language
        val systemPrompt = promptManager.getGentleCopySystemPrompt(request.mode, language)

        android.util.Log.d("GentleCopy", "Starting API call for mode=${request.mode}")

        // Build rich user prompt with all context
        val userPrompt = buildString {
            append("Current system state:\n")
            append("- Mode: ${request.mode}\n")
            append("- Tasks completed today: ${request.completedTodayCount}\n")
            append("- Tasks pending: ${request.pendingItemsCount}\n")
            append("- Notifications used: ${request.notificationBudgetUsed}/${request.notificationBudgetTotal}\n")

            if (request.todayWindows.isNotEmpty()) {
                append("- Today's planned items: ${request.todayWindows.take(5).joinToString(", ")}\n")
            }

            if (request.recentActions.isNotEmpty()) {
                append("- Recent week actions: ")
                request.recentActions.entries.joinToString(", ") { "${it.key}=${it.value}" }
                    .let { append(it) }
                append("\n")
            }

            append("\nGenerate a supportive system message.")
        }

        android.util.Log.d("GentleCopy", "Building prompt for mode=${request.mode}, pending=${request.pendingItemsCount}, completed=${request.completedTodayCount}")

        val rawResponse = genAiRepository.getCompletion(
            prompt = userPrompt,
            systemPrompt = systemPrompt
        )

        android.util.Log.d("GentleCopy", "Raw API response: $rawResponse")

        // Check if API returned null (authentication or network error)
        if (rawResponse == null) {
            android.util.Log.e("GentleCopy", "API returned null - check logs for GenAiRepository error")
            throw IllegalStateException("API returned null response. Check authentication and network connection.")
        }

        // Clean up the response (remove markdown code blocks and HTML comments)
        var cleanedResponse = rawResponse.trim()

        // Remove markdown code fences (```json...``` or ```...```)
        val jsonPattern = Regex("""```(?:json)?\s*(.*?)\s*```""", RegexOption.DOT_MATCHES_ALL)
        val jsonMatch = jsonPattern.find(cleanedResponse)
        if (jsonMatch != null) {
            cleanedResponse = jsonMatch.groupValues[1].trim()
        }

        // Remove HTML comments (<!-- ... -->)
        cleanedResponse = cleanedResponse.replace(Regex("""<!--.*?-->""", RegexOption.DOT_MATCHES_ALL), "").trim()

        android.util.Log.d("GentleCopy", "Cleaned response: $cleanedResponse")

        // Check if response is empty after cleaning
        if (cleanedResponse.isBlank() || cleanedResponse == "{}") {
            android.util.Log.e("GentleCopy", "API returned empty response")
            throw IllegalStateException("API returned empty response")
        }

        return try {
            val parsed = json.decodeFromString<GentleCopyResponseDto>(cleanedResponse)

            // Validate that we have meaningful content
            if (parsed.title.isBlank() || parsed.body.isBlank()) {
                throw IllegalStateException("LLM returned empty title or body")
            }

            GentleCopyResponse(
                title = parsed.title.take(20), // Enforce character limit
                body = parsed.body.take(60),   // Enforce character limit
                tone = parsed.tone,
                safetyTag = parsed.safetyTag
            )
        } catch (e: Exception) {
            android.util.Log.e("GentleCopy", "Parsing failed. Cleaned response was: $cleanedResponse", e)
            throw IllegalStateException("Failed to parse GentleCopy response: ${e.message}", e)
        }
    }

    /**
     * Calls the real Multi-modal pipeline to decompose a physical chore image.
     */
    suspend fun analyzeChoreImage(imageFile: File, lang: String = "en"): List<MicroChore> {
        // 1. Upload image to get server-side file metadata
        val genAiFile = genAiRepository.uploadFile(imageFile) ?: return emptyList()

        // 2. Prepare Multi-modal Prompt
        val systemPrompt = promptManager.getTidySnapSystemPrompt(lang)
        val userPrompt = "I have uploaded an image of a chore. Please analyze the physical state and decompose it into 3 micro-tasks."

        // 3. Get LLM Decomposition using the 'files' array for multi-modal support
        val rawResponse = genAiRepository.getCompletion(
            prompt = userPrompt,
            systemPrompt = systemPrompt,
            files = listOf(genAiFile)
        )

        return try {
            json.decodeFromString<List<MicroChore>>(rawResponse ?: "[]")
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Normalizes natural language task input into structured plan JSON.
     * Used for LLM-based task creation.
     */
    suspend fun normalizeTaskInput(
        userInput: String,
        systemPrompt: String
    ): String {
        android.util.Log.d("LLMTaskNormalizer", "Sending request with input: $userInput")

        val rawResponse = genAiRepository.getCompletion(
            prompt = userInput,
            systemPrompt = systemPrompt,
            files = emptyList()
        )

        android.util.Log.d("LLMTaskNormalizer", "Raw API response: $rawResponse")

        if (rawResponse == null) {
            android.util.Log.e("LLMTaskNormalizer", "API returned null response")
            throw IllegalStateException("LLM API returned null response. Please check your API key and network connection.")
        }

        // Clean up the response (remove markdown code blocks and HTML comments)
        var cleanedResponse = rawResponse.trim()

        // Remove markdown code fences (```json...``` or ```...```)
        val jsonPattern = Regex("""```(?:json)?\s*(.*?)\s*```""", RegexOption.DOT_MATCHES_ALL)
        val jsonMatch = jsonPattern.find(cleanedResponse)
        if (jsonMatch != null) {
            cleanedResponse = jsonMatch.groupValues[1].trim()
        }

        // Remove HTML comments (<!-- ... -->)
        cleanedResponse = cleanedResponse.replace(Regex("""<!--.*?-->""", RegexOption.DOT_MATCHES_ALL), "").trim()

        android.util.Log.d("LLMTaskNormalizer", "Cleaned response: $cleanedResponse")

        return cleanedResponse
    }
}

@kotlinx.serialization.Serializable
private data class GentleCopyResponseDto(
    val title: String,
    val body: String,
    val tone: String = "calm",
    val safetyTag: String? = null
)
