package com.jnkim.poschedule.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jnkim.poschedule.data.local.entity.PlanItemEntity
import com.jnkim.poschedule.data.local.entity.PlanItemSource
import com.jnkim.poschedule.data.local.entity.PlanItemWindow
import com.jnkim.poschedule.data.repo.PlanRepository
import com.jnkim.poschedule.domain.engine.RuleScheduler
import com.jnkim.poschedule.domain.model.Mode
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@HiltWorker
class DailyPlanWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val planRepository: PlanRepository,
    private val ruleScheduler: RuleScheduler
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val date = LocalDate.now()
            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            
            // 1. Ensure PlanDay exists
            if (planRepository.getPlanDay(dateStr) == null) {
                planRepository.insertPlanDay(dateStr, Mode.NORMAL)
            }

            // 2. Generate deterministic routines (Advisory)
            val wakeEstimate = LocalTime.of(8, 0)
            val bedTarget = LocalTime.of(23, 0)
            val deterministicRoutines = ruleScheduler.generateDailyPlan(date, wakeEstimate, bedTarget)

            // 3. Merge into PlanItemEntity (Phase 2 Model)
            // Strategy: Clear existing DETERMINISTIC items for today, but keep MANUAL ones.
            planRepository.deleteItemsBySource(dateStr, PlanItemSource.DETERMINISTIC)

            deterministicRoutines.forEach { routine ->
                val item = PlanItemEntity(
                    id = routine.id,
                    date = dateStr,
                    title = routine.type.name.replace("_", " "),
                    type = routine.type,
                    source = PlanItemSource.DETERMINISTIC,
                    window = mapRoutineToWindow(routine.type),
                    status = "PENDING",
                    isCore = routine.isCore,
                    startTimeMillis = routine.windowStart.toEpochMilli(),
                    endTimeMillis = routine.windowEnd.toEpochMilli()
                )
                planRepository.insertPlanItem(item)
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun mapRoutineToWindow(type: com.jnkim.poschedule.domain.model.RoutineType): PlanItemWindow {
        return when (type) {
            com.jnkim.poschedule.domain.model.RoutineType.MEDS_AM -> PlanItemWindow.MORNING
            com.jnkim.poschedule.domain.model.RoutineType.MEAL -> PlanItemWindow.ANYTIME // Or specific based on time
            com.jnkim.poschedule.domain.model.RoutineType.WIND_DOWN, 
            com.jnkim.poschedule.domain.model.RoutineType.MEDS_PM -> PlanItemWindow.EVENING
            else -> PlanItemWindow.ANYTIME
        }
    }
}
