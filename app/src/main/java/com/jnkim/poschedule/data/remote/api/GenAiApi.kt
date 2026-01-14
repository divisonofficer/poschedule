package com.jnkim.poschedule.data.remote.api

import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class GenAiRequest(
    val messages: List<GenAiMessage>,
    val temperature: Float = 0.7f,
    val response_format: String? = "json"
)

@Serializable
data class GenAiMessage(
    val role: String,
    val content: String // For multimodal, this might be a complex structure in some APIs
)

@Serializable
data class GenAiResponse(
    val choices: List<GenAiChoice>
)

@Serializable
data class GenAiChoice(
    val message: GenAiMessage
)

@Serializable
data class FileUploadResponse(
    val code: String,
    val data: FileData
)

@Serializable
data class FileData(
    val file_url: String
)

interface GenAiApi {
    @POST("v2/athena/chats/{model}/completions")
    suspend fun getCompletion(
        @Path("model") model: String = "m1",
        @Query("site_name") siteName: String,
        @Header("Authorization") bearerToken: String,
        @Header("X-API-Key") apiKey: String,
        @Body request: GenAiRequest
    ): GenAiResponse

    @Multipart
    @POST("v2/athena/chats/{model}/files")
    suspend fun uploadFile(
        @Path("model") model: String = "m1",
        @Query("site_name") siteName: String,
        @Header("Authorization") bearerToken: String,
        @Part file: MultipartBody.Part
    ): FileUploadResponse
}
