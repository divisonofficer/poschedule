package com.jnkim.poschedule.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jnkim.poschedule.data.local.entity.NotificationLogEntity

/**
 * DAO for NotificationLogEntity - manages notification budget tracking.
 *
 * Key operations:
 * - Insert: Record each sent notification
 * - Count: Query daily budget usage
 * - Cleanup: Remove old logs (optional maintenance)
 */
@Dao
interface NotificationLogDao {

    /**
     * Insert a notification log entry.
     * Used immediately after showing a notification to track budget.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: NotificationLogEntity)

    /**
     * Count notifications sent today for budget enforcement.
     *
     * @param dateStr Date in ISO format (e.g., "2026-01-14")
     * @return Number of notifications sent on this date
     */
    @Query("SELECT COUNT(*) FROM notification_log WHERE date = :dateStr")
    suspend fun countSentToday(dateStr: String): Int

    /**
     * Get all notification logs for a specific date (debugging/analytics).
     *
     * @param dateStr Date in ISO format
     * @return List of notification logs for the date
     */
    @Query("SELECT * FROM notification_log WHERE date = :dateStr ORDER BY sentAt ASC")
    suspend fun getLogsForDate(dateStr: String): List<NotificationLogEntity>

    /**
     * Delete notification logs older than a certain date.
     * Optional cleanup to prevent unbounded growth.
     *
     * @param beforeDate ISO date string; logs before this date will be deleted
     */
    @Query("DELETE FROM notification_log WHERE date < :beforeDate")
    suspend fun deleteLogsBefore(beforeDate: String)

    /**
     * Get total notification count across all dates (analytics).
     */
    @Query("SELECT COUNT(*) FROM notification_log")
    suspend fun getTotalNotificationCount(): Int
}
