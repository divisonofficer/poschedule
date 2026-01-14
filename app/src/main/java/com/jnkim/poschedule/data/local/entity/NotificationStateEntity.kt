package com.jnkim.poschedule.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_states")
data class NotificationStateEntity(
    @PrimaryKey val key: String,
    val lastSentAtMillis: Long,
    val cooldownUntilMillis: Long,
    val ignoreStats: Boolean = false
)
