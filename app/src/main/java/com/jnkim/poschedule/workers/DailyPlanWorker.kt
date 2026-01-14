package com.jnkim.poschedule.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jnkim.poschedule.data.local.entity.PlanItemSource
import com.jnkim.poschedule.data.repo.PlanRepository
import com.jnkim.poschedule.domain.engine.RecurrenceEngine
import com.jnkim.poschedule.domain.model.Mode
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@HiltWorker
class DailyPlanWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val planRepository: PlanRepository,
    private val recurrenceEngine: RecurrenceEngine
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val today = LocalDate.now()
            
            // Slice 1: Rolling window expansion [-1 .. +7]
            for (offset in -1..7) {
                val targetDate = today.plusDays(offset.toLong())
                val dateStr = targetDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

                // 1. Ensure PlanDay exists
                if (planRepository.getPlanDay(dateStr) == null) {
                    planRepository.insertPlanDay(dateStr, Mode.NORMAL)
                }

                // 2. Fetch all Active Series
                val activeSeries = planRepository.getAllActiveSeries()

                // 3. Clear existing DETERMINISTIC instances for this date to avoid duplicates
                planRepository.deleteItemsBySource(dateStr, PlanItemSource.DETERMINISTIC)

                // 4. Expand Rules -> Instances
                activeSeries.forEach { series ->
                    recurrenceEngine.expand(series, targetDate)?.let { instance ->
                        planRepository.insertPlanItem(instance)
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
