package com.jnkim.poschedule.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jnkim.poschedule.data.local.dao.LlmResponseDao
import com.jnkim.poschedule.data.local.dao.NotificationLogDao
import com.jnkim.poschedule.data.local.dao.PlanDao
import com.jnkim.poschedule.data.local.dao.PlanRichDataDao
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

    /**
     * Migration from v9 to v10:
     * - Phase 1: Rich Plan Detail View
     * - Adds 9 nullable calendar-grade fields to plan_items table
     * - Supports meeting URLs, locations, contacts, and source evidence
     * - All fields nullable for backward compatibility
     */
    private val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Meeting/Call fields
            database.execSQL("ALTER TABLE plan_items ADD COLUMN joinUrl TEXT")
            database.execSQL("ALTER TABLE plan_items ADD COLUMN webUrl TEXT")

            // Location fields
            database.execSQL("ALTER TABLE plan_items ADD COLUMN locationText TEXT")
            database.execSQL("ALTER TABLE plan_items ADD COLUMN mapQuery TEXT")

            // Deep link / App fields
            database.execSQL("ALTER TABLE plan_items ADD COLUMN deepLinkUrl TEXT")
            database.execSQL("ALTER TABLE plan_items ADD COLUMN packageName TEXT")

            // Contact fields
            database.execSQL("ALTER TABLE plan_items ADD COLUMN contactName TEXT")
            database.execSQL("ALTER TABLE plan_items ADD COLUMN contactEmail TEXT")

            // Source evidence
            database.execSQL("ALTER TABLE plan_items ADD COLUMN sourceSnippet TEXT")
        }
    }

    /**
     * Migration from v10 to v11:
     * - Phase 1.5: Scalable Rich Data Architecture
     * - Creates separate normalized tables for rich plan metadata
     * - Deprecates v10 fields in plan_items (kept for backward compatibility)
     * - Enables unlimited extensibility for future rich data types
     *
     * New tables:
     * - plan_meetings: Meeting URLs with auto-detected type (Zoom/Teams/Webex/Meet)
     * - plan_locations: Location data with GPS coordinates
     * - plan_contacts: Contact information (1:N - multiple contacts per plan)
     * - plan_notes: User notes, tags, color codes, and AI source evidence
     */
    private val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create plan_meetings table (1:1 with plan_items)
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS plan_meetings (
                    planId TEXT PRIMARY KEY NOT NULL,
                    joinUrl TEXT NOT NULL,
                    meetingType TEXT NOT NULL,
                    webUrl TEXT,
                    meetingId TEXT,
                    FOREIGN KEY(planId) REFERENCES plan_items(id) ON DELETE CASCADE
                )
            """)
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_plan_meetings_planId ON plan_meetings(planId)")

            // Create plan_locations table (1:1 with plan_items)
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS plan_locations (
                    planId TEXT PRIMARY KEY NOT NULL,
                    locationText TEXT NOT NULL,
                    mapQuery TEXT,
                    address TEXT,
                    latitude REAL,
                    longitude REAL,
                    FOREIGN KEY(planId) REFERENCES plan_items(id) ON DELETE CASCADE
                )
            """)
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_plan_locations_planId ON plan_locations(planId)")

            // Create plan_contacts table (1:N with plan_items)
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS plan_contacts (
                    id TEXT PRIMARY KEY NOT NULL,
                    planId TEXT NOT NULL,
                    name TEXT,
                    email TEXT,
                    phoneNumber TEXT,
                    role TEXT,
                    avatarUrl TEXT,
                    FOREIGN KEY(planId) REFERENCES plan_items(id) ON DELETE CASCADE
                )
            """)
            database.execSQL("CREATE INDEX IF NOT EXISTS index_plan_contacts_planId ON plan_contacts(planId)")

            // Create plan_notes table (1:1 with plan_items)
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS plan_notes (
                    planId TEXT PRIMARY KEY NOT NULL,
                    notes TEXT,
                    tags TEXT,
                    sourceSnippet TEXT,
                    colorTag TEXT,
                    FOREIGN KEY(planId) REFERENCES plan_items(id) ON DELETE CASCADE
                )
            """)
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_plan_notes_planId ON plan_notes(planId)")
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
        .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
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

    @Provides
    fun providePlanRichDataDao(database: AppDatabase): PlanRichDataDao {
        return database.planRichDataDao()
    }
}
