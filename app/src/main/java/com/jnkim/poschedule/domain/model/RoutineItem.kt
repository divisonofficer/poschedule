package com.jnkim.poschedule.domain.model

import java.time.Instant

data class RoutineItem(
    val id: String,
    val type: RoutineType,
    val isCore: Boolean,
    val windowStart: Instant,
    val windowEnd: Instant,
    val completed: Boolean = false
)
