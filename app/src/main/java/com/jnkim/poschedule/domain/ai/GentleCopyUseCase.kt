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
    val completedTodayCount: Int
)

@Singleton
class GentleCopyUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val tokenManager: AuthTokenManager
) {
    suspend fun generateMessage(request: GentleCopyRequest): GentleCopyResponse {
        val settings = settingsRepository.settingsFlow.first()
        val language = settings.language // Use the app-internal setting
        
        // Safety Gate: Check if AI is enabled and API key exists
        if (!settings.aiEnabled || tokenManager.getApiKey() == null) {
            return getFallbackMessage(request.mode, language)
        }

        // Pass the app-specific language to the simulated/real generator
        return getSimulatedAiMessage(request, language)
    }

    private fun getSimulatedAiMessage(request: GentleCopyRequest, language: String): GentleCopyResponse {
        val isKo = language == "ko"
        return when (request.mode) {
            Mode.RECOVERY -> GentleCopyResponse(
                if (isKo) "잠시 숨 고르기" else "A small breath",
                if (isKo) "오늘 벌써 ${request.completedTodayCount}개의 일을 해내셨네요. 나머지는 천천히 해도 괜찮아요." 
                else "You've completed ${request.completedTodayCount} things. It's okay to let the other ${request.pendingItemsCount} wait.",
                "calm"
            )
            Mode.LOW_MOOD -> GentleCopyResponse(
                if (isKo) "친절한 한 걸음" else "Kindness first",
                if (isKo) "천천히 가도 괜찮습니다. 당신의 속도에 맞춰 작은 일 하나부터 시작해봐요."
                else "It's okay to take it slow. Just one tiny step at your own pace.",
                "calm"
            )
            Mode.BUSY -> GentleCopyResponse(
                if (isKo) "집중 모드" else "Focus Mode",
                if (isKo) "불필요한 알림을 줄였습니다. 핵심 루틴에만 집중하세요."
                else "Notifications reduced. Focus on your core routines for now.",
                "firm"
            )
            Mode.NORMAL -> GentleCopyResponse(
                if (isKo) "시스템 안정" else "System Stable",
                if (isKo) "오늘 계획대로 잘 진행되고 있습니다. 다음 단계는 무엇인가요?"
                else "The day is moving well. You've completed ${request.completedTodayCount} tasks. What's next?",
                "calm"
            )
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
    val tone: String
)
