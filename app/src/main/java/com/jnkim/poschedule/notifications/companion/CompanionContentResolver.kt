package com.jnkim.poschedule.notifications.companion

import android.app.Notification
import android.content.Context
import com.jnkim.poschedule.data.local.entity.PlanItemEntity
import com.jnkim.poschedule.data.repo.SettingsRepository
import com.jnkim.poschedule.domain.ai.GentleCopyRequest
import com.jnkim.poschedule.domain.ai.GentleCopyUseCase
import com.jnkim.poschedule.domain.model.Mode
import com.jnkim.poschedule.domain.model.ModeConstants
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves all content for the Status Companion notification.
 *
 * Assembles:
 * - Mode badge and label from ModeConstants
 * - Supportive message from GentleCopyUseCase
 * - Next important plan from CompanionPlanSelector
 * - Visibility setting from user preferences
 */
@Singleton
class CompanionContentResolver @Inject constructor(
    private val gentleCopyUseCase: GentleCopyUseCase,
    private val settingsRepository: SettingsRepository
) {

    /**
     * Resolves complete notification content.
     *
     * @param mode Current mode
     * @param plans List of all plan items for today
     * @param context Android context
     * @return CompanionContent ready for notification building
     */
    suspend fun resolveContent(
        mode: Mode,
        plans: List<PlanItemEntity>,
        context: Context
    ): CompanionContent {
        val settings = settingsRepository.settingsFlow.first()
        val language = settings.language

        // 1. Mode badge and label
        val modeBadge = ModeConstants.getModeBadge(mode)
        val modeLabel = ModeConstants.getModeLabel(mode, context)

        // 2. Supportive message from GentleCopyUseCase
        val supportiveMessage = generateSupportiveMessage(mode, plans)

        // 3. Next important plan
        val currentTime = System.currentTimeMillis()
        val nextPlan = CompanionPlanSelector.selectNextImportant(
            plans = plans,
            mode = mode,
            currentTime = currentTime,
            context = context,
            language = language
        )

        // 4. Visibility based on settings
        val visibility = if (settings.lockscreenDetailsEnabled) {
            Notification.VISIBILITY_PUBLIC
        } else {
            Notification.VISIBILITY_PRIVATE
        }

        return CompanionContent(
            modeBadge = modeBadge,
            modeLabel = modeLabel,
            supportiveMessage = supportiveMessage,
            nextPlan = nextPlan,
            visibility = visibility
        )
    }

    /**
     * Generates supportive message using GentleCopyUseCase.
     *
     * @param mode Current mode
     * @param plans All plan items for today
     * @return Supportive message string
     */
    private suspend fun generateSupportiveMessage(
        mode: Mode,
        plans: List<PlanItemEntity>
    ): String {
        // Count plan statuses
        val pendingCount = plans.count { it.status == "PENDING" }
        val completedCount = plans.count { it.status == "DONE" }
        val snoozedCount = plans.count { it.snoozeUntil != null && it.snoozeUntil > System.currentTimeMillis() }
        val skippedCount = plans.count { it.status == "SKIPPED" }

        // Build request for GentleCopyUseCase
        val request = GentleCopyRequest(
            mode = mode,
            pendingItemsCount = pendingCount,
            completedTodayCount = completedCount,
            notificationBudgetUsed = 0, // Not relevant for companion
            notificationBudgetTotal = 7, // Default budget
            todayWindows = plans.map { it.title },
            recentActions = mapOf(
                "DONE" to completedCount,
                "SNOOZED" to snoozedCount,
                "SKIPPED" to skippedCount
            )
        )

        return try {
            val response = gentleCopyUseCase.generateMessage(request)
            response.body
        } catch (e: Exception) {
            android.util.Log.w("CompanionContentResolver", "Failed to generate gentle copy", e)
            // Fallback message based on mode
            getFallbackMessage(mode)
        }
    }

    /**
     * Returns a simple fallback message if GentleCopyUseCase fails.
     *
     * @param mode Current mode
     * @return Fallback message string
     */
    private suspend fun getFallbackMessage(mode: Mode): String {
        val settings = settingsRepository.settingsFlow.first()
        val isKo = settings.language == "ko"

        return when (mode) {
            Mode.RECOVERY -> if (isKo) "휴식과 회복을 우선으로 합니다." else "Prioritizing rest and recovery."
            Mode.LOW_MOOD -> if (isKo) "한 걸음씩 천천히 나아갑니다." else "Taking it one step at a time."
            Mode.BUSY -> if (isKo) "오늘도 좋은 하루 보내세요." else "Have a productive day."
            Mode.NORMAL -> if (isKo) "오늘도 좋은 하루입니다." else "Have a great day."
        }
    }
}
