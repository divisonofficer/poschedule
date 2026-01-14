package com.jnkim.poschedule.domain.engine

import com.jnkim.poschedule.domain.model.RoutineItem
import com.jnkim.poschedule.domain.model.RoutineType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RuleScheduler @Inject constructor() {

    fun generateDailyPlan(
        date: LocalDate,
        wakeEstimate: LocalTime,
        bedTarget: LocalTime
    ): List<RoutineItem> {
        val zoneId = ZoneId.systemDefault()
        val routines = mutableListOf<RoutineItem>()
        val dateStr = date.toString()

        // MEDS_AM: wake -> wake + 60m
        routines.add(
            RoutineItem(
                id = "MEDS_AM_$dateStr",
                type = RoutineType.MEDS_AM,
                isCore = true,
                windowStart = date.atTime(wakeEstimate).atZone(zoneId).toInstant(),
                windowEnd = date.atTime(wakeEstimate).plusMinutes(60).atZone(zoneId).toInstant()
            )
        )

        // MEDS_PM: bed - 120m -> bed - 30m
        routines.add(
            RoutineItem(
                id = "MEDS_PM_$dateStr",
                type = RoutineType.MEDS_PM,
                isCore = true,
                windowStart = date.atTime(bedTarget).minusMinutes(120).atZone(zoneId).toInstant(),
                windowEnd = date.atTime(bedTarget).minusMinutes(30).atZone(zoneId).toInstant()
            )
        )

        // MEAL 1
        routines.add(
            RoutineItem(
                id = "MEAL_LUNCH_$dateStr",
                type = RoutineType.MEAL,
                isCore = true,
                windowStart = date.atTime(LocalTime.of(11, 30)).atZone(zoneId).toInstant(),
                windowEnd = date.atTime(LocalTime.of(13, 30)).atZone(zoneId).toInstant()
            )
        )

        // MEAL 2
        routines.add(
            RoutineItem(
                id = "MEAL_DINNER_$dateStr",
                type = RoutineType.MEAL,
                isCore = true,
                windowStart = date.atTime(LocalTime.of(18, 0)).atZone(zoneId).toInstant(),
                windowEnd = date.atTime(LocalTime.of(20, 0)).atZone(zoneId).toInstant()
            )
        )

        // WIND_DOWN
        routines.add(
            RoutineItem(
                id = "WIND_DOWN_$dateStr",
                type = RoutineType.WIND_DOWN,
                isCore = false,
                windowStart = date.atTime(bedTarget).minusMinutes(60).atZone(zoneId).toInstant(),
                windowEnd = date.atTime(bedTarget).atZone(zoneId).toInstant()
            )
        )

        return routines
    }
}
