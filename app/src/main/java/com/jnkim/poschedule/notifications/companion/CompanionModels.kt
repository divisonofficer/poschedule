package com.jnkim.poschedule.notifications.companion

import android.app.Notification

/**
 * Data models for the Status Companion notification system.
 *
 * These models represent the content displayed in the ongoing status notification.
 */

/**
 * Complete content for the status companion notification.
 *
 * **V2 (2026 Upgrade):** Enhanced with day summary, multiple plans, and current time.
 *
 * @property modeBadge Emoji representing the current mode (e.g., "🌊", "⚡", "🎯", "💙")
 * @property modeLabel Localized mode label (e.g., "회복 모드", "Recovery Mode")
 * @property supportiveMessage AI-generated gentle message from GentleCopyUseCase
 * @property daySummary Summary statistics for today's plans (V2)
 * @property nextPlans List of upcoming plans (max 2), empty if none (V2: was single nextPlan)
 * @property visibility Notification visibility: VISIBILITY_PRIVATE or VISIBILITY_PUBLIC
 * @property currentTime Formatted current time for title display (V2)
 */
data class CompanionContent(
    val modeBadge: String,
    val modeLabel: String,
    val supportiveMessage: String,
    val daySummary: DaySummary,
    val nextPlans: List<NextPlanInfo>,
    val visibility: Int,
    val currentTime: String
)

/**
 * Summary statistics for today's plans (V2 - 2026 Upgrade).
 *
 * Provides at-a-glance progress awareness with completed, pending, and core task counts.
 * Displayed as: "오늘: ✅ 2   ⏳ 3   ⭐ 1"
 *
 * @property completedCount Number of DONE plans today (✅)
 * @property pendingCount Number of PENDING plans today (⏳)
 * @property coreCount Number of core (isCore=true) plans today, regardless of status (⭐)
 */
data class DaySummary(
    val completedCount: Int,
    val pendingCount: Int,
    val coreCount: Int
)

/**
 * Information about the next upcoming plan.
 *
 * @property planId Unique ID of the plan item
 * @property emoji Plan emoji icon (from EmojiMapper or custom)
 * @property title Plan title
 * @property targetTime Target time in milliseconds (epoch)
 * @property formattedTime Human-readable formatted time (e.g., "오후 3:00", "3:00 PM")
 */
data class NextPlanInfo(
    val planId: Long,
    val emoji: String,
    val title: String,
    val targetTime: Long,
    val formattedTime: String
)
