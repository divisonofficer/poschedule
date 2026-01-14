package com.jnkim.poschedule

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.appcompat.app.AlertDialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.jnkim.poschedule.data.repo.SettingsRepository
import com.jnkim.poschedule.notifications.NotificationHelper
import com.jnkim.poschedule.ui.nav.PoscheduleNavHost
import com.jnkim.poschedule.ui.nav.Route
import com.jnkim.poschedule.ui.theme.PoscheduleTheme
import com.jnkim.poschedule.ui.viewmodel.AuthViewModel
import com.jnkim.poschedule.utils.DeviceCompatibilityHelper
import com.jnkim.poschedule.workers.DailyPlanWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Permission result handled globally or per-screen
    }

    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach { entry ->
            val permission = entry.key
            val isGranted = entry.value
            // Log or handle individual permission results if needed
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        notificationHelper.createNotificationChannels()

        // Apply language setting
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsRepository.settingsFlow.collect { settings ->
                    val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(settings.language)
                    if (AppCompatDelegate.getApplicationLocales() != appLocale) {
                        AppCompatDelegate.setApplicationLocales(appLocale)
                    }
                }
            }
        }

        // Request necessary permissions at startup
        requestAppPermissions()

        // Request battery optimization exemption for Xiaomi/Samsung devices
        requestBatteryOptimizationIfNeeded()

        // Trigger Daily Plan generation for demo/MVP purposes
        val workRequest = OneTimeWorkRequestBuilder<DailyPlanWorker>().build()
        WorkManager.getInstance(this).enqueue(workRequest)

        enableEdgeToEdge()
        setContent {
            // Collect settings for theme configuration
            val settings by settingsRepository.settingsFlow.collectAsState(initial = null)

            PoscheduleTheme(
                themeMode = settings?.themeMode ?: "TIME_ADAPTIVE",
                weatherEffectsEnabled = settings?.weatherEffectsEnabled ?: true,
                manualWeatherState = settings?.manualWeatherState ?: "CLEAR"
            ) {
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

    /**
     * Request necessary permissions for the app to function properly.
     *
     * - CAMERA: For TidySnap feature
     * - POST_NOTIFICATIONS: For notification delivery (Android 13+)
     */
    private fun requestAppPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // Camera permission (all versions)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }

        // Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Request all needed permissions at once
        if (permissionsToRequest.isNotEmpty()) {
            requestMultiplePermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    /**
     * Request battery optimization exemption for devices known to have aggressive optimization.
     *
     * Shows a dialog explaining why this is needed and allows user to exempt the app.
     * Only shown on Xiaomi/Samsung devices when battery optimization is enabled.
     */
    private fun requestBatteryOptimizationIfNeeded() {
        // Only request on aggressive battery optimization devices
        if (!DeviceCompatibilityHelper.shouldRequestBatteryOptimization(this)) {
            return
        }

        val deviceName = when {
            DeviceCompatibilityHelper.isXiaomiDevice() -> "Xiaomi"
            DeviceCompatibilityHelper.isSamsungDevice() -> "Samsung"
            else -> return
        }

        AlertDialog.Builder(this)
            .setTitle("배터리 최적화 해제")
            .setMessage(
                "$deviceName 기기에서 알림이 제대로 작동하려면 배터리 최적화를 해제해야 합니다.\n\n" +
                "다음 화면에서 'Poschedule'을 찾아 '제한 없음' 또는 '최적화 안 함'을 선택해주세요.\n\n" +
                "이 설정은 앱이 백그라운드에서 알림을 보낼 수 있도록 합니다."
            )
            .setPositiveButton("설정하기") { _, _ ->
                try {
                    val intent = DeviceCompatibilityHelper.getRequestBatteryOptimizationIntent(this)
                    if (intent != null) {
                        startActivity(intent)
                    }
                } catch (e: Exception) {
                    // If request intent fails, fall back to settings
                    val settingsIntent = DeviceCompatibilityHelper.getBatteryOptimizationSettingsIntent(this)
                    if (settingsIntent != null) {
                        startActivity(settingsIntent)
                    }
                }
            }
            .setNegativeButton("나중에") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }
}
