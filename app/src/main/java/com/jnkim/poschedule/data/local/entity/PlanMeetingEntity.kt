package com.jnkim.poschedule.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Meeting type enum - automatically detected from URL patterns
 */
enum class MeetingType {
    ZOOM,       // zoom.us/*
    TEAMS,      // teams.microsoft.com/*
    WEBEX,      // webex.com/*
    GOOGLE_MEET, // meet.google.com/*
    OTHER       // Generic URL
}

/**
 * PlanMeetingEntity - Stores online meeting information for plans
 *
 * Design: 1:1 relationship with PlanItemEntity
 * Cascade delete: When plan is deleted, meeting info is auto-deleted
 *
 * Phase 1.5: Separated from PlanItemEntity for scalability
 */
@Entity(
    tableName = "plan_meetings",
    foreignKeys = [
        ForeignKey(
            entity = PlanItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["planId"], unique = true)] // 1:1 relationship
)
data class PlanMeetingEntity(
    @PrimaryKey val planId: String,     // FK to plan_items.id
    val joinUrl: String,                 // Primary meeting URL (Zoom/Teams/Webex/Meet)
    val meetingType: MeetingType,        // Auto-detected from joinUrl
    val webUrl: String? = null,          // Related web page (optional)
    val meetingId: String? = null        // Extracted meeting ID (e.g., Zoom: 123-456-789)
)
