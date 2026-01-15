package com.jnkim.poschedule.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.jnkim.poschedule.data.local.dao.LlmResponseDao
import com.jnkim.poschedule.data.local.dao.NotificationLogDao
import com.jnkim.poschedule.data.local.dao.PlanDao
import com.jnkim.poschedule.data.local.dao.RoutineDao
import com.jnkim.poschedule.data.local.entity.*

@Database(
    entities = [
        RoutineItemEntity::class,
        RoutineLogEntity::class,
        EventEntity::class,
        NotificationStateEntity::class,
        PlanDayEntity::class,
        PlanItemEntity::class,
        PlanSeriesEntity::class,
        PlanSeriesExceptionEntity::class,
        NotificationLogEntity::class,
        LlmResponseEntity::class
    ],
    version = 9, // v9: Add iconEmoji field to plan_series and plan_items for LLM-generated icons
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun routineDao(): RoutineDao
    abstract fun planDao(): PlanDao
    abstract fun notificationLogDao(): NotificationLogDao
    abstract fun llmResponseDao(): LlmResponseDao
}
