package com.jnkim.poschedule.data.ai

import kotlinx.serialization.Serializable

/**
 * LLM response for task normalization.
 * Converts natural language input into structured plan specification.
 */
@Serializable
data class LLMTaskResponse(
    val version: String = "1.0",
    val language: String,  // "ko-KR" or "en-US"
    val intent: String,    // "create_plan" or "unsure"
    val plan: NormalizedPlan? = null,
    val confidence: Double,  // 0.0 to 1.0
    val clarifications: List<String> = emptyList(),
    val alternatives: List<AlternativePlan> = emptyList()
)

/**
 * Normalized plan structure from LLM.
 * Maps to PlanSeries domain model (recurring) or one-time event.
 */
@Serializable
data class NormalizedPlan(
    val title: String,
    val planType: String,  // ROUTINE, TASK, CHORE, MEETING
    val routineType: String? = null,  // MEDS_AM, MEAL, etc. (if ROUTINE)
    val iconEmoji: String? = null,
    val importance: String = "MEDIUM",  // LOW, MEDIUM, HIGH
    val time: TimeSpec,
    val recurrence: RecurrenceSpec,
    val specificDate: String? = null,  // yyyy-MM-dd (for one-time events, when recurrence.kind = "NONE")
    val note: String? = null
)

/**
 * Time specification for a plan.
 * Supports multiple anchor types: WAKE, BED, FIXED, FLEX.
 */
@Serializable
data class TimeSpec(
    val anchor: String,  // WAKE, BED, FIXED, FLEX
    val fixedHour: Int? = null,  // 0-23 (if FIXED)
    val fixedMinute: Int? = null,  // 0-59 (if FIXED)
    val offset: Int? = null,  // minutes (if WAKE/BED)
    val duration: Int = 15,  // minutes
    val windowType: String = "SOFT"  // SOFT or HARD
)

/**
 * Recurrence pattern specification.
 * Supports NONE, DAILY, WEEKLY, WEEKDAYS, MONTHLY.
 */
@Serializable
data class RecurrenceSpec(
    val kind: String,  // NONE, DAILY, WEEKLY, MONTHLY, WEEKDAYS
    val weekdays: List<Int>? = null,  // 1=Mon, 7=Sun (if WEEKLY)
    val monthDay: Int? = null  // 1-31 (if MONTHLY)
)

/**
 * Alternative plan suggestion from LLM.
 * Provides simplified alternatives when the main plan has low confidence or ambiguity.
 */
@Serializable
data class AlternativePlan(
    val title: String,
    val planType: String,  // ROUTINE, TASK, CHORE, MEETING
    val routineType: String? = null,  // MEDS_AM, MEAL, etc. (if ROUTINE)
    val iconEmoji: String? = null  // LLM-generated emoji icon
)
