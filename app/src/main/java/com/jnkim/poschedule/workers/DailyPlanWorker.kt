package com.jnkim.poschedule.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jnkim.poschedule.data.local.entity.PlanItemSource
import com.jnkim.poschedule.data.repo.PlanRepository
import com.jnkim.poschedule.domain.engine.RecurrenceEngine
import com.jnkim.poschedule.domain.model.Mode
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@HiltWorker
class DailyPlanWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val planRepository: PlanRepository,
    private val recurrenceEngine: RecurrenceEngine,
    private val notificationScheduler: NotificationScheduler
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "DailyPlanWorker"
    }

    /**
     * CRITICAL TIMEZONE BEHAVIOR:
     * This worker ALWAYS uses the system's local timezone for date calculations.
     * - LocalDate.now() returns the current date in the local timezone (e.g., 2026-01-14 in Korea)
     * - All plan items are associated with local dates, not UTC dates
     * - When expanding plans, we pass LOCAL dates to RecurrenceEngine
     *
     * Example: In Korea (UTC+9):
     * - 2026-01-14 02:00 KST = 2026-01-13 17:00 UTC
     * - But we still use "2026-01-14" as the date for plans, because it's the 14th in Korea
     */
    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting daily plan expansion")

            // Get current date in LOCAL timezone (e.g., Jan 14 in Korea, even if it's Jan 13 in UTC)
            val today = LocalDate.now()
            val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

            // Slice 1: Rolling window expansion [-1 .. +7]
            // Expand plans for yesterday, today, and the next 7 days
            for (offset in -1..7) {
                val targetDate = today.plusDays(offset.toLong())
                val dateStr = targetDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

                // 1. Ensure PlanDay exists for OS layer state
                if (planRepository.getPlanDay(dateStr) == null) {
                    planRepository.insertPlanDay(dateStr, Mode.NORMAL)
                }

                // 2. Fetch all Active Routine Templates (Series)
                val activeSeries = planRepository.getAllActiveSeries()

                // 3. Expansion Logic with Status Preservation
                // Fetch existing items to see what user has already done
                val existingItems = planRepository.getPlanItems(dateStr).first()
                val statusMap = existingItems.associate { it.id to it.status }

                // Clear DETERMINISTIC items ONLY if they are still PENDING
                // This prevents clearing items the user has already marked DONE/SKIPPED/SNOOZED.
                planRepository.deleteItemsBySource(dateStr, PlanItemSource.DETERMINISTIC)

                // 4. Expand Rules -> Instances
                activeSeries.forEach { series ->
                    recurrenceEngine.expand(series, targetDate)?.let { instance ->
                        // If user previously had this item and changed its status, preserve it
                        val finalInstance = if (statusMap.containsKey(instance.id) && statusMap[instance.id] != "PENDING") {
                            instance.copy(status = statusMap[instance.id]!!)
                        } else {
                            instance
                        }
                        planRepository.insertPlanItem(finalInstance)
                    }
                }
            }

            // CF-4: Schedule notifications for next window start
            val todayItems = planRepository.getPlanItems(todayStr).first()
            val pendingCount = todayItems.count { it.status == "PENDING" }
            Log.d(TAG, "Plan expansion complete. Found $pendingCount pending items for today")

            notificationScheduler.scheduleNextWindowNotification(todayItems)

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "DailyPlanWorker failed", e)
            Result.retry() // Retry transient failures
        }
    }
}
