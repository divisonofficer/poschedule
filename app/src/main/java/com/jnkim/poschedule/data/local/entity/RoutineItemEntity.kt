package com.jnkim.poschedule.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jnkim.poschedule.domain.model.RoutineType

@Entity(tableName = "routine_items")
data class RoutineItemEntity(
    @PrimaryKey val id: String,
    val type: RoutineType,
    val isCore: Boolean,
    val windowStartMillis: Long,
    val windowEndMillis: Long,
    val completed: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)
