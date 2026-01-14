package com.jnkim.poschedule.domain.model

import java.time.LocalTime

/**
 * Represents the six phases of the day for time-adaptive theming.
 * Each phase has a default time range that influences UI color schemes.
 *
 * The UI should subtly answer: "What part of the day am I in right now?"
 * without reading the clock, opening a calendar, or thinking cognitively.
 */
enum class DayPhase {
    DAWN,      // 05:00-07:59 - Gentle wake-up (soft pinks/purples like sunrise)
    MORNING,   // 08:00-11:59 - Clarity (clear blues like morning sky)
    NOON,      // 12:00-13:59 - Peak light (bright warm whites)
    AFTERNOON, // 14:00-17:59 - Steady (neutral tones)
    EVENING,   // 18:00-21:59 - Warm slowdown (oranges/golds like sunset)
    NIGHT;     // 22:00-04:59 - Rest (deep blues/purples like night sky)

    companion object {
        /**
         * Determines the current day phase based on the device's local time.
         *
         * CRITICAL TIMEZONE BEHAVIOR:
         * Uses LocalTime.now() which returns the current time in the system's LOCAL timezone.
         *
         * Example in Korea (UTC+9):
         * - If UTC time is 13:00 (1 PM), LocalTime.now() returns 22:00 (10 PM KST)
         * - This correctly returns DayPhase.EVENING
         *
         * Example at midnight boundary:
         * - If UTC time is 23:00 (11 PM), LocalTime.now() returns 08:00 (8 AM KST next day)
         * - This correctly returns DayPhase.MORNING
         *
         * This function ALWAYS uses local time, never UTC.
         * No manual timezone conversion is needed.
         *
         * @return The current DayPhase based on local time
         */
        fun fromCurrentTime(): DayPhase {
            val now = LocalTime.now()
            val hour = now.hour

            return when (hour) {
                in 5..7 -> DAWN
                in 8..11 -> MORNING
                in 12..13 -> NOON
                in 14..17 -> AFTERNOON
                in 18..21 -> EVENING
                else -> NIGHT // 22..23 or 0..4
            }
        }
    }
}
