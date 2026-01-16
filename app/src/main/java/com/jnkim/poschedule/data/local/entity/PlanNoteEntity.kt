package com.jnkim.poschedule.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * PlanNoteEntity - Stores user notes, tags, and AI source evidence
 *
 * Design: 1:1 relationship with PlanItemEntity
 * Cascade delete: When plan is deleted, notes are auto-deleted
 *
 * Phase 1.5: Separated from PlanItemEntity for scalability
 *
 * Fields:
 * - notes: User free-form memo (max 5000 chars)
 * - tags: Comma-separated tags for categorization (e.g., "work,urgent,meeting")
 * - sourceSnippet: AI transparency - what the LLM analyzed to create this plan
 */
@Entity(
    tableName = "plan_notes",
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
data class PlanNoteEntity(
    @PrimaryKey val planId: String,        // FK to plan_items.id
    val notes: String? = null,             // User free-form memo (max 5000 chars)
    val tags: String? = null,              // Comma-separated tags (e.g., "work,urgent")
    val sourceSnippet: String? = null,     // AI source evidence (first 1000 chars of analyzed content)
    val colorTag: String? = null           // Custom color code for visual categorization (e.g., "#FF5733")
)
