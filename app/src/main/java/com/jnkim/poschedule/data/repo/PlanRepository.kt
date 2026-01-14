package com.jnkim.poschedule.data.repo

import com.jnkim.poschedule.data.local.dao.PlanDao
import com.jnkim.poschedule.data.local.entity.PlanDayEntity
import com.jnkim.poschedule.data.local.entity.PlanItemEntity
import com.jnkim.poschedule.data.local.entity.PlanItemSource
import com.jnkim.poschedule.data.local.entity.PlanItemWindow
import com.jnkim.poschedule.domain.model.Mode
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlanRepository @Inject constructor(
    private val planDao: PlanDao
) {
    fun getPlanItems(date: String): Flow<List<PlanItemEntity>> {
        return planDao.getPlanItems(date)
    }

    suspend fun getPlanDay(date: String): PlanDayEntity? {
        return planDao.getPlanDay(date)
    }

    suspend fun insertPlanDay(date: String, mode: Mode) {
        planDao.insertPlanDay(
            PlanDayEntity(
                date = date,
                mode = mode,
                generatedAtMillis = System.currentTimeMillis(),
                sourceVersion = "2.0"
            )
        )
    }

    suspend fun insertPlanItem(item: PlanItemEntity) {
        planDao.insertPlanItem(item)
    }

    suspend fun addManualItem(date: String, title: String, window: PlanItemWindow) {
        val item = PlanItemEntity(
            id = UUID.randomUUID().toString(),
            date = date,
            title = title,
            type = null,
            source = PlanItemSource.MANUAL,
            window = window,
            status = "PENDING",
            isCore = false,
            startTimeMillis = null,
            endTimeMillis = null
        )
        planDao.insertPlanItem(item)
    }

    suspend fun updateItemStatus(id: String, status: String) {
        planDao.updatePlanItemStatus(id, status)
    }

    suspend fun deleteItem(id: String) {
        planDao.deletePlanItem(id)
    }

    suspend fun deleteItemsBySource(date: String, source: PlanItemSource) {
        planDao.deleteItemsBySource(date, source)
    }
}
