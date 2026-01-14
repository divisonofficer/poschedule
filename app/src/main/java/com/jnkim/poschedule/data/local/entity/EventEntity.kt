package com.jnkim.poschedule.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val priority: Int,
    val title: String
)
