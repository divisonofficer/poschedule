package com.jnkim.poschedule.workers

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.jnkim.poschedule.data.local.entity.PlanItemEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NotificationScheduler - Manages reliable window-start notification scheduling.
 *
 * CF-4: Ensures notifications fire at window start (±2 min Android Doze tolerance)
 * without requiring SCHEDULE_EXACT_ALARM permission.
 *
 * Strategy:
 * - Use OneTimeWorkRequest with setInitialDelay for near-exact timing
 * - Schedule for next pending window start
 * - After showing notification, reschedule for next window
 */
@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    companion object {
        private const val TAG = "NotificationScheduler"
        private const val NOTIFICATION_WORK_NAME = "notification_window_check"
        private const val MIN_DELAY_MS = 0L
        private const val MAX_DELAY_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    /**
     * Schedule notification worker to run at the next window start.
     *
     * @param items List of plan items to find next window from
     */
    fun scheduleNextWindowNotification(items: List<PlanItemEntity>) {
        val nextWindow = findNextPendingWindowStart(items)

        if (nextWindow == null) {
            Log.d(TAG, "No pending windows found, skipping scheduling")
            return
        }

        val now = Instant.now()
        val delayMillis = (nextWindow.toEpochMilli() - now.toEpochMilli()).coerceIn(MIN_DELAY_MS, MAX_DELAY_MS)

        if (delayMillis < 0) {
            Log.d(TAG, "Next window is in the past, scheduling immediate check")
            scheduleImmediateCheck()
            return
        }

        val delayMinutes = TimeUnit.MILLISECONDS.toMinutes(delayMillis)
        Log.d(TAG, "Scheduling notification check in $delayMinutes minutes at window start: $nextWindow")

        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .addTag("window_start_notification")
            .build()

        // Use REPLACE to cancel any existing scheduled work
        workManager.enqueueUniqueWork(
            NOTIFICATION_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    /**
     * Schedule immediate notification check (for expired windows or manual triggers).
     */
    fun scheduleImmediateCheck() {
        Log.d(TAG, "Scheduling immediate notification check")

        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .addTag("immediate_notification")
            .build()

        workManager.enqueueUniqueWork(
            NOTIFICATION_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    /**
     * Find the next pending window start time from a list of plan items.
     *
     * @param items List of plan items with windows
     * @return Next window start Instant, or null if none found
     */
    private fun findNextPendingWindowStart(items: List<PlanItemEntity>): Instant? {
        val now = Instant.now()

        Log.d(TAG, "Finding next window from ${items.size} items")

        val pendingItems = items.filter { it.status == "PENDING" && it.startTimeMillis != null }
        Log.d(TAG, "Found ${pendingItems.size} pending items with startTimeMillis")

        pendingItems.forEach { item ->
            val start = Instant.ofEpochMilli(item.startTimeMillis!!)
            val isAfterNow = start.isAfter(now)
            Log.d(TAG, "Item: ${item.title} | start=$start | now=$now | isAfter=$isAfterNow | startMillis=${item.startTimeMillis}")
        }

        val result = pendingItems
            .mapNotNull { item ->
                val start = Instant.ofEpochMilli(item.startTimeMillis!!)
                if (start.isAfter(now)) start else null
            }
            .minOrNull()

        Log.d(TAG, "Next window start: $result")
        return result
    }

    /**
     * Cancel all scheduled notification work.
     * Used when user disables notifications or clears all plans.
     */
    fun cancelAllScheduledNotifications() {
        Log.d(TAG, "Cancelling all scheduled notifications")
        workManager.cancelUniqueWork(NOTIFICATION_WORK_NAME)
    }
}
