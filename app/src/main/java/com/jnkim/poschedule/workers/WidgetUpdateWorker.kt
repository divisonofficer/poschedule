package com.jnkim.poschedule.workers

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.jnkim.poschedule.data.repo.PlanRepository
import com.jnkim.poschedule.widget.PoscheduleWidget
import com.jnkim.poschedule.widget.data.WidgetStateRepository
import com.jnkim.poschedule.widget.model.UrgencyLevel
import com.jnkim.poschedule.widget.model.WidgetState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Worker that updates widget state and triggers re-render.
 *
 * Responsibilities:
 * - Fetch next pending task from database
 * - Calculate time remaining and urgency level
 * - Update widget state in DataStore
 * - Notify Glance to re-render all widget instances
 * - Schedule next update at next window start
 */
@HiltWorker
class WidgetUpdateWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val planRepository: PlanRepository,
    private val widgetStateRepository: WidgetStateRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Widget update triggered")

            // 1. Fetch next pending task for today
            val dateStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val nextTask = planRepository.getNextPendingTask(dateStr)
            val planDay = planRepository.getPlanDay(dateStr)

            // 2. Calculate time remaining and urgency
            val widgetState = buildWidgetState(nextTask, planDay)

            // 3. Update DataStore
            widgetStateRepository.updateState(widgetState)

            // 4. Notify Glance to re-render all widget instances
            val glanceManager = GlanceAppWidgetManager(applicationContext)
            val glanceIds = glanceManager.getGlanceIds(PoscheduleWidget::class.java)

            glanceIds.forEach { glanceId ->
                PoscheduleWidget().update(applicationContext, glanceId)
            }

            Log.d(TAG, "Widget updated successfully with ${glanceIds.size} instances")

            // TODO: 5. Schedule next update at next window start (Phase 4)

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Widget update failed", e)
            Result.retry()
        }
    }

    /**
     * Build widget state from task and plan day.
     */
    private fun buildWidgetState(
        task: com.jnkim.poschedule.data.local.entity.PlanItemEntity?,
        planDay: com.jnkim.poschedule.data.local.entity.PlanDayEntity?
    ): WidgetState {
        if (task == null) {
            // No pending tasks
            return WidgetState(
                task = null,
                timeUntilStart = null,
                urgencyLevel = UrgencyLevel.NORMAL,
                mode = planDay?.mode ?: com.jnkim.poschedule.domain.model.Mode.NORMAL,
                lastUpdated = System.currentTimeMillis()
            )
        }

        val now = Instant.now()
        val startTime = task.startTimeMillis?.let { Instant.ofEpochMilli(it) }
        val endTime = task.endTimeMillis?.let { Instant.ofEpochMilli(it) }

        // Calculate time until start
        val timeUntilStart = if (startTime != null) {
            val minutesUntilStart = java.time.Duration.between(now, startTime).toMinutes()
            when {
                minutesUntilStart > 60 -> "${minutesUntilStart / 60}h ${minutesUntilStart % 60}m"
                minutesUntilStart > 0 -> "${minutesUntilStart}m"
                minutesUntilStart >= -5 -> "Now"
                else -> "Overdue ${-minutesUntilStart}m"
            }
        } else {
            "Anytime"
        }

        // Calculate urgency level based on time until end
        val urgencyLevel = if (endTime != null) {
            val minutesUntilEnd = java.time.Duration.between(now, endTime).toMinutes()
            when {
                minutesUntilEnd < 10 -> UrgencyLevel.URGENT
                minutesUntilEnd < 30 -> UrgencyLevel.MODERATE
                else -> UrgencyLevel.NORMAL
            }
        } else {
            UrgencyLevel.NORMAL
        }

        return WidgetState(
            task = task,
            timeUntilStart = timeUntilStart,
            urgencyLevel = urgencyLevel,
            mode = planDay?.mode ?: com.jnkim.poschedule.domain.model.Mode.NORMAL,
            lastUpdated = System.currentTimeMillis()
        )
    }

    companion object {
        private const val TAG = "WidgetUpdateWorker"

        /**
         * Enqueue an immediate widget update.
         * Called after user actions (Done/Skip/Snooze) or when widget needs refresh.
         */
        fun enqueueImmediateUpdate(context: Context) {
            val request = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
                .addTag("immediate_widget_update")
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
