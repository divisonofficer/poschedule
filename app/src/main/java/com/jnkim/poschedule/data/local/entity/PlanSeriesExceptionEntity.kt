package com.jnkim.poschedule.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "plan_series_exceptions",
    foreignKeys = [
        ForeignKey(
            entity = PlanSeriesEntity::class,
            parentColumns = ["id"],
            childColumns = ["seriesId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("seriesId")]
)
data class PlanSeriesExceptionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val seriesId: String,
    val date: String // yyyy-MM-dd
)
