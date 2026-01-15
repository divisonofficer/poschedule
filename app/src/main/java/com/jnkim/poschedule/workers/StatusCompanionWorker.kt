package com.jnkim.poschedule.workers

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jnkim.poschedule.data.repo.PlanRepository
import com.jnkim.poschedule.data.repo.SettingsRepository
import com.jnkim.poschedule.notifications.NotificationConstants
import com.jnkim.poschedule.notifications.companion.CompanionContentResolver
import com.jnkim.poschedule.notifications.companion.StatusNotificationBuilder
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Periodic worker that updates the Status Companion notification every 15 minutes.
 *
 * Responsibilities:
 * - Check if companion feature is enabled
 * - Fetch current mode and plans
 * - Resolve notification content
 * - Build and post notification
 * - Cancel notification if feature disabled
 */
@HiltWorker
class StatusCompanionWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val planRepository: PlanRepository,
    private val settingsRepository: SettingsRepository,
    private val contentResolver: CompanionContentResolver
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "StatusCompanionWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting status companion update")

            // 1. Check if companion feature is enabled
            val settings = settingsRepository.settingsFlow.first()
            if (!settings.statusCompanionEnabled) {
                Log.d(TAG, "Companion feature disabled, cancelling notification")
                cancelNotification()
                return Result.success()
            }

            // 2. Get current date and plan day
            val dateStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val planDay = planRepository.getPlanDay(dateStr)

            if (planDay == null) {
                Log.w(TAG, "No plan day found for $dateStr, skipping update")
                return Result.success()
            }

            val currentMode = planDay.mode

            // 3. Get all plan items for today
            val plans = planRepository.getPlanItems(dateStr).first()

            // 4. Resolve notification content
            val content = contentResolver.resolveContent(
                mode = currentMode,
                plans = plans,
                context = appContext
            )

            // 5. Build notification
            val notification = StatusNotificationBuilder.buildStatusNotification(
                context = appContext,
                content = content
            )

            // 6. Post notification
            val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(
                NotificationConstants.NOTIFICATION_ID_COMPANION,
                notification
            )

            Log.d(TAG, "Status companion notification updated successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update status companion notification", e)
            Result.failure()
        }
    }

    /**
     * Cancels the companion notification.
     */
    private fun cancelNotification() {
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NotificationConstants.NOTIFICATION_ID_COMPANION)
        Log.d(TAG, "Status companion notification cancelled")
    }
}
