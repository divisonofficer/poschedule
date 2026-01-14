package com.jnkim.poschedule.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jnkim.poschedule.data.local.dao.NotificationLogDao
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

    /**
     * Migration from v4 to v5:
     * - Adds snoozeUntil column to plan_items table
     * - Creates notification_log table for budget tracking
     */
    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add snoozeUntil field to plan_items
            database.execSQL("ALTER TABLE plan_items ADD COLUMN snoozeUntil INTEGER")

            // Create notification_log table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS notification_log (
                    id TEXT PRIMARY KEY NOT NULL,
                    itemId TEXT NOT NULL,
                    sentAt INTEGER NOT NULL,
                    date TEXT NOT NULL,
                    notificationClass TEXT NOT NULL
                )
            """)
        }
    }

    /**
     * Migration from v5 to v6:
     * - Clears all existing plan series and deterministic items
     * - Required due to incorrect anchor offset calculation in previous versions
     * - Users must recreate their routines with the fixed UI
     */
    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Clear all series data (users will recreate with fixed offset logic)
            database.execSQL("DELETE FROM plan_series")
            database.execSQL("DELETE FROM plan_items WHERE source = 'DETERMINISTIC'")
            database.execSQL("DELETE FROM plan_series_exceptions")
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "poschedule_db"
        )
        .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
        .fallbackToDestructiveMigration() // Keep as fallback for dev
        .build()
    }

    @Provides
    fun provideRoutineDao(database: AppDatabase): RoutineDao {
        return database.routineDao()
    }

    @Provides
    fun providePlanDao(database: AppDatabase): PlanDao {
        return database.planDao()
    }

    @Provides
    fun provideNotificationLogDao(database: AppDatabase): NotificationLogDao {
        return database.notificationLogDao()
    }
}
