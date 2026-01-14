package com.jnkim.poschedule.notifications.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jnkim.poschedule.data.repo.PlanRepository
import com.jnkim.poschedule.notifications.NotificationConstants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var planRepository: PlanRepository

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val routineId = intent.getStringExtra(NotificationConstants.EXTRA_ROUTINE_ID) ?: return

        // Use a background scope to update the DB
        CoroutineScope(Dispatchers.IO).launch {
            when (action) {
                NotificationConstants.ACTION_DONE -> {
                    planRepository.updateItemStatus(routineId, "DONE")
                }
                NotificationConstants.ACTION_SNOOZE_15 -> {
                    // In a real implementation, we'd update a 'snoozeCount' and move the window
                    planRepository.updateItemStatus(routineId, "SNOOZED")
                }
            }
            
            // Trigger a re-evaluation of the system mode if needed
            // (TodayViewModel will automatically pick up the status change via Flow)
        }
    }
}
