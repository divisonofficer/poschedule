package com.jnkim.poschedule.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routine_logs")
data class RoutineLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineItemId: String,
    val action: String, // DONE, SKIP, SNOOZE
    val timestamp: Long = System.currentTimeMillis()
)
