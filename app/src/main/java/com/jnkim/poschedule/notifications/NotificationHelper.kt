package com.jnkim.poschedule.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.jnkim.poschedule.MainActivity
import com.jnkim.poschedule.R
import com.jnkim.poschedule.domain.model.NotificationCandidate
import com.jnkim.poschedule.domain.model.NotificationClass
import com.jnkim.poschedule.notifications.receiver.NotificationActionReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager = 
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createNotificationChannels() {
        val channels = listOf(
            NotificationChannel(
                NotificationConstants.CHANNEL_CORE_ROUTINES,
                context.getString(R.string.channel_core_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = context.getString(R.string.channel_core_desc) },

            NotificationChannel(
                NotificationConstants.CHANNEL_ADAPTIVE_UPDATES,
                context.getString(R.string.channel_updates_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ),

            NotificationChannel(
                NotificationConstants.CHANNEL_RECOVERY_NUDGES,
                context.getString(R.string.channel_recovery_name),
                NotificationManager.IMPORTANCE_LOW
            )
        )
        notificationManager.createNotificationChannels(channels)
    }

    fun showNotification(candidate: NotificationCandidate, title: String, body: String) {
        val channelId = when (candidate.clazz) {
            NotificationClass.CORE -> NotificationConstants.CHANNEL_CORE_ROUTINES
            NotificationClass.UPDATE -> NotificationConstants.CHANNEL_ADAPTIVE_UPDATES
            NotificationClass.RECOVERY -> NotificationConstants.CHANNEL_RECOVERY_NUDGES
            else -> NotificationConstants.CHANNEL_CORE_ROUTINES
        }

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val doneIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationConstants.ACTION_DONE
            putExtra(NotificationConstants.EXTRA_ROUTINE_ID, candidate.id)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context, candidate.id.hashCode(), doneIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationConstants.ACTION_SNOOZE_15
            putExtra(NotificationConstants.EXTRA_ROUTINE_ID, candidate.id)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context, candidate.id.hashCode() + 1, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(0, context.getString(R.string.action_done), donePendingIntent)
            .addAction(0, context.getString(R.string.action_snooze), snoozePendingIntent)

        notificationManager.notify(candidate.id.hashCode(), builder.build())
    }
}
