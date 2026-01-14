package com.jnkim.poschedule.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
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
    private val arbitrator: NotificationArbitrator,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val dateStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val planDay = planRepository.getPlanDay(dateStr) ?: return Result.success()
            val currentMode = planDay.mode
            
            // 1. Fetch pending items
            val allItems = planRepository.getPlanItems(dateStr).first()
            val candidates = allItems.filter { it.status == "PENDING" && isTimeSensitive(it) }

            if (candidates.isEmpty()) return Result.success()

            // 2. Simple tally for budget
            val sentTodayCount = 0 

            // 3. Arbitrate
            val toNotify = arbitrator.arbitrate(candidates, currentMode, sentTodayCount)

            // 4. Show Notifications
            toNotify.forEach { item ->
                val candidate = NotificationCandidate(
                    id = item.id,
                    clazz = if (item.isCore) NotificationClass.CORE else NotificationClass.UPDATE,
                    urgency = 0.8,
                    importance = if (item.isCore) 0.9 else 0.5,
                    actionability = 1.0,
                    userCost = 0.2,
                    deadline = Instant.now().plus(1, ChronoUnit.HOURS)
                )
                notificationHelper.showNotification(
                    candidate = candidate,
                    title = item.title,
                    body = getSupportiveBody(item, currentMode)
                )
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun isTimeSensitive(item: PlanItemEntity): Boolean {
        if (item.startTimeMillis == null) return true
        val now = Instant.now().toEpochMilli()
        return now >= item.startTimeMillis && now <= (item.endTimeMillis ?: Long.MAX_VALUE)
    }

    private fun getSupportiveBody(item: PlanItemEntity, mode: Mode): String {
        return when (mode) {
            Mode.RECOVERY -> "Just this one tiny step for now. You've got this."
            Mode.LOW_MOOD -> "It's okay to take it slow. Just a gentle nudge for ${item.title}."
            Mode.BUSY -> "Quick reminder for your core task."
            Mode.NORMAL -> "A good time for ${item.title}."
        }
    }
}
