package com.jnkim.poschedule.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jnkim.poschedule.domain.model.Mode

@Entity(tableName = "plan_days")
data class PlanDayEntity(
    @PrimaryKey val date: String, // yyyy-MM-dd
    val mode: Mode,
    val generatedAtMillis: Long,
    val sourceVersion: String
)
