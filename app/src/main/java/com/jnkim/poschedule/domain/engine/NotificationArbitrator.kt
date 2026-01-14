package com.jnkim.poschedule.domain.engine

import com.jnkim.poschedule.data.local.entity.PlanItemEntity
import com.jnkim.poschedule.data.repo.SettingsRepository
import com.jnkim.poschedule.domain.model.Mode
import kotlinx.coroutines.flow.first
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationArbitrator @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    /**
     * Decides which plan items are eligible for a notification right now.
     * Implements the advanced scoring and budget rules.
     */
    suspend fun arbitrate(
        candidates: List<PlanItemEntity>,
        currentMode: Mode,
        notificationsSentToday: Int
    ): List<PlanItemEntity> {
        val settings = settingsRepository.settingsFlow.first()
        val now = LocalTime.now()

        // 1. Hard Constraint: Quiet Hours
        if (isWithinQuietHours(now, settings.quietHoursStart, settings.quietHoursEnd)) {
            return emptyList()
        }

        // 2. Hard Constraint: Daily Budget
        if (notificationsSentToday >= settings.dailyBudget) {
            // Over budget: only allow CORE items if they are truly critical
            return candidates.filter { it.isCore }
        }

        // 3. Scoring & Thresholding
        // Using the formula from PLAN.md Section 9
        val scoredCandidates = candidates.map { it to calculateScore(it, currentMode) }
            .filter { it.second > 0.65f } // Arbitrary threshold for promotion
            .sortedByDescending { it.second }

        // 4. Mode-based Capacity
        val capacity = when (currentMode) {
            Mode.RECOVERY, Mode.LOW_MOOD -> 1 // Max 1 notification at a time
            Mode.BUSY -> 0 // Only Core routines pass through 2.0 filter above
            Mode.NORMAL -> 2 // Allow slight batching
        }

        return scoredCandidates.take(capacity).map { it.first }
    }

    private fun calculateScore(item: PlanItemEntity, mode: Mode): Float {
        // weights from PLAN.md
        val wPriority = 0.3f
        val wUrgency = 0.25f
        val wImportance = 0.25f
        val wActionability = 0.15f
        val wUserCost = 0.3f
        val wFatigue = 0.4f

        // Base variables
        val priority = if (item.isCore) 1.0f else 0.4f
        val urgency = calculateUrgency(item)
        val importance = if (item.isCore) 0.9f else 0.5f
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

    private fun calculateUrgency(item: PlanItemEntity): Float {
        if (item.endTimeMillis == null) return 0.5f
        val now = System.currentTimeMillis()
        val totalWindow = item.endTimeMillis - (item.startTimeMillis ?: now)
        val remaining = item.endTimeMillis - now
        
        if (remaining < 0) return 1.0f // Overdue
        
        // As window closes, urgency increases
        return (1.0f - (remaining.toFloat() / totalWindow.toFloat())).coerceIn(0.1f, 1.0f)
    }

    private fun isWithinQuietHours(now: LocalTime, startStr: String, endStr: String): Boolean {
        return try {
            val start = LocalTime.parse(startStr)
            val end = LocalTime.parse(endStr)
            if (start.isBefore(end)) {
                now.isAfter(start) && now.isBefore(end)
            } else {
                now.isAfter(start) || now.isBefore(end)
            }
        } catch (e: Exception) {
            false
        }
    }
}
