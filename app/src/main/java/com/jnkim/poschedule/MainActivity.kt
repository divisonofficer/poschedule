package com.jnkim.poschedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.jnkim.poschedule.notifications.NotificationHelper
import com.jnkim.poschedule.ui.nav.PoscheduleNavHost
import com.jnkim.poschedule.ui.nav.Route
import com.jnkim.poschedule.ui.theme.PoscheduleTheme
import com.jnkim.poschedule.ui.viewmodel.AuthViewModel
import com.jnkim.poschedule.workers.DailyPlanWorker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        notificationHelper.createNotificationChannels()

        // Trigger Daily Plan generation for demo/MVP purposes
        val workRequest = OneTimeWorkRequestBuilder<DailyPlanWorker>().build()
        WorkManager.getInstance(this).enqueue(workRequest)

        enableEdgeToEdge()
        setContent {
            PoscheduleTheme {
                val authViewModel: AuthViewModel = hiltViewModel()
                val startDestination = if (authViewModel.isUserLoggedIn()) {
                    Route.Today.path
                } else {
                    Route.Login.path
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PoscheduleNavHost(startDestination = startDestination)
                }
            }
        }
    }
}
