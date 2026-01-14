package com.jnkim.poschedule.domain.engine

import com.jnkim.poschedule.data.local.dao.PlanDao
import com.jnkim.poschedule.data.local.entity.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurrenceEngine @Inject constructor(
    private val planDao: PlanDao
) {

    /**
     * Expands a routine series into a plan item for a specific date if it matches the rules.
     * Implements Section 5.4 Anchored Windows logic and respects Slice 2 Exceptions.
     */
    suspend fun expand(
        series: PlanSeriesEntity, 
        date: LocalDate,
        wakeEstimate: LocalTime = LocalTime.of(8, 0), // TODO: Fetch from user prefs
        bedTarget: LocalTime = LocalTime.of(23, 0)
    ): PlanItemEntity? {
        
        // 1. Check for Slice 2 Exceptions
        if (planDao.isExcluded(series.id, date.toString())) {
            return null
        }

        // 2. Check Recurrence Matching
        val isMatch = when (series.frequency) {
            RecurrenceFrequency.DAILY -> true
            RecurrenceFrequency.WEEKLY -> {
                val dayOfWeek = date.dayOfWeek.value // 1 (Mon) to 7 (Sun)
                val activeDays = series.byDays?.split(",")?.map { it.trim().toInt() } ?: emptyList()
                activeDays.contains(dayOfWeek)
            }
            RecurrenceFrequency.MONTHLY -> {
                val dayOfMonth = date.dayOfMonth
                val targetDay = series.byDays?.trim()?.toIntOrNull() ?: 1
                dayOfMonth == targetDay
            }
        }

        if (!isMatch) return null

        // 3. Calculate Anchored Window (G4: Notification Calibration)
        val dateStr = date.toString()
        val zoneId = ZoneId.systemDefault()
        
        val anchorTime = when (series.anchor) {
            TimeAnchor.WAKE -> wakeEstimate
            TimeAnchor.BED -> bedTarget
            TimeAnchor.FIXED -> LocalTime.of(9, 0) // Placeholder
        }

        val startTime = date.atTime(anchorTime.plusMinutes(series.startOffsetMin.toLong()))
            .atZone(zoneId).toInstant().toEpochMilli()
        val endTime = date.atTime(anchorTime.plusMinutes(series.endOffsetMin.toLong()))
            .atZone(zoneId).toInstant().toEpochMilli()

        return PlanItemEntity(
            id = "${series.id}_$dateStr", // Deterministic Instance ID
            date = dateStr,
            title = series.title,
            type = series.routineType,
            source = PlanItemSource.DETERMINISTIC,
            window = mapToLegacyWindow(series.anchor),
            status = "PENDING",
            isCore = series.isCore,
            startTimeMillis = startTime,
            endTimeMillis = endTime,
            seriesId = series.id
        )
    }

    private fun mapToLegacyWindow(anchor: TimeAnchor): PlanItemWindow {
        return when (anchor) {
            TimeAnchor.WAKE -> PlanItemWindow.MORNING
            TimeAnchor.BED -> PlanItemWindow.EVENING
            TimeAnchor.FIXED -> PlanItemWindow.MIDDAY
        }
    }
}
