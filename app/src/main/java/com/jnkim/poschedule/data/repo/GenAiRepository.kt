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
     * Fetches and caches user API key from the server.
     * If successful, saves the API key to AuthTokenManager.
     */
    suspend fun fetchAndCacheApiKey(): String? {
        val accessToken = tokenManager.getAccessToken()
        if (accessToken == null) {
            android.util.Log.e("GenAiRepository", "Cannot fetch API key: access token is missing")
            return null
        }

        return try {
            android.util.Log.d("GenAiRepository", "Fetching API key from server...")
            val response = api.getUserApiKeys(
                bearerToken = "Bearer $accessToken"
            )

            if (response.code == "200" && response.data.isNotEmpty()) {
                val rawApiKey = response.data[0].rawApiKey
                android.util.Log.d("GenAiRepository", "API key fetched successfully: ${rawApiKey.take(10)}...")
                tokenManager.saveApiKey(rawApiKey)
                rawApiKey
            } else {
                android.util.Log.e("GenAiRepository", "No API keys found. Please create one at https://genai.postech.ac.kr/api-key/")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("GenAiRepository", "Failed to fetch API key: ${e.message}", e)
            null
        }
    }

    /**
     * Executes a chat completion request, optionally including uploaded files for multi-modal analysis.
     */
    suspend fun getCompletion(
        prompt: String,
        systemPrompt: String,
        files: List<GenAiFile> = emptyList()
    ): String? {
        val settings = settingsRepository.settingsFlow.first()
        val accessToken = tokenManager.getAccessToken()
        var apiKey = tokenManager.getApiKey()

        android.util.Log.d("GenAiRepository", "Checking credentials: accessToken=${accessToken != null}, apiKey=${apiKey != null}, siteName=${settings.siteName}")

        if (accessToken == null) {
            android.util.Log.e("GenAiRepository", "Access token is missing. Please log in first.")
            return null
        }

        // If API key is missing or invalid (simulated key), try to fetch it from server
        if (apiKey == null || apiKey.startsWith("sk-simulated")) {
            android.util.Log.w("GenAiRepository", "API key is missing or invalid (simulated). Attempting to fetch from server...")
            apiKey = fetchAndCacheApiKey()
            if (apiKey == null) {
                android.util.Log.e("GenAiRepository", "Failed to fetch API key. Please create one at https://genai.postech.ac.kr/api-key/")
                return null
            }
        }

        // Combine system prompt and user prompt into a single message
        val combinedMessage = buildString {
            append(systemPrompt)
            append("\n\n")
            append("---\n\n")
            append(prompt)
        }

        val request = GenAiRequest(
            message = combinedMessage,
            stream = false,
            files = files
        )

        // Retry up to 3 times for timeout/network errors
        var lastException: Exception? = null
        repeat(3) { attempt ->
            try {
                val fullUrl = "$baseUrl/agent/api/a3/claude?site_name=${settings.siteName}"
                android.util.Log.d("GenAiRepository", "=== API REQUEST DEBUG (Attempt ${attempt + 1}/3) ===")
                android.util.Log.d("GenAiRepository", "Full URL: $fullUrl")
                android.util.Log.d("GenAiRepository", "Headers:")
                android.util.Log.d("GenAiRepository", "  X-API-Key: ${apiKey.take(20)}...")
                android.util.Log.d("GenAiRepository", "  Origin: https://genai.postech.ac.kr")
                android.util.Log.d("GenAiRepository", "  Referer: https://genai.postech.ac.kr/")
                android.util.Log.d("GenAiRepository", "  User-Agent: Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36...")
                android.util.Log.d("GenAiRepository", "Body:")
                android.util.Log.d("GenAiRepository", "  message length: ${combinedMessage.length} chars")
                android.util.Log.d("GenAiRepository", "  stream: false")
                android.util.Log.d("GenAiRepository", "  files: ${request.files.size} files")
                android.util.Log.d("GenAiRepository", "========================")

                val response = api.getCompletion(
                    model = "a3/claude",  // Using Claude model
                    siteName = settings.siteName,
                    apiKey = apiKey,
                    request = request
                )
                val content = response.replies
                android.util.Log.d("GenAiRepository", "API response received: ${content.take(100)}")
                return content
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 401) {
                    // 401 error - API key might be invalid, try to refresh it
                    android.util.Log.w("GenAiRepository", "Got 401 error. Attempting to refresh API key...")
                    val newApiKey = fetchAndCacheApiKey()

                    if (newApiKey != null && newApiKey != apiKey) {
                        // Retry with new API key
                        android.util.Log.d("GenAiRepository", "Retrying with refreshed API key...")
                        try {
                            val response = api.getCompletion(
                                model = "a3/claude",
                                siteName = settings.siteName,
                                apiKey = newApiKey,
                                request = request
                            )
                            android.util.Log.d("GenAiRepository", "Retry successful!")
                            return response.replies
                        } catch (retryException: Exception) {
                            android.util.Log.e("GenAiRepository", "Retry also failed: ${retryException.message}", retryException)
                            return null
                        }
                    } else {
                        android.util.Log.e("GenAiRepository", "Failed to refresh API key or got same key")
                        return null
                    }
                } else {
                    android.util.Log.e("GenAiRepository", "API call failed with HTTP ${e.code()}: ${e.message()}", e)
                    return null
                }
            } catch (e: java.net.SocketTimeoutException) {
                lastException = e
                android.util.Log.w("GenAiRepository", "Timeout on attempt ${attempt + 1}/3: ${e.message}")
                if (attempt < 2) {
                    android.util.Log.d("GenAiRepository", "Waiting 2 seconds before retry...")
                    kotlinx.coroutines.delay(2000) // Wait 2 seconds before retry
                }
            } catch (e: java.io.IOException) {
                lastException = e
                android.util.Log.w("GenAiRepository", "Network error on attempt ${attempt + 1}/3: ${e.message}")
                if (attempt < 2) {
                    android.util.Log.d("GenAiRepository", "Waiting 2 seconds before retry...")
                    kotlinx.coroutines.delay(2000) // Wait 2 seconds before retry
                }
            } catch (e: Exception) {
                lastException = e
                android.util.Log.e("GenAiRepository", "API call failed on attempt ${attempt + 1}/3: ${e.message}", e)
                // For other exceptions, don't retry
                return null
            }
        }

        // All retries exhausted
        android.util.Log.e("GenAiRepository", "All 3 attempts failed. Last error: ${lastException?.message}", lastException)
        return null
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
