package com.jnkim.poschedule.data.local.dao

import androidx.room.*
import com.jnkim.poschedule.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {
    // --- PlanDay (OS Layer State) ---
    @Query("SELECT * FROM plan_days WHERE date = :date")
    suspend fun getPlanDay(date: String): PlanDayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanDay(planDay: PlanDayEntity)

    // --- PlanInstance (Individual Occurrences) ---
    /**
     * Get plan items sorted by effective display time:
     * 1. SNOOZED items: sorted by snoozeUntil
     * 2. Items with startTimeMillis: sorted by startTimeMillis
     * 3. Others: sorted by createdAt
     *
     * This ensures items appear in chronological order regardless of snooze status.
     */
    @Query("""
        SELECT * FROM plan_items
        WHERE date = :date
        ORDER BY
            CASE
                WHEN status = 'SNOOZED' AND snoozeUntil IS NOT NULL THEN snoozeUntil
                WHEN startTimeMillis IS NOT NULL THEN startTimeMillis
                ELSE createdAt
            END ASC
    """)
    fun getPlanItems(date: String): Flow<List<PlanItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanItem(planItem: PlanItemEntity)

    @Update
    suspend fun updatePlanItem(planItem: PlanItemEntity)

    @Query("DELETE FROM plan_items WHERE id = :id")
    suspend fun deletePlanItem(id: String)

    @Query("UPDATE plan_items SET status = :status WHERE id = :id")
    suspend fun updatePlanItemStatus(id: String, status: String)

    @Query("UPDATE plan_items SET status = :status, snoozeUntil = :snoozeUntilMillis WHERE id = :id")
    suspend fun updatePlanItemWithSnooze(id: String, status: String, snoozeUntilMillis: Long)

    @Query("DELETE FROM plan_items WHERE date = :date AND source = :source")
    suspend fun deleteItemsBySource(date: String, source: PlanItemSource)

    // --- PlanSeries (Recurrence Rules) ---
    @Query("SELECT * FROM plan_series WHERE archived = 0")
    suspend fun getAllActiveSeries(): List<PlanSeriesEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeries(series: PlanSeriesEntity)

    @Query("UPDATE plan_series SET archived = 1 WHERE id = :seriesId")
    suspend fun archiveSeries(seriesId: String)

    @Query("DELETE FROM plan_items WHERE seriesId = :seriesId AND date >= :fromDate")
    suspend fun deleteFutureInstances(seriesId: String, fromDate: String)

    // --- Exceptions (G2: Deletion Semantics) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addException(exception: PlanSeriesExceptionEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM plan_series_exceptions WHERE seriesId = :seriesId AND date = :date)")
    suspend fun isExcluded(seriesId: String, date: String): Boolean

    // --- Data Cleanup (for migrations) ---
    @Query("DELETE FROM plan_series")
    suspend fun deleteAllSeries()

    @Query("DELETE FROM plan_items WHERE source = 'DETERMINISTIC'")
    suspend fun deleteAllDeterministicItems()

    @Query("DELETE FROM plan_series_exceptions")
    suspend fun deleteAllExceptions()

    // --- Widget Support ---
    /**
     * Get the next pending task for widget display.
     * Returns the earliest pending task that:
     * - Is in PENDING status
     * - Is not snoozed (or snooze has expired)
     * - Has a defined start time
     * Ordered by start time ascending.
     */
    @Query("""
        SELECT * FROM plan_items
        WHERE date = :date
        AND status = 'PENDING'
        AND (snoozeUntil IS NULL OR snoozeUntil <= :currentTimeMillis)
        AND (startTimeMillis IS NOT NULL)
        ORDER BY startTimeMillis ASC
        LIMIT 1
    """)
    suspend fun getNextPendingTask(date: String, currentTimeMillis: Long): PlanItemEntity?
}
