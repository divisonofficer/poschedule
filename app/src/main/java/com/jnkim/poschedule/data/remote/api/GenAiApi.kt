package com.jnkim.poschedule.data.remote.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import retrofit2.http.*

// New simplified API format for /agent/api endpoints
@Serializable
data class GenAiRequest(
    val message: String,
    val stream: Boolean = false,
    val files: List<GenAiFile> = emptyList()
)

@Serializable
data class GenAiFile(
    val id: String,
    val name: String,
    val url: String
)

@Serializable
data class GenAiResponse(
    val replies: String
)

@Serializable
data class FileUploadResponse(
    val files: List<FileUploadData>
)

@Serializable
data class FileUploadData(
    val id: String,
    val name: String
)

@Serializable
data class ApiKeysResponse(
    val code: String,
    val message: String? = null,
    val data: List<ApiKeyData>
)

@Serializable
data class ApiKeyData(
    val id: Int,
    val rawApiKey: String,
    val apiKeyPreview: String,
    val createdAt: String
)

interface GenAiApi {
    /**
     * Chat completion using the agent API.
     * Supports: a1/gpt, a2/gemini, a3/claude
     * Default: a3/claude (Claude model)
     *
     * IMPORTANT: Only uses X-API-Key (NOT Authorization)
     * IMPORTANT: Origin, Referer, and User-Agent headers are required for authentication
     */
    @POST("agent/api/{model}")
    suspend fun getCompletion(
        @Path("model") model: String = "a3/claude",
        @Header("X-API-Key") apiKey: String,
        @Header("Origin") origin: String = "https://genai.postech.ac.kr",
        @Header("Referer") referer: String = "https://genai.postech.ac.kr/",
        @Header("User-Agent") userAgent: String = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
        @Body request: GenAiRequest
    ): GenAiResponse

    /**
     * Upload file for multi-modal analysis.
     *
     * IMPORTANT: Origin, Referer, and User-Agent headers are required for authentication
     */
    @Multipart
    @POST("v2/athena/chats/{model}/files")
    suspend fun uploadFile(
        @Path("model") model: String = "m1",
        @Query("site_name") siteName: String,
        @Header("Authorization") bearerToken: String,
        @Header("Origin") origin: String = "https://genai.postech.ac.kr",
        @Header("Referer") referer: String = "https://genai.postech.ac.kr/",
        @Header("User-Agent") userAgent: String = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
        @Part file: MultipartBody.Part
    ): FileUploadResponse

    /**
     * Retrieves user API keys from the GenAI server.
     *
     * IMPORTANT: Uses Authorization header (NOT X-API-Key)
     * IMPORTANT: Referer header is required
     */
    @GET("v2/datahub/user-api-keys")
    suspend fun getUserApiKeys(
        @Header("Authorization") bearerToken: String,
        @Header("Referer") referer: String = "https://genai.postech.ac.kr/"
    ): ApiKeysResponse
}
