package com.jnkim.poschedule.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jnkim.poschedule.data.local.entity.RoutineItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routine_items ORDER BY windowStartMillis ASC")
    fun getAllRoutines(): Flow<List<RoutineItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineItemEntity)

    @Query("UPDATE routine_items SET completed = :completed WHERE id = :id")
    suspend fun updateCompletionStatus(id: String, completed: Boolean)

    @Query("DELETE FROM routine_items")
    suspend fun deleteAllRoutines()
}
