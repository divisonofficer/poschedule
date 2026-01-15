package com.jnkim.poschedule.data.ai

import android.util.Log
import com.jnkim.poschedule.data.local.AuthTokenManager
import com.jnkim.poschedule.data.remote.api.*
import com.jnkim.poschedule.util.ImageUtils
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Client wrapper for Google Gemini REST API v1.
 * Uses direct HTTP calls instead of SDK due to SDK-backend sync issues.
 *
 * Official endpoint: https://generativelanguage.googleapis.com/v1/models/{model}:generateContent
 *
 * Supported free-tier models (as of 2025):
 * - gemini-2.5-flash: Recommended for general use, supports vision
 * - gemini-2.5-flash-lite: Fastest, lowest cost
 * - gemini-2.5-pro: Advanced, higher latency
 */
@Singleton
class GeminiClient @Inject constructor(
    private val tokenManager: AuthTokenManager,
    private val geminiApi: GeminiApi
) {
    private companion object {
        const val TAG = "GeminiClient"
    }

    /**
     * Validates Gemini API key by making a test request to v1 endpoint.
     * @param apiKey The API key to validate
     * @param modelName The Gemini model to use (default: gemini-2.5-flash, free-tier compatible)
     * @return true if valid, false otherwise
     */
    suspend fun validateApiKey(apiKey: String, modelName: String = "gemini-2.5-flash"): Boolean {
        return try {
            Log.d(TAG, "Validating API key with model: $modelName (v1 REST API)")

            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart(text = "Hello"))
                    )
                )
            )

            val response = geminiApi.generateContent(
                model = modelName,
                apiKey = apiKey,
                request = request
            )

            if (response.error != null) {
                Log.e(TAG, "API validation failed: ${response.error.message}")
                return false
            }

            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            Log.d(TAG, "API key validation successful: ${text?.take(20)}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "API key validation failed: ${e.message}", e)
            false
        }
    }

    /**
     * Generate text completion using Gemini API v1.
     * Supports multimodal input (text + images) via Base64 inline encoding.
     *
     * @param prompt User prompt text
     * @param systemPrompt System instructions for the model
     * @param files List of image files (automatically resized to 384x384 and Base64 encoded)
     * @param modelName The Gemini model to use (free-tier: "gemini-2.5-flash", "gemini-2.5-flash-lite", "gemini-2.5-pro")
     * @return Generated text response, or null if request fails
     */
    suspend fun getCompletion(
        prompt: String,
        systemPrompt: String,
        files: List<GenAiFile> = emptyList(),
        modelName: String = "gemini-2.5-flash"
    ): String? {
        val apiKey = tokenManager.getGeminiApiKey()
        if (apiKey == null) {
            Log.e(TAG, "Gemini API key not found")
            return null
        }

        return try {
            Log.d(TAG, "Using Gemini model: $modelName (v1 REST API)")

            // Build parts: text first (including system prompt), then images
            val parts = mutableListOf<GeminiPart>()

            // Combine system prompt and user prompt into a single text part
            // (REST API only accepts role="user" or role="model", not "system")
            val combinedPrompt = buildString {
                append(systemPrompt)
                append("\n\n")
                append(prompt)
            }
            parts.add(GeminiPart(text = combinedPrompt))

            // Add image parts (Base64 inline)
            if (files.isNotEmpty()) {
                Log.d(TAG, "Processing ${files.size} image(s) for vision analysis")

                for (file in files) {
                    val imageFile = File(file.url.removePrefix("file://"))

                    if (!imageFile.exists()) {
                        Log.w(TAG, "Image file not found: ${imageFile.absolutePath}")
                        continue
                    }

                    // Resize to 384x384 and encode to Base64
                    val base64Image = ImageUtils.resizeAndEncodeToBase64(imageFile)
                    if (base64Image == null) {
                        Log.w(TAG, "Failed to encode image: ${imageFile.name}")
                        continue
                    }

                    val mimeType = ImageUtils.getMimeType(imageFile)
                    parts.add(
                        GeminiPart(
                            inlineData = GeminiInlineData(
                                mimeType = mimeType,
                                data = base64Image
                            )
                        )
                    )
                    Log.d(TAG, "Added image: ${imageFile.name}, type: $mimeType")
                }
            }

            val request = GeminiRequest(
                contents = listOf(
                    // Single user message with system prompt + user prompt + images
                    GeminiContent(
                        role = "user",
                        parts = parts
                    )
                )
            )

            val response = geminiApi.generateContent(
                model = modelName,
                apiKey = apiKey,
                request = request
            )

            if (response.error != null) {
                Log.e(TAG, "Gemini API error: ${response.error.message}")
                return null
            }

            val result = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

            Log.d(TAG, "Gemini response: ${result?.take(100)}")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API call failed: ${e.message}", e)
            null
        }
    }
}
