package com.jnkim.poschedule.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * NotificationLogEntity tracks sent notifications for daily budget enforcement.
 *
 * Purpose: Ensures the app respects the 7 notifications/day limit by recording
 * every notification sent. Budget resets at midnight (new date).
 *
 * @property id Unique identifier for this log entry
 * @property itemId The plan item ID that triggered this notification
 * @property sentAt Timestamp when notification was sent (Instant.toEpochMilli())
 * @property date Date string in ISO format (e.g., "2026-01-14") for daily grouping
 * @property notificationClass Type of notification (CORE/UPDATE/RECOVERY/SOFT/SUMMARY)
 */
@Entity(tableName = "notification_log")
data class NotificationLogEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val itemId: String,

    val sentAt: Long, // Instant.toEpochMilli()

    val date: String, // LocalDate ISO format for efficient daily queries

    val notificationClass: String // CORE, UPDATE, RECOVERY, SOFT, SUMMARY
)
