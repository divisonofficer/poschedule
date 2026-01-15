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
     * Builds the status companion notification.
     *
     * @param context Android context
     * @param content Resolved companion content
     * @return Notification ready to be posted
     */
    fun buildStatusNotification(
        context: Context,
        content: CompanionContent
    ): Notification {
        // Build notification title
        val title = "${content.modeBadge} ${content.modeLabel}"

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
            .setContentText(content.supportiveMessage) // Summary text
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

        // Add action buttons if there's a next plan
        if (content.nextPlan != null) {
            // Snooze button
            val snoozeIntent = Intent(context, StatusCompanionActionReceiver::class.java).apply {
                action = NotificationConstants.ACTION_COMPANION_SNOOZE
                putExtra("plan_id", content.nextPlan.planId)
            }
            val snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                content.nextPlan.planId.toInt() + 1000,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Done button
            val doneIntent = Intent(context, StatusCompanionActionReceiver::class.java).apply {
                action = NotificationConstants.ACTION_COMPANION_DONE
                putExtra("plan_id", content.nextPlan.planId)
            }
            val donePendingIntent = PendingIntent.getBroadcast(
                context,
                content.nextPlan.planId.toInt() + 2000,
                doneIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            builder
                .addAction(
                    0,
                    context.getString(R.string.companion_action_snooze),
                    snoozePendingIntent
                )
                .addAction(
                    0,
                    context.getString(R.string.companion_action_done),
                    donePendingIntent
                )
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

        builder.addAction(
            0,
            context.getString(R.string.companion_action_open),
            openPendingIntent
        )

        return builder.build()
    }

    /**
     * Builds the notification body text.
     *
     * Combines supportive message with next plan details.
     *
     * @param context Android context
     * @param content Companion content
     * @return Formatted body text
     */
    private fun buildBody(context: Context, content: CompanionContent): String {
        val supportiveMsg = content.supportiveMessage

        return if (content.nextPlan != null) {
            val nextPlanText = context.getString(
                R.string.companion_next_plan,
                "${content.nextPlan.emoji} ${content.nextPlan.title}",
                content.nextPlan.formattedTime
            )
            "$supportiveMsg\n\n$nextPlanText"
        } else {
            val noPlansMsg = context.getString(R.string.companion_no_plans)
            "$supportiveMsg\n\n$noPlansMsg"
        }
    }
}
