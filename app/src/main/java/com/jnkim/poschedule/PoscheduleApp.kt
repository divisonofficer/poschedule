package com.jnkim.poschedule

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.jnkim.poschedule.workers.DailyPlanWorker
import com.jnkim.poschedule.workers.NotificationWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class PoscheduleApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun getWorkManagerConfiguration(): Configuration =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        schedulePeriodicWorkers()
    }

    private fun schedulePeriodicWorkers() {
        val workManager = WorkManager.getInstance(this)

        // 1. DailyPlanWorker: Keep the rolling window expanded.
        // Runs every 4 hours to handle day transitions and background updates.
        val planRequest = PeriodicWorkRequestBuilder<DailyPlanWorker>(4, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            "DailyPlanExpansion",
            ExistingPeriodicWorkPolicy.KEEP,
            planRequest
        )

        // 2. NotificationWorker: G4 Calibration (±5m window).
        // Must run frequently enough to catch the start of windows.
        // Note: PeriodicWork minimum interval is 15 minutes.
        val notificationRequest = PeriodicWorkRequestBuilder<NotificationWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
            .build()

        workManager.enqueueUniquePeriodicWork(
            "CalibratedNotifications",
            ExistingPeriodicWorkPolicy.KEEP,
            notificationRequest
        )
    }
}
