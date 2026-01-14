package com.jnkim.poschedule.domain.engine

import com.jnkim.poschedule.domain.model.Mode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModeStateMachine @Inject constructor() {
    
    /**
     * Calculates the system mode based on user behavior and environment.
     * Implementing the logic from PLAN.md and PLAN_P2.md.
     */
    fun calculateMode(
        adherenceRate: Float,        // Percentage of tasks completed
        consecutiveSnoozes: Int,     // How many times the current session was delayed
        missedCoreRoutines: Int,     // Number of critical tasks past their window
        isCurrentlyBusy: Boolean     // Based on external calendar events
    ): Mode {
        
        // 1. Critical Failure: RECOVERY
        // If user misses 2+ core routines (Meds/Meals), prioritize recovery.
        if (missedCoreRoutines >= 2 || adherenceRate < 0.4f) {
            return Mode.RECOVERY
        }

        // 2. High Resistance: LOW_MOOD
        // If user snoozes frequently, suggest a gentler approach.
        if (consecutiveSnoozes >= 3) {
            return Mode.LOW_MOOD
        }

        // 3. Environmental Load: BUSY
        // If the user's schedule is packed (detected via calendar integration).
        if (isCurrentlyBusy) {
            return Mode.BUSY
        }
        
        // 4. Default: NORMAL
        return Mode.NORMAL
    }
}
