package com.jnkim.poschedule.notifications.companion

import android.content.Context
import com.jnkim.poschedule.data.local.entity.PlanItemEntity
import com.jnkim.poschedule.domain.model.EmojiMapper
import com.jnkim.poschedule.domain.model.Mode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Selects the "next important" plan for the Status Companion notification.
 *
 * Uses simplified scoring based on NotificationArbitrator formula:
 * - Priority (0.3), Urgency (0.25), Importance (0.25), Actionability (0.15)
 * - Minus UserCost (0.3) and Fatigue (0.4)
 *
 * Filters plans by:
 * - Status: PENDING only (not DONE/SKIPPED)
 * - Time window: 6h for NORMAL/LOW_MOOD, 12h for RECOVERY/BUSY
 *
 * Returns the top scored plan or null if none available.
 */
object CompanionPlanSelector {

    /**
     * Selects the next important plan.
     *
     * @param plans List of all plan items for today
     * @param mode Current mode
     * @param currentTime Current time in milliseconds
     * @param context Android context for string formatting
     * @param language Language code (e.g., "ko", "en") for time formatting
     * @return NextPlanInfo for the selected plan, or null if no plans in window
     */
    fun selectNextImportant(
        plans: List<PlanItemEntity>,
        mode: Mode,
        currentTime: Long,
        context: Context,
        language: String
    ): NextPlanInfo? {
        // Determine lookAhead window based on mode
        val lookAheadHours = when (mode) {
            Mode.NORMAL, Mode.LOW_MOOD -> 6
            Mode.RECOVERY, Mode.BUSY -> 12
        }
        val lookAheadMillis = lookAheadHours * 60 * 60 * 1000L
        val windowEnd = currentTime + lookAheadMillis

        // Filter eligible plans
        val eligiblePlans = plans.filter { plan ->
            // Status must be PENDING
            val isPending = plan.status == "PENDING"

            // Must have a start time and be within the lookahead window
            val isInWindow = plan.startTimeMillis != null &&
                plan.startTimeMillis >= currentTime &&
                plan.startTimeMillis <= windowEnd

            isPending && isInWindow
        }

        if (eligiblePlans.isEmpty()) {
            return null
        }

        // Score and select top plan
        val scoredPlans = eligiblePlans.map { plan ->
            plan to calculateScore(plan, mode, currentTime)
        }.sortedByDescending { it.second }

        val topPlan = scoredPlans.firstOrNull()?.first ?: return null

        // Format time based on language
        val formattedTime = formatTime(topPlan.startTimeMillis ?: currentTime, language)

        // Get emoji for the plan
        val emoji = EmojiMapper.getEmojiForPlan(
            title = topPlan.title,
            planType = topPlan.type?.name
        )

        return NextPlanInfo(
            planId = topPlan.id.hashCode().toLong(),
            emoji = emoji,
            title = topPlan.title,
            targetTime = topPlan.startTimeMillis ?: currentTime,
            formattedTime = formattedTime
        )
    }

    /**
     * Calculates score for a plan using NotificationArbitrator formula.
     *
     * @param plan Plan item to score
     * @param mode Current mode (affects userCost and fatigue)
     * @param currentTime Current time in milliseconds
     * @return Score value (higher is more important)
     */
    private fun calculateScore(plan: PlanItemEntity, mode: Mode, currentTime: Long): Float {
        // Weights from NotificationArbitrator
        val wPriority = 0.3f
        val wUrgency = 0.25f
        val wImportance = 0.25f
        val wActionability = 0.15f
        val wUserCost = 0.3f
        val wFatigue = 0.4f

        // Base variables
        val priority = if (plan.isCore) 1.0f else 0.4f
        val urgency = calculateUrgency(plan, currentTime)
        val importance = if (plan.isCore) 0.9f else 0.5f
        val actionability = 1.0f // Manual tasks are highly actionable

        // Mode-aware costs
        val userCost = when (mode) {
            Mode.RECOVERY -> 0.8f
            Mode.LOW_MOOD -> 0.7f
            Mode.BUSY -> 0.9f
            Mode.NORMAL -> 0.3f
        }

        val fatigue = if (mode == Mode.RECOVERY) 0.8f else 0.2f

        return (wPriority * priority) +
                (wUrgency * urgency) +
                (wImportance * importance) +
                (wActionability * actionability) -
                (wUserCost * userCost) -
                (wFatigue * fatigue)
    }

    /**
     * Calculates urgency based on how much time remains in the window.
     *
     * @param plan Plan item
     * @param currentTime Current time in milliseconds
     * @return Urgency value (0.1 to 1.0, higher means more urgent)
     */
    private fun calculateUrgency(plan: PlanItemEntity, currentTime: Long): Float {
        if (plan.endTimeMillis == null) return 0.5f

        val totalWindow = plan.endTimeMillis - (plan.startTimeMillis ?: currentTime)
        val remaining = plan.endTimeMillis - currentTime

        if (remaining < 0) return 1.0f // Overdue

        // As window closes, urgency increases
        return (1.0f - (remaining.toFloat() / totalWindow.toFloat())).coerceIn(0.1f, 1.0f)
    }

    /**
     * Formats time based on language.
     *
     * @param timeMillis Time in milliseconds
     * @param language Language code ("ko" or "en")
     * @return Formatted time string (e.g., "오후 3:00" or "3:00 PM")
     */
    private fun formatTime(timeMillis: Long, language: String): String {
        val locale = if (language == "ko") Locale.KOREAN else Locale.ENGLISH
        val pattern = if (language == "ko") "a h:mm" else "h:mm a"
        val formatter = SimpleDateFormat(pattern, locale)
        return formatter.format(Date(timeMillis))
    }
}
