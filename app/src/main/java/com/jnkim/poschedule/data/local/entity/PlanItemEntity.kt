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
    val source: PlanItemSource,
    val window: PlanItemWindow,
    val status: String, // PENDING, DONE, SNOOZED, SKIPPED
    val isCore: Boolean,
    val startTimeMillis: Long?,
    val endTimeMillis: Long?,
    val createdAt: Long = System.currentTimeMillis(),
    val originRef: String? = null, // e.g. tidySnapSessionId
    val seriesId: String? = null   // Link to PlanSeriesEntity
)
