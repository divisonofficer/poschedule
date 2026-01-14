package com.jnkim.poschedule.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jnkim.poschedule.data.local.dao.NotificationLogDao
import com.jnkim.poschedule.data.local.entity.NotificationLogEntity
import com.jnkim.poschedule.data.local.entity.PlanItemEntity
import com.jnkim.poschedule.data.repo.PlanRepository
import com.jnkim.poschedule.domain.engine.NotificationArbitrator
import com.jnkim.poschedule.domain.model.Mode
import com.jnkim.poschedule.domain.model.NotificationCandidate
import com.jnkim.poschedule.domain.model.NotificationClass
import com.jnkim.poschedule.notifications.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val planRepository: PlanRepository,
    private val notificationLogDao: NotificationLogDao,
    private val arbitrator: NotificationArbitrator,
    private val notificationHelper: NotificationHelper,
    private val notificationScheduler: NotificationScheduler
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "NotificationWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            val dateStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val planDay = planRepository.getPlanDay(dateStr) ?: return Result.success()
            val currentMode = planDay.mode

            // 1. Fetch all items for today
            val allItems = planRepository.getPlanItems(dateStr).first()

            // 2. Reset expired snoozes to PENDING
            val now = Instant.now()
            allItems.forEach { item ->
                if (item.status == "SNOOZED" && item.snoozeUntil != null) {
                    val snoozeExpiry = Instant.ofEpochMilli(item.snoozeUntil)
                    if (now.isAfter(snoozeExpiry)) {
                        Log.d(TAG, "Snooze expired for ${item.id}, resetting to PENDING")
                        planRepository.updateItemStatus(item.id, "PENDING")
                    }
                }
            }

            // 3. Re-fetch items after snooze reset
            val currentItems = planRepository.getPlanItems(dateStr).first()

            // 4. Filter candidates: PENDING + not snoozed + within window
            val candidates = currentItems.filter { item ->
                item.status == "PENDING" &&
                (item.snoozeUntil == null || Instant.ofEpochMilli(item.snoozeUntil).isBefore(now)) &&
                isCalibratedCandidate(item, now)
            }

            if (candidates.isEmpty()) {
                Log.d(TAG, "No candidates found for notification")
                return Result.success()
            }

            // 5. Get actual budget count from database (CF-1)
            val sentTodayCount = notificationLogDao.countSentToday(dateStr)
            Log.d(TAG, "Budget check: $sentTodayCount notifications sent today")

            // 6. Arbitrate (Respect Cognitive Budget)
            val toNotify = arbitrator.arbitrate(candidates, currentMode, sentTodayCount)

            if (toNotify.isEmpty()) {
                Log.d(TAG, "Arbitrator suppressed all candidates (budget or scoring)")
                return Result.success()
            }

            // 7. Show Notifications and log to database
            toNotify.forEach { item ->
                val notificationClass = if (item.isCore) NotificationClass.CORE else NotificationClass.UPDATE
                val candidate = NotificationCandidate(
                    id = item.id,
                    clazz = notificationClass,
                    urgency = calculateCalibratedUrgency(item, now),
                    importance = if (item.isCore) 0.9 else 0.5,
                    actionability = 1.0,
                    userCost = 0.2,
                    deadline = now.plus(1, ChronoUnit.HOURS)
                )

                notificationHelper.showNotification(
                    candidate = candidate,
                    title = item.title,
                    body = getSupportiveBody(item, currentMode)
                )

                // Log notification for budget tracking
                notificationLogDao.insert(
                    NotificationLogEntity(
                        itemId = item.id,
                        sentAt = now.toEpochMilli(),
                        date = dateStr,
                        notificationClass = notificationClass.name
                    )
                )
                Log.d(TAG, "Sent notification for ${item.id}")
            }

            // CF-4: After showing notifications, reschedule for next window
            val remainingItems = planRepository.getPlanItems(dateStr).first()
            notificationScheduler.scheduleNextWindowNotification(remainingItems)
            Log.d(TAG, "Rescheduled notification worker for next window")

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "NotificationWorker failed", e)
            Result.retry() // Retry transient failures
        }
    }

    /**
     * G4 Implementation: Candidate if within ±5 mins of calibrated window start.
     */
    private fun isCalibratedCandidate(item: PlanItemEntity, now: Instant): Boolean {
        val startMillis = item.startTimeMillis ?: return true // Manual tasks are always candidates
        val start = Instant.ofEpochMilli(startMillis)
        
        // Window: [Start - 5m, End]
        val fiveMinsAgo = now.minus(5, ChronoUnit.MINUTES)
        return start.isBefore(now) && now.isBefore(Instant.ofEpochMilli(item.endTimeMillis ?: Long.MAX_VALUE))
    }

    private fun calculateCalibratedUrgency(item: PlanItemEntity, now: Instant): Double {
        val endMillis = item.endTimeMillis ?: return 0.5
        val totalWindow = endMillis - (item.startTimeMillis ?: now.toEpochMilli())
        val remaining = endMillis - now.toEpochMilli()
        return (1.0 - (remaining.toDouble() / totalWindow.toDouble())).coerceIn(0.1, 1.0)
    }

    private fun getSupportiveBody(item: PlanItemEntity, mode: Mode): String {
        return when (mode) {
            Mode.RECOVERY -> "하나씩 차근차근 해봐요. 지금은 이것만 생각하세요."
            Mode.LOW_MOOD -> "부담 갖지 마세요. 가벼운 마음으로 ${item.title} 어때요?"
            Mode.BUSY -> "바쁜 중에도 잊지 마세요."
            Mode.NORMAL -> "지금 ${item.title} 하기 좋은 시간입니다."
        }
    }
}
