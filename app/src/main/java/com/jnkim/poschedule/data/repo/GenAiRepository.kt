package com.jnkim.poschedule.data.repo

import com.jnkim.poschedule.data.local.AuthTokenManager
import com.jnkim.poschedule.data.remote.api.*
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GenAiRepository @Inject constructor(
    private val api: GenAiApi,
    private val tokenManager: AuthTokenManager,
    private val settingsRepository: SettingsRepository
) {
    private val baseUrl = "https://genai.postech.ac.kr"

    /**
     * Executes a chat completion request, optionally including uploaded files for multi-modal analysis.
     */
    suspend fun getCompletion(
        prompt: String, 
        systemPrompt: String,
        files: List<GenAiFile> = emptyList()
    ): String? {
        val settings = settingsRepository.settingsFlow.first()
        val accessToken = tokenManager.getAccessToken() ?: return null
        val apiKey = tokenManager.getApiKey() ?: return null

        val request = GenAiRequest(
            messages = listOf(
                GenAiMessage(role = "system", content = systemPrompt),
                GenAiMessage(role = "user", content = prompt)
            ),
            files = files
        )

        return try {
            val response = api.getCompletion(
                siteName = settings.siteName,
                bearerToken = "Bearer $accessToken",
                apiKey = apiKey,
                request = request
            )
            response.choices.firstOrNull()?.message?.content
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Uploads an image binary to the POSTECH GenAI server.
     * Constructs the server URL for the file as required by the A2 API.
     */
    suspend fun uploadFile(file: File): GenAiFile? {
        val settings = settingsRepository.settingsFlow.first()
        val accessToken = tokenManager.getAccessToken() ?: return null

        val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
        // Note: Using "files" as the part name to match server expectation
        val body = MultipartBody.Part.createFormData("files", file.name, requestFile)

        return try {
            val response = api.uploadFile(
                siteName = settings.siteName,
                bearerToken = "Bearer $accessToken",
                file = body
            )
            
            // Fixed the access to 'files' array from FileUploadResponse
            // Note: In some versions of the API it returns a single data object, in others a list.
            // Based on your TS reference, it returns an object with a 'files' array.
            // If the GenAiApi was defined with data: FileData, we use that. 
            // If it was defined with files: List, we use that.
            
            // Re-checking GenAiApi.kt: It currently has data: FileData. 
            // I will align the Repository to the actual GenAiApi.kt I wrote last.
            
            val fileId = response.data.id ?: "upload_${System.currentTimeMillis()}"
            val fileUrl = "$baseUrl/v2/athena/chats/m1/files/$fileId?site_name=${settings.siteName}"
            
            GenAiFile(
                id = fileId,
                name = file.name,
                url = fileUrl
            )
        } catch (e: Exception) {
            null
        }
    }
}
