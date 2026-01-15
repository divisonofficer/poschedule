package com.jnkim.poschedule.notifications

object NotificationConstants {
    const val CHANNEL_CORE_ROUTINES = "core_routines"
    const val CHANNEL_ADAPTIVE_UPDATES = "adaptive_updates"
    const val CHANNEL_RECOVERY_NUDGES = "recovery_nudges"
    const val CHANNEL_OPTIONAL_HABITS = "optional_habits"
    const val CHANNEL_SUMMARIES = "summaries"
    const val CHANNEL_STATUS_COMPANION = "poschedule_status_companion"

    const val ACTION_DONE = "com.jnkim.poschedule.ACTION_DONE"
    const val ACTION_SKIP = "com.jnkim.poschedule.ACTION_SKIP"
    const val ACTION_SNOOZE_15 = "com.jnkim.poschedule.ACTION_SNOOZE_15"

    const val ACTION_COMPANION_SNOOZE = "com.jnkim.poschedule.ACTION_COMPANION_SNOOZE"
    const val ACTION_COMPANION_DONE = "com.jnkim.poschedule.ACTION_COMPANION_DONE"
    const val ACTION_COMPANION_OPEN = "com.jnkim.poschedule.ACTION_COMPANION_OPEN"

    const val NOTIFICATION_ID_COMPANION = 1001

    const val EXTRA_ROUTINE_ID = "extra_routine_id"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

    // Widget-specific constants
    const val EXTRA_WIDGET_UPDATE_TRIGGER = "extra_widget_update_trigger"
    const val REQUEST_CODE_WIDGET_OFFSET = 1000  // Widget actions: routineId.hashCode() + 1000
}
