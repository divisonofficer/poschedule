package com.jnkim.poschedule.data.local.dao

import androidx.room.*
import com.jnkim.poschedule.data.local.entity.PlanDayEntity
import com.jnkim.poschedule.data.local.entity.PlanItemEntity
import com.jnkim.poschedule.data.local.entity.PlanItemSource
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {
    @Query("SELECT * FROM plan_days WHERE date = :date")
    suspend fun getPlanDay(date: String): PlanDayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanDay(planDay: PlanDayEntity)

    @Query("SELECT * FROM plan_items WHERE date = :date ORDER BY createdAt ASC")
    fun getPlanItems(date: String): Flow<List<PlanItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanItem(planItem: PlanItemEntity)

    @Update
    suspend fun updatePlanItem(planItem: PlanItemEntity)

    @Query("DELETE FROM plan_items WHERE id = :id")
    suspend fun deletePlanItem(id: String)

    @Query("UPDATE plan_items SET status = :status WHERE id = :id")
    suspend fun updatePlanItemStatus(id: String, status: String)

    @Query("DELETE FROM plan_items WHERE date = :date AND source = :source")
    suspend fun deleteItemsBySource(date: String, source: PlanItemSource)
}
