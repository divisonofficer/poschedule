package com.jnkim.poschedule.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jnkim.poschedule.domain.model.RoutineType

enum class PlanType {
    ROUTINE, TASK, CHORE
}

enum class TimeAnchor {
    WAKE, BED, FIXED
}

enum class RecurrenceFrequency {
    DAILY, WEEKLY, MONTHLY
}

@Entity(tableName = "plan_series")
data class PlanSeriesEntity(
    @PrimaryKey val id: String,
    val title: String,
    val type: PlanType,
    val routineType: RoutineType?,
    val isCore: Boolean,
    val anchor: TimeAnchor,
    val startOffsetMin: Int,
    val endOffsetMin: Int,
    val frequency: RecurrenceFrequency,
    val interval: Int = 1,
    val byDays: String? = null,
    val isActive: Boolean = true, // Added to match DAO
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
