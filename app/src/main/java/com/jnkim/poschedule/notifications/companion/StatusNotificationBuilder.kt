package com.jnkim.poschedule.notifications.companion

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.jnkim.poschedule.MainActivity
import com.jnkim.poschedule.R
import com.jnkim.poschedule.notifications.NotificationConstants

/**
 * Builds the Status Companion notification.
 *
 * Creates a BigTextStyle notification with:
 * - Title: Mode badge + label (e.g., "🌊 회복 모드")
 * - BigText: Supportive message + next plan details
 * - Actions: Snooze, Done, Open Day buttons
 * - Properties: ongoing, LOW priority, CATEGORY_STATUS
 */
object StatusNotificationBuilder {

    /**
     * Builds the status companion notification (V2 - 2026 Upgrade).
     *
     * Enhanced with day summary, multiple plans, and current time in title.
     *
     * @param context Android context
     * @param content Resolved companion content
     * @return Notification ready to be posted
     */
    fun buildStatusNotification(
        context: Context,
        content: CompanionContent
    ): Notification {
        // Build notification title with time
        val title = buildTitle(content.modeBadge, content.modeLabel, content.currentTime)

        // Build notification body
        val body = buildBody(context, content)

        // Create main tap intent (opens MainActivity)
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            NotificationConstants.NOTIFICATION_ID_COMPANION,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val builder = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_STATUS_COMPANION)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content.supportiveMessage) // Summary text in collapsed state
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(body)
            )
            .setContentIntent(mainPendingIntent)
            .setOngoing(true) // Can't be swiped away
            .setPriority(NotificationCompat.PRIORITY_LOW) // Silent, non-intrusive
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(content.visibility)
            .setAutoCancel(false) // Don't dismiss on tap

        // Add action buttons
        addActionButtons(builder, context, content)

        return builder.build()
    }

    /**
     * Builds notification title with right-aligned time (V2 - 2026 Upgrade).
     *
     * @param modeBadge Mode emoji (e.g., "🌊")
     * @param modeLabel Localized mode label (e.g., "회복 모드")
     * @param currentTime Formatted time (e.g., "15:33")
     * @return Formatted title string with adaptive spacing
     */
    private fun buildTitle(modeBadge: String, modeLabel: String, currentTime: String): String {
        val leftPart = "$modeBadge $modeLabel"

        // Calculate adaptive spacing (notification width ≈ 40 chars)
        val targetWidth = 40
        val spacesNeeded = (targetWidth - leftPart.length - currentTime.length).coerceAtLeast(2)
        val spacing = " ".repeat(spacesNeeded)

        return "$leftPart$spacing$currentTime"
    }

    /**
     * Truncates plan title for display (V2 - 2026 Upgrade).
     *
     * Prevents overflow on HyperOS/MIUI devices by limiting title length.
     *
     * @param title Plan title
     * @param maxLength Maximum character length (default 30)
     * @return Truncated title with ellipsis if needed
     */
    private fun truncatePlanTitle(title: String, maxLength: Int = 30): String {
        return if (title.length > maxLength) {
            title.take(maxLength - 1) + "…"
        } else {
            title
        }
    }

    /**
     * Builds the notification body text with high information density (V2 - 2026 Upgrade).
     *
     * Format (5 lines max):
     * [Supportive message]
     *
     * 오늘: ✅ 2   ⏳ 3   ⭐ 1
     *
     * 다음
     * [Plan 1 emoji + title (time)]
     * [Plan 2 emoji + title (time)]
     *
     * @param context Android context
     * @param content Companion content
     * @return Formatted body text
     */
    private fun buildBody(context: Context, content: CompanionContent): String {
        val builder = StringBuilder()

        // 1. Supportive message
        builder.append(content.supportiveMessage)
        builder.append("\n\n")

        // 2. Day summary line
        val summaryLine = context.getString(
            R.string.companion_day_summary,
            content.daySummary.completedCount,
            content.daySummary.pendingCount,
            content.daySummary.coreCount
        )
        builder.append(summaryLine)
        builder.append("\n\n")

        // 3. Next plans section
        if (content.nextPlans.isEmpty()) {
            val noPlansMsg = context.getString(R.string.companion_no_plans)
            builder.append(noPlansMsg)
        } else {
            val nextLabel = context.getString(R.string.companion_next_label)
            builder.append(nextLabel)
            builder.append("\n")

            // Add up to 2 plans
            content.nextPlans.forEachIndexed { index, plan ->
                val truncatedTitle = truncatePlanTitle(plan.title)
                val planLine = context.getString(
                    R.string.companion_plan_line,
                    plan.emoji,
                    truncatedTitle,
                    plan.formattedTime
                )
                builder.append(planLine)

                if (index < content.nextPlans.size - 1) {
                    builder.append("\n")
                }
            }
        }

        return builder.toString()
    }

    /**
     * Adds action buttons to notification (V2 - 2026 Upgrade).
     *
     * If plans available: [⏰ 15분 후] [✔ 완료] [🧭 오늘]
     * If no plans: [🧭 오늘] only
     *
     * Actions apply to first plan in list.
     *
     * @param builder NotificationCompat.Builder to add actions to
     * @param context Android context
     * @param content Companion content
     */
    private fun addActionButtons(
        builder: NotificationCompat.Builder,
        context: Context,
        content: CompanionContent
    ) {
        if (content.nextPlans.isNotEmpty()) {
            val firstPlan = content.nextPlans.first()

            // Snooze button
            val snoozeIntent = Intent(context, StatusCompanionActionReceiver::class.java).apply {
                action = NotificationConstants.ACTION_COMPANION_SNOOZE
                putExtra("plan_id", firstPlan.planId)
            }
            val snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                firstPlan.planId.toInt() + 1000,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Done button
            val doneIntent = Intent(context, StatusCompanionActionReceiver::class.java).apply {
                action = NotificationConstants.ACTION_COMPANION_DONE
                putExtra("plan_id", firstPlan.planId)
            }
            val donePendingIntent = PendingIntent.getBroadcast(
                context,
                firstPlan.planId.toInt() + 2000,
                doneIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            builder
                .addAction(0, context.getString(R.string.companion_action_snooze_v2), snoozePendingIntent)
                .addAction(0, context.getString(R.string.companion_action_done_v2), donePendingIntent)
        }

        // Open Day button (always present)
        val openIntent = Intent(context, StatusCompanionActionReceiver::class.java).apply {
            action = NotificationConstants.ACTION_COMPANION_OPEN
        }
        val openPendingIntent = PendingIntent.getBroadcast(
            context,
            NotificationConstants.NOTIFICATION_ID_COMPANION + 3000,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        builder.addAction(0, context.getString(R.string.companion_action_open_v2), openPendingIntent)
    }
}
