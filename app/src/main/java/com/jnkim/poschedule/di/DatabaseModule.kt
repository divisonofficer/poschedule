package com.jnkim.poschedule.di

import android.content.Context
import androidx.room.Room
import com.jnkim.poschedule.data.local.dao.PlanDao
import com.jnkim.poschedule.data.local.dao.RoutineDao
import com.jnkim.poschedule.data.local.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "poschedule_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideRoutineDao(database: AppDatabase): RoutineDao {
        return database.routineDao()
    }

    @Provides
    fun providePlanDao(database: AppDatabase): PlanDao {
        return database.planDao()
    }
}
