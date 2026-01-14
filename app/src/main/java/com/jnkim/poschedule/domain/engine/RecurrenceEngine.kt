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
     *
     * CRITICAL TIMEZONE BEHAVIOR:
     * - `date` parameter represents a LOCAL date (e.g., 2026-01-14 in Korea)
     * - All time calculations are done in the system's local timezone
     * - The returned PlanItemEntity.date field ALWAYS matches the input `date` parameter
     * - Timestamps are stored in UTC but represent the local time on the given local date
     */
    suspend fun expand(
        series: PlanSeriesEntity,
        date: LocalDate,
        wakeEstimate: LocalTime = LocalTime.of(8, 0), // TODO: Fetch from user prefs
        bedTarget: LocalTime = LocalTime.of(23, 0)
    ): PlanItemEntity? {

        // CRITICAL: Use the input date parameter directly for the date string
        // This ensures the plan is always associated with the correct LOCAL date
        val dateStr = date.toString() // e.g., "2026-01-14"

        // 1. Check for Slice 2 Exceptions
        if (planDao.isExcluded(series.id, dateStr)) {
            return null
        }

        // 2. Check Recurrence Matching
        val isMatch = when (series.frequency) {
            RecurrenceFrequency.DAILY -> true
            RecurrenceFrequency.WEEKLY -> {
                val dayOfWeek = date.dayOfWeek.value // 1 (Mon) to 7 (Sun)
                val activeDays = series.byDays?.split(",")?.mapNotNull { it.trim().toIntOrNull() } ?: emptyList()
                activeDays.isEmpty() || activeDays.contains(dayOfWeek)
            }
            RecurrenceFrequency.MONTHLY -> {
                val dayOfMonth = date.dayOfMonth
                val targetDay = series.byDays?.trim()?.toIntOrNull() ?: 1
                dayOfMonth == targetDay
            }
        }

        if (!isMatch) return null

        // 3. Calculate Anchored Window (G4: Notification Calibration)
        // IMPORTANT: All time calculations use the system's local timezone
        val zoneId = ZoneId.systemDefault()

        val anchorTime = when (series.anchor) {
            TimeAnchor.WAKE -> wakeEstimate
            TimeAnchor.BED -> bedTarget
            TimeAnchor.FIXED -> LocalTime.MIDNIGHT // FIXED anchor is absolute time from midnight
        }

        // Build LocalDateTime for the given date in local timezone
        // Example: date=2026-01-14, anchorTime=00:00, offset=1080min → 2026-01-14T18:00 (local)
        val startDateTime = date.atTime(anchorTime).plusMinutes(series.startOffsetMin.toLong())
        val endDateTime = date.atTime(anchorTime).plusMinutes(series.endOffsetMin.toLong())

        // Convert to UTC timestamps for storage
        // Example: 2026-01-14T18:00+09:00 → 2026-01-14T09:00:00Z
        val startTime = startDateTime.atZone(zoneId).toInstant().toEpochMilli()
        val endTime = endDateTime.atZone(zoneId).toInstant().toEpochMilli()

        // Debug logging for timestamp generation
        android.util.Log.d("RecurrenceEngine",
            "Expanding ${series.title} for $dateStr | " +
            "anchor=${series.anchor}, startOffset=${series.startOffsetMin}, endOffset=${series.endOffsetMin} | " +
            "startDateTime=$startDateTime, endDateTime=$endDateTime | " +
            "startTime=$startTime, endTime=$endTime | " +
            "timezone=${zoneId.id}")

        return PlanItemEntity(
            id = "${series.id}_$dateStr", // Deterministic Instance ID
            date = dateStr, // CRITICAL: Always use the input date, never derive from timestamp
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
