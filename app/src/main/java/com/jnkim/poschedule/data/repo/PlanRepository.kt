package com.jnkim.poschedule.data.repo

import com.jnkim.poschedule.data.local.dao.PlanDao
import com.jnkim.poschedule.data.local.entity.*
import com.jnkim.poschedule.domain.model.Mode
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlanRepository @Inject constructor(
    private val planDao: PlanDao
) {
    // --- Instance Management ---
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
                sourceVersion = "3.0"
            )
        )
    }

    suspend fun updateItemStatus(id: String, status: String) {
        planDao.updatePlanItemStatus(id, status)
    }

    suspend fun insertPlanItem(item: PlanItemEntity) {
        planDao.insertPlanItem(item)
    }

    suspend fun deleteItem(id: String) {
        planDao.deletePlanItem(id)
    }

    suspend fun deleteItemsBySource(date: String, source: PlanItemSource) {
        planDao.deleteItemsBySource(date, source)
    }

    // --- Slice 2: Deletion Semantics ---
    
    /**
     * "Remove this occurrence" logic. Adds an exception for the date.
     */
    suspend fun removeOccurrence(seriesId: String, date: String) {
        planDao.addException(PlanSeriesExceptionEntity(seriesId = seriesId, date = date))
        // Find and delete the existing instance for this date
        val instanceId = "${seriesId}_$date"
        planDao.deletePlanItem(instanceId)
    }

    /**
     * "Stop repeating" logic. Archives the series and purges future instances.
     */
    suspend fun stopRepeatingSeries(seriesId: String, fromDate: String) {
        planDao.archiveSeries(seriesId)
        planDao.deleteFutureInstances(seriesId, fromDate)
    }

    // --- Series Management ---
    suspend fun getAllActiveSeries(): List<PlanSeriesEntity> {
        return planDao.getAllActiveSeries()
    }

    suspend fun addSeries(series: PlanSeriesEntity) {
        planDao.insertSeries(series)
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
}
