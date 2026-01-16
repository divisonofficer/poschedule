package com.jnkim.poschedule.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * PlanContactEntity - Stores contact information for plans
 *
 * Design: 1:N relationship with PlanItemEntity (multiple contacts per plan)
 * Cascade delete: When plan is deleted, all contacts are auto-deleted
 *
 * Phase 1.5: Separated from PlanItemEntity for scalability
 *
 * Use cases:
 * - Meeting attendees
 * - Person to meet with
 * - Emergency contact
 */
@Entity(
    tableName = "plan_contacts",
    foreignKeys = [
        ForeignKey(
            entity = PlanItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["planId"])] // 1:N relationship - no unique constraint
)
data class PlanContactEntity(
    @PrimaryKey val id: String,            // Unique contact ID (UUID)
    val planId: String,                    // FK to plan_items.id
    val name: String? = null,              // Contact name
    val email: String? = null,             // Email address
    val phoneNumber: String? = null,       // Phone number or KakaoTalk ID
    val role: String? = null,              // e.g., "Organizer", "Attendee", "Optional"
    val avatarUrl: String? = null          // Profile image URL (future)
)
