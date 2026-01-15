package com.jnkim.poschedule.data.remote.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for Google Gemini REST API v1
 * Official documentation: https://ai.google.dev/api/rest/v1/models/generateContent
 */
interface GeminiApi {
    /**
     * Generate content using Gemini models (v1 endpoint)
     */
    @POST("v1/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

@Serializable
data class GeminiRequest(
    @SerialName("contents")
    val contents: List<GeminiContent>
)

@Serializable
data class GeminiContent(
    @SerialName("role")
    val role: String? = null,
    @SerialName("parts")
    val parts: List<GeminiPart>
)

@Serializable
data class GeminiPart(
    @SerialName("text")
    val text: String? = null,
    @SerialName("inlineData")
    val inlineData: GeminiInlineData? = null
)

@Serializable
data class GeminiInlineData(
    @SerialName("mimeType")
    val mimeType: String,
    @SerialName("data")
    val data: String
)

@Serializable
data class GeminiResponse(
    @SerialName("candidates")
    val candidates: List<GeminiCandidate>? = null,
    @SerialName("error")
    val error: GeminiError? = null
)

@Serializable
data class GeminiCandidate(
    @SerialName("content")
    val content: GeminiContent
)

@Serializable
data class GeminiError(
    @SerialName("code")
    val code: Int,
    @SerialName("message")
    val message: String,
    @SerialName("status")
    val status: String
)
