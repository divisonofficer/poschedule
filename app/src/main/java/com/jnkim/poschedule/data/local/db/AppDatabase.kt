package com.jnkim.poschedule.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.jnkim.poschedule.data.local.dao.PlanDao
import com.jnkim.poschedule.data.local.dao.RoutineDao
import com.jnkim.poschedule.data.local.entity.RoutineItemEntity
import com.jnkim.poschedule.data.local.entity.RoutineLogEntity
import com.jnkim.poschedule.data.local.entity.EventEntity
import com.jnkim.poschedule.data.local.entity.NotificationStateEntity
import com.jnkim.poschedule.data.local.entity.PlanDayEntity
import com.jnkim.poschedule.data.local.entity.PlanItemEntity

@Database(
    entities = [
        RoutineItemEntity::class,
        RoutineLogEntity::class,
        EventEntity::class,
        NotificationStateEntity::class,
        PlanDayEntity::class,
        PlanItemEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun routineDao(): RoutineDao
    abstract fun planDao(): PlanDao
}
