package com.jnkim.poschedule.data.remote.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import retrofit2.http.*

@Serializable
data class GenAiRequest(
    val messages: List<GenAiMessage>,
    val temperature: Float = 0.7f,
    val response_format: String? = "json",
    val files: List<GenAiFile> = emptyList(),
    @SerialName("chat_threads_id") val chatThreadsId: String? = null,
    @SerialName("chat_room_id") val chatRoomsId: String? = null
)

@Serializable
data class GenAiMessage(
    val role: String,
    val content: String
)

@Serializable
data class GenAiFile(
    val id: String,
    val name: String,
    val url: String
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
    val id: String? = null,
    @SerialName("file_url") val fileUrl: String
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
