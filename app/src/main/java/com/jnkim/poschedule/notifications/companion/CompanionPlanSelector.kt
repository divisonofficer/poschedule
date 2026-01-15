package com.jnkim.poschedule.notifications.companion

import android.content.Context
import com.jnkim.poschedule.data.local.entity.PlanItemEntity
import com.jnkim.poschedule.domain.model.EmojiMapper
import com.jnkim.poschedule.domain.model.Mode
import java.text.SimpleDateFormat
import java.util.Calendar
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
     * Selects the next important plan (legacy method).
     *
     * **Deprecated in V2:** Use selectNextImportantPlans() for multiple plan support.
     * This method delegates to selectNextImportantPlans() with maxCount = 1.
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
        return selectNextImportantPlans(
            plans = plans,
            mode = mode,
            currentTime = currentTime,
            context = context,
            language = language,
            maxCount = 1
        ).firstOrNull()
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

    /**
     * Selects multiple next important plans (V2 - 2026 Upgrade).
     *
     * Uses same scoring formula as selectNextImportant() but returns up to maxCount plans.
     *
     * @param plans List of all plan items for today
     * @param mode Current mode
     * @param currentTime Current time in milliseconds
     * @param context Android context for string formatting
     * @param language Language code (e.g., "ko", "en") for time formatting
     * @param maxCount Maximum number of plans to return (default 2)
     * @return List of NextPlanInfo for selected plans (may be empty)
     */
    fun selectNextImportantPlans(
        plans: List<PlanItemEntity>,
        mode: Mode,
        currentTime: Long,
        context: Context,
        language: String,
        maxCount: Int = 2
    ): List<NextPlanInfo> {
        // Determine lookAhead window based on mode
        val lookAheadHours = when (mode) {
            Mode.NORMAL, Mode.LOW_MOOD -> 6
            Mode.RECOVERY, Mode.BUSY -> 12
        }
        val lookAheadMillis = lookAheadHours * 60 * 60 * 1000L
        val windowEnd = currentTime + lookAheadMillis

        // Filter eligible plans
        val eligiblePlans = plans.filter { plan ->
            val isPending = plan.status == "PENDING"
            val isInWindow = plan.startTimeMillis != null &&
                plan.startTimeMillis >= currentTime &&
                plan.startTimeMillis <= windowEnd
            isPending && isInWindow
        }

        if (eligiblePlans.isEmpty()) {
            return emptyList()
        }

        // Score and select top plans
        val scoredPlans = eligiblePlans.map { plan ->
            plan to calculateScore(plan, mode, currentTime)
        }.sortedByDescending { it.second }
            .take(maxCount)

        // Map to NextPlanInfo
        return scoredPlans.map { (plan, _) ->
            val formattedTime = formatTimeForPlan(
                timeMillis = plan.startTimeMillis ?: currentTime,
                currentTime = currentTime,
                language = language
            )

            val emoji = EmojiMapper.getEmojiForPlan(
                title = plan.title,
                planType = plan.type?.name
            )

            NextPlanInfo(
                planId = plan.id.hashCode().toLong(),
                emoji = emoji,
                title = plan.title,
                targetTime = plan.startTimeMillis ?: currentTime,
                formattedTime = formattedTime
            )
        }
    }

    /**
     * Formats time for a plan with smart date awareness (V2 - 2026 Upgrade).
     *
     * Shows exact time if within 2 hours, otherwise shows relative day.
     *
     * @param timeMillis Plan target time in milliseconds
     * @param currentTime Current time in milliseconds
     * @param language Language code ("ko" or "en")
     * @return Formatted time string (e.g., "15:42", "오늘", "내일", "Tomorrow")
     */
    private fun formatTimeForPlan(
        timeMillis: Long,
        currentTime: Long,
        language: String
    ): String {
        val timeDiff = timeMillis - currentTime
        val twoHoursInMillis = 2 * 60 * 60 * 1000L

        // If within 2 hours, show exact time
        if (timeDiff <= twoHoursInMillis) {
            val locale = if (language == "ko") Locale.KOREAN else Locale.ENGLISH
            val pattern = if (language == "ko") "HH:mm" else "h:mm a"
            val formatter = SimpleDateFormat(pattern, locale)
            return formatter.format(Date(timeMillis))
        }

        // Check if today or tomorrow
        val planCalendar = Calendar.getInstance().apply {
            this.timeInMillis = timeMillis
        }
        val currentCalendar = Calendar.getInstance().apply {
            this.timeInMillis = currentTime
        }

        val planDay = planCalendar.get(Calendar.DAY_OF_YEAR)
        val currentDay = currentCalendar.get(Calendar.DAY_OF_YEAR)

        return when {
            planDay == currentDay -> if (language == "ko") "오늘" else "Today"
            planDay == currentDay + 1 -> if (language == "ko") "내일" else "Tomorrow"
            else -> {
                // Fallback to date for plans beyond tomorrow
                val locale = if (language == "ko") Locale.KOREAN else Locale.ENGLISH
                val pattern = if (language == "ko") "M월 d일" else "MMM d"
                val formatter = SimpleDateFormat(pattern, locale)
                formatter.format(Date(timeMillis))
            }
        }
    }
}
