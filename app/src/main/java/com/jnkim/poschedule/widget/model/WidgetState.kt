package com.jnkim.poschedule.widget.model

import com.jnkim.poschedule.data.local.entity.PlanItemEntity
import com.jnkim.poschedule.domain.model.Mode

/**
 * Widget state model for home screen widget.
 *
 * Represents the current state of the widget including the next pending task,
 * urgency information, and mode state.
 */
data class WidgetState(
    /**
     * Next pending task to display, or null if no tasks are pending.
     */
    val task: PlanItemEntity? = null,

    /**
     * Human-readable time until task starts.
     * Examples: "Starts in 15m", "Now", "Overdue by 10m"
     */
    val timeUntilStart: String? = null,

    /**
     * Urgency level based on time remaining until task window ends.
     */
    val urgencyLevel: UrgencyLevel = UrgencyLevel.NORMAL,

    /**
     * Current mode (NORMAL, RECOVERY, etc.) for styling and messaging.
     */
    val mode: Mode = Mode.NORMAL,

    /**
     * Timestamp when this state was last updated (System.currentTimeMillis()).
     * Used for cache invalidation.
     */
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Urgency level for visual styling in the widget.
 * Determines color coding and emphasis.
 */
enum class UrgencyLevel {
    /**
     * More than 30 minutes until task window ends.
     * Visual: Green indicator
     */
    NORMAL,

    /**
     * 10-30 minutes until task window ends.
     * Visual: Yellow indicator
     */
    MODERATE,

    /**
     * Less than 10 minutes until task window ends, or task is overdue.
     * Visual: Red indicator
     */
    URGENT
}
