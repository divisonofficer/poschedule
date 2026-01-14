package com.jnkim.poschedule.data.local.db

import androidx.room.TypeConverter
import com.jnkim.poschedule.domain.model.RoutineType

class Converters {
    @TypeConverter
    fun fromRoutineType(value: RoutineType): String {
        return value.name
    }

    @TypeConverter
    fun toRoutineType(value: String): RoutineType {
        return RoutineType.valueOf(value)
    }
}
