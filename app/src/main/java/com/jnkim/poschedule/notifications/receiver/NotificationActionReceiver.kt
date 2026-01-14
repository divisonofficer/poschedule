package com.jnkim.poschedule.notifications.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.jnkim.poschedule.data.repo.PlanRepository
import com.jnkim.poschedule.notifications.NotificationConstants
import com.jnkim.poschedule.workers.WidgetUpdateWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var planRepository: PlanRepository

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val routineId = intent.getStringExtra(NotificationConstants.EXTRA_ROUTINE_ID) ?: return
        val isFromWidget = intent.getBooleanExtra(NotificationConstants.EXTRA_WIDGET_UPDATE_TRIGGER, false)

        Log.d(TAG, "Received action: $action for item: $routineId (fromWidget=$isFromWidget)")

        // Use a background scope to update the DB
        CoroutineScope(Dispatchers.IO).launch {
            when (action) {
                NotificationConstants.ACTION_DONE -> {
                    Log.d(TAG, "Marking item $routineId as DONE")
                    planRepository.updateItemStatus(routineId, "DONE")
                }

                NotificationConstants.ACTION_SKIP -> {
                    Log.d(TAG, "Marking item $routineId as SKIP")
                    planRepository.updateItemStatus(routineId, "SKIP")
                }

                NotificationConstants.ACTION_SNOOZE_15 -> {
                    val snoozeUntil = Instant.now().plus(15, ChronoUnit.MINUTES).toEpochMilli()
                    Log.d(TAG, "Snoozing item $routineId until ${Instant.ofEpochMilli(snoozeUntil)}")
                    planRepository.updateItemWithSnooze(routineId, "SNOOZED", snoozeUntil)
                }
            }

            // Trigger immediate widget update if action came from widget
            if (isFromWidget) {
                Log.d(TAG, "Triggering widget update after action")
                WidgetUpdateWorker.enqueueImmediateUpdate(context)
            }

            // Trigger a re-evaluation of the system mode if needed
            // (TodayViewModel will automatically pick up the status change via Flow)
        }
    }

    companion object {
        private const val TAG = "NotificationActionReceiver"
    }
}
