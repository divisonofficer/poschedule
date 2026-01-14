package com.jnkim.poschedule.data.repo

import com.jnkim.poschedule.data.local.dao.RoutineDao
import com.jnkim.poschedule.data.local.entity.RoutineItemEntity
import com.jnkim.poschedule.domain.model.RoutineItem
import com.jnkim.poschedule.domain.model.RoutineType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoutineRepositoryImpl @Inject constructor(
    private val routineDao: RoutineDao
) {
    fun getAllRoutines(): Flow<List<RoutineItem>> {
        return routineDao.getAllRoutines().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun insertRoutine(routine: RoutineItem) {
        routineDao.insertRoutine(routine.toEntity())
    }

    suspend fun updateCompletion(id: String, completed: Boolean) {
        routineDao.updateCompletionStatus(id, completed)
    }

    private fun RoutineItemEntity.toDomain() = RoutineItem(
        id = id,
        type = type,
        isCore = isCore,
        windowStart = Instant.ofEpochMilli(windowStartMillis),
        windowEnd = Instant.ofEpochMilli(windowEndMillis),
        completed = completed
    )

    private fun RoutineItem.toEntity() = RoutineItemEntity(
        id = id,
        type = type,
        isCore = isCore,
        windowStartMillis = windowStart.toEpochMilli(),
        windowEndMillis = windowEnd.toEpochMilli(),
        completed = completed
    )
}
