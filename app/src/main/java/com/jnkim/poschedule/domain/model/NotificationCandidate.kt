package com.jnkim.poschedule.domain.model

import java.time.Instant

data class NotificationCandidate(
    val id: String,
    val clazz: NotificationClass,
    val urgency: Double,
    val importance: Double,
    val actionability: Double,
    val userCost: Double,
    val deadline: Instant,
    val cooldownKey: String? = null
)
