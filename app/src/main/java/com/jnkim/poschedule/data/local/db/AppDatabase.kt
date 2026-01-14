package com.jnkim.poschedule.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
        NotificationLogEntity::class
    ],
    version = 6, // v6: Clear invalid series due to anchor offset bug fix
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun routineDao(): RoutineDao
    abstract fun planDao(): PlanDao
    abstract fun notificationLogDao(): NotificationLogDao
}
