package com.jnkim.poschedule.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * PlanLocationEntity - Stores location/place information for plans
 *
 * Design: 1:1 relationship with PlanItemEntity
 * Cascade delete: When plan is deleted, location info is auto-deleted
 *
 * Phase 1.5: Separated from PlanItemEntity for scalability
 */
@Entity(
    tableName = "plan_locations",
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
data class PlanLocationEntity(
    @PrimaryKey val planId: String,       // FK to plan_items.id
    val locationText: String,              // Human-readable location (e.g., "Room 301", "Starbucks Gangnam")
    val mapQuery: String? = null,          // Google Maps search query or geo URI (e.g., "geo:37.5665,126.9780")
    val address: String? = null,           // Full address (optional)
    val latitude: Double? = null,          // GPS coordinates (optional)
    val longitude: Double? = null
)
