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
 * @property modeBadge Emoji representing the current mode (e.g., "🌊", "⚡", "🎯", "💙")
 * @property modeLabel Localized mode label (e.g., "회복 모드", "Recovery Mode")
 * @property supportiveMessage AI-generated gentle message from GentleCopyUseCase
 * @property nextPlan Information about the next upcoming plan, or null if none
 * @property visibility Notification visibility: VISIBILITY_PRIVATE or VISIBILITY_PUBLIC
 */
data class CompanionContent(
    val modeBadge: String,
    val modeLabel: String,
    val supportiveMessage: String,
    val nextPlan: NextPlanInfo?,
    val visibility: Int
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
