package com.jnkim.poschedule.domain.model

import android.content.Context
import com.jnkim.poschedule.R

/**
 * Constants and helper functions for Mode display.
 *
 * Provides emoji badges and localized labels for each mode.
 */
object ModeConstants {
    /**
     * Returns the emoji badge for a given mode.
     *
     * @param mode The mode to get the badge for
     * @return Emoji string representing the mode
     */
    fun getModeBadge(mode: Mode): String = when (mode) {
        Mode.NORMAL -> "💙"
        Mode.RECOVERY -> "🌊"
        Mode.LOW_MOOD -> "⚡"
        Mode.BUSY -> "🎯"
    }

    /**
     * Returns the localized mode label.
     *
     * @param mode The mode to get the label for
     * @param context Android context for accessing string resources
     * @return Localized mode label (e.g., "회복 모드" or "Recovery Mode")
     */
    fun getModeLabel(mode: Mode, context: Context): String = when (mode) {
        Mode.NORMAL -> context.getString(R.string.mode_normal)
        Mode.RECOVERY -> context.getString(R.string.mode_recovery)
        Mode.LOW_MOOD -> context.getString(R.string.mode_low_mood)
        Mode.BUSY -> context.getString(R.string.mode_busy)
    }

    /**
     * Returns the mode badge and label combined.
     *
     * @param mode The mode to format
     * @param context Android context for accessing string resources
     * @return Formatted string like "🌊 회복 모드"
     */
    fun getFormattedMode(mode: Mode, context: Context): String {
        return "${getModeBadge(mode)} ${getModeLabel(mode, context)}"
    }
}
