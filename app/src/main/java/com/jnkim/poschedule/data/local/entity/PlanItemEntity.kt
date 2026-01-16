package com.jnkim.poschedule.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jnkim.poschedule.domain.model.RoutineType

enum class PlanItemSource {
    DETERMINISTIC,
    MANUAL,
    VISION,
    LLM_SUGGESTED
}

enum class PlanItemWindow {
    MORNING,
    MIDDAY,
    EVENING,
    ANYTIME
}

@Entity(tableName = "plan_items")
data class PlanItemEntity(
    @PrimaryKey val id: String,
    val date: String, // yyyy-MM-dd
    val title: String,
    val type: RoutineType?,
    val iconEmoji: String? = null, // LLM-generated emoji icon (overrides RoutineType default)
    val source: PlanItemSource,
    val window: PlanItemWindow,
    val status: String, // PENDING, DONE, SNOOZED, SKIPPED
    val isCore: Boolean,
    val startTimeMillis: Long?,
    val endTimeMillis: Long?,
    val createdAt: Long = System.currentTimeMillis(),
    val originRef: String? = null, // e.g. tidySnapSessionId
    val seriesId: String? = null,   // Link to PlanSeriesEntity
    val snoozeUntil: Long? = null,  // Instant.toEpochMilli() when snooze expires, null if not snoozed

    // ============================================================================
    // DEPRECATED FIELDS (Phase 1 - kept for backward compatibility)
    // ============================================================================
    // These fields are deprecated in favor of separate rich data tables.
    // They will be migrated to plan_meetings, plan_locations, plan_contacts, plan_notes
    // in future versions. New code should use PlanWithRichData queries instead.

    @Deprecated("Use plan_meetings table instead", ReplaceWith("PlanMeetingEntity"))
    val joinUrl: String? = null,

    @Deprecated("Use plan_meetings table instead", ReplaceWith("PlanMeetingEntity"))
    val webUrl: String? = null,

    @Deprecated("Use plan_locations table instead", ReplaceWith("PlanLocationEntity"))
    val locationText: String? = null,

    @Deprecated("Use plan_locations table instead", ReplaceWith("PlanLocationEntity"))
    val mapQuery: String? = null,

    @Deprecated("Not used - removed in Phase 1.5")
    val deepLinkUrl: String? = null,

    @Deprecated("Not used - removed in Phase 1.5")
    val packageName: String? = null,

    @Deprecated("Use plan_contacts table instead", ReplaceWith("PlanContactEntity"))
    val contactName: String? = null,

    @Deprecated("Use plan_contacts table instead", ReplaceWith("PlanContactEntity"))
    val contactEmail: String? = null,

    @Deprecated("Use plan_notes table instead", ReplaceWith("PlanNoteEntity"))
    val sourceSnippet: String? = null

    // Phase 1.5: Use PlanWithRichData for querying plans with rich metadata
    // Separate tables allow unlimited extensibility without schema changes
)
