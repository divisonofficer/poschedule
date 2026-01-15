package com.jnkim.poschedule.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jnkim.poschedule.data.local.dao.LlmResponseDao
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

    /**
     * Migration from v6 to v7:
     * - Adds llm_responses table for storing LLM API responses
     * - Used for debugging JSON parsing errors and analyzing LLM output patterns
     * - Includes indices on requestedAt and parseSuccess for efficient querying
     */
    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create llm_responses table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS llm_responses (
                    id TEXT PRIMARY KEY NOT NULL,
                    requestedAt INTEGER NOT NULL,
                    userPrompt TEXT NOT NULL,
                    systemPrompt TEXT NOT NULL,
                    modelName TEXT NOT NULL,
                    rawResponse TEXT NOT NULL,
                    parseSuccess INTEGER NOT NULL,
                    errorMessage TEXT,
                    responseTimeMs INTEGER NOT NULL
                )
            """)

            // Create index on requestedAt for faster time-based queries
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_llm_responses_requestedAt
                ON llm_responses(requestedAt)
            """)

            // Create index on parseSuccess for filtering failed parses
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_llm_responses_parseSuccess
                ON llm_responses(parseSuccess)
            """)
        }
    }

    /**
     * Migration from v7 to v8:
     * - No schema changes, only version bump to match entity annotations
     * - Indices on llm_responses were already created in v6->v7 migration
     * - This migration exists to align version numbers after adding @Index annotations to LlmResponseEntity
     */
    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // No-op migration: indices already exist from v6->v7
            // This migration only exists to bump the version number
        }
    }

    /**
     * Migration from v8 to v9:
     * - Adds iconEmoji field to plan_series and plan_items tables
     * - Enables LLM-generated emoji icons for tasks (e.g., 🎹 for piano lessons, 🏓 for table tennis)
     */
    private val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add iconEmoji to plan_series
            database.execSQL("ALTER TABLE plan_series ADD COLUMN iconEmoji TEXT")

            // Add iconEmoji to plan_items
            database.execSQL("ALTER TABLE plan_items ADD COLUMN iconEmoji TEXT")
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
        .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
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

    @Provides
    fun provideLlmResponseDao(database: AppDatabase): LlmResponseDao {
        return database.llmResponseDao()
    }
}
