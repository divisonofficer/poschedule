package com.jnkim.poschedule.domain.ai

import com.jnkim.poschedule.data.local.AuthTokenManager
import com.jnkim.poschedule.data.repo.SettingsRepository
import com.jnkim.poschedule.domain.model.Mode
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

data class GentleCopyRequest(
    val mode: Mode,
    val pendingItemsCount: Int,
    val completedTodayCount: Int,
    val notificationBudgetUsed: Int,
    val notificationBudgetTotal: Int,
    val todayWindows: List<String>,  // Titles of today's plan items
    val recentActions: Map<String, Int>  // Counts by status (DONE/SNOOZED/SKIPPED)
)

@Singleton
class GentleCopyUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val tokenManager: AuthTokenManager,
    private val genAiClient: com.jnkim.poschedule.data.ai.GenAiClient  // NEW DEPENDENCY
) {
    // In-memory cache: key = "$date-$mode", value = (response, timestamp)
    private val messageCache = mutableMapOf<String, Pair<GentleCopyResponse, Long>>()

    private fun getCacheKey(date: String, mode: Mode): String = "$date-$mode"

    private fun getCached(date: String, mode: Mode): GentleCopyResponse? {
        val key = getCacheKey(date, mode)
        val (response, timestamp) = messageCache[key] ?: return null

        // Cache valid until day boundary
        val now = System.currentTimeMillis()
        val dayBoundary = java.time.LocalDate.now()
            .plusDays(1)
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        return if (now < dayBoundary) response else {
            messageCache.remove(key)
            null
        }
    }

    private fun putCache(date: String, mode: Mode, response: GentleCopyResponse) {
        val key = getCacheKey(date, mode)
        messageCache[key] = response to System.currentTimeMillis()
    }

    suspend fun generateMessage(request: GentleCopyRequest): GentleCopyResponse {
        val settings = settingsRepository.settingsFlow.first()
        val language = settings.language
        val today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)

        // Check cache first
        getCached(today, request.mode)?.let {
            android.util.Log.d("GentleCopy", "Cache hit for $today-${request.mode}")
            return it
        }

        // Safety Gate: Check if AI is enabled and API key exists
        if (!settings.aiEnabled || tokenManager.getApiKey() == null) {
            android.util.Log.d("GentleCopy", "Safety gate: AI=${settings.aiEnabled}, Key=${tokenManager.getApiKey() != null}")
            return getFallbackMessage(request.mode, language)
        }

        // Call real API via GenAiClient
        return try {
            android.util.Log.d("GentleCopy", "Calling real API for mode=${request.mode}")
            val response = genAiClient.generateGentleCopy(request)
            // Cache successful response
            putCache(today, request.mode, response)
            android.util.Log.d("GentleCopy", "API success, cached response")
            response
        } catch (e: Exception) {
            // Fallback on any error (network, parsing, 401, 429, etc.)
            android.util.Log.w("GentleCopyUseCase", "LLM call failed, using fallback", e)
            getFallbackMessage(request.mode, language)
        }
    }

    private fun getFallbackMessage(mode: Mode, language: String): GentleCopyResponse {
        val isKo = language == "ko"
        return when (mode) {
            Mode.RECOVERY -> GentleCopyResponse(
                if (isKo) "회복 모드" else "Recovery Mode", 
                if (isKo) "휴식과 핵심 루틴을 우선으로 합니다." else "Prioritizing rest and core routines.", 
                "calm"
            )
            Mode.LOW_MOOD -> GentleCopyResponse(
                if (isKo) "나에게 친절하기" else "Be Kind", 
                if (isKo) "한 번에 한 걸음씩만 나아갑니다." else "Taking it one step at a time.", 
                "calm"
            )
            Mode.BUSY -> GentleCopyResponse(
                if (isKo) "집중 모드" else "Busy Mode", 
                if (isKo) "불필요한 방해를 최소화합니다." else "Minimizing interruptions.", 
                "firm"
            )
            Mode.NORMAL -> GentleCopyResponse(
                if (isKo) "시스템 안정" else "Stable", 
                if (isKo) "오늘의 계획을 차근차근 진행합니다." else "Proceeding with your plan.", 
                "calm"
            )
        }
    }
}

data class GentleCopyResponse(
    val title: String,
    val body: String,
    val tone: String,
    val safetyTag: String? = null  // Optional safety flag
)
