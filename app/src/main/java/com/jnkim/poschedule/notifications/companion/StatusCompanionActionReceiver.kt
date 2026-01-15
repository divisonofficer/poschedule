package com.jnkim.poschedule.notifications.companion

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.jnkim.poschedule.MainActivity
import com.jnkim.poschedule.data.repo.PlanRepository
import com.jnkim.poschedule.notifications.NotificationConstants
import com.jnkim.poschedule.workers.StatusCompanionRefreshWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Handles quick actions from the Status Companion notification.
 *
 * Actions:
 * - ACTION_COMPANION_SNOOZE: Snooze the next plan for 15 minutes
 * - ACTION_COMPANION_DONE: Mark the next plan as DONE
 * - ACTION_COMPANION_OPEN: Open MainActivity with Today tab
 *
 * All actions trigger an immediate notification refresh.
 */
@AndroidEntryPoint
class StatusCompanionActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var planRepository: PlanRepository

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        Log.d(TAG, "Received companion action: $action")

        when (action) {
            NotificationConstants.ACTION_COMPANION_SNOOZE -> {
                handleSnooze(context, intent)
            }

            NotificationConstants.ACTION_COMPANION_DONE -> {
                handleDone(context, intent)
            }

            NotificationConstants.ACTION_COMPANION_OPEN -> {
                handleOpen(context)
            }
        }
    }

    /**
     * Handles snooze action - postpones the plan for 15 minutes.
     */
    private fun handleSnooze(context: Context, intent: Intent) {
        val planId = intent.getLongExtra("plan_id", -1L)
        if (planId == -1L) {
            Log.w(TAG, "Snooze action missing plan_id")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snoozeUntil = Instant.now().plus(15, ChronoUnit.MINUTES).toEpochMilli()
                val planIdString = planId.toString()

                Log.d(TAG, "Snoozing plan $planId until ${Instant.ofEpochMilli(snoozeUntil)}")
                planRepository.updateItemWithSnooze(planIdString, "SNOOZED", snoozeUntil)

                // Trigger immediate notification refresh
                triggerRefresh(context)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to snooze plan $planId", e)
            }
        }
    }

    /**
     * Handles done action - marks the plan as DONE.
     */
    private fun handleDone(context: Context, intent: Intent) {
        val planId = intent.getLongExtra("plan_id", -1L)
        if (planId == -1L) {
            Log.w(TAG, "Done action missing plan_id")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val planIdString = planId.toString()

                Log.d(TAG, "Marking plan $planId as DONE")
                planRepository.updateItemStatus(planIdString, "DONE")

                // Trigger immediate notification refresh
                triggerRefresh(context)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark plan $planId as DONE", e)
            }
        }
    }

    /**
     * Handles open action - launches MainActivity with Today tab.
     */
    private fun handleOpen(context: Context) {
        try {
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            Log.d(TAG, "Launching MainActivity")
            context.startActivity(launchIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open MainActivity", e)
        }
    }

    /**
     * Triggers an immediate refresh of the companion notification.
     */
    private fun triggerRefresh(context: Context) {
        val refreshWork = OneTimeWorkRequestBuilder<StatusCompanionRefreshWorker>().build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "refresh_status_companion",
            ExistingWorkPolicy.REPLACE,
            refreshWork
        )

        Log.d(TAG, "Triggered companion notification refresh")
    }

    companion object {
        private const val TAG = "StatusCompanionActionRx"
    }
}
