package com.jnkim.poschedule.domain.ai

import android.util.Log
import com.jnkim.poschedule.data.ai.GenAiClient
import com.jnkim.poschedule.data.ai.LLMTaskResponse
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for normalizing natural language task input using LLM.
 * Handles validation, sanitization, and error recovery.
 */
@Singleton
class LLMTaskNormalizerUseCase @Inject constructor(
    private val genAiClient: GenAiClient,
    private val promptManager: PromptManager
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Normalizes user's natural language input into structured plan.
     *
     * @param userInput Natural language description of the task
     * @param locale Language code ("ko" or "en")
     * @return Result containing validated LLMTaskResponse
     */
    suspend operator fun invoke(
        userInput: String,
        locale: String = "ko"
    ): Result<LLMTaskResponse> {
        return try {
            val systemPrompt = promptManager.getLLMTaskNormalizerPrompt(locale)
            val rawResponse = genAiClient.normalizeTaskInput(
                userInput = userInput,
                systemPrompt = systemPrompt
            )

            Log.d(TAG, "LLM raw response: $rawResponse")

            val response = json.decodeFromString<LLMTaskResponse>(rawResponse)

            // Client-side validation
            val validated = validateAndSanitize(response)

            Log.d(TAG, "Normalized plan: intent=${validated.intent}, confidence=${validated.confidence}")

            Result.success(validated)
        } catch (e: Exception) {
            Log.e(TAG, "LLM normalization failed", e)
            Result.failure(e)
        }
    }

    /**
     * Validates and sanitizes LLM response to ensure safe values.
     * Clamps numeric fields to acceptable ranges and filters invalid data.
     */
    private fun validateAndSanitize(response: LLMTaskResponse): LLMTaskResponse {
        val plan = response.plan ?: return response

        // Clamp time values to safe ranges
        val sanitizedTime = plan.time.copy(
            offset = plan.time.offset?.coerceIn(-180, 720),
            fixedHour = plan.time.fixedHour?.coerceIn(0, 23),
            fixedMinute = plan.time.fixedMinute?.coerceIn(0, 59),
            duration = plan.time.duration.coerceIn(1, 240)
        )

        // Validate weekdays (1=Monday, 7=Sunday)
        val sanitizedRecurrence = plan.recurrence.copy(
            weekdays = plan.recurrence.weekdays?.filter { it in 1..7 }
        )

        val sanitizedPlan = plan.copy(
            time = sanitizedTime,
            recurrence = sanitizedRecurrence
        )

        return response.copy(plan = sanitizedPlan)
    }

    companion object {
        private const val TAG = "LLMTaskNormalizerUseCase"
    }
}
