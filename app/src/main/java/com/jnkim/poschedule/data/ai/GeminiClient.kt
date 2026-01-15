package com.jnkim.poschedule.data.ai

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.jnkim.poschedule.data.local.AuthTokenManager
import com.jnkim.poschedule.data.remote.api.GenAiFile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Client wrapper for Google Generative AI (Gemini) SDK.
 * Provides a simplified interface for text generation with API key validation.
 */
@Singleton
class GeminiClient @Inject constructor(
    private val tokenManager: AuthTokenManager
) {
    private companion object {
        const val MODEL_NAME = "gemini-1.5-flash"
        const val TAG = "GeminiClient"
    }

    /**
     * Validates Gemini API key by making a test request.
     * @param apiKey The API key to validate
     * @return true if valid, false otherwise
     */
    suspend fun validateApiKey(apiKey: String): Boolean {
        return try {
            val model = GenerativeModel(
                modelName = MODEL_NAME,
                apiKey = apiKey
            )

            // Simple test prompt
            val response = model.generateContent("Hello")
            val text = response.text

            Log.d(TAG, "API key validation successful: ${text?.take(20)}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "API key validation failed: ${e.message}", e)
            false
        }
    }

    /**
     * Generate text completion using Gemini API.
     * Maps to POSTECH API response format for compatibility.
     *
     * @param prompt User prompt text
     * @param systemPrompt System instructions for the model
     * @param files List of files (vision support not yet implemented in Phase 1)
     * @return Generated text response, or null if request fails
     */
    suspend fun getCompletion(
        prompt: String,
        systemPrompt: String,
        files: List<GenAiFile> = emptyList()
    ): String? {
        val apiKey = tokenManager.getGeminiApiKey()
        if (apiKey == null) {
            Log.e(TAG, "Gemini API key not found")
            return null
        }

        return try {
            val model = GenerativeModel(
                modelName = MODEL_NAME,
                apiKey = apiKey,
                systemInstruction = content { text(systemPrompt) }
            )

            // TODO: Phase 2.5 - Implement file uploads for vision (requires Base64 encoding)
            if (files.isNotEmpty()) {
                Log.w(TAG, "Gemini file upload not yet implemented, ignoring ${files.size} files")
            }

            val response = model.generateContent(prompt)
            val result = response.text

            Log.d(TAG, "Gemini response: ${result?.take(100)}")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API call failed: ${e.message}", e)
            null
        }
    }
}
