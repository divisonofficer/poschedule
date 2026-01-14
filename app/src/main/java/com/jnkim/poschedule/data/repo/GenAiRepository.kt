package com.jnkim.poschedule.data.repo

import com.jnkim.poschedule.data.local.AuthTokenManager
import com.jnkim.poschedule.data.remote.api.GenAiApi
import com.jnkim.poschedule.data.remote.api.GenAiMessage
import com.jnkim.poschedule.data.remote.api.GenAiRequest
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
    suspend fun getCompletion(prompt: String, systemPrompt: String): String? {
        val settings = settingsRepository.settingsFlow.first()
        val accessToken = tokenManager.getAccessToken() ?: return null
        val apiKey = tokenManager.getApiKey() ?: return null

        val request = GenAiRequest(
            messages = listOf(
                GenAiMessage(role = "system", content = systemPrompt),
                GenAiMessage(role = "user", content = prompt)
            )
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
     * Uploads an image file to POSTECH GenAI and returns the server URL.
     */
    suspend fun uploadFile(file: File): String? {
        val settings = settingsRepository.settingsFlow.first()
        val accessToken = tokenManager.getAccessToken() ?: return null

        val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

        return try {
            val response = api.uploadFile(
                siteName = settings.siteName,
                bearerToken = "Bearer $accessToken",
                file = body
            )
            response.data.file_url
        } catch (e: Exception) {
            null
        }
    }
}
